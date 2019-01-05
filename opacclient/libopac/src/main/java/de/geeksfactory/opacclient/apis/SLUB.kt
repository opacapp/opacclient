package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import de.geeksfactory.opacclient.utils.get
import de.geeksfactory.opacclient.utils.html
import de.geeksfactory.opacclient.utils.text

/**
 * OpacApi implementation for SLUB. https://slub-dresden.de
 *
 * @author Steffen Rehberg, Jan 2019
 */
open class SLUB : OkHttpBaseApi() {
    protected lateinit var baseurl: String
    protected val ENCODING = "UTF-8"

    override fun init(library: Library, factory: HttpClientFactory) {
        super.init(library, factory)
        baseurl = library.data.getString("baseurl")
    }

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        TODO("not implemented")
    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult {
        TODO("not implemented")
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        TODO("not implemented")
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        TODO("not implemented")
    }

    override fun getResult(position: Int): DetailedItem? {
        // getResultById is implemented and every search result has an id set, so getResult is not used
        return null
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        TODO("not implemented")
    }

    override fun prolong(media: String, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult {
        TODO("not implemented")
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        TODO("not implemented")
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult {
        TODO("not implemented")
    }

    override fun account(account: Account): AccountData {
        TODO("not implemented")
    }

    override fun checkAccountData(account: Account) {
        TODO("not implemented")
    }

    override fun getShareUrl(id: String?, title: String?): String {
        TODO("not implemented")
    }

    override fun getSupportFlags(): Int {
        return 0
    }

    override fun getSupportedLanguages(): Set<String>? {
        //TODO("not implemented") 
        return null
    }

    override fun parseSearchFields(): List<SearchField> {
        val doc = httpGet(baseurl, ENCODING).html
        return doc.select("ul#search-in-field-options li").map {
            TextSearchField().apply {
                id = it["name"]
                displayName = it.text
            }
        }
    }

    override fun setLanguage(language: String?) {
        //TODO("not implemented")
    }

}