package de.geeksfactory.opacclient.apis;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.jsoup.Jsoup;
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
            new String[]{"tuebingen.html"};

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
        Adis.parseMediaList(Jsoup.parse(html), "", media, false);
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getDeadline());
            assertTrue(item.getDeadline().isAfter(new LocalDate(2010, 1, 1))); // sensible dates
            assertNotNull(item.getId());
        }
    }
}
