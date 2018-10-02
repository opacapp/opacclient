package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import okhttp3.FormBody
import org.jsoup.Jsoup

class Koha : OkHttpBaseApi() {
    protected lateinit var sru: SRU
    protected lateinit var baseurl: String

    override fun init(library: Library, http_client_factory: HttpClientFactory) {
        super.init(library, http_client_factory)
        baseurl = library.data.getString("baseurl")
        sru = SRU()
        // TODO: initialize SRU
    }

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        return sru.search(query)
    }

    override fun parseSearchFields(): List<SearchField> {
        return sru.parseSearchFields()
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        return sru.searchGetPage(page)
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        return sru.getResultById(id, homebranch)
    }

    override fun getResult(position: Int): DetailedItem {
        return sru.getResult(position)
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prolong(media: String?, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun account(account: Account): AccountData {
        login(account)
        return AccountData(account.id)
    }

    override fun checkAccountData(account: Account) {
        login(account)
    }

    private fun login(account: Account) {
        val formBody = FormBody.Builder()
                .add("koha_login_context", "opac")
                .add("koha_login_context", "opac")  // sic! two times
                .add("userid", account.name)
                .add("password", account.password)
        val doc = Jsoup.parse(httpPost("$baseurl/cgi-bin/koha/opac-user.pl", formBody.build(), "UTF-8"))
        if (doc.select(".alert").size > 0) {
            throw OpacApi.OpacErrorException(doc.select(".alert").text())
        }
    }

    override fun getShareUrl(id: String?, title: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING
    }

    override fun getSupportedLanguages(): Set<String>? {
        return null
    }

    override fun setLanguage(language: String?) {

    }

    override fun filterResults(filter: Filter?, option: Filter.Option?): SearchRequestResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}