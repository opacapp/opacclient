package de.geeksfactory.opacclient.apis;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ZonesTest extends BaseHtmlTest {
    private static final String BASE_URL = "https://katalog.stbib-koeln.de/alswww2.dll/";

    @Test
    public void testAccountPages() {
        Document page1 = Jsoup.parse(readResource("/zones/medialist/koeln_pages_1.html"));
        Document page2 = Jsoup.parse(readResource("/zones/medialist/koeln_pages_2.html"));
        page1.setBaseUri(BASE_URL);
        page2.setBaseUri(BASE_URL);

        String nextPage1 = Zones.findNextPageUrl(page1);
        assertNotNull(nextPage1);
        assertEquals(nextPage1,
                "https://katalog.stbib-koeln.de/alswww2" +
                        ".dll/Obj_4051458325195?Style=Portal3&SubStyle=&Lang=GER&ResponseEncoding" +
                        "=utf-8&Method=PageDown&PageSize=10");
        String nextPage2 = Zones.findNextPageUrl(page2);
        assertNull(nextPage2);
    }
}
