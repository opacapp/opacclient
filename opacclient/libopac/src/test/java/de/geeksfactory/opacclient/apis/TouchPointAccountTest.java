package de.geeksfactory.opacclient.apis;

import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.objects.AccountData;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TouchPointAccountTest extends BaseAccountTest {
    private String file;

    public TouchPointAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"chemnitz.html", "munchenbsb.html"};

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
        String html = readResource("/touchpoint/medialist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<Map<String, String>> media =
                TouchPoint.parse_medialist(Jsoup.parse(html));
        assertTrue(media.size() > 0);
        for (Map<String, String> item : media) {
            assertContainsData(item, AccountData.KEY_LENT_TITLE);
            assertContainsData(item, AccountData.KEY_LENT_DEADLINE);
            assertContainsData(item, AccountData.KEY_LENT_DEADLINE_TIMESTAMP);
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/pica_lbs/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<Map<String, String>> media =
                TouchPoint.parse_reslist(Jsoup.parse(html));
        assertTrue(media.size() > 0);
    }
}
