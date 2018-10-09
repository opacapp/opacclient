package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.i18n.StringProvider
import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import okhttp3.FormBody
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Koha : OkHttpBaseApi() {
    protected lateinit var sru: SRU
    protected lateinit var baseurl: String
    protected val NOT_RENEWABLE = "NOT_RENEWABLE"

    override fun init(library: Library, factory: HttpClientFactory) {
        super.init(library, factory)
        baseurl = library.data.getString("baseurl")
        sru = SRU()
        val sruLibrary = Library().apply {
            api = "sru"
            data = JSONObject().apply {
                put("baseurl", library.data.optString("sru_url", "$baseurl:9999/biblios"))
                put("sharelink", "{$baseurl}cgi-bin/koha/opac-detail.pl?biblionumber=%s")
                put("idSearchQuery", "rec:id")
            }
        }
        sru.init(sruLibrary, factory)
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

    override fun prolong(media: String, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult {
        if (media.startsWith(NOT_RENEWABLE)) {
            return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR, media.substring(NOT_RENEWABLE.length))
        }

        var doc = login(account)
        var borrowernumber = doc.select("input[name=borrowernumber]").first()?.attr("value")

        doc = Jsoup.parse(httpGet("$baseurl/cgi-gin/koha/opac-renew.pl?from=opac_user&item=$media&borrowernumber=$borrowernumber", "UTF-8"))
        val label = doc.select(".blabel").first()
        if (label != null && label.hasClass("label-success")) {
            return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR, label?.text()
                    ?: stringProvider.getString(StringProvider.ERROR))
        }
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult {
        login(account)

        val (biblionumber, reserveId) = media.split(":")
        val formBody = FormBody.Builder()
                .add("biblionumber", biblionumber)
                .add("reserve_id", reserveId)
                .add("submit", "")
        val doc = Jsoup.parse(httpPost("$baseurl/cgi-bin/koha/opac-modrequest.pl", formBody.build(), "UTF-8"))
        val input = doc.select("input[name=reserve_id][value=$reserveId]").first()
        if (input == null) {
            return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.ERROR, stringProvider.getString(StringProvider.ERROR))
        }
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
                    "title" -> {
                        item.title = content
                        val link = col.select("a[href]").first()?.attr("href")
                        if (link != null) {
                            item.id = BaseApi.getQueryParamsFirst(link)["biblionumber"]
                        }
                    }
                    "date_due" -> if (item is LentItem) item.deadline = parseDate(col)
                    "branch" -> if (item is LentItem) {
                        item.lendingBranch = content
                    } else if (item is ReservedItem) {
                        item.branch = content
                    }
                    "expirationdate" -> if (item is ReservedItem) item.expirationDate = parseDate(col)
                    "status" -> item.status = content
                    "modify" -> if (item is ReservedItem) {
                        item.cancelData = "${col.select("input[name=biblionumber]").attr("value")}:${col.select("input[name=reserve_id]").attr("value")}"
                    }
                    "renew" -> if (item is LentItem) {
                        val input = col.select("input[name=item]").first()
                        if (input != null) {
                            item.prolongData = input.attr("value")
                            item.isRenewable = true
                        } else {
                            item.prolongData = NOT_RENEWABLE + content
                            item.isRenewable = false
                        }
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