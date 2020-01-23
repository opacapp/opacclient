package de.geeksfactory.opacclient.apis

import com.shazam.shazamcrest.matcher.Matchers.sameBeanAs
import de.geeksfactory.opacclient.i18n.StringProvider
import de.geeksfactory.opacclient.i18n.StringProvider.*
import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Matchers
import org.mockito.Mockito

private class TestStringProvider : StringProvider {
    override fun getString(identifier: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFormattedString(identifier: String?, vararg args: Any?): String {
        return when (identifier) {
            RESERVED_POS -> String.format("vorgemerkt, Pos. %s", *args)
            HOLD -> String.format("liegt seit %s bereit", *args)
            REQUEST_READY -> String.format("seit %s abholbereit (Magazinbestellung)", *args)
            else -> identifier!!
        }
    }

    override fun getQuantityString(identifier: String?, count: Int, vararg args: Any?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMediaTypeName(mediaType: SearchResult.MediaType?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class SLUBAccountTest() : BaseHtmlTest() {
    var slub = SLUB()

    init {
        slub.stringProvider = TestStringProvider()
    }

    @Test
    fun testParseEmptyAccountData() {
        val json = JSONObject(readResource("/slub/account/empty-account.json"))

        val accountdata = slub.parseAccountData(Account(), json)

        assertEquals("1,23 EUR", accountdata.pendingFees)
        assertEquals(DateTimeFormat.shortDate().print(LocalDate("2020-03-31")), accountdata.validUntil)
        assertTrue(accountdata.lent.isEmpty())
        assertTrue(accountdata.reservations.isEmpty())
    }

    @Test
    fun testParseAccountData() {
        val json = JSONObject(readResource("/slub/account/account.json"))
        val fmt = DateTimeFormat.shortDate()
        val lentitem1 = LentItem().apply {
            title = "¬Der¬ neue Kosmos-Baumführer"
            author = "Bachofer, Mark"
            setDeadline("2019-06-03")
            format = "B"
            //id = "31626878"
            barcode = "31626878"
            isRenewable = true
            prolongData = barcode
        }
        val reserveditem1 = ReservedItem().apply {
            // reserve
            title = "Pareys Buch der Bäume"
            author = "Mitchell, Alan"
            format = "B"
            //id = "30963742"
            status = "vorgemerkt, Pos. 1"
            cancelData = "30963742_1"
        }
        val reserveditem2 = ReservedItem().apply {
            // hold
            title = "Welcher Baum ist das?"
            author = "Mayer, Joachim ¬[VerfasserIn]¬"
            format = "B"
            //id = "34778398"
            branch = "ZwB Forstwissenschaft"
            status = String.format("liegt seit %s bereit", fmt.print(LocalDate("2019-05-10")))
        }
        val reserveditem3 = ReservedItem().apply {
            // request ready
            title = "Englische Synonyme als Fehlerquellen"
            author = "Meyer, Jürgen"
            format = "B"
            //id = "20550495"
            branch = "Zentralbibliothek Ebene 0 SB-Regal"
            status = String.format("seit %s abholbereit (Magazinbestellung)", fmt.print(LocalDate("2019-05-04")))
        }
        val accountdata = slub.parseAccountData(Account(), json)

        assertEquals(2, accountdata.lent.size)
        assertEquals(3, accountdata.reservations.size)
        assertThat(lentitem1, samePropertyValuesAs(accountdata.lent[0]))
        assertEquals("vorgemerkt", accountdata.lent[1].status)
        assertThat(accountdata.reservations, hasItems(sameBeanAs(reserveditem1),
                sameBeanAs(reserveditem2), sameBeanAs(reserveditem3)))
    }

    @Test
    fun testParseAccountDataIllInProgress() {
        val json = JSONObject(readResource("/slub/account/account-ill_in_progress.json"))
        val reserveditem = ReservedItem().apply {
            title = "Kotlin"
            author = "Szwillus, Karl"
            //id = "145073"
            branch = "zell1"
            status = "Bestellung ausgelöst"
        }

        val accountdata = slub.parseAccountData(Account(), json)

        assertEquals(0, accountdata.lent.size)
        assertEquals(1, accountdata.reservations.size)
        assertThat(reserveditem, samePropertyValuesAs(accountdata.reservations[0]))
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
    fun testParseSearchResultsWithNullCreationDate() {
        val json = JSONObject(readResource("/slub/search/search-null_creation_date.json"))
        val result1 = SearchResult().apply {
            innerhtml = "<b>Tu en hagiois patros hēmōn Maximu tu homologetu Hapanta = S.P.N. Maximi Confessoris Opera omnia eruta, Latine transl., notisque ill. opera et studio Francisci Combefis. Adauxit Franciscus Oehler. Accurante et denuo recognoscente J.-P. Migne</b><br>Maximus Confessor"
            type = SearchResult.MediaType.BOOK
            id = "0-1093989777"
        }

        val searchresults = slub.parseSearchResults(json)

        assertEquals(1, searchresults.total_result_count)
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
            addDetail(Detail("Inhaltsverzeichnis", "http://d-nb.info/970689268/04"))
            id = "0-1182402208"
            copies = arrayListOf(Copy().apply {
                barcode = "31541466"
                department = "Freihand"
                branch = "Bereichsbibliothek DrePunct"
                status = "Ausleihbar"
                shelfmark = "ST 233 H939"
            })
            collectionId = "0-1183957874"
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

    @Test
    fun testParseResultByIdMultipleParts() {
        val json = JSONObject(readResource("/slub/search/item-multiple_parts_item.json"))
        val volumes = listOf<Volume>(
                Volume("0-1453040935", "[3]: Principles of digital image processing"),
                Volume("0-1347927328", "[2]: Principles of digital image processing"),
                Volume("0-1347930884", "[1]: Principles of digital image processing")
        )

        // is part of "Undergraduate topics in computer science" but no id (--> collectionid) given
        val item = slub.parseResultById(json.getString("id"), json)

        assertThat(volumes, sameBeanAs(item.volumes))
        assertNull(item.collectionId)
    }

    @Test
    fun testParseResultByIdUmlaute() {
        val json = JSONObject(readResource("/slub/search/item-with_umlaute_in_title_and_volumes.json"))
        val volume = Volume("0-1149529121", "(inse,5): in 6 Bänden")

        val item = slub.parseResultById(json.getString("id"), json)

        assertEquals("Urania-Tierreich: in 6 Bänden", item.title)
        assertThat(item.volumes, hasItem(sameBeanAs(volume)))
    }

    @Test
    fun testParseResultByIdThumbnail() {
        val json = JSONObject(readResource("/slub/search/item-fotothek.json"))
        val expected = DetailedItem().apply {
            addDetail(Detail("Medientyp", "Foto"))
            addDetail(Detail("Titel", "Maya"))
            title = "Maya"
            addDetail(Detail("Sprache", "Kein linguistischer Inhalt"))
            addDetail(Detail("Schlagwörter", "Skulptur; Statue; Ortskatalog zur Kunst und Architektur"))
            id = "dswarm-67-b2FpOmRldXRzY2hlZm90b3RoZWsuZGU6YTg0NTA6Om9ianwzMzA1NTgxMHxkZl9oYXVwdGthdGFsb2dfMDEwMDMzNg"
            cover = "http://fotothek.slub-dresden.de/thumbs/df_hauptkatalog_0100336.jpg"
            addDetail(Detail("In der Deutschen Fotothek ansehen", "http://www.deutschefotothek.de/obj33055810.html"))
        }

        val item = slub.parseResultById(json.getString("id"), json)

        assertThat(item, sameBeanAs(expected).ignoring("details"))
        assertThat(HashSet(expected.details), sameBeanAs(HashSet(item.details)))
    }

    @Test
    fun testParseResultByIdLinks() {
        val json = JSONObject(readResource("/slub/search/item-links.json"))
        val expected = DetailedItem().apply {
            addDetail(Detail("Medientyp", "Buch"))
            addDetail(Detail("Titel", "JUnit-Profiwissen: effizientes Arbeiten mit der Standardbibliothek für automatisierte Tests in Java"))
            title = "JUnit-Profiwissen: effizientes Arbeiten mit der Standardbibliothek für automatisierte Tests in Java"
            addDetail(Detail("Beteiligte", "Tamm, Michael [Autor/In]"))
            addDetail(Detail("Erschienen", "Heidelberg dpunkt.Verl. 2013 "))
            addDetail(Detail("ISBN", "3864900204; 9783864900204"))
            addDetail(Detail("Sprache", "Deutsch"))
            addDetail(Detail("Schlagwörter", "Java; JUnit"))
            addDetail(Detail("Beschreibung", "Literaturverz. S. 351"))
            id = "0-727434322"
            addDetail(Detail("Inhaltsverzeichnis","http://www.gbv.de/dms/tib-ub-hannover/727434322.pdf"))
            addDetail(Detail("Inhaltstext","http://deposit.d-nb.de/cgi-bin/dokserv?id=4155321&prov=M&dok_var=1&dok_ext=htm"))
            addDetail(Detail("Zugang zur Ressource (via ProQuest Ebook Central)", "http://wwwdb.dbod.de/login?url=http://slub.eblib.com/patron/FullRecord.aspx?p=1575685"))
            addDetail(Detail("Online-Ausgabe", "Tamm, Michael: JUnit-Profiwissen (SLUB)"))
        }

        val item = slub.parseResultById(json.getString("id"), json)

        assertThat(item, sameBeanAs(expected).ignoring("details"))
        assertThat(HashSet(expected.details), sameBeanAs(HashSet(item.details)))
    }

    @Test
    fun testParseResultByIdLinksGeneralNoLabel() {
        val json = JSONObject(readResource("/slub/search/item-links_general_no_label.json"))
        val expected = DetailedItem().apply {
            addDetail(Detail("Medientyp", "ElectronicThesis"))
            addDetail(Detail("Titel", "A Study of Classic Maya Rulership"))
            title = "A Study of Classic Maya Rulership"
            addDetail(Detail("Sprache", "Englisch"))
            addDetail(Detail("Schlagwörter", "Archaeology; Latin American history; Ancient history; Native American studies"))
            id = "ai-34-b2FpOnBxZHRvYWkucHJvcXVlc3QuY29tOjM0Nzc2Mzg"
            addDetail(Detail("Link", "http://pqdtopen.proquest.com/#viewpdf?dispub=3477638"))
        }

        val item = slub.parseResultById(json.getString("id"), json)

        assertThat(item, sameBeanAs(expected).ignoring("details"))
        assertThat(HashSet(expected.details), sameBeanAs(HashSet(item.details)))
    }
}

@RunWith(Parameterized::class)
class SLUBAccountMockTest(private val response: String,
                                     private val expectedException: Class<out Exception?>?,
                                     private val expectedExceptionMsg: String?) : BaseHtmlTest() {
    private val slub = Mockito.spy(SLUB::class.java)
    init {
        slub.init(Library().apply {
            data = JSONObject().apply {
                put("baseurl", "https://test.de")
            }
        }, HttpClientFactory("test"))
    }
    private val account = Account().apply {
        name = "x"
        password = "x"
    }

    @JvmField
    @Rule
    var thrown: ExpectedException = ExpectedException.none()

    @Test
    fun testCheckAccountData() {
        Mockito.doReturn(response).`when`(slub).httpPost(Matchers.any(), Matchers.any(), Matchers.any())
        if (expectedException != null) {
            thrown.expect(expectedException)
            thrown.expectMessage(expectedExceptionMsg)
        }

        slub.requestAccount(account, "", null)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
                // validate: status as string
                arrayOf("{\"status\":\"1\",\"message\":\"credentials_are_valid\"}", null, null),
                arrayOf("{\"message\":\"error_credentials_invalid\",\"arguments\":{\"controller\":\"API\",\"action\":\"validate\",\"username\":\"123456\"},\"status\":\"-1\"}", OpacApi.OpacErrorException::class.java, "error_credentials_invalid"),
                // POST not accepted, malformed request, e.g. invalid action
                arrayOf("<!doctype html><title>.</title>", OpacApi.OpacErrorException::class.java, "Request didn't return JSON object"),
                // delete: status as int or string
                arrayOf("{\"status\":1,\"message\":\"Reservation deleted\"}", null, null),
                arrayOf("{\"status\":\"-1\",\"message\":\"Item not reserved\"}", OpacApi.OpacErrorException::class.java, "Item not reserved"),
                // pickup: status as boolean
                arrayOf("{\"status\":true,\"message\":\"n\\/a\"}", null, null),
                arrayOf("{\"status\":false,\"message\":\"Ungültige Barcodenummer\"}", OpacApi.OpacErrorException::class.java, "Ungültige Barcodenummer")
        )
    }
}