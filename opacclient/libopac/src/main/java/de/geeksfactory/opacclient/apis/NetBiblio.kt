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
import org.json.JSONException
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

    override fun search(query: List<SearchQuery>): SearchRequestResult? {
        return null
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
