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
import okhttp3.HttpUrl
import org.joda.time.format.DateTimeFormat
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import java.net.URL

open class NetBiblio : OkHttpBaseApi() {
    protected lateinit var opacUrl: String
    protected val ENCODING = "UTF-8"
    protected val PAGE_SIZE = 25
    protected var lang = "en"
    protected var searchResultId: String? = null

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
        if (!initialised) start()
        searchResultId = null
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
                body.add("Request.SearchField", q.key)
                textCount ++
            }
        }
        if (textCount == 0) {
            body.add("Request.SearchTerm", "")
            body.add("Request.SearchField", "W")
            textCount ++
        }
        if (textCount == 1) {
            body.add("Request.SearchOperator", "AND")
            body.add("Request.SearchTerm", "")
            body.add("Request.SearchField", "W")
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

        body.add("Request.PageSize", "$PAGE_SIZE")

        val doc = httpPost("$opacUrl/search/extended/submit", body.build(), ENCODING).html
        return parseSearch(doc, 1)
    }

    protected open fun parseSearch(doc: Document, page: Int): SearchRequestResult {
        // save search result ID for later access
        val href = doc.select(".next-page a").first()?.attr("href")
        if (href != null) searchResultId = BaseApi.getQueryParamsFirst(href)["searchResultId"]

        val resultcountElem = doc.select(".wo-grid-meta-resultcount").first()
                ?: return SearchRequestResult(emptyList(), 0, page)
        val countText = resultcountElem.text
        val totalCount = Regex("\\d+").findAll(countText).last().value.toInt()

        val results = doc.select(".wo-grid-table tbody tr").groupBy { row ->
            // there are multiple rows for one entry. Their CSS IDs all have the same prefix
            val id = row.attr("id")
            Regex("wo-row_(\\d+)").find(id)!!.groupValues[1]
        }.map { entry ->
            val rows = entry.value
            val cols = rows[0].select("td[style=font-size: 14px;]")
            val titleCol = cols.first()
            SearchResult().apply {
                val title = titleCol.select("a").text
                val author = titleCol.ownText()
                val moreInfo = cols.drop(1).map { it.text }.joinToString(" / ")
                this.id = "noticeId=${entry.key}"
                innerhtml = "<b>$title</b><br>${author ?: ""}<br>${moreInfo ?: ""}"
                cover = rows[0].select(".wo-cover").first()?.attr("src")

                val availIcon = rows[0].select(".wo-disposability-icon").attr("src")
                status = when {
                    availIcon.endsWith("yes.png") -> SearchResult.Status.GREEN
                    availIcon.endsWith("no.png") -> SearchResult.Status.RED
                    else -> SearchResult.Status.UNKNOWN
                }
            }
        }

        return SearchRequestResult(results, totalCount, page)
    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult? {
        return null
    }

    override fun searchGetPage(page: Int): SearchRequestResult? {
        val doc = httpGet("$opacUrl/search/shortview?searchType=Extended" +
                "&searchResultId=$searchResultId&page=$page&pageSize=$PAGE_SIZE", ENCODING).html
        return parseSearch(doc, page)
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        val doc = httpGet("$opacUrl/search/notice?$id", ENCODING).html
        return parseDetail(doc, id)
    }

    protected fun parseDetail(doc: Document, id: String): DetailedItem {
        return DetailedItem().apply {
            title = doc.select(".wo-marc-title").first().text
            this.id = id
            cover = doc.select(".wo-cover").first()?.attr("src")
            details.addAll(doc.select("#lst-fullview_Details .wo-list-label").toList()
                    .associateWith { label -> label.nextElementSibling() }
                    .map {
                        entry -> Detail(entry.key.text, entry.value.text)
                    })

            val description = doc.select(
                    ".wo-list-content-no-label[style=background-color:#F3F3F3;]").text
            details.add(Detail(stringProvider.getString(StringProvider.DESCRIPTION), description))

            val medialinks = doc.select(".wo-linklist-multimedialinks .wo-link a")
            if (medialinks.size > 0) {
                val link = medialinks.first()
                if (link.attr("href").contains("multimedialinks/link?url")) {
                    val url = BaseApi.getQueryParamsFirst(link.attr("href"))["url"]
                    addDetail(Detail(link.text(), url))
                }
            }

            val copyCols = doc.select(".wo-grid-table > thead > tr > th").map { it.text.trim() }
            val df = DateTimeFormat.forPattern("dd.MM.yyyy")
            copies = doc.select(".wo-grid-table > tbody > tr").map { row ->
                Copy().apply {
                    row.select("td").zip(copyCols).forEach {(col, header) ->
                        val headers = header.split(" / ").map { it.trim() }
                        val data = col.html().split("<br>").map { it.html.text.trim() }
                        headers.zip(data).forEach {
                            (type, data) ->
                            when (type) {
                                "" -> {}
                                "Bibliothek" -> branch = data
                                "Aktueller Standort" -> location = data
                                "Signatur", "Call number", "Cote" -> shelfmark = data
                                "Verfügbarkeit", "Disposability", "Disponsibilité" -> status = data
                                "Fälligkeitsdatum", "Due date", "Date d'échéance" ->
                                    returnDate = if (data.isEmpty()) {
                                        null
                                    } else {
                                        df.parseLocalDate(data)
                                    }
                                "Anz. Res." -> reservations = data
                                "Reservieren", "Reserve", "Réserver" -> {
                                    val button = col.select("a").first()
                                    if (button != null) {
                                        resInfo = BaseApi.getQueryParamsFirst(button.attr("href"))["selectedItems"]
                                        isReservable = true
                                    }
                                }
                                "Exemplarnr", "Item number", "No d'exemplaire" -> barcode = data
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getResult(position: Int): DetailedItem? {
        return null
    }

    var reservationItemId: String? = null
    override fun reservation(item: DetailedItem, account: Account, useraction: Int,
                             selection: String?): OpacApi.ReservationResult? {
        if (useraction == 0 && selection == null) {
            // step 1: select item
            val reservableCopies = item.copies.filter { it.resInfo != null }
            when (reservableCopies.size) {
                0 -> return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR, stringProvider.getString(StringProvider.NO_COPY_RESERVABLE))
                1 -> return reservation(item, account, 1, reservableCopies.first()!!.resInfo)
                else -> {
                    val options = reservableCopies.map { copy ->
                        hashMapOf(
                                "key" to copy.resInfo,
                                "value" to "${copy.branch} ${copy.status} ${copy.returnDate}"
                        )
                    }
                    return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.SELECTION_NEEDED).apply {
                        actionIdentifier = 1
                        this.selection = options
                    }
                }
            }
        } else if (useraction == 1) {
            reservationItemId = selection
            val doc = httpGet("$opacUrl/account/makeitemreservation?selectedItems%5B0%5D=$selection", ENCODING).html
            val warning = doc.select("label:has(.wo-reservationkind[checked])").text
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.CONFIRMATION_NEEDED).apply {
                details = listOf(arrayOf(warning))
            }
        } else if (useraction == OpacApi.MultiStepResult.ACTION_CONFIRMATION) {
            val body = FormBody.Builder()
                    .add("ItemID", reservationItemId!!)
                    .add("ReservationKind", "Reservation") // TODO: maybe don't hardcode this
                    .add("AddessId" /* sic! */, "26305") //TODO: don't hardcode this
                    .build()

            val doc = httpPost("$opacUrl/account/makeitemreservation", body, ENCODING).html
            if (doc.select(".alert-success").size == 1) {
                return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.OK)
            } else {
                return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR)
            }
        } else {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR)
        }
    }

    override fun prolong(media: String, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult? {
        if (!initialised) start()
        login(account)

        val doc = httpGet("$opacUrl/account/renew?selectedItems%5B0%5D=$media", ENCODING).html
        if (doc.select(".alert-success").size == 1) {
            return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR,
                    doc.select(".alert-danger").text)
        }
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult? {
        if (!initialised) start()
        login(account)

        val lentDoc = httpGet("$opacUrl/account/circulations", ENCODING).html
        val lent = parseItems(lentDoc, ::LentItem)

        val builder = HttpUrl.parse("$opacUrl/account/renew")!!.newBuilder()
        lent.forEachIndexed { index, item ->
            if (item.prolongData != null) {
                builder.addQueryParameter("selectedItems[$index]", item.prolongData)
            }
        }
        builder.addQueryParameter("returnUrl", "${URL(opacUrl).path}/account/circulations")

        val doc = httpGet(builder.build().toString(), ENCODING).html
        if (doc.select(".alert-success").size == 1) {
            return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.ERROR,
                    doc.select(".alert-danger").text)
        }
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult? {
        if (!initialised) start()
        login(account)

        val doc = httpGet("$opacUrl/account/deletereservations?selectedItems%5B0%5D=$media", ENCODING).html
        if (doc.select(".alert-success").size == 1) {
            return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.ERROR,
                    doc.select(".alert-danger").text)
        }
    }

    override fun account(account: Account): AccountData? {
        if (!initialised) start()
        login(account)

        val overview = httpGet("$opacUrl/account", ENCODING).html
        val resDoc = httpGet("$opacUrl/account/reservations", ENCODING).html
        val lentDoc = httpGet("$opacUrl/account/circulations", ENCODING).html

        return AccountData(account.id).apply {
            val feesString = overview.select("a[href$=fees]").first()?.text
            pendingFees = if (feesString != null) {
                Regex("\\(([^\\)]+)\\)").find(feesString)?.groups?.get(1)?.value
            } else null
            validUntil = overview.select(
                    ".wo-list-label:contains(Abonnement (Ende)) + .wo-list-content, " +
                            ".wo-list-label:contains(Subscription (end)) + .wo-list-content," +
                            " .wo-list-label:contains(Abonnement (Fin)) + .wo-list-content").first()?.text
            reservations = parseItems(resDoc, ::ReservedItem)
            lent = parseItems(lentDoc, ::LentItem)
        }
    }

    internal fun <I : AccountItem> parseItems(doc: Document, constructor: () -> I): List<I> {
        val table = doc.select(".wo-grid-table").first() ?: return emptyList()
        val cols = table.select("> thead > tr > th").map { it.text.trim() }
        val rows = table.select("> tbody > tr")

        val df = DateTimeFormat.forPattern("dd.MM.yyyy")

        return rows.map { row ->
            constructor().apply {
                var renewals: Int? = null
                var reservations: Int? = null

                row.select("td").zip(cols).forEach { (col, header) ->
                    val headers = header.split(" / ").map { it.trim() }
                    val data = col.html().split("<br>").map { it.html.text.trim() }

                    headers.zip(data).forEach { (type, data) ->
                        when (type) {
                            "" -> {
                                val checkbox = col.select("input[type=checkbox]").first()
                                val coverImg = col.select(".wo-cover").first()
                                if (checkbox != null) {
                                    when {
                                        this is LentItem -> prolongData = checkbox["value"]
                                        this is ReservedItem -> cancelData = checkbox["value"]
                                    }
                                } else if (coverImg != null) {
                                    cover = coverImg["src"]
                                }
                            }
                            "Bibliothek" -> when {
                                this is LentItem -> lendingBranch = data
                                this is ReservedItem -> branch = data
                            }
                            "Autor", "Author", "Auteur" -> {
                                author = data
                                val link = col.select("a").first()
                                if (link != null) {
                                    val nr = BaseApi.getQueryParamsFirst(link["href"])["noticeNr"]
                                    id = "noticeNr=$nr"
                                }
                            }
                            "Titel", "Title", "Titre" -> {
                                title = data
                                val link = col.select("a").first()
                                if (link != null) {
                                    val nr = BaseApi.getQueryParamsFirst(link["href"])["noticeNr"]
                                    id = "noticeNr=$nr"
                                }
                            }
                            "Medienart" -> format = data
                            "Fälligkeitsdatum", "Due date", "Date d'échéance" -> when {
                                this is LentItem -> deadline = df.parseLocalDate(data)
                            }
                            "Exemplarnr.", "Item number", "No d'exemplaire" -> when {
                                this is LentItem -> barcode = data
                            }
                            "Verlängerungen", "Renewals", "Prolongations" ->
                                renewals = data.toIntOrNull()
                            "Anz. Res." -> reservations = data.toIntOrNull()
                            //"Aktueller Standort" ->
                            //"Signatur", "Call number", "Cote" ->
                            //"Bereich" ->
                            //"Jahr" ->
                            //"Datum der Verlängerung", "Renewal date", "Date de prolongation"
                        }
                    }
                }

                val renewalsText = if (renewals != null && renewals!! > 0)
                    "${renewals}x ${stringProvider.getString(StringProvider.PROLONGED_ABBR)}"
                else null
                val reservationsText = if (reservations != null && reservations!! > 0) {
                    stringProvider.getQuantityString(StringProvider.RESERVATIONS_NUMBER, reservations!!, reservations)
                } else null
                status = listOf(renewalsText, reservationsText).filter { it != null }
                        .joinToString(", ")
                if (status.isEmpty()) status = null
            }
        }
    }

    override fun checkAccountData(account: Account) {
        login(account)
    }

    private fun login(account: Account) {
        val formData = FormBody.Builder()
                .add("ReturnUrl", "${URL(opacUrl).path}/account")
                .add("Username", account.name)
                .add("Password", account.password)
                .add("SaveUsernameInCookie", "false")
                .add("StayLoggedIn", "false").build()
        val doc = httpPost("$opacUrl/account/login", formData, ENCODING).html
        if (doc.select(".alert").size > 0) {
            throw OpacApi.OpacErrorException(doc.select(".alert").first().ownText())
        }
    }

    override fun getShareUrl(id: String, title: String?): String? {
        return "$opacUrl/search/notice?$id"
    }

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING or OpacApi
                .SUPPORT_FLAG_WARN_RESERVATION_FEES or OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL
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
