package de.geeksfactory.opacclient.apis

import com.shazam.shazamcrest.matcher.Matchers.sameBeanAs
import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.DropdownSearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import de.geeksfactory.opacclient.utils.html
import okhttp3.FormBody
import okhttp3.RequestBody
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hamcrest.beans.HasPropertyWithValue.hasProperty
import org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs
import org.joda.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer

class BIB2Test : BaseHtmlTest() {
    private val api = spy(BIB2::class.java)

    init {
        api.init(Library().apply {
            data = JSONObject().apply {
                put("baseurl", "bib2.test")
            }
        }, HttpClientFactory("test"))
    }

    @Test
    fun parseSearchFields() {
        val html = readResource("/bib2/BIB2 WebOPAC.htm", "ISO-8859-1")
        doReturn(html).`when`(api).httpGet(any(), any())

        val searchFields = api.parseSearchFields()
        assertEquals(4, searchFields.filterIsInstance(TextSearchField::class.java).size)
        assertEquals(4, searchFields.filterIsInstance(DropdownSearchField::class.java).size)
    }

    @Test
    fun searchSeachWithCorrectParameters(){
        val html = readResource("/bib2/searchresult_single.htm", "ISO-8859-1")
        val query = listOf(
                SearchQuery(TextSearchField().apply { id = ":Wort+F1[0]" }, "abc"),
                SearchQuery(TextSearchField().apply { id = "F2" }, ""))
        val requestBody = FormBody.Builder()
                .add("KEY", "")
                .add(":Wort+F1[0]", "abc")
                .build() as RequestBody
        val requestBodyCaptor = ArgumentCaptor.forClass(RequestBody::class.java)
        doReturn(html).`when`(api).httpPost(eq("bib2.test/list"), requestBodyCaptor.capture(), eq("ISO-8859-1"))
        api.search(query)
        assertThat( requestBodyCaptor.value, sameBeanAs(requestBody))
    }

    @Test
    fun searchSinglePageResult() {
        val html = readResource("/bib2/searchresult_single.htm", "ISO-8859-1")
        doReturn(html).`when`(api).httpPost(any(), any(), any())
        api.queryUrl = "xyz"
        api.missingItemsCount = 42

        api.search(emptyList())
        assertNull(api.queryUrl)
        assertEquals(0, api.missingItemsCount)
    }

    @Test
    fun searchMultiPageResult() {
        val html = readResource("/bib2/searchresult_multi_page1.htm", "ISO-8859-1")
        doReturn(html).`when`(api).httpPost(any(), any(), any())
        api.queryUrl = "xyz"
        api.missingItemsCount = 42

        api.search(emptyList())
        assertEquals("list?F001=3&C106=F003&C101=196641492519323672.0&C109=1969&C105=AND&F023=16&C108=<PAGE_NUMBER>", api.queryUrl)
        assertEquals(4, api.missingItemsCount)
    }

    @Test
    fun searchError() {
        val html = readResource("/bib2/searchresult_error.htm", "ISO-8859-1")
        doReturn(html).`when`(api).httpPost(any(), any(), any())

        val thrown = assertThrows(OpacApi.OpacErrorException::class.java) { api.search(emptyList()) }
        assertTrue(thrown!!.message!!.equals("Bitte geben Sie wenigstens ein Suchkriterium ein."))
    }

    @Test
    fun searchGetPage() {
        val html = readResource("/bib2/searchresult_multi_page1.htm", "ISO-8859-1")
        doReturn(html).`when`(api).httpGet(any(), any())
        api.queryUrl = "list?F001=3&C106=F003&C101=196641492519323672.0&C109=1969&C105=AND&F023=16&C108=<PAGE_NUMBER>"
        val actual = api.searchGetPage(42)
        assertEquals(41, actual.page_index)
        verify(api).httpGet(contains("C108=41"), eq("ISO-8859-1"))
    }

    @Test
    fun parseSearchResultsNothingFound() {
        val doc = readResource("/bib2/searchresult_nothing_found.htm", "ISO-8859-1").html
        val actual = api.parseSearchResults(doc, 0)

        assertEquals(0, actual.total_result_count)
        assertEquals(0, actual.results.size)
        assertEquals(0, actual.page_index)
    }

    @Test
    fun parseSearchResultsMultiPage() {
        val doc = readResource("/bib2/searchresult_multi_page1.htm", "ISO-8859-1").html
        val actual = api.parseSearchResults(doc, 42)
        val searchResult1 = SearchResult().apply {
            innerhtml = "<b>107 Via Ferrata, 107 Klettersteige</b><br>Papandreou, Gerard"
            type = SearchResult.MediaType.BOOK
            id = "3937"
            status = SearchResult.Status.GREEN
        }
        val searchResult2 = SearchResult().apply {
            innerhtml = "<b>7x7 Genussklettersteige</b>"
            type = SearchResult.MediaType.MAP
            id = "4003"
            status = SearchResult.Status.YELLOW
        }
        val searchResult3 = SearchResult().apply {
            innerhtml = "<b>Allgäu</b><br>Z13-08/1988"
            type = SearchResult.MediaType.MAGAZINE
            id = "1690"
            status = SearchResult.Status.RED
        }

        assertEquals(69, actual.total_result_count)
        assertEquals(46, actual.results.size)
        assertEquals(42, actual.page_index)
        assertThat(searchResult1, samePropertyValuesAs(actual.results[0]))
        assertThat(searchResult2, samePropertyValuesAs(actual.results[1]))
        assertThat(searchResult3, samePropertyValuesAs(actual.results[2]))
        // should not contain missing item # 9:
        assertThat(actual.results, not(hasItem(hasProperty("id", `is`("3324")))))
    }

    @Test
    fun getResultByIdWithCoverImage() {
        val html = readResource("/bib2/detail.htm", "ISO-8859-1")
        doReturn(html).`when`(api).httpGet(any(), any())
        val img = javaClass.getResourceAsStream("/bib2/title_picture_25221").readBytes()
        doAnswer(Answer<Unit> {
            (it.arguments[0] as DetailedItem).coverBitmap = img
        }).`when`(api).downloadCover(Mockito.any(DetailedItem::class.java))
        val expected = DetailedItem().apply {
            addDetail(Detail("Aufnahmedatum:", "03.09.2019"))
            addDetail(Detail("Medienart:", "Führer"))
            addDetail(Detail("Sachgruppe:", "Kletterführer"))
            addDetail(Detail("Signatur:", "F2799A"))
            addDetail(Detail("Haupttitel:", "Kletterführer Frankenjura Band 2 / Deutschland"))
            addDetail(Detail("Untertitel:", "Kletterführer"))
            addDetail(Detail("Autor:", "Schwertner, Sebastian"))
            addDetail(Detail("Ausgabe:", "11. Ausgabe"))
            addDetail(Detail("Erscheinungsjahr:", "2018"))
            addDetail(Detail("Verlag:", "Panico Alpinverlagag, Köngen"))
            addDetail(Detail("Schlagwörter:", "/Bergsteigen; /Bergsteigen/Klettern; /Jahr; /Jahr/1990 ...;"))
            addDetail(Detail("Kurzbeschreibung:", "Zeile1\n\nZeile2"))
            title = "Kletterführer Frankenjura Band 2 / Deutschland"
            cover = "bib2.test/title_picture_25221"
            coverBitmap = img
            copies = listOf(
                    Copy().apply {
                        shelfmark = "F2799A/1"
                        issue = "2018"
                        location = "Führer/Deutschland"
                        status = "nicht ausleihbar"
                    },
                    Copy().apply {
                        shelfmark = "F2799A/2"
                        issue = ""
                        location = "Führer/Deutschland"
                        returnDate = LocalDate("2021-04-08")
                    }
            )
        }
        val actual = api.getResultById("42", null)
        assertThat(actual, sameBeanAs(expected))
        verify(api).httpGet("bib2.test/detail?C102=42", "ISO-8859-1")
    }

    @Test
    fun getResultByIdWithoutCoverImage() {
        val html = readResource("/bib2/detail_no_cover.htm", "ISO-8859-1")
        doReturn(html).`when`(api).httpGet(any(), any())
        val img = javaClass.getResourceAsStream("/bib2/title_picture_7502").readBytes()
        doAnswer(Answer<Unit> {
            (it.arguments[0] as DetailedItem).coverBitmap = img
        }).`when`(api).downloadCover(Mockito.any(DetailedItem::class.java))
        val expected = DetailedItem().apply {
            addDetail(Detail("Aufnahmedatum:", "01.01.2019"))
            addDetail(Detail("Signatur:", "X123"))
            addDetail(Detail("Haupttitel:", "Haupttitel"))
            title = "Haupttitel"
            copies = listOf(
                    Copy().apply {
                        shelfmark = "X123/1"
                        issue = ""
                        location = ""
                    }
            )
        }
        val actual = api.getResultById("42", null)
        assertThat(actual, sameBeanAs(expected))
        verify(api).httpGet("bib2.test/detail?C102=42", "ISO-8859-1")
    }
}

