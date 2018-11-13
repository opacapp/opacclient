package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.i18n.StringProvider
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
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode

open class NetBiblio : OkHttpBaseApi() {
    protected lateinit var opacUrl: String
    protected val ENCODING = "UTF-8"
    protected var lang = "en"

    override fun init(library: Library, http_client_factory: HttpClientFactory?) {
        super.init(library, http_client_factory)
        try {
            this.opacUrl = library.data["baseurl"] as String
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    override fun start() {
        super.start()
        val langCode = when {
            supportedLanguages.contains(lang) -> lang
            supportedLanguages.contains("en")
                // Fall back to English if language not available
            -> "en"
            supportedLanguages.contains("de")
                // Fall back to German if English not available
            -> "de"
            else -> ""
        }
        httpGet("$opacUrl/Site/ChangeLanguage?language=$langCode", ENCODING).html
    }

    override fun parseSearchFields(): List<SearchField>? {
        if (!initialised) start()

        val doc = httpGet("$opacUrl/search/extended", ENCODING).html

        // text fields
        val options = doc.select(".wo-searchfield-dropdown").first().select("option")
        val textFields = options.map { o ->
            TextSearchField().apply {
                id = o["value"]
                displayName = o.text
            }
        }

        // filter fields
        val filterFields = doc.select(".wo-filterfield").flatMap { panel ->
            val title = panel.select(".panel-title").text.trim()
            when (panel["data-filterfieldtype"]) {
                "Checkbox" -> {
                    val checkboxes = panel.select("input[type=checkbox]")
                    val modeSelector = panel.select("input[type=radio][name$=-mode][checked]").first()
                    listOf(
                            DropdownSearchField().apply {
                                id = checkboxes[0]["name"]
                                displayName = title
                                dropdownValues = listOf(DropdownSearchField.Option("", "")) +
                                        checkboxes.map { checkbox ->
                                            DropdownSearchField.Option(
                                                    checkbox["value"],
                                                    checkbox.nextElementSibling().text.trim())
                                        }
                                if (modeSelector != null) {
                                    data = JSONObject().apply {
                                        put("modeKey", modeSelector.attr("name"))
                                        put("modeValue", modeSelector.attr("value"))
                                    }
                                }
                            }
                    )
                }
                "Date" -> {
                    val textBoxes = panel.select("input[type=text]")
                    textBoxes.mapIndexed { i, field ->
                        TextSearchField().apply {
                            id = field["name"]
                            displayName = if (i == 0) title else ""
                            hint = (field.previousSibling() as TextNode).text.trim()
                            isHalfWidth = i == 1
                        }
                    }
                }
                else -> emptyList()
            }
        }

        return textFields + filterFields
    }

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        val body = FormBody.Builder()

        var textCount = 0
        for (q in query) {
            if (q.searchField is TextSearchField && !q.key.startsWith("Filter.")) {
                if (q.value.isNullOrBlank()) continue
                if (textCount == 1) {
                    body.add("Request.SearchOperator", "AND")
                } else if (textCount >= 2) {
                    throw OpacApi.OpacErrorException(stringProvider.getString(StringProvider.COMBINATION_NOT_SUPPORTED))
                }
                body.add("Request.SearchTerm", q.value)
                body.add("Request.SearchKey", q.key)
                textCount ++
            }
        }
        if (textCount == 0) {
            body.add("Request.SearchTerm", "")
            body.add("Request.SearchKey", "W")
            textCount ++
        }
        if (textCount == 1) {
            body.add("Request.SearchOperator", "AND")
            body.add("Request.SearchTerm", "")
            body.add("Request.SearchKey", "W")
        }

        for (q in query) {
            if (q.searchField is TextSearchField && q.key.startsWith("Filter.")) {
                body.add(q.key, q.value)
            } else if (q.searchField is DropdownSearchField) {
                if (q.searchField.data != null && q.searchField.data.has("modeKey")) {
                    body.add(q.searchField.data.getString("modeKey"), q.searchField.data.getString("modeValue"))
                }
                body.add(q.key, q.value)
            }
        }

        body.add("Request.PageSize", "25")

        val doc = httpPost("$opacUrl/search/extended/submit", body.build(), ENCODING).html
        return parseSearch(doc, 1)
    }

    protected fun parseSearch(doc: Document, page: Int): SearchRequestResult {
        val countText = doc.select("wo-grid-meta-resultcount").text
        val totalCount = Regex("\\d+").findAll(countText).last().value.toInt()

        val results = doc.select(".wo-grid-table tbody tr").groupBy { row ->
            // there are multiple rows for one entry. Their CSS IDs all have the same prefix
            val id = row.attr("id")
            Regex("wo-row_\\d+").find(id)!!.value
        }.map { entry ->
            val rows = entry.value
            val titleRow = rows[0]
            SearchResult().apply {
                innerhtml = titleRow.select("td[style=font-size: 14px;] a").text
            }
        }

        return SearchRequestResult(results, totalCount, page)
    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult? {
        return null
    }

    override fun searchGetPage(page: Int): SearchRequestResult? {
        return null
    }

    override fun getResultById(id: String, homebranch: String): DetailedItem? {
        return null
    }

    override fun getResult(position: Int): DetailedItem? {
        return null
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int,
                             selection: String): OpacApi.ReservationResult? {
        return null
    }

    override fun prolong(media: String, account: Account, useraction: Int, selection: String): OpacApi.ProlongResult? {
        return null
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String): OpacApi.ProlongAllResult? {
        return null
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String): OpacApi.CancelResult? {
        return null
    }

    override fun account(account: Account): AccountData? {
        return null
    }

    override fun checkAccountData(account: Account) {

    }

    override fun getShareUrl(id: String, title: String): String? {
        return null
    }

    override fun getSupportFlags(): Int {
        return 0
    }

    override fun getSupportedLanguages(): Set<String>? {
        val doc = httpGet(opacUrl, ENCODING).html
        val languages = findLanguages(doc)

        // change language once to find out what the current language was
        val doc2 = httpGet("$opacUrl/Site/ChangeLanguage?language=${languages[0]}", ENCODING).html
        val myLanguage = (findLanguages(doc2) - languages).first()

        // change back to previous language
        httpGet("$opacUrl/Site/ChangeLanguage?language=$myLanguage", ENCODING).html
        return languages.toSet() + myLanguage
    }

    private fun findLanguages(doc: Document): List<String> {
        val regex = Regex("language=([^&]+)")
        return doc.select(".dropdown-menu a[href*=ChangeLanguage]").map { a ->
            regex.find(a["href"])!!.groups[1]!!.value
        }
    }

    override fun setLanguage(language: String) {
        lang = language
    }
}
