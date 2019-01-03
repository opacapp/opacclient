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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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