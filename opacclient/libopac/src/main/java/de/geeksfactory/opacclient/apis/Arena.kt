package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.i18n.StringProvider
import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.networking.NotReachableException
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.DropdownSearchField
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import de.geeksfactory.opacclient.utils.get
import de.geeksfactory.opacclient.utils.html
import de.geeksfactory.opacclient.utils.text
import okhttp3.FormBody
import org.joda.time.format.DateTimeFormat
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL

/**
 * OpacApi implementation for the Arena OPAC developed by Axiell. https://www.axiell.de/arena/
 *
 * @author Johan von Forstner, January 2019
 */
open class Arena : OkHttpBaseApi() {
    protected lateinit var opacUrl: String
    protected val ENCODING = "UTF-8"
    protected var searchDoc: Document? = null

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
        val searchForm = httpGet("$opacUrl/extended-search", ENCODING).html
                .select(".arena-extended-search-original").first()

        val formData = FormBody.Builder().apply {
            searchForm.select("input[type=hidden]").forEach { hidden ->
                add(hidden["name"], hidden["value"])
            }
            val submit = searchForm.select("input[type=submit]").first()
            add(submit["name"], submit["value"])
            query.forEach { q ->
                add(q.key, q.value)
            }
        }.build()
        val doc = httpPost(searchForm["action"], formData, ENCODING).html
        return parseSearch(doc)
    }

    internal fun parseSearch(doc: Document, page: Int = 1): SearchRequestResult {
        searchDoc = doc

        val countRegex = Regex("\\d+-\\d+ (?:von|of|av) (\\d+)")
        val count = countRegex.find(doc.select(".arena-record-counter").text)?.groups?.get(1)?.value?.toInt()
                ?: 0
        val coverAjaxUrls = getAjaxUrls(doc)

        val results = doc.select(".arena-record").mapIndexed { i, record ->
            SearchResult().apply {
                val title = record.select(".arena-record-title").text
                val year = record.select(".arena-record-year .arena-value").first()?.text
                val author = record.select(".arena-record-author .arena-value").map { it.text }.joinToString(", ")

                innerhtml = "<b>$title</b><br>$author ${year ?: ""}"
                id = record.select(".arena-record-id").first().text
                cover = getCover(record, coverAjaxUrls)
            }
        }
        return SearchRequestResult(results, count, page)
    }

    internal fun getCover(record: Element, coverAjaxUrls: Map<String, String>? = null): String? {
        val coverHolder = record.select(".arena-record-cover").first()
        if (coverHolder != null) {
            val id = coverHolder["id"]
            if (coverAjaxUrls != null && coverAjaxUrls.containsKey(id)) {
                // get cover via ajax
                val xml = httpGet(coverAjaxUrls[id], ENCODING)
                val url = Regex("<img src=\"([^\"]+)").find(xml)?.groups?.get(1)
                        ?.value?.replace("&amp;", "&")
                return if (url != null) URL(URL(opacUrl), url).toString() else null
            } else if (coverHolder.select("img:not([src*=indicator.gif])").size > 0) {
                // cover is included as img tag
                val url = coverHolder.select("img:not([src*=indicator.gif])").first()["src"]
                return URL(URL(opacUrl), url).toString()
            }
        }
        return null
    }

    internal fun getAjaxUrls(doc: Document): Map<String, String> {
        val regex = Regex(Regex.escape(
                "<script type=\"text/javascript\">Wicket.Event.add(window,\"domready\",function(b){var a=wicketAjaxGet(\"")
                + "([^\"]+)"
                + Regex.escape("\",function(){}.bind(this),function(){}.bind(this),function(){return Wicket.\$(\"")
                + "([^\"]+)"
                + Regex.escape("\")!=null}.bind(this))});</script>"))
        return regex.findAll(doc.html()).associate { match ->
            Pair(match.groups[2]!!.value, match.groups[1]!!.value)
        }
    }

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        val doc = searchDoc ?: throw NotReachableException()
        val pageLinks = doc.select(".arena-record-navigation").first()
                .select(".arena-page-number > a, .arena-page-number > span")

        // determining the link to get to the right page is not straightforward, so we try to find
        // the link to the right page.
        val from = Integer.valueOf(pageLinks.first().text())
        val to = Integer.valueOf(pageLinks.last().text())
        val linkToClick: Element
        val willBeCorrectPage: Boolean

        if (page < from) {
            linkToClick = pageLinks.first()
            willBeCorrectPage = false
        } else if (page > to) {
            linkToClick = pageLinks.last()
            willBeCorrectPage = false
        } else {
            linkToClick = pageLinks.get(page - from)
            willBeCorrectPage = true
        }

        if (linkToClick.tagName() == "span") {
            // we are trying to get the page we are already on
            return parseSearch(doc, page)
        }

        val newDoc = httpGet(linkToClick["href"], ENCODING).html
        if (willBeCorrectPage) {
            return parseSearch(newDoc, page)
        } else {
            searchDoc = newDoc
            return searchGetPage(page)
        }
    }

    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        val url = getUrl(id)
        val doc = httpGet(url, ENCODING).html
        doc.setBaseUri(url)
        return parseDetail(doc)
    }

    private fun parseDetail(doc: Document): DetailedItem {
        val urls = getAjaxUrls(doc).filterKeys { it.contains("crDetailWicket") }
        val holdingsUrl = if (urls.isNotEmpty()) urls.values.iterator().next() else null
        val holdingsDoc = if (holdingsUrl != null) {
            val ajaxResp = httpGet(URL(URL(opacUrl), holdingsUrl).toString(), ENCODING).html
            ajaxResp.select("component").first().text.html
        } else null

        return DetailedItem().apply {
            title = doc.select(".arena-detail-title").text

            doc.select(".arena-catalogue-detail .arena-field").forEach { field ->
                addDetail(Detail(field.text, field.nextElementSibling().text))
            }
            doc.select(".arena-detail-link > a").forEach { link ->
                val href = link["href"]
                if (href.contains("onleihe") && href.contains("mediaInfo")) {
                    addDetail(Detail(link.text(), href));
                }
            }

            id = doc.select(".arena-record-id").first().text
            cover = doc.select(".arena-detail-cover img").first()?.absUrl("href")
            copies = holdingsDoc?.select(".arena-row")?.map { row ->
                Copy().apply {
                    department = row.select(".arena-holding-department .arena-value").text
                    shelfmark = row.select(".arena-holding-shelf-mark .arena-value").text
                    status = row.select(".arena-availability-right").text
                }
            } ?: emptyList()
            isReservable = doc.select(".arena-reservation-button-login, a[href*=reservationButton]").first() != null
            reservation_info = if (isReservable) id else null
        }
    }

    override fun getResult(position: Int): DetailedItem? {
        return null
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        login(account)
        val details = httpGet(getUrl(item.id), ENCODING).html
        val url = details.select(" a[href*=reservationButton]").first()?.attr("href")
        val doc = httpGet(url, ENCODING).html
        val form = doc.select("form[action*=reservationForm]").first()
        if (selection == null) {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.SELECTION_NEEDED).apply {
                actionIdentifier = OpacApi.ReservationResult.ACTION_BRANCH
                this.selection = form.select(".arena-select").first().select("option").map { option ->
                    hashMapOf(
                            "key" to option["value"],
                            "value" to option.text
                    )
                }
            }
        }

        val formData = FormBody.Builder()
        form.select("input[type=hidden]").forEach { input ->
            formData.add(input["name"], input["value"])
        }
        formData.add("branch", selection)
        val resultDoc = httpPost(form["action"], formData.build(), ENCODING).html

        val errorPanel = resultDoc.select(".feedbackPanelWARNING").first()
        if (errorPanel != null) {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR, errorPanel.text)
        } else {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.OK)
        }
    }

    override fun prolong(media: String, account: Account, useraction: Int, selection: String?): OpacApi.ProlongResult {
        login(account)
        val loansDoc = httpGet("$opacUrl/protected/loans", ENCODING).html
        val internalError = OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR, stringProvider.getString(StringProvider.INTERNAL_ERROR))

        val row = loansDoc.select("tr:has(.arena-record-id:contains($media)").first()
                ?: return internalError

        val js = row.select(".arena-renewal-status input[type=submit]").first()["onclick"]
        val url = Regex("window\\.location\\.href='([^']+)'").find(js)?.groups?.get(1)?.value
                ?: return internalError

        val doc = httpGet(url, ENCODING).html
        val error = doc.select(".arena-internal-error-description-value").first()
        return if (error != null) {
            OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR, error.text)
        } else if (doc.select(".arena-renewal-fail-table").size > 0) {
            OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.ERROR, stringProvider.getString(StringProvider.PROLONGING_IMPOSSIBLE))
        } else {
            OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.OK)
        }
    }

    override fun prolongAll(account: Account, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        // it seems that this does not work without JavaScript, and I couldn't find out why.
        return OpacApi.ProlongAllResult(OpacApi.MultiStepResult.Status.ERROR)
    }

    override fun cancel(media: String, account: Account, useraction: Int, selection: String?): OpacApi.CancelResult {
        val resDoc = httpGet("$opacUrl/protected/reservations", ENCODING).html
        val internalError = OpacApi.CancelResult(OpacApi.MultiStepResult.Status.ERROR,
                stringProvider.getString(StringProvider.INTERNAL_ERROR))

        val record = resDoc.select(".arena-record-container:has(.arena-record-id:contains($media)")
                .first() ?: return internalError
        // find the URL that needs to be called to select the item
        val url = record.select(".arena-select-item a").first()?.attr("href")

        val selectedDoc = httpGet(url, ENCODING).html
        val cancelUrl = selectedDoc.select(".arena-delete").first().attr("href")
        val resultDoc = httpGet(cancelUrl, ENCODING).html
        val errorPanel = resultDoc.select(".feedbackPanelWARNING").first()
        if (errorPanel != null) {
            return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.ERROR, errorPanel.text)
        } else {
            return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.OK)
        }
    }

    override fun account(account: Account): AccountData {
        login(account)

        val profileDoc = httpGet("$opacUrl/protected/profile", ENCODING).html
        val feesDoc = httpGet("$opacUrl/protected/debts", ENCODING).html
        val loansDoc = httpGet("$opacUrl/protected/loans", ENCODING).html
        val reservationsDoc = httpGet("$opacUrl/protected/reservations", ENCODING).html

        return AccountData(account.id).apply {
            pendingFees = parseFees(feesDoc)
            lent = parseLent(loansDoc)
            reservations = parseReservations(reservationsDoc)
            //validUntil = parseValidUntil(profileDoc)
        }
    }

    internal fun parseReservations(doc: Document): List<ReservedItem> {
        return doc.select(".arena-record").map {  record ->
            ReservedItem().apply {
                id = record.select(".arena-record-id").first().text
                title = record.select(".arena-record-title").first()?.text
                author = record.select(".arena-record-author .arena-value")
                        .map { it.text }.joinToString(", ")
                format = record.select(".arena-record-media .arena-value").first()?.text
                cover = getCover(record)
                branch = record.select(".arena-record-branch .arena-value").first()?.text
                status = record.select(".arena-result-info .arena-value").first()?.text
                cancelData = id
            }
        }
    }

    internal fun parseLent(doc: Document): List<LentItem> {
        val df = DateTimeFormat.forPattern("dd.MM.yyyy")
        return doc.select("#loansTable > tbody > tr").map {  tr ->
            LentItem().apply {
                id = tr.select(".arena-record-id").first().text
                title = tr.select(".arena-record-title").first()?.text
                author = tr.select(".arena-record-author .arena-value")
                        .map { it.text }.joinToString(", ")
                format = tr.select(".arena-record-media .arena-value").first()?.text
                lendingBranch = tr.select(".arena-renewal-branch .arena-value").first()?.text
                deadline = df.parseLocalDate(tr.select(".arena-renewal-date-value").first().text)
                isRenewable = tr.hasClass("arena-renewal-true")
                prolongData = id
                status = tr.select(".arena-renewal-status-message").first()?.text
            }
        }
    }

    internal fun parseFees(feesDoc: Document): String? {
        return feesDoc.select(".arena-charges-total-debt span:eq(1)").first()?.text
    }

    /*internal fun parseValidUntil(profileDoc: Document): String? {
        return profileDoc.select(".arena-charges-total-debt span:eq(2)").first()?.text
    }*/

    override fun checkAccountData(account: Account) {
        login(account)
    }

    fun login(account: Account) {
        val loginForm = httpGet("$opacUrl/welcome", ENCODING).html
                .select(".arena-patron-signin form").first() ?: return
        val formData = FormBody.Builder()
                .add("openTextUsernameContainer:openTextUsername", account.name)
                .add("textPassword", account.password)
        loginForm.select("input[type=hidden]").forEach { input ->
            formData.add(input["name"], input["value"])
        }

        val doc = httpPost(loginForm["action"], formData.build(), ENCODING).html
        val errorPanel = doc.select(".feedbackPanelWARNING").first()
        if (errorPanel != null) {
            throw OpacApi.OpacErrorException(errorPanel.text)
        }
    }

    override fun getShareUrl(id: String, title: String?): String {
        return getUrl(id)
    }

    private fun getUrl(id: String) =
            "$opacUrl/results?p_p_id=searchResult_WAR_arenaportlets&p_r_p_687834046_search_item_id=$id"

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING
    }

    override fun getSupportedLanguages(): MutableSet<String>? {
        return null
    }

    override fun setLanguage(language: String) {

    }
}