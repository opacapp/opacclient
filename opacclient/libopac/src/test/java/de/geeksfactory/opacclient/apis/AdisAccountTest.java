package de.geeksfactory.opacclient.apis;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.objects.LentItem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class AdisAccountTest extends BaseHtmlTest {
    private String file;

    public AdisAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"tuebingen.html", "Stuttgart_StB.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseMediaList() throws OpacApi.OpacErrorException, JSONException {
        String html = readResource("/adis/medialist/" + file);
        //if (html == null) return; // we may not have all files for all libraries
        List<LentItem> media = new ArrayList<>();
        Adis.parseMediaList(Jsoup.parse(html), "", false, media);
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getDeadline());
            assertTrue(item.getDeadline().isAfter(new LocalDate(2010, 1, 1))); // sensible dates
            assertNotNull(item.getId());
        }
    }

    @Test
    public void testAccountPage() throws OpacApi.OpacErrorException, JSONException {
        String html = readResource("/adis/account/" + file);
        if (html == null) return; // we may not have all files for all libraries
        Adis adis = new Adis();
        Document doc = Jsoup.parse(html);
        final String url = "file:///.";
        doc.setBaseUri(url);
        Adis.AccountPageReturn apr = adis.parseAccount(doc);
        System.out.printf("alink = %s%n", apr.alink);
        for (String[] rlink : apr.rlinks) {
            assertTrue(rlink.length == 2);
            System.out.printf("rlink: %s = %s%n", rlink[0], rlink[1]);
        }
    }
}
