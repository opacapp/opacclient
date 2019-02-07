package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.i18n.DummyStringProvider
import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.Library
import de.geeksfactory.opacclient.utils.html
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Matchers.anyString
import org.mockito.Matchers.eq
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import java.util.*

class ArenaSearchTest : BaseHtmlTest() {
    val arena = spy(Arena::class.java)

    init {
        arena.init(Library().apply {
            data = JSONObject().apply {
                put("baseurl", "https://arena.stabi-ludwigsburg.de/web/arena")
            }
        }, HttpClientFactory("test"))
    }

    @Test
    fun `test retrieving cover with ajax`() {
        val xml = "<ajax-response>\n" +
                "<component id=\"id__searchResult__WAR__arenaportlets____90\">\n" +
                "<![CDATA[\n" +
                "<div class=\"arena-record-cover\" id=\"id__searchResult__WAR__arenaportlets____90\"><div>\n" +
                " <div class=\"arena-book-jacket\">\n" +
                " <a href=\"https://arena.stabi-ludwigsburg.de/web/arena/search?p_p_id=searchResult_WAR_arenaportlets&amp;p_p_lifecycle=2&amp;p_p_state=normal&amp;p_p_mode=view&amp;p_p_resource_id=%2FsearchResult%2F%3Fwicket%3Ainterface%3D%3A2%3AsearchResultPanel%3AdataContainer%3AdataView%3A1%3AcontainerItem%3Aitem%3AindexedRecordPanel%3AcoverDecorPanel%3Acontent%3AcoverLink%3A%3AILinkListener%3A%3A&amp;p_p_cacheability=cacheLevelPage\" target=\"_self\">\n" +
                " <img src=\"/arena-portlets/portletResources?resource=resourceCover&amp;portalSiteId=496278502&amp;recordId=762730&amp;agencyName=ADE101010&amp;width=0&amp;height=0\" alt=\"Titelseite\" title=\"Test 2018\"/>\n" +
                " </a>\n" +
                " </div>\n" +
                " </div></div>\n" +
                "]]>\n" +
                "</component>\n" +
                "</ajax-response>"
        doReturn(xml).`when`(arena).httpGet(eq("https://url"), anyString())

        val script = "<script type=\"text/javascript\" >Wicket.Event.add(window,\"domready\",function(b){var a=wicketAjaxGet(\"https://url\",function(){}.bind(this),function(){}.bind(this),function(){return Wicket.\$(\"id__searchResult__WAR__arenaportlets____90\")!=null}.bind(this))});</script>"
        val coverHolder = "<div class=\"arena-record-cover\" id=\"id__searchResult__WAR__arenaportlets____90\"><div><img alt=\"Loading...\" src=\"/arena-portlets/searchResult/ps:searchResult_WAR_arenaportlets_LAYOUT_21521/resources/org.apache.wicket.ajax.AbstractDefaultAjaxBehavior/indicator.gif\"/></div>"
        val doc = "<html><head>$script</head><body>$coverHolder</body></html>".html
        val ajaxUrls = arena.getAjaxUrls(doc)

        assertEquals(1, ajaxUrls.size)

        val cover = arena.getCover(doc.select("div").first(), ajaxUrls)
        assertEquals("https://arena.stabi-ludwigsburg.de/arena-portlets/portletResources?resource=resourceCover&portalSiteId=496278502&recordId=762730&agencyName=ADE101010&width=0&height=0", cover)
    }

    @Test
    fun `test retrieving cover from img tag`() {
        val coverHolder = "<div class=\"arena-record-cover\">\n" +
                "    <div class=\"arena-book-jacket\"> <a href=\"https://bibliotek.sodertalje.se/web/arena/search?p_p_id=searchResult_WAR_arenaportlets&amp;p_p_lifecycle=1&amp;p_p_state=normal&amp;p_p_mode=view&amp;p_p_col_id=column-2&amp;p_p_col_count=3&amp;_searchResult_WAR_arenaportlets__wu=%2FsearchResult%2F%3Fwicket%3Ainterface%3D%3A0%3AsearchResultPanel%3AdataContainer%3AdataView%3A1%3AcontainerItem%3Aitem%3AindexedRecordPanel%3AcoverDecorPanel%3AcoverLink%3A%3AILinkListener%3A%3A\" target=\"_self\"> <img src=\"/arena-portlets/portletResources?resource=resourceCover&amp;portalSiteId=37779237&amp;recordId=104266&amp;agencyName=ASE100117&amp;width=0&amp;height=0\" alt=\"Omslagsbild\" title=\"700 segelb&aring;tar i test\" /> </a> </div>\n" +
                "</div>"
        val doc = "<html><head></head><body>$coverHolder</body></html>".html
        val cover = arena.getCover(doc.select("div").first(), emptyMap())
        assertEquals("https://arena.stabi-ludwigsburg.de/arena-portlets/portletResources?resource=resourceCover&portalSiteId=37779237&recordId=104266&agencyName=ASE100117&width=0&height=0", cover)
    }
}

@RunWith(Parameterized::class)
class ArenaAccountTest(private val file: String) : BaseHtmlTest() {
    val arena = Arena()

    init {
        arena.stringProvider = DummyStringProvider()
        arena.init(Library().apply {
            data = JSONObject().apply {
                put("baseurl", "https://arena.stabi-beispielburg.de/web/arena")
            }
        }, HttpClientFactory("OpacTest/1.0"))
    }

    @Test
    fun testParseFees() {
        val doc = readResource("/arena/fees/$file")?.html ?: return
        val fees = arena.parseFees(doc)
        when (file) {
            "ludwigsburg.html" -> assertEquals("5,00", fees)
        }
    }

    @Test
    fun testParseLent() {
        val doc = readResource("/arena/lent/$file")?.html ?: return
        val lent = arena.parseLent(doc)

        if (file.contains("_empty")) {
            Assert.assertTrue(lent.isEmpty())
        } else {
            Assert.assertTrue(lent.isNotEmpty())
        }
        for (item in lent) {
            BaseHtmlTest.assertContainsData(item.title)
            Assert.assertNotNull(item.deadline)
            Assert.assertNotNull(item.id)
            BaseHtmlTest.assertContainsData(item.format)
        }
    }

    @Test
    fun testParseReservations() {
        val doc = readResource("/arena/reservations/$file")?.html ?: return
        val reservations = arena.parseReservations(doc)

        if (file.contains("_empty")) {
            Assert.assertTrue(reservations.isEmpty())
        } else {
            Assert.assertTrue(reservations.isNotEmpty())
        }
        for (item in reservations) {
            BaseHtmlTest.assertContainsData(item.title)
            Assert.assertNotNull(item.id)
            Assert.assertNotNull(item.cover)
            BaseHtmlTest.assertContainsData(item.branch)
            BaseHtmlTest.assertContainsData(item.format)
        }
    }

    companion object {

        private val FILES = arrayOf("ludwigsburg.html", "ludwigsburg_empty.html")

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
