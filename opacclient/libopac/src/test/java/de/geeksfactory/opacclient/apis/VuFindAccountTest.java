package de.geeksfactory.opacclient.apis;

import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class VuFindAccountTest extends BaseHtmlTest {
    private String file;

    public VuFindAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"kreisre.html", "kreisre-ebook.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseMediaList() {
        String html = readResource("/vufind/medialist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<LentItem> media = VuFind.parse_lent(Jsoup.parse(html));
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertContainsData(item.getTitle());
            assertNotNull(item.getDeadline());
            assertContainsData(item.getFormat());
            if (!item.getFormat().equals("eBook") && !item.getFormat().equals("eAudio")) {
                assertContainsData(item.getBarcode());
                assertContainsData(item.getStatus());
            }
        }
    }

    @Test
    public void testParseResList() {
        String html = readResource("/vufind/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<ReservedItem> media = VuFind.parse_reservations(Jsoup.parse(html));
        assertTrue(media.size() > 0);

        int i = 0;
        for (ReservedItem item : media) {
            assertContainsData(item.getTitle());
            assertContainsData(item.getBranch());
            if (file.equals("kreisre.html")) {
                if (i == 1) {
                    assertEquals("Abholbereit", item.getStatus());
                } else {
                    assertContainsData(item.getCancelData());
                }
            }
            i++;
        }
    }
}
