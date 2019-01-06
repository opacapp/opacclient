package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.networking.NotReachableException
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.DropdownSearchField
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import de.geeksfactory.opacclient.utils.get
import de.geeksfactory.opacclient.utils.html
import de.geeksfactory.opacclient.utils.text
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.nodes.Document
import java.net.URL
import java.util.*

/**
 * OpacApi implementation for the Arena OPAC developed by Axiell. https://www.axiell.de/arena/
 *
 * @author Johan von Forstner, January 2019
 */
class Arena: OkHttpBaseApi() {
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

        val results = doc.select(".arena-record").mapIndexed { i, record ->
            SearchResult().apply {
                val title = record.select(".arena-record-title").text
                val year = record.select(".arena-record-year .arena-value").first()?.text
                val author = record.select(".arena-record-author .arena-value").map { it.text }.joinToString(", ")

                innerhtml = "<b>$title</b><br>$author ${year ?: ""}"
                id = record.select(".arena-record-id").first().text


                // fetch cover
                val data = FormBody.Builder()
                        .add("p_p_id", "searchResult_WAR_arenaportlets")
                        .add("p_p_lifecycle", "2")
                        .add("p_p_state", "normal")
                        .add("p_p_mode", "view")
                        .add("p_p_resource_id", "/searchResult/?wicket:interface=:4:searchResultPanel:dataContainer:dataView:${i + 1}:containerItem:item:indexedRecordPanel:coverDecorPanel::IBehaviorListener:0:")
                        .add("p_p_cacheability", "cacheLevelPage")
                        .add("p_p_col_id", "column-2")
                        .add("p_p_col_pos", "1")
                        .add("p_p_col_count", "2")
                        .build()

                val xml = httpPostWicket("$opacUrl/search?random=${Random().nextDouble()}", data, ENCODING)
                val url = Regex("<img src=\"([^\"]+)").find(xml)?.groups?.get(1)?.toString()
                cover = if (url != null) URL(URL(opacUrl), url).toString() else null
            }
        }
        return SearchRequestResult(results, count, page)
    }

    private fun httpPostWicket(url: String, data: FormBody, encoding: String): CharSequence {
        var requestbuilder: Request.Builder = Request.Builder()
                .url(BaseApi.cleanUrl(url))
                .header("Accept", "*/*")
                .header("User-Agent", userAgent)
                .header("Wicket-Ajax", "true")

        if (data.contentType() != null) {
            requestbuilder = requestbuilder.header("Content-Type", data.contentType()!!.toString())
        }
        val request = requestbuilder.post(data).build()
        try {
            val response = http_client.newCall(request).execute()
            return response.body()!!.string()
        } catch (e: Exception) {
            throw NotReachableException()
        }
    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getResult(position: Int): DetailedItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupportFlags(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupportedLanguages(): MutableSet<String>? {
        return null
    }

    override fun setLanguage(language: String) {

    }
}