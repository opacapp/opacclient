package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import okhttp3.FormBody
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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
        val doc = login(account)
        val accountData = AccountData(account.id)

        accountData.lent = parseItems(doc, ::LentItem, "#checkoutst")
        accountData.reservations = parseItems(doc, ::ReservedItem, "#holdst")

        val feesDoc = Jsoup.parse(httpGet("$baseurl/cgi-bin/koha/opac-account.pl", "UTF-8"))
        accountData.pendingFees = parseFees(feesDoc)

        return accountData
    }

    private fun parseFees(feesDoc: Document): String? {
        val text = feesDoc.select("td.sum").text()
        return if (!text.isBlank()) text else null
    }

    private fun <I : AccountItem> parseItems(doc: Document, constructor: () -> I, id: String): List<I> {
        val lentTable = doc.select(id).first() ?: return emptyList()

        return lentTable.select("tbody tr").map { row ->
            val item = constructor()
            row.select("td").forEach { col ->
                val type = col.attr("class").split(" ")[0]
                val content = col.text()
                when (type) {
                    "itype" -> item.format = content
                    "title" -> item.title = content
                    "date_due" -> if (item is LentItem) item.deadline = parseDate(col)
                    "branch" -> if (item is LentItem) {
                        item.lendingBranch = content
                    } else if (item is ReservedItem) {
                        item.branch = content
                    }
                    "expirationdate" -> if (item is ReservedItem) item.expirationDate = parseDate(col)
                    "status" -> item.status = content
                    "modify" -> if (item is ReservedItem) {
                        item.cancelData = col.select("input[name=reserve_id]").attr("value")
                    }
                    // "call_no" -> Signatur
                    // "fines" -> Gebühren (Ja/Nein)
                    // "reserve_date" -> Reservierungsdatum
                }
            }
            item
        }
    }

    private fun parseDate(col: Element): LocalDate? {
        val select = col.select("span[title]").first() ?: return null
        val isoformat = select.attr("title")
        // example: <span title="2018-11-02T23:59:00">
        return LocalDateTime(isoformat).toLocalDate()
    }

    override fun checkAccountData(account: Account) {
        login(account)
    }

    private fun login(account: Account): Document {
        val formBody = FormBody.Builder()
                .add("koha_login_context", "opac")
                .add("koha_login_context", "opac")  // sic! two times
                .add("userid", account.name)
                .add("password", account.password)
        val doc = Jsoup.parse(httpPost("$baseurl/cgi-bin/koha/opac-user.pl", formBody.build(), "UTF-8"))
        if (doc.select(".alert").size > 0) {
            throw OpacApi.OpacErrorException(doc.select(".alert").text())
        }
        return doc
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