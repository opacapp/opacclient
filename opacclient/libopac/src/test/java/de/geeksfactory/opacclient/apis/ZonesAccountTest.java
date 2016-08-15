package de.geeksfactory.opacclient.apis;

import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ZonesAccountTest extends BaseHtmlTest {
    private String file;

    public ZonesAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"koeln.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseMediaList() throws OpacApi.OpacErrorException {
        String html = readResource("/zones/medialist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<LentItem> media = Zones.parseMediaList(Jsoup.parse(html));
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getTitle());
            assertNotNull(item.getDeadline());
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/zones/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<ReservedItem> media = Zones.parseResList(Jsoup.parse(html));
    }

    @Test
    public void testParseSummary() throws OpacApi.OpacErrorException {
        String html = readResource("/zones/summary/" + file);
        if (html == null) return;  // we may not have all files for all libraries
        AccountData adata = new AccountData(0);
        Zones.AccountLinks links = new Zones.AccountLinks(Jsoup.parse(html), adata);
        assertEquals(
                "https://katalog.stbib-koeln.de/alswww2" +
                        ".dll/APS_ZONES?fn=MyLoans&Style=Portal3&SubStyle=&Lang=GER" +
                        "&ResponseEncoding=utf-8",
                links.getLentLink());
        assertEquals(
                "https://katalog.stbib-koeln.de/alswww2" +
                        ".dll/APS_ZONES?fn=MyReservations&PageSize=10&Style=Portal3&SubStyle" +
                        "=&Lang=GER&ResponseEncoding=utf-8",
                links.getResLink());
        assertEquals("€ 0,00", adata.getPendingFees());
        assertEquals("22/04/2017", adata.getValidUntil());
    }
}
