package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.*
import de.geeksfactory.opacclient.utils.get
import de.geeksfactory.opacclient.utils.html
import de.geeksfactory.opacclient.utils.text
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * OpacApi implementation for the BIBLIS LISSY OPAC.
 */
open class Lissy : OkHttpBaseApi() {
    protected lateinit var baseurl: String
    protected val ENCODING = "ISO-8859-1"

    override fun init(library: Library, factory: HttpClientFactory, debug: Boolean) {
        super.init(library, factory, debug)
        baseurl = library.data.getString("baseurl")
    }

    override fun cleanUrl(myURL: String?): String? {
        // LISSY does not like us messing with its URLs
        return myURL
    }

    private fun initAnonymousSession(): Document {
        val frameset = httpGet("$baseurl/lissy/lissy.ly?pg=login&bnr=guest", ENCODING).html
        frameset.setBaseUri("$baseurl/lissy/lissy.ly?pg=login&bnr=guest")
        val topframeUrl = frameset.select("frame")[0].absUrl("src")
        val topframe = httpGet(topframeUrl, ENCODING).html
        topframe.setBaseUri(topframeUrl)
        val pageframeUrl = topframe.select("frame")[1].absUrl("src")
        val pageframe = httpGet(pageframeUrl, ENCODING).html
        pageframe.setBaseUri(pageframeUrl)
        return pageframe
    }

    override fun parseSearchFields(): List<SearchField> {
        val doc = initAnonymousSession()

        return doc.select("form[name=inputform] select[name=Anf1_Attribut]").first().select("option").map { o ->
            TextSearchField().apply {
                id = o["value"]
                displayName = o.text.trim()
            }
        }
    }

    private fun searchValueCheckLength(value: String) {
        // Ported from LISSY's JS code
        if (value.length == 0) return
        if (value.length <= 2) {
            throw OpacApi.OpacErrorException("Ein Suchbegriff muss mindestens 3 Zeichen lang sein!\nSuchterm: " + " " + value);
        }
        /*
        Bei Eingabe mehrerer Sucheinträge (durch Leerzeichen getrennt)
        Trunkierungen nach weniger als 3 Zeichen zulassen.
        Trunkierte Einzeleinträge mit weniger als 3 Zeichen allerdings
        nicht als Stichwort suchen.
        */
        if (value.length <= 3 && (value[value.length - 1] == '*' || value[value.length - 1] == '#')) {
            throw OpacApi.OpacErrorException("Eine Trunkierung ist erst ab einer Wortlänge von 3 Zeichen erlaubt!\nSuchterm: " + " " + value);
        }
    }

    private fun searchAddSearchText(current: String, operator: String, id: String, value: String): String {
        // Ported from LISSY's JS code
        if (value.isBlank()) return current

        val lissyType = when (id) {
            "5015" -> "FS" // Freie Suche
            "4" -> "TI" // Titel
            "1" -> "AU" // Autor
            "1018" -> "VL" // Verlag
            "5002" -> "IS" // ISBN
            "31" -> "EJ" // Erscheinungsjahr
            "46" -> "SW" // Schlagwort
            "59" -> "VO" // Verlagsort
            "99" -> "ST" // Stichworte
            else -> ""
        }
        var searchParam = "($lissyType=$value)"
        // Bei Autorensuche "AU" gleichzeitig auch Körperschaftssuche "KV" durchführen
        if (lissyType == "AU") {
            searchParam = "($searchParam OR (KV=$value))";
        }
        if (current.isBlank()) return searchParam
        else return "$current $operator $searchParam"
    }

    private var searchPageUrl: String? = null

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        val form = initAnonymousSession()

        // Build search query
        var searchQuery = ""
        query.forEach {
            searchValueCheckLength(it.value)
            searchQuery = searchAddSearchText(searchQuery, "AND", it.searchField.id, it.value)
        }
        searchQuery = "($searchQuery)"

        val formData = FormBody.Builder().apply {
            form.select("form[name=inputform] input[type=hidden]").forEach { hidden ->
                if (hidden["name"] != "searchtext") {
                    add(hidden["name"], hidden["value"])
                }
            }
            add("searchtext", searchQuery)
        }.build()

        // Submit search
        val waitpage = httpPost(form.select("form[name=inputform]")[0].absUrl("action"), formData, ENCODING).html

        // LISSY responds with a JS-based redirect
        val js = waitpage.select("head script")[0].data()
        val match = Regex("window\\.location\\.replace\\(\"(.*)\"\\)").find(js)
                ?: throw OpacApi.OpacErrorException("Could not find redirect")

        // After the redirect we have a frameset again
        val resultframeset = httpGet(baseurl + match.groupValues[1], ENCODING).html
        resultframeset.setBaseUri(baseurl + match.groupValues[1])
        val pageframeUrl = resultframeset.select("frame")[1].absUrl("src")
        val doc = httpGet(pageframeUrl, ENCODING).html
        doc.setBaseUri(pageframeUrl)

        doc.select("a[href*=pgnum]").forEach {
            if (it.attr("href").contains("pgnum=2")) {
                searchPageUrl = it.absUrl("href")
            }
        }

