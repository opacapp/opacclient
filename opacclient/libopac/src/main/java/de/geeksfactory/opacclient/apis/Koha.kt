package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.i18n.StringProvider
import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.*
import de.geeksfactory.opacclient.utils.get
import de.geeksfactory.opacclient.utils.html
import de.geeksfactory.opacclient.utils.text
import okhttp3.FormBody
import okhttp3.HttpUrl
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Koha : OkHttpBaseApi() {
    protected lateinit var baseurl: String
    protected val NOT_RENEWABLE = "NOT_RENEWABLE"
    protected var searchQuery: List<SearchQuery>? = null

    override fun init(library: Library, factory: HttpClientFactory) {
        super.init(library, factory)
        baseurl = library.data.getString("baseurl")
    }

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        this.searchQuery = query
        val builder = searchUrl(query)
        val doc = httpGet(builder.build().toString(), ENCODING).html
        return parseSearch(doc, 1)
    }

    private val mediatypes = mapOf(
            "book" to SearchResult.MediaType.BOOK,
            "film" to SearchResult.MediaType.MOVIE,
            "sound" to SearchResult.MediaType.CD_MUSIC,
            "newspaper" to SearchResult.MediaType.MAGAZINE
    )

    private fun parseSearch(doc: Document, page: Int): SearchRequestResult {
        val countText = doc.select("#numresults").first().text
        val totalResults = Regex("\\d+").findAll(countText).last().value.toInt()

        val results = doc.select(".searchresults table tr").map { row ->
            val titleA = row.select("a.title").first()
            val title = titleA.ownText()
            val biblionumber = Regex("biblionumber=([^&]+)").find(titleA["href"])!!.groupValues[1]
            val author = row.select(".author").first()?.text
            var summary = row.select(".results_summary").first()?.text
            if (summary != null) {
                summary = summary.split(" | ").dropLast(1).joinToString(" | ")
            }
            val mediatypeImg = row.select(".materialtype").first()?.attr("src")?.split("/")?.last()?.removeSuffix(".png")
            SearchResult().apply {
                cover = row.select(".coverimages img").first()?.attr("src")
                id = biblionumber
                innerhtml = "<b>$title</b><br>${author ?: ""}<br>${summary ?: ""}"
                type = mediatypes[mediatypeImg]
            }
        }

        return SearchRequestResult(results, totalResults, page)
    }

    private fun searchUrl(query: List<SearchQuery>): HttpUrl.Builder {
        val builder = HttpUrl.parse("$baseurl/cgi-bin/koha/opac-search.pl")!!.newBuilder()
        for (q in query) {
            if (q.value.isBlank()) continue

            if (q.searchField is TextSearchField) {
                builder.addQueryParameter("idx", q.key)
                builder.addQueryParameter("q", q.value)
            } else if (q.searchField is DropdownSearchField) {
                builder.addQueryParameter(q.searchField.data!!.getString("id"), q.value)
            } else if (q.searchField is CheckboxSearchField) {
                if (q.value!!.toBoolean()) {
                    builder.addQueryParameter("limit", q.key)
                }
            }
        }
        return builder
    }

    private val ENCODING = "UTF-8"

    override fun parseSearchFields(): List<SearchField> {
        val doc = httpGet("$baseurl/cgi-bin/koha/opac-search.pl", ENCODING).html

        // free search field
        val freeSearchField = TextSearchField().apply {
            id = "kw,wrdl"
            displayName = "Bibliothekskatalog"
        }

        // text fields
        val options = doc.select("#search-field_0").first().select("option")
        val textFields = options.map { o ->
            TextSearchField().apply {
                id = o["value"]
                displayName = o.text.trim()
            }
        }

        // filter fields
        val fieldsets = doc.select("#advsearches fieldset")
        val tabs = doc.select("#advsearches .ui-tabs-nav li")
        val filterFields = fieldsets.zip(tabs).map { (tab, fieldset) ->
            val title = tab.text.trim()
            val checkboxes = fieldset.select("input[type=checkbox]")
            DropdownSearchField().apply {
                id = title  // input["name"] is always "limit", so we can't use it as an ID
                displayName = title
                dropdownValues = listOf(DropdownSearchField.Option("", "")) +
                        checkboxes.map { checkbox ->
                            DropdownSearchField.Option(
                                    checkbox["value"],
                                    checkbox.nextElementSibling().text.trim())
                        }
                data = JSONObject().apply {
                    put("id", checkboxes[0]["name"])
                }
            }
        }

        // dropdown fields
        val dropdowns = doc.select("legend + label + select")
        val dropdownFields = dropdowns.map { dropdown ->
            val title = dropdown.previousElementSibling().previousElementSibling().text.removeSuffix(":")
            DropdownSearchField().apply {
                id = title  // input["name"] is almost testalways "limit", so we can't use it as an ID
                displayName = title
                dropdownValues = dropdown.select("option").map { option ->
                    DropdownSearchField.Option(
                            option["value"],
                            option.text)
                }
                data = JSONObject().apply {
                    put("id", dropdown["name"])
                }
            }
        }

        // available checkbox
        val available = doc.select("#available-items").first()
        var availableCheckbox = emptyList<SearchField>()
        if (available != null) {
            availableCheckbox = listOf(
                    CheckboxSearchField().apply {
                        id = available["value"]
                        displayName = available.parent().text
                    }
            )
        }

        return listOf(freeSearchField) + textFields + filterFields + dropdownFields + availableCheckbox
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        if (searchQuery == null) {
            throw OpacApi.OpacErrorException(stringProvider.getString(StringProvider.INTERNAL_ERROR))
        }

        val builder = searchUrl(searchQuery!!)
        builder.addQueryParameter("offset", (20 * page).toString())
        val doc = httpGet(builder.build().toString(), ENCODING).html
        return parseSearch(doc, 1)
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        return DetailedItem()
    }

    override fun getResult(position: Int): DetailedItem {
        return DetailedItem()
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

        doc = Jsoup.parse(httpGet("$baseurl/cgi-bin/koha/opac-renew.pl?from=opac_user&item=$media&borrowernumber=$borrowernumber", ENCODING))
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
        val doc = Jsoup.parse(httpPost("$baseurl/cgi-bin/koha/opac-modrequest.pl", formBody.build(), ENCODING))
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

        val feesDoc = Jsoup.parse(httpGet("$baseurl/cgi-bin/koha/opac-account.pl", ENCODING))
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
        val doc = Jsoup.parse(httpPost("$baseurl/cgi-bin/koha/opac-user.pl", formBody.build(), ENCODING))
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