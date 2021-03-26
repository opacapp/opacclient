/*
 * Copyright (C) 2021 by Steffen Rehberg under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.DropdownSearchField
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import de.geeksfactory.opacclient.utils.html
import okhttp3.FormBody
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * OpacApi implementation for BIB2
 *
 * @author Steffen Rehberg, Mar 2021
 */
open class BIB2 : OkHttpSearchOnlyApi() {
    protected lateinit var baseurl: String
    protected val ENCODING = "ISO-8859-1"
    internal var queryUrl: String? = null
    internal var missingItemsCount: Int = 0

    private val mediaTypes = mapOf(
            "Datenträger" to SearchResult.MediaType.CD,
            "Ordner" to SearchResult.MediaType.PACKAGE,
            "Sammelmappe" to SearchResult.MediaType.PACKAGE,
            "Karte" to SearchResult.MediaType.MAP,
            "Film" to SearchResult.MediaType.MOVIE,
            "Zeitschrift" to SearchResult.MediaType.MAGAZINE,
            "Foto" to SearchResult.MediaType.ART
    )

    override fun init(library: Library, factory: HttpClientFactory) {
        super.init(library, factory)
        baseurl = library.data.getString("baseurl")
    }

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        val formBodyBuilder = FormBody.Builder()
                .add("KEY", "")
        for (sq in query) {
            if (sq.value.isNotBlank()) {
                formBodyBuilder.add(sq.key, sq.value)
            }
        }
        val formBody = formBodyBuilder.build()
        val doc = httpPost("$baseurl/list", formBody, ENCODING).html
        doc.select("form span#bred").run {
            if (size > 0) throw OpacApi.OpacErrorException(get(0).text())
        }
        queryUrl = doc.select("a:containsOwn(Weiter)").run {
            if (size > 0) attr("href").replace("C108=1", "C108=<PAGE_NUMBER>") else null
        }
        missingItemsCount = 0
        return parseSearchResults(doc, 0)
    }

    override fun searchGetPage(page: Int): SearchRequestResult {
        val bib2Page = page - 1      // in the url of following pages, page n is referred to as page n-1
        val doc = httpGet("$baseurl/$queryUrl".replace("<PAGE_NUMBER>", bib2Page.toString()), ENCODING).html
        return parseSearchResults(doc, bib2Page)
    }

    internal fun parseSearchResults(doc: Document, page: Int): SearchRequestResult {
        val totalResultCount = doc.select("table tr:eq(2) b").run {
            if (size > 0) text().toInt() else 0
        }
        val searchResults = mutableListOf<SearchResult>()
        if (totalResultCount > 0) {
            for (script in doc.select("table script")) {
                val sr = parseSearchResult(script.data())
                if (sr != null) {
                    searchResults.add(sr)
                } else {
                    missingItemsCount++
                }
            }
        }
        return SearchRequestResult(searchResults, totalResultCount - missingItemsCount, page)
    }

    private fun parseSearchResult(script: String): SearchResult? {
        fun Element.getTextLine(i: Int): String =
                Jsoup.parse(this.html().replace("<br>", "$$$")).text().split("$$$")[i]

        val cols = Regex("""document.writeln\('(.*)'\);""")
                .findAll(script)
                .joinToString(" ", "<table>", "</table>") { it.groupValues[1] }
                .html
                .select("td")
        val (available, reference, total) = cols[4].text().split('/').map { it.trim().toInt() }
        return if (total > 0) {
            SearchResult().apply {
                cols[1].getTextLine(1).run {
                    if (isNotEmpty()) type = mediaTypes.getOrElse(this) { SearchResult.MediaType.BOOK }
                }
                id = Regex("""C102=\d+""").find(cols[2].child(0).attr("href"))!!.value.substring(5)
                innerhtml = "<b>${cols[2].child(0).text()}</b>"
                cols[if (SearchResult.MediaType.MAGAZINE.equals(type)) 1 else 3].getTextLine(0).run {
                    if (isNotEmpty()) innerhtml += "<br>$this" // if magazine then shelf mark (--> includes year) else author
                }
                status = when {
                    available > 0 -> SearchResult.Status.GREEN
                    reference > 0 -> SearchResult.Status.YELLOW
                    else -> SearchResult.Status.RED
                }
            }
        } else {
            null
        }
    }

    override fun getResultById(id: String?, homebranch: String?): DetailedItem {
        val doc = httpGet("$baseurl/detail?C102=$id", ENCODING).html
        val script = doc.select("script").last().data()
        val rows = Regex("""document.write\('(.*)'\);""")
                .findAll(script)
                .drop(1)
                .joinToString(" ", "<table>", "</table>") { it.groupValues[1] }
                .html
                .select("body>table>tbody>tr")
        val di = DetailedItem()
        rows.dropLast(3)       // drop lines for cover, availability summary and copies table
                .filter { it.children().size == 2 && it.child(1).hasText() }
                .forEach {
                    val desc = it.child(0).text()
                    val content = it.child(1)
                            .html()
                            .replace("<br>", "$$$")
                            .html
                            .text()
                            .replace("$$$", "\n")
                    di.addDetail(Detail(desc, content))
                    if (desc == "Haupttitel:") di.title = content
                }
        di.cover = "$baseurl/${rows.select("img").attr("src")}"
        downloadCover(di)
        with(di.coverBitmap) {
            if (take(3).equals(listOf<Byte>(71, 73, 70)) && get(9).toInt() == 0) {
                // GIF Image less than 16 Pixels high is "(kein Bild)" image --> remove it
                di.cover = null
                di.coverBitmap = null
            }
        }
        val dateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")
        rows.last().select("tbody>tr")
                .drop(1)
                .forEach {
                    di.addCopy(Copy().apply {
                        shelfmark = it.child(0).text()
                        issue = it.child(1).text()
                        location = it.child(2).text()
                        it.child(3).text().run {
                            if (equals("nein")) status = "nicht ausleihbar"
                        }
                        it.child(4).text().run {
                            if (isNotEmpty()) returnDate = dateFormat.parseLocalDate(this)
                        }
                    })
                }
        return di
    }

    override fun getResult(position: Int): DetailedItem? = null

    override fun getShareUrl(id: String?, title: String?): String = "$baseurl/detail?C102=$id"

    override fun filterResults(filter: Filter, option: Filter.Option): SearchRequestResult? = null

    override fun getSupportFlags(): Int = OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING

    override fun getSupportedLanguages(): Set<String>? = null

    override fun setLanguage(language: String?) {}

    override fun parseSearchFields(): List<SearchField> {
        val doc = httpGet(baseurl, ENCODING).html
        val textFields = doc.select("form[name=\"OPACFORM\"] table table:lt(2) tr:has(input)").map {
            TextSearchField().apply {
                displayName = it.select("td:eq(0)").text().also {
                    if (it.equals("Stichwort")) {
                        isFreeSearch = true
                        isAdvanced = false
                    } else {
                        isAdvanced = true
                    }
                }
                id = it.select("td:eq(1) input").attr("name")
            }
        }
        val dropDownFields = doc.select("form[name=\"OPACFORM\"] table table:lt(2) tr:has(select)").map {
            DropdownSearchField().apply {
                displayName = it.select("td:eq(0)").text().also {
                    isAdvanced = !it.equals("Medienart")
                }
                id = it.select("td:eq(1) select").attr("name")
                dropdownValues = it.select("td:eq(1) select option").map {
                    DropdownSearchField.Option(it.attr("value"), it.text())
                }
            }
        }
        return textFields + dropDownFields
    }
}