        return parseSearch(doc, 1)
    }

    protected open fun parseSearch(doc: Document, page: Int): SearchRequestResult {
        val metatable = doc.select("table").first().select("td")
        if (metatable.size == 2) return SearchRequestResult(emptyList(), 0, 1)
        val totalResults = Integer.parseInt(metatable[2].text().split(" ").last())
        val results = mutableListOf<SearchResult>()

        doc.select(".resultstable tr").forEach { tr ->
            if (tr.select("th").size > 0) return@forEach
            val sr = SearchResult()
            sr.apply {
                id = tr.child(1).select("a").first().absUrl("href")
                status = when (tr.child(2).text()) {
                    "ja" -> SearchResult.Status.GREEN
                    "nein" -> SearchResult.Status.RED
                    "online" -> SearchResult.Status.UNKNOWN
                    else -> SearchResult.Status.UNKNOWN
                }
                innerhtml = tr.child(1).select("a").first().text()
                type = when (tr.select("img").first().attr("src").split("/").last()) {
                    "buch.gif" -> SearchResult.MediaType.BOOK
                    "4.3.gif" -> SearchResult.MediaType.BOOK
                    "5.1Fantasy.gif" -> SearchResult.MediaType.BOOK
                    "Jugendsachbuch.gif" -> SearchResult.MediaType.BOOK
                    "Bilderbuch.gif" -> SearchResult.MediaType.BOOK
                    "Thriller.gif" -> SearchResult.MediaType.BOOK
                    "Krimi.gif" -> SearchResult.MediaType.BOOK
                    "Hoercd.gif" -> SearchResult.MediaType.CD
                    "cd.gif" -> SearchResult.MediaType.CD
                    "cdnote.gif" -> SearchResult.MediaType.CD_MUSIC
                    "dvd.gif" -> SearchResult.MediaType.DVD
                    "Konsole.gif" -> SearchResult.MediaType.GAME_CONSOLE
                    "eBook1SRM1.gif" -> SearchResult.MediaType.EBOOK
                    "eBook.gif" -> SearchResult.MediaType.EBOOK
                    "eAudio1SRM1.gif" -> SearchResult.MediaType.EAUDIO
                    "kopfhoer.gif" -> SearchResult.MediaType.AUDIO_CASSETTE
                    else -> SearchResult.MediaType.UNKNOWN
                }
            }

            results.add(sr)
        }

        return SearchRequestResult(results, totalResults, page)
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        val url = searchPageUrl!!.replace("pgnum=2", "pgnum=$page")
        val waitpage = httpGet(url, ENCODING).html
        // LISSY responds with a JS-based redirect
        val js = waitpage.select("head script")[0].data()
        val match = Regex("window\\.location\\.replace\\(\"(.*)\"\\)").find(js)
                ?: throw OpacApi.OpacErrorException("Could not find redirect")
        val doc = httpGet(baseurl + match.groupValues[1], ENCODING).html
        doc.setBaseUri(baseurl + match.groupValues[1])
        return parseSearch(doc, page)
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        // id is an URL
        val waitpage = httpGet(id, ENCODING).html

        // LISSY responds with a JS-based redirect
        val js = waitpage.select("head script")[0].data()
        val match = Regex("window\\.location\\.replace\\(\"(.*)\"\\)").find(js)
                ?: throw OpacApi.OpacErrorException("Could not find redirect")

        val doc = httpGet(baseurl + match.groupValues[1], ENCODING).html
        doc.setBaseUri(baseurl + match.groupValues[1])
        return parseDetail(doc)
    }

    private fun parseDetail(doc: Document): DetailedItem {
        return DetailedItem().apply {
            val td = doc.select("table")[1].select("td")[1]
            val lines = Jsoup.parse(td.html().replace(Regex("</?(p|P|br|BR)>"), "####")).text().split("####")
            lines.forEachIndexed { index, s ->
                if (index == 0) {
                    title = s
                    addDetail(Detail("Titel", s.trim()))
                } else if (s.isBlank()) {
                } else if (s.contains(":") && s.split(":")[0].length < 30) {
                    addDetail(Detail(s.split(":")[0], s.split(":")[1].trim()))
                } else {
                    addDetail(Detail("", s.trim()))
                }
            }

            val img = doc.select("table")[1].select("img").first()
            if (!img.attr("src").contains(".gif")) {
                cover = img.absUrl("src")
            }
        }
    }

    override fun getResult(position: Int): DetailedItem? {
        // Should not be called because every media has an ID
        return null
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        TODO()
    }

    override fun prolong(media: String, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult {
        TODO()
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        TODO()
    }

    override fun prolongMultiple(media: List<String>,
                                 account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.UNSUPPORTED)
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult {
        TODO()
    }

    override fun account(account: Account): AccountData {
        TODO()
    }

    override fun checkAccountData(account: Account) {
        login(account)
    }

    private fun login(account: Account): Document {
        TODO()
    }

    override fun getShareUrl(id: String?, title: String?): String {
        TODO()
    }

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING or OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL or
                OpacApi.SUPPORT_FLAG_WARN_RESERVATION_FEES
    }

    override fun getSupportedLanguages(): Set<String>? {
        return null
    }

    override fun setLanguage(language: String?) {

    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult {
        TODO("not implemented")
    }
}
