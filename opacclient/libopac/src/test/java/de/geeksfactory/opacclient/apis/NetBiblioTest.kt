package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.i18n.DummyStringProvider
import de.geeksfactory.opacclient.objects.LentItem
import de.geeksfactory.opacclient.objects.ReservedItem
import de.geeksfactory.opacclient.utils.html
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

@RunWith(Parameterized::class)
class NetBiblioAccountTest(private val file: String) : BaseHtmlTest() {
    val netbiblio = NetBiblio()

    init {
        netbiblio.stringProvider = DummyStringProvider()
    }

    @Test
    fun testParseLent() {
        val doc = readResource("/netbiblio/lent/$file")?.html ?: return
        val lent = netbiblio.parseItems(doc, ::LentItem)

        if (file.contains("_empty")) {
            assertTrue(lent.isEmpty())
        } else {
            assertTrue(lent.isNotEmpty())
        }
        for (item in lent) {
            BaseHtmlTest.assertContainsData(item.title)
            assertNotNull(item.deadline)
            assertNotNull(item.id)
            BaseHtmlTest.assertContainsData(item.format)
        }
    }

    @Test
    fun testParseReservations() {
        val doc = readResource("/netbiblio/reservations/$file")?.html ?: return
        val reservations = netbiblio.parseItems(doc, ::ReservedItem)

        if (file.contains("_empty")) {
            assertTrue(reservations.isEmpty())
        } else {
            assertTrue(reservations.isNotEmpty())
        }
        for (item in reservations) {
            BaseHtmlTest.assertContainsData(item.title)
            assertNotNull(item.cancelData)
            assertNotNull(item.id)
            BaseHtmlTest.assertContainsData(item.branch)
            BaseHtmlTest.assertContainsData(item.format)
        }
    }

    companion object {

        private val FILES = arrayOf("basel.html", "basel_empty.html")

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
