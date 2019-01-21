package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.DropdownSearchField
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import de.geeksfactory.opacclient.utils.get
import de.geeksfactory.opacclient.utils.html
import de.geeksfactory.opacclient.utils.text
import okhttp3.FormBody
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL

/**
 * OpacApi implementation for the Arena OPAC developed by Axiell. https://www.axiell.de/arena/
 *
 * @author Johan von Forstner, January 2019
 */
open class Arena : OkHttpBaseApi() {
    protected lateinit var opacUrl: String
    protected val ENCODING = "UTF-8"

    override fun init(library: Library, factory: HttpClientFactory) {
        super.init(library, factory)
        opacUrl = library.data.getString("baseurl")
    }

    override fun parseSearchFields(): List<SearchField> {
        val doc = httpGet("$opacUrl/extended-search", ENCODING).html

        // text fields
        val textFields = doc.select(".arena-extended-search-original-field-container")
                .map { container ->
            TextSearchField().apply {
                id = container.select("input").first()["name"]
                displayName = container.select("span").first().text()
            }
        }

        // dropdown fields
        val dropdownFields = listOf("category", "media-class", "target-audience", "accession-date").map {
            doc.select(".arena-extended-search-$it-container")
        }.filter { it.size > 0 }.map { it.first() }.map { container ->
            DropdownSearchField().apply {
                val select = container.select("select").first()

                id = select["name"]
                displayName = container.select("label").first().text()
                val options = select.select("option").map { option ->
                    DropdownSearchField.Option(
                            option["value"],
                            option.text)
                }.toMutableList()
                if (select.hasAttr("multiple")) {
                    options.add(0, DropdownSearchField.Option("", ""))
                }
                dropdownValues = options
            }
        }

        // year field
        val yearFields = listOf("from", "to").map {
            doc.select(".arena-extended-search-publication-year-$it-container")
        }.filter { it.size > 0 }.map { it.first() }.mapIndexed {i, container ->
            TextSearchField().apply {
                id = container.select("input").first()["name"]
                displayName = container.parent().child(0).ownText()
                hint = container.select("label").first().text()
                isHalfWidth = i == 1
            }
        }

        return textFields + dropdownFields + yearFields
    }

    override fun search(query: MutableList<SearchQuery>): SearchRequestResult {
        val searchForm = httpGet("$opacUrl/extended-search", ENCODING).html
                .select(".arena-extended-search-original").first()

        val formData = FormBody.Builder().apply {
            searchForm.select("input[type=hidden]").forEach { hidden ->
                add(hidden["name"], hidden["value"])
            }
            val submit = searchForm.select("input[type=submit]").first()
            add(submit["name"], submit["value"])
            query.forEach { q ->
                add(q.key, q.value)
            }
        }.build()
        val doc = httpPost(searchForm["action"], formData, ENCODING).html
        return parseSearch(doc)
    }

    internal fun parseSearch(doc: Document, page: Int = 1): SearchRequestResult {
        val count = doc.select(".feedbackPanelinfo").text.replace(Regex("[^\\d]"), "").toInt()
        val coverAjaxUrls = getAjaxUrls(doc)

        val results = doc.select(".arena-record").mapIndexed { i, record ->
            SearchResult().apply {
                val title = record.select(".arena-record-title").text
                val year = record.select(".arena-record-year .arena-value").first()?.text
                val author = record.select(".arena-record-author .arena-value").map { it.text }.joinToString(", ")

                innerhtml = "<b>$title</b><br>$author ${year ?: ""}"
                id = record.select(".arena-record-id").first().text
                cover = getCover(record, coverAjaxUrls)
            }
        }
        return SearchRequestResult(results, count, page)
    }

    internal fun getCover(record: Element, coverAjaxUrls: Map<String, String>): String? {
        val coverHolder = record.select(".arena-record-cover").first()
        if (coverHolder != null) {
            val id = coverHolder["id"]
            if (coverAjaxUrls.containsKey(id)) {
                // get cover via ajax
                val xml = httpGet(coverAjaxUrls[id], ENCODING)
                val url = Regex("<img src=\"([^\"]+)").find(xml)?.groups?.get(1)
                        ?.value?.replace("&amp;", "&")
                return if (url != null) URL(URL(opacUrl), url).toString() else null
            } else if (coverHolder.select("img:not([src*=indicator.gif])").size > 0) {
                // cover is included as img tag
                val url = coverHolder.select("img:not([src*=indicator.gif])").first()["src"]
                return URL(URL(opacUrl), url).toString()
            }
        }
        return null
    }

    internal fun getAjaxUrls(doc: Document): Map<String, String> {
        val regex = Regex(Regex.escape(
                "<script type=\"text/javascript\">Wicket.Event.add(window,\"domready\",function(b){var a=wicketAjaxGet(\"")
                + "([^\"]+)"
                + Regex.escape("\",function(){}.bind(this),function(){}.bind(this),function(){return Wicket.\$(\"")
                + "([^\"]+)"
                + Regex.escape("\")!=null}.bind(this))});</script>"))
        return regex.findAll(doc.html()).associate { match ->
            Pair(match.groups[2]!!.value, match.groups[1]!!.value)
        }
    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        val url = "$opacUrl/results?p_p_id=searchResult_WAR_arenaportlets&p_r_p_687834046_search_item_id=$id"
        val doc = httpGet(url, ENCODING).html
        doc.setBaseUri(url)
        return parseDetail(doc)
    }

    private fun parseDetail(doc: Document): DetailedItem {
        val urls = getAjaxUrls(doc).filterKeys { it.contains("crDetailWicket") }
        val holdingsUrl = if (urls.isNotEmpty()) urls.values.iterator().next() else null
        val holdingsDoc = if (holdingsUrl != null) {
            val ajaxResp = httpGet(URL(URL(opacUrl), holdingsUrl).toString(), ENCODING).html
            ajaxResp.select("component").first().text.html
        } else null

        return DetailedItem().apply {
            title = doc.select(".arena-detail-title").text
            doc.select(".arena-catalogue-detail .arena-field").forEach { field ->
                addDetail(Detail(field.text, field.nextElementSibling().text))
            }
            id = doc.select(".arena-record-id").first().text
            cover = doc.select(".arena-detail-cover img").first()?.absUrl("href")
            copies = holdingsDoc?.select(".arena-row")?.map { row ->
                Copy().apply {
                    department = row.select(".arena-holding-department .arena-value").text
                    shelfmark = row.select(".arena-holding-shelf-mark .arena-value").text
                    status = row.select(".arena-availability-right").text
                }
            } ?: emptyList()
        }
    }

    override fun getResult(position: Int): DetailedItem? {
        return null
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prolong(media: String, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun account(account: Account): AccountData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkAccountData(account: Account) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getShareUrl(id: String, title: String?): String {
        return "$opacUrl/results?p_p_id=searchResult_WAR_arenaportlets&p_r_p_687834046_search_item_id=$id"
    }

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING
    }

    override fun getSupportedLanguages(): MutableSet<String>? {
        return null
    }

    override fun setLanguage(language: String) {

    }
}