package de.geeksfactory.opacclient.apis

import com.shazam.shazamcrest.matcher.Matchers.sameBeanAs
import de.geeksfactory.opacclient.objects.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class SLUBAccountTest() : BaseHtmlTest() {
    var slub = SLUB()

    @Test
    fun testParseEmptyAccountData() {
        val json = JSONObject(readResource("/slub/account/empty-account.json"))

        val accountdata = slub.parseAccountData(Account(), json)

        assertEquals("1,23 EUR", accountdata.pendingFees)
        assertEquals("31.03.20", accountdata.validUntil)
        assertTrue(accountdata.lent.isEmpty())
        assertTrue(accountdata.reservations.isEmpty())
    }

    @Test
    fun testParseAccountData() {
        val json = JSONObject(readResource("/slub/account/account.json"))
        val lentitem1 = LentItem().apply {
            title = "¬Der¬ neue Kosmos-Baumführer"
            author = "Bachofer, Mark"
            setDeadline("2019-06-03")
            format = "B"
            barcode = "31626878"
            isRenewable = true
            prolongData = barcode
        }

        val accountdata = slub.parseAccountData(Account(), json)

        assertEquals(2, accountdata.lent.size)
        assertEquals(3, accountdata.reservations.size)
        assertThat(lentitem1, samePropertyValuesAs(accountdata.lent[0]))
        assertEquals("vorgemerkt", accountdata.lent[1].status)
    }
}

class SLUBSearchTest() : BaseHtmlTest() {
    var slub = SLUB()

    @Test
    fun testParseEmptySearchResults() {
        val json = JSONObject(readResource("/slub/search/empty-search.json"))

        val searchresults = slub.parseSearchResults(json)

        assertEquals(0, searchresults.total_result_count)
        assertTrue(searchresults.results.isEmpty())
    }

    @Test
    fun testParseSearchResults() {
        val json = JSONObject(readResource("/slub/search/simple-search.json"))
        val result1 = SearchResult().apply {
            innerhtml = "<b>Mastering software testing with JUnit 5 comprehensive guide to develop high quality Java applications Boni García</b><br>Garcia, Boni<br>(2017)"
            type = SearchResult.MediaType.BOOK
            id = "0-1014939550"
        }

        val searchresults = slub.parseSearchResults(json)

        assertEquals(2, searchresults.total_result_count)
        assertThat(result1, samePropertyValuesAs(searchresults.results[0]))
    }

    @Test
    fun testParseResultById() {
        val json = JSONObject(readResource("/slub/search/simple-item.json"))
        val expected = DetailedItem().apply {
            addDetail(Detail("Medientyp", "Buch"))
            addDetail(Detail("Titel", "Unit-Tests mit JUnit"))
            title = "Unit-Tests mit JUnit"
            addDetail(Detail("Beteiligte", "Hunt, Andrew; Thomas, David [Autor/In]"))
            addDetail(Detail("Erschienen", "München Wien Hanser 2004 "))
            addDetail(Detail("Erschienen in", "Pragmatisch Programmieren; 2"))
            addDetail(Detail("ISBN", "3446228241; 3446404694; 9783446404694; 9783446228245"))
            addDetail(Detail("Sprache", "Deutsch"))
            addDetail(Detail("Schlagwörter", "Quellcode; Softwaretest; JUnit"))
            id = "0-1182402208"
            copies = arrayListOf(Copy().apply {
                barcode = "31541466"
                department = "Freihand"
                branch = "Bereichsbibliothek DrePunct"
                status = "Ausleihbar"
                shelfmark = "ST 233 H939"
            })
        }

        val item = slub.parseResultById(json.getString("id"), json)

        //details are in unspecified order, see https://stackoverflow.com/a/4920304/3944322
        assertThat(item, sameBeanAs(expected).ignoring("details"))
        assertThat(HashSet(item.details), sameBeanAs(HashSet(expected.details)))
    }

    @Test
    fun testParseResultByIdCopiesInMultipleArrays() {
        val json = JSONObject(readResource("/slub/search/item-copies_in_multiple_arrays.json"))
        val copyFirst = Copy().apply {
            barcode = "10418078"
            department = "Magazin Zeitschriften"
            branch = "Zentralbibliothek"
            status = "Bestellen zur Benutzung im Haus, kein Versand per Fernleihe, nur Kopienlieferung"
            shelfmark = "19 4 01339 0 0024 1 01"
        }
        val copyLast = Copy().apply {
            barcode = "33364639"
            department = "Magazin Zeitschriften"
            branch = "Zentralbibliothek"
            status = "Bestellen zur Benutzung im Haus, kein Versand per Fernleihe, nur Kopienlieferung"
            shelfmark = "19 4 01339 1 1969 1 01"
        }

        val item = slub.parseResultById(json.getString("id"), json)

        assertEquals(19, item.copies.size)
        // the copies arrays may occur in any order
        assertThat(item.copies, hasItems(sameBeanAs(copyFirst), sameBeanAs(copyLast)))
    }
}