package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.*
import de.geeksfactory.opacclient.utils.get
import de.geeksfactory.opacclient.utils.html
import de.geeksfactory.opacclient.utils.text
import okhttp3.FormBody
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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

    private fun followJsRedirect(waitpage: Document): Document {
        val js = waitpage.select("head script")[0].data()
        val match = Regex("window\\.location\\.replace\\(\"(.*)\"\\)").find(js)
                ?: throw OpacApi.OpacErrorException("Could not find redirect")

        // After the redirect we have a frameset again
        val res = httpGet(baseurl + match.groupValues[1], ENCODING).html
        res.setBaseUri(baseurl + match.groupValues[1])
        return res
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
        val resultframeset = followJsRedirect(httpPost(form.select("form[name=inputform]")[0].absUrl("action"), formData, ENCODING).html)
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
        val doc = followJsRedirect(httpGet(url, ENCODING).html)
        return parseSearch(doc, page)
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        // id is an URL
        val doc = followJsRedirect(httpGet(id, ENCODING).html)
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
            if (!img.attr("src").contains("empty.")) {
                cover = img.absUrl("src")
            }
        }
    }

    override fun getResult(position: Int): DetailedItem? {
        // Should not be called because every media has an ID
        return null
    }

    data class AccountMenu(
            val lentUrl: String,
            val reservationsUrl: String
    )

    private fun login(account: Account): AccountMenu {
        val frameset = httpGet("$baseurl/lissy/lissy.ly?pg=bnrlogin", ENCODING).html
        frameset.setBaseUri("$baseurl/lissy/lissy.ly?pg=bnrlogin")
        val topframeUrl = frameset.select("frame")[0].absUrl("src")
        val topframe = httpGet(topframeUrl, ENCODING).html
        topframe.setBaseUri(topframeUrl)
        val pageframeUrl = topframe.select("frame")[1].absUrl("src")
        val pageframe = httpGet(pageframeUrl, ENCODING).html
        pageframe.setBaseUri(pageframeUrl)

        val formData = FormBody.Builder().apply {
            pageframe.select("form[name=form1] input[type=hidden]").forEach { hidden ->
                add(hidden["name"], hidden["value"])
            }
            add("bnr", account.name)
            add("gd", account.password)
        }.build()

        val postUrl = pageframe.select("form[name=form1]").first().absUrl("action")
        val lframeset = httpPost(postUrl, formData, ENCODING).html
        lframeset.setBaseUri(postUrl)
        val ltopframeUrl = lframeset.select("frame")[0].absUrl("src")
        val ltopframe = httpGet(ltopframeUrl, ENCODING).html
        ltopframe.setBaseUri(ltopframeUrl)
        val lpageframeUrl = ltopframe.select("frame")[1].absUrl("src")
        val lpageframe = httpGet(lpageframeUrl, ENCODING).html
        lpageframe.setBaseUri(lpageframeUrl)

        if (lpageframe.select("table").size == 0) {
            throw OpacApi.OpacErrorException(lpageframe.select("body").text())
        }
        val lmenuframeUrl = ltopframe.select("frame")[0].absUrl("src")
        val lmenuframe = httpGet(lmenuframeUrl, ENCODING).html
        lmenuframe.setBaseUri(lmenuframeUrl)
        return AccountMenu(
                lentUrl = lmenuframe.select("a").find { it.attr("href").contains("type=entl") }?.absUrl("href")
                        ?: throw OpacApi.OpacErrorException("Lent items not found in menu"),
                reservationsUrl = lmenuframe.select("a").find { it.attr("href").contains("type=vorg") }?.absUrl("href")
                        ?: throw OpacApi.OpacErrorException("Lent items not found in menu")
        )
    }

    override fun checkAccountData(account: Account) {
        login(account)
    }

    override fun account(account: Account): AccountData {
        val menu = login(account)
        val dateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")

        return AccountData(account.id).apply {
            lent = mutableListOf()
            val lentDoc = followJsRedirect(httpGet(menu.lentUrl, ENCODING).html)
            lentDoc.select("form table").first()?.select("tr")?.forEachIndexed { idx, tr ->
                if (idx == 0) return@forEachIndexed  // Header row

                lent.add(LentItem().apply {
                    barcode = tr.child(2).text().trim()
                    title = tr.child(3).text().trim()
                    deadline = dateFormat.parseLocalDate(tr.child(4).text().replace(Regex("[^0-9.]"), ""))
                    isRenewable = tr.child(0).select("input").first() != null
                    prolongData = if (isRenewable) barcode else null
                })
            }

            reservations = mutableListOf()
            val resDoc = httpGet(menu.reservationsUrl, ENCODING).html
            resDoc.select("form table").first()?.select("tr")?.forEachIndexed { idx, tr ->
                if (idx == 0) return@forEachIndexed  // Header row

                reservations.add(ReservedItem().apply {
                    title = tr.child(5).text().trim()
                    val ready = tr.child(6).text().replace(Regex("[^0-9.]"), "")
                    readyDate = if (ready.isBlank()) dateFormat.parseLocalDate(ready) else null
                    val expiry = tr.child(7).text().replace(Regex("[^0-9.]"), "")
                    expirationDate = if (ready.isBlank()) dateFormat.parseLocalDate(expiry) else null
                    cancelData = tr.child(4).text().trim()
                })
            }
        }
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        TODO()
    }

    override fun prolong(media: String, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult {
        val menu = login(account)
        val lentDoc = followJsRedirect(httpGet(menu.lentUrl, ENCODING).html)

        val tr = lentDoc.select("form table").first().select("tr").find { it ->
            media == it.child(2).text().trim()
        } ?: return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR, "Medium not found")

        val form = lentDoc.select("form[name=form1]").first()

        val formData = FormBody.Builder().apply {
            form.select("input[type=hidden]").forEach { field ->
                add(field["name"], field["value"])
            }
            tr.select("input[type=checkbox]").forEach { field ->
                add(field["name"], field["value"])
            }
        }.build()
        val resultframe = httpPost(form.absUrl("action"), formData, ENCODING).html
        resultframe.setBaseUri(form.absUrl("action"))
        val resultUrl = resultframe.select("frame")[1].absUrl("src")
        val result = httpGet(resultUrl, ENCODING).html
        result.setBaseUri(resultUrl)
        if (result.select("table tr")[1].child(3).text().trim() == "Nein") {
            return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR, result.select("table tr")[1].child(4).text().trim())
        }
        return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.OK)
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        return prolongMultiple(null, account, useraction, selection)
    }

    override fun prolongMultiple(media: List<String>?,
                                 account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        val menu = login(account)
        val lentDoc = followJsRedirect(httpGet(menu.lentUrl, ENCODING).html)

        val trs = lentDoc.select("form table").first().select("tr").filter { it ->
            media == null || media.contains(it.child(2).text().trim())
        }
        if (media != null && trs.size != media.size)
            return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.ERROR, "Medium not found")

        val form = lentDoc.select("form[name=form1]").first()

        val formData = FormBody.Builder().apply {
            form.select("input[type=hidden]").forEach { field ->
                add(field["name"], field["value"])
            }
            trs.forEach {
                it.select("input[type=checkbox]").forEach { field ->
                    add(field["name"], field["value"])
                }
            }
        }.build()
        val resultframe = httpPost(form.absUrl("action"), formData, ENCODING).html
        resultframe.setBaseUri(form.absUrl("action"))
        val resultUrl = resultframe.select("frame")[1].absUrl("src")
        val result = httpGet(resultUrl, ENCODING).html
        result.setBaseUri(resultUrl)
        val list = result.select("table tr").drop(1).map {
            mapOf(
                    OpacApi.ProlongAllResult.KEY_LINE_TITLE to it.child(1).text().trim(),
                    OpacApi.ProlongAllResult.KEY_LINE_NR to it.child(0).text().trim(),
                    OpacApi.ProlongAllResult.KEY_LINE_NEW_RETURNDATE to it.child(2).text().trim(),
                    OpacApi.ProlongAllResult.KEY_LINE_MESSAGE to it.child(4).text().trim()
            )
        }

        return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.OK, list)
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult {
        val menu = login(account)
        val resDoc = httpGet(menu.reservationsUrl, ENCODING).html
        resDoc.setBaseUri(menu.reservationsUrl)

        val tr = resDoc.select("form table").first().select("tr").find { it ->
            it.child(4).text().trim() == media
        } ?: return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.ERROR, "Medium not found")

        val form = resDoc.select("form[name=form1]").first()

        val formData = FormBody.Builder().apply {
            form.select("input[type=hidden]").forEach { field ->
                add(field["name"], field["value"])
            }
            tr.select("input[type=checkbox]").forEach { field ->
                add(field["name"], field["value"])
            }
        }.build()
        val result = httpPost(form.absUrl("action"), formData, ENCODING).html
        val msg = result.select("h4 + p > font").first()
        return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.OK, msg?.text())
    }

    override fun getShareUrl(id: String?, title: String?): String {
        TODO()
    }

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING or OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL or
                OpacApi.SUPPORT_FLAG_WARN_RESERVATION_FEES or OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_MULTIPLE
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
