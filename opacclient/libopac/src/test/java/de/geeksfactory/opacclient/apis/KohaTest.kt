package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.objects.LentItem
import de.geeksfactory.opacclient.objects.ReservedItem
import org.json.JSONObject
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.ArrayList

@RunWith(Parameterized::class)
class KohaAccountTest(private val file: String) : BaseHtmlTest() {
    val koha = Koha()

    @Test
    fun testParseAccount() {
        val html = readResource("/koha/account/$file")

        val doc = Jsoup.parse(html)
        val lent = koha.parseItems(doc, ::LentItem, "#checkoutst")
        val reservations = koha.parseItems(doc, ::ReservedItem, "#holdst")

        assertTrue(lent.isNotEmpty())
        for (item in lent) {
            BaseHtmlTest.assertContainsData(item.title)
            assertNotNull(item.deadline)
            BaseHtmlTest.assertContainsData(item.lendingBranch)
            BaseHtmlTest.assertContainsData(item.format)
        }

        if (file == "heilbronn.html") assertTrue(reservations.isEmpty())
    }

    @Test
    fun testParseFees() {
        val html = readResource("/koha/fees/$file")
        val doc = Jsoup.parse(html)
        val fees = koha.parseFees(doc)
        assertEquals("10,00", fees)
    }

    companion object {

        private val FILES = arrayOf("heilbronn.html")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun files(): Collection<Array<String>> {
            val files = ArrayList<Array<String>>()
            for (file in FILES) {
                files.add(arrayOf(file))
            }
            return files
        }
    }
}
