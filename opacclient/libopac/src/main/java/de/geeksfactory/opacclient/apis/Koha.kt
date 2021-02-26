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
import org.joda.time.format.DateTimeFormat
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern

/**
 * OpacApi implementation for the open source Koha OPAC. https://koha-community.org/
 *
 * It includes special cases to account for changes in the "LMSCloud" variant
 * (https://www.lmscloud.de/) . Account features were only tested with LMSCloud.
 *
 * @author Johan von Forstner, November 2018
 */
open class Koha : OkHttpBaseApi() {
    protected lateinit var baseurl: String
    protected val NOT_RENEWABLE = "NOT_RENEWABLE"
    protected val ENCODING = "UTF-8"
    protected var searchQuery: List<SearchQuery>? = null

    override fun init(library: Library, factory: HttpClientFactory) {
        super.init(library, factory)
        baseurl = library.data.getString("baseurl")
    }

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        this.searchQuery = query
        val builder = searchUrl(query)
        val url = builder.build().toString()
        val doc = httpGet(url, ENCODING).html
        doc.setBaseUri(url)

        if (doc.select(".searchresults").size == 0 && doc.select(".unapi-id").size == 1) {
            // only a single result
            val item = parseDetail(doc)
            return SearchRequestResult(listOf(
                    SearchResult().apply {
                        cover = item.cover
                        id = item.id
                        type = item.mediaType

                        val author = item.details.find { it.desc == "Von" }?.content
                        innerhtml = "<b>${item.title}</b><br>${author ?: ""}"
                    }
            ), 1, 1)
        } else {
            return parseSearch(doc, 1)
        }
    }

    private val mediatypes = mapOf(
            "book" to SearchResult.MediaType.BOOK,
            "film" to SearchResult.MediaType.MOVIE,
            "sound" to SearchResult.MediaType.CD_MUSIC,
            "newspaper" to SearchResult.MediaType.MAGAZINE
    )

    protected open fun parseSearch(doc: Document, page: Int): SearchRequestResult {
        if (doc.select("#noresultsfound").first() != null
                || doc.select(".span12:contains(Keine Treffer)").first() != null) {
            return SearchRequestResult(emptyList(), 0, page)
        } else if (doc.select("select[name=idx]").size > 1) {
            // We're back on the search page, no valid (likely empty) query
            throw OpacApi.OpacErrorException(stringProvider.getString("no_criteria_input"))
        } else if (doc.select("#numresults").first() == null) {
            throw OpacApi.OpacErrorException(stringProvider.getString("unknown_error"))
        }

        val countText = doc.select("#numresults").first().text
        val totalResults = Regex("\\d+").findAll(countText).last().value.toInt()

        val results = doc.select(".searchresults table tr").map { row ->


            SearchResult().apply {
                cover = row.select(".coverimages img").first()?.attr("src")

                var titleA = row.select("a.title").first()
                if (titleA == null) {
                    titleA = row.select("a[href*=opac-detail.pl]:not(:has(img, .no-image))").first()
                }
                val biblionumber = Regex("biblionumber=([^&]+)").find(titleA["href"])!!.groupValues[1]
                id = biblionumber

                val title = titleA.ownText()
                val author = row.select(".author").first()?.text
                var summary = row.select(".results_summary").first()?.text
                if (summary != null) {
                    summary = summary.split(" | ").dropLast(1).joinToString(" | ")
                }
                innerhtml = "<b>$title</b><br>${author ?: ""}<br>${summary ?: ""}"

                val mediatypeImg = row.select(".materialtype").first()?.attr("src")?.split("/")?.last()?.removeSuffix(".png")
                type = mediatypes[mediatypeImg]

                status = if (row.select(".available").size > 0 && row.select(".unavailable").size > 0) {
                    SearchResult.Status.YELLOW
                } else if (row.select(".available").size > 0) {
                    SearchResult.Status.GREEN
                } else if (row.select(".unavailable").size > 0) {
                    SearchResult.Status.RED
                } else null
            }
        }

        return SearchRequestResult(results, totalResults, page)
    }

    private fun searchUrl(query: List<SearchQuery>): HttpUrl.Builder {
        val builder = HttpUrl.parse("$baseurl/cgi-bin/koha/opac-search.pl")!!.newBuilder()
        for (q in query) {
            if (q.value.isBlank()) continue

            if (q.searchField is TextSearchField || q.searchField is BarcodeSearchField) {
                builder.addQueryParameter("idx", q.key)
                builder.addQueryParameter("q", q.value)
            } else if (q.searchField is DropdownSearchField) {
                if (q.value.contains('=')) {
                    val parts = q.value.split("=")
                    builder.addQueryParameter(parts[0], parts[1])
                } else {
                    builder.addQueryParameter(q.searchField.data!!.getString("id"), q.value)
                }
            } else if (q.searchField is CheckboxSearchField) {
                if (q.value!!.toBoolean()) {
                    builder.addQueryParameter("limit", q.key)
                }
            }
        }
        return builder
    }

    override fun parseSearchFields(): List<SearchField> {
        val doc = httpGet("$baseurl/cgi-bin/koha/opac-search.pl", ENCODING).html

        // free search field
        val freeSearchField = TextSearchField().apply {
            id = "kw,wrdl"
            displayName = "Freitext"
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
        val tabs = doc.select("#advsearches > ul > li")
        val filterFields = fieldsets.zip(tabs).map { (fieldset, tab) ->
            val title = tab.text.trim()
            val checkboxes = fieldset.select("input[type=checkbox]")
            DropdownSearchField().apply {
                id = title  // input["name"] is always "limit", so we can't use it as an ID
                displayName = title
                dropdownValues = listOf(DropdownSearchField.Option("", "")) +
                        checkboxes.map { checkbox ->
                            DropdownSearchField.Option(
                                    checkbox["name"] + "=" + checkbox["value"],
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
        builder.addQueryParameter("offset", (20 * (page - 1)).toString())
        val url = builder.build().toString()
        val doc = httpGet(url, ENCODING).html
        doc.setBaseUri(url)
        return parseSearch(doc, 1)
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        val doc = httpGet("$baseurl/cgi-bin/koha/opac-detail.pl?biblionumber=$id", ENCODING).html
        return parseDetail(doc)
    }

    private fun parseDetail(doc: Document): DetailedItem {
        return DetailedItem().apply {
            val titleElem = doc.select("h1.title, #opacxslt h2").first()
            title = titleElem.ownText()
            this.id = doc.select(".unapi-id").first()["title"].removePrefix("koha:biblionumber:")

            if (doc.select("h5.author, span.results_summary").size > 0) {
                // LMSCloud
                doc.select("h5.author, span.results_summary").forEach { row ->
                    // remove "Read more" link
                    row.select(".truncable-txt-readmore").remove()

                    // go through the details
                    if (row.tagName() == "h5" || row.select("> span.label").size > 0) {
                        // just one detail on this line
                        val title = row.text.split(":").first().trim()
                        val value = row.text.split(":").drop(1).joinToString(":").trim()
                        if (!title.isBlank() || !value.isBlank()) addDetail(Detail(title, value))
                    } else {
                        // multiple details on this line
                        row.select("> span").forEach { span ->
                            val title = span.text.split(":").first().trim()
                            val value = span.text.split(":").drop(1).joinToString(":").trim()
                            if (!title.isBlank() || !value.isBlank()) addDetail(Detail(title, value))
                        }
                    }
                }
            } else if (doc.select("#opacxslt dt.labelxslt").size > 0) {
                // seen at https://catalogue.univ-amu.fr
                doc.select("#opacxslt dt.labelxslt").forEach { dt ->
                    val title = dt.text
                    var elem = dt
                    var value = ""
                    while (elem.nextElementSibling() != null && elem.nextElementSibling().tagName() == "dd") {
                        elem = elem.nextElementSibling()
                        if (!value.isBlank()) value += "\n"
                        value += elem.text
                    }
                    addDetail(Detail(title, value))
                }
            }

            val mediatypeImg = doc.select(".materialtype").first()?.attr("src")?.split("/")?.last()?.removeSuffix(".png")
            mediaType = mediatypes[mediatypeImg]

            cover = doc.select("#bookcover img").first()?.attr("src")

            val df = DateTimeFormat.forPattern("dd.MM.yyyy")
            copies = doc.select(".holdingst > tbody > tr").map { row ->
                Copy().apply {
                    for (td in row.select("td")) {
                        row.select(".branch-info-tooltip").remove()
                        val text = if (td.text.isNotBlank()) td.text.trim() else null
                        when {
                            // td.classNames().contains("itype") -> media type
                            td.classNames().contains("location") -> branch = text
                            td.classNames().contains("collection") -> location = text
                            td.classNames().contains("vol_info") -> issue = text
                            td.classNames().contains("call_no") -> {
                                if (td.select(".isDivibibTitle").size > 0 && td.select("a").size > 0) {
                                    // Onleihe item
                                    val link = td.select("a").first()["href"]
                                    if (link.startsWith("javascript:void")) {
                                        val onclick = td.select("a").first()["onclick"]

                                        val pattern = Pattern.compile(".*accessDivibibResource\\([^,]*,[ ]*['\"]([0-9]+)['\"][ ]*\\).*")
                                        val matcher = pattern.matcher(onclick)
                                        if (matcher.matches()) {
                                            addDetail(Detail("_onleihe_id", matcher.group(1)))
                                        }
                                    } else {
                                        addDetail(Detail("Onleihe", link))
                                    }
                                }
                                shelfmark = text?.removeSuffix("Hier klicken zum Ausleihen.")
                                        ?.removeSuffix("Hier klicken zum Reservieren.")
                            }
                            td.classNames().contains("status") -> status = text
                            td.classNames().contains("date_due") ->
                                if (text != null) {
                                    val dateText = text.removeSuffix(" 00:00") // seen with Onleihe items
                                    returnDate = df.parseLocalDate(dateText)
                                }
                            td.classNames().contains("holds_count") -> reservations = text
                        }
                    }
                }
            }

            isReservable = doc.select("a.reserve").size > 0
        }
    }

    override fun getResult(position: Int): DetailedItem? {
        // Should not be called because every media has an ID
        return null
    }

    var reservationFeeConfirmed = false
    var selectedCopy: String? = null
    val ACTION_ITEM = 101

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        try {
            login(account)
        } catch (e: OpacApi.OpacErrorException) {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR, e.message)
        }

        when (useraction) {
            0 -> {
                reservationFeeConfirmed = false
                selectedCopy = null
            }
            OpacApi.MultiStepResult.ACTION_CONFIRMATION -> reservationFeeConfirmed = true
            ACTION_ITEM -> selectedCopy = selection
        }

        var doc = httpGet("$baseurl/cgi-bin/koha/opac-reserve.pl?biblionumber=${item.id}", ENCODING).html

        if (doc.select(".alert").size > 0) {
            val alert = doc.select(".alert").first()
            val message = alert.text
            if (alert.id() != "reserve_fee") {
                return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR, message)
            }
        }

        val body = FormBody.Builder()
        body.add("place_reserve", "1")
        val reserveMode = doc.select("input[name=reserve_mode]").`val`()
        body.add("reserve_mode", reserveMode)  // can be "single" or "multi"
        body.add("single_bib", item.id)
        body.add("expiration_date_${item.id}", "")
        val checkboxes = doc.select("input[name=checkitem_${item.id}]")
        if (checkboxes.size > 0) {
            body.add("biblionumbers", "${item.id}/")
            if (checkboxes.first()["value"] == "any") {
                body.add("reqtype_${item.id}", "any")
                body.add("checkitem_${item.id}", "any")
                body.add("selecteditems", "${item.id}///")
            } else {
                if (selectedCopy != null) {
                    body.add("reqtype_${item.id}", "Specific")
                    body.add("checkitem_${item.id}", selectedCopy)
                    body.add("selecteditems", "${item.id}/$selectedCopy//")
                } else {
                    val copies = doc.select(".copiesrow tr:has(td)")
                    val activeCopies = copies.filter { row ->
                        !row.select("input").first().hasAttr("disabled")
                    }
                    if (copies.size > 0 && activeCopies.isEmpty()) {
                        return OpacApi.ReservationResult(
                                OpacApi.MultiStepResult.Status.ERROR,
                                stringProvider.getString(StringProvider.NO_COPY_RESERVABLE)
                        )
                    }
                    // copy selection
                    return OpacApi.ReservationResult(
                            OpacApi.MultiStepResult.Status.SELECTION_NEEDED,
                            doc.select(".copiesrow caption").text).apply {
                        actionIdentifier = ACTION_ITEM
                        setSelection(activeCopies.map { row ->
                            HashMap<String, String>().apply {
                                put("key", row.select("input").first()["value"])
                                put("value", "${row.select(".itype").text}\n${row.select("" +
                                        ".homebranch").text}\n${row.select(".information").text}")
                            }
                        })
                    }
                }
            }
        } else if (reserveMode == "single") {
            body.add("biblionumbers", "")
            body.add("selecteditems", "")
        } else if (doc.select(".holdrow .alert").size > 0) {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR,
                    doc.select(".holdrow .alert").text().trim())
        }

        if (!reservationFeeConfirmed && doc.select(".alert").size > 0) {
            val alert = doc.select(".alert").first()
            val message = alert.text
            if (alert.id() == "reserve_fee") {
                val res = OpacApi.ReservationResult(
                        OpacApi.MultiStepResult.Status.CONFIRMATION_NEEDED)
                res.details = arrayListOf(arrayOf(message))
                return res
            }
        }

        doc = httpPost("$baseurl/cgi-bin/koha/opac-reserve.pl", body.build(), ENCODING).html

        if (doc.select("input[type=hidden][name=biblionumber][value=${item.id}]").size > 0) {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR)
        }
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
        var doc = login(account)
        var borrowernumber = doc.select("input[name=borrowernumber]").first()?.attr("value")

        doc = Jsoup.parse(httpGet("$baseurl/cgi-bin/koha/opac-renew.pl?from=opac_user&borrowernumber=$borrowernumber", ENCODING))
        val label = doc.select(".blabel").first()
        if (label != null && label.hasClass("label-success")) {
            return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.ERROR, label?.text()
                    ?: stringProvider.getString(StringProvider.ERROR))
        }
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

        accountData.lent = parseItems(doc, ::LentItem, "#checkoutst").toMutableList()
        accountData.reservations = parseItems(doc, ::ReservedItem, "#holdst").toMutableList()

        val feesDoc = Jsoup.parse(httpGet("$baseurl/cgi-bin/koha/opac-account.pl", ENCODING))
        accountData.pendingFees = parseFees(feesDoc)

        if (doc.select(".alert").size > 0) {
            accountData.warning = doc.select(".alert").text
        }

        return accountData
    }

    internal fun parseFees(feesDoc: Document): String? {
        val text = feesDoc.select("td.sum").text()
        return if (!text.isBlank()) text else null
    }

    internal fun <I : AccountItem> parseItems(doc: Document, constructor: () -> I, id: String): List<I> {
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
                            item.status = col.select("span.renewals").text()
                        } else {
                            item.prolongData = NOT_RENEWABLE + content
                            item.isRenewable = false
                            item.status = col.text()
                        }
                        item.status = item.status.trim().replaceFirst(Regex("^\\(?(.*?)\\)?\$"), "$1")
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
        val isoformat = select.attr("title").replace(" ", "T")
        // example: <span title="2018-11-02T23:59:00">
        // or <span title="2018-11-02 23:59:00">
        if (isoformat.startsWith("0000-00-00")) {
            return null
        } else {
            return LocalDateTime(isoformat).toLocalDate()
        }
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
        if (doc.select(".alert").size > 0 && doc.select("#opac-auth").size > 0) {
            throw OpacApi.OpacErrorException(doc.select(".alert").text())
        }
        return doc
    }

    override fun getShareUrl(id: String?, title: String?): String {
        return "$baseurl/cgi-bin/koha/opac-detail.pl?biblionumber=$id"
    }

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING or OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL or
                OpacApi.SUPPORT_FLAG_WARN_RESERVATION_FEES
    }

    override fun getSupportedLanguages(): Set<String>? {
        return null
    }

    override fun setLanguage(language: String?) {

    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
