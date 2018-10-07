package de.geeksfactory.opacclient.apis

import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import de.geeksfactory.opacclient.i18n.StringProvider
import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.Account
import de.geeksfactory.opacclient.objects.AccountData
import de.geeksfactory.opacclient.objects.Detail
import de.geeksfactory.opacclient.objects.DetailedItem
import de.geeksfactory.opacclient.objects.Filter
import de.geeksfactory.opacclient.objects.Filter.Option
import de.geeksfactory.opacclient.objects.Library
import de.geeksfactory.opacclient.objects.SearchRequestResult
import de.geeksfactory.opacclient.objects.SearchResult
import de.geeksfactory.opacclient.objects.SearchResult.MediaType
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import de.geeksfactory.opacclient.utils.ISBNTools

class SRU : ApacheBaseApi(), OpacApi {

    protected lateinit var opac_url: String
    protected lateinit var data: JSONObject
    protected val resultcount = 10
    protected var shareUrl: String? = null
    private var currentSearchParams: String? = null
    private var searchDoc: Document? = null

    override fun init(lib: Library, httpClientFactory: HttpClientFactory) {
        super.init(lib, httpClientFactory)
        this.data = lib.data

        try {
            this.opac_url = data.getString("baseurl")
            if (data.has("sharelink")) {
                shareUrl = data.getString("sharelink")
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    @Throws(IOException::class, OpacApi.OpacErrorException::class)
    override fun search(queryList: List<SearchQuery>): SearchRequestResult {
        val params = StringBuilder()

        var index = 0
        start()

        queryList.forEach { sq ->
            if (!sq.value.isBlank()) {
                if (index != 0) {
                    params.append("%20and%20")
                }
                params.append(sq.key).append("%3D").append(sq.value)
                index += 1
            }
        }

        if (index == 0) {
            throw OpacApi.OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT))
        }
        currentSearchParams = params.toString()
        val xml = httpGet(opac_url
                + "?version=1.1&operation=searchRetrieve&maximumRecords="
                + resultcount
                + "&recordSchema=mods&sortKeys=relevance,,1&query="
                + currentSearchParams, defaultEncoding)

        return parseResult(xml)
    }

    @Throws(OpacApi.OpacErrorException::class)
    private fun parseResult(xml: String): SearchRequestResult {
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        searchDoc = doc
        if (doc.select("diag|diagnostic").size > 0) {
            throw OpacApi.OpacErrorException(doc.select("diag|message").text())
        }

        val resultcount: Int
        val results = ArrayList<SearchResult>()

        resultcount = Integer.valueOf(doc.select("zs|numberOfRecords").text())

        val records = doc.select("zs|records > zs|record")
        var i = 0
        for (record in records) {
            val sr = SearchResult()
            val title = getDetail(record, "titleInfo title")
            val firstName = getDetail(record, "name > namePart[type=given]")
            val lastName = getDetail(record, "name > namePart[type=family]")
            val year = getDetail(record, "dateIssued")
            val mType = getDetail(record, "physicalDescription > form")
            val isbn = getDetail(record, "identifier[type=isbn]")
            val coverUrl = getDetail(record, "url[displayLabel=C Cover]")
            val additionalInfo = "$firstName $lastName, $year"
            sr.innerhtml = "<b>$title</b><br>$additionalInfo"
            sr.type = defaulttypes[mType]
            sr.nr = i
            sr.id = getDetail(record, "recordIdentifier")
            if (coverUrl == "") {
                sr.cover = ISBNTools.getAmazonCoverURL(isbn, false)
            } else {
                sr.cover = coverUrl
            }
            results.add(sr)
            i++
        }

        return SearchRequestResult(results, resultcount, 1)
    }

    private fun getDetail(record: Element, selector: String): String {
        return if (record.select(selector).size > 0) {
            record.select(selector).first().text()
        } else {
            ""
        }
    }

    @Throws(IOException::class)
    override fun filterResults(filter: Filter, option: Option): SearchRequestResult? {
        return null
    }

    @Throws(IOException::class, OpacApi.OpacErrorException::class)
    override fun searchGetPage(page: Int): SearchRequestResult {
        if (!initialised) {
            start()
        }

        val xml = httpGet(opac_url
                + "?version=1.1&operation=searchRetrieve&maximumRecords="
                + resultcount
                + "&recordSchema=mods&sortKeys=relevance,,1&startRecord="
                + (page * resultcount + 1).toString() + "&query="
                + currentSearchParams, defaultEncoding)
        return parseResult(xml)
    }

    @Throws(IOException::class, OpacApi.OpacErrorException::class)
    override fun getResultById(id: String, homebranch: String?): DetailedItem {
        val idSearchQuery = data.optString("idSearchQuery", "pica:ppn")

        val xml = httpGet(opac_url
                + "?version=1.1&operation=searchRetrieve&maximumRecords="
                + resultcount
                + "&recordSchema=mods&sortKeys=relevance,,1&query="
                + idSearchQuery + "%3D" + id, defaultEncoding)
        searchDoc = Jsoup.parse(xml, "", Parser.xmlParser())
        if (searchDoc!!.select("diag|diagnostic").size > 0) {
            throw OpacApi.OpacErrorException(searchDoc!!.select("diag|message")
                    .text())
        }
        if (searchDoc!!.select("zs|record").size != 1) { // should not
            // happen
            throw OpacApi.OpacErrorException(
                    stringProvider.getString(StringProvider.INTERNAL_ERROR))
        }
        return parseDetail(searchDoc!!.select("zs|record").first())
    }

    private fun parseDetail(record: Element): DetailedItem {
        val title = getDetail(record, "titleInfo title")
        val firstName = getDetail(record, "name > namePart[type=given]")
        val lastName = getDetail(record, "name > namePart[type=family]")
        val year = getDetail(record, "dateIssued")
        val desc = getDetail(record, "abstract")
        val isbn = getDetail(record, "identifier[type=isbn]")
        val coverUrl = getDetail(record, "url[displayLabel=C Cover]")

        val item = DetailedItem()
        item.title = title
        item.addDetail(Detail("Autor", "$firstName $lastName"))
        item.addDetail(Detail("Jahr", year))
        item.addDetail(Detail("Beschreibung", desc))
        if (coverUrl == "" && isbn.length > 0) {
            item.cover = ISBNTools.getAmazonCoverURL(isbn, true)
        } else if (coverUrl != "") {
            item.cover = coverUrl
        }

        if (isbn.length > 0) {
            item.addDetail(Detail("ISBN", isbn))
        }

        return item
    }

    @Throws(IOException::class, OpacApi.OpacErrorException::class)
    override fun getResult(position: Int): DetailedItem {
        return parseDetail(searchDoc!!.select("zs|records > zs|record")[position])
    }

    @Throws(IOException::class)
    override fun reservation(item: DetailedItem, account: Account,
                             useraction: Int, selection: String): OpacApi.ReservationResult? {
        return null
    }

    @Throws(IOException::class)
    override fun prolong(media: String, account: Account, useraction: Int,
                         selection: String): OpacApi.ProlongResult? {
        return null
    }

    @Throws(IOException::class)
    override fun prolongAll(account: Account, useraction: Int,
                            selection: String): OpacApi.ProlongAllResult? {
        return null
    }

    @Throws(IOException::class, JSONException::class, OpacApi.OpacErrorException::class)
    override fun account(account: Account): AccountData? {
        return null
    }

    @Throws(OpacApi.OpacErrorException::class, IOException::class)
    override fun parseSearchFields(): List<SearchField> {
        val xml = httpGet(opac_url, defaultEncoding)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val indices = doc.select("zs|recordData explain indexInfo index")
        return indices.map { index ->
            val title = index.select("> title").text()
            val idElem = index.select("map name[set]").first()
            val fieldId = "${idElem.attr("set")}:${idElem.text()}"

            val field = TextSearchField().apply {
                id = fieldId
                displayName = title
            }

            field
        }
    }

    override fun getShareUrl(id: String, title: String): String? {
        return if (shareUrl != null) {
            String.format(shareUrl!!, id)
        } else {
            null
        }
    }

    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING
    }

    @Throws(IOException::class, OpacApi.OpacErrorException::class)
    override fun cancel(media: String, account: Account, useraction: Int,
                        selection: String): OpacApi.CancelResult? {
        return null
    }

    override fun getDefaultEncoding(): String {
        return "UTF-8"
    }

    @Throws(IOException::class, JSONException::class, OpacApi.OpacErrorException::class)
    override fun checkAccountData(account: Account) {
        // TODO Auto-generated method stub

    }

    override fun setLanguage(language: String) {
        // TODO Auto-generated method stub

    }

    @Throws(IOException::class)
    override fun getSupportedLanguages(): Set<String>? {
        // TODO Auto-generated method stub
        return null
    }

    companion object {

        protected var defaulttypes = HashMap<String, MediaType>()

        init {
            defaulttypes["print"] = MediaType.BOOK
            defaulttypes["large print"] = MediaType.BOOK
            defaulttypes["braille"] = MediaType.UNKNOWN
            defaulttypes["electronic"] = MediaType.EBOOK
            defaulttypes["microfiche"] = MediaType.UNKNOWN
            defaulttypes["microfilm"] = MediaType.UNKNOWN
            defaulttypes["Tontraeger"] = MediaType.AUDIOBOOK
        }
    }

}
