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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TouchPointAccountTest extends BaseHtmlTest {
    private String file;

    public TouchPointAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"chemnitz.html", "munchenbsb.html", "winterthur.html", "munchenbsb2.html"};

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
        List<LentItem> media = TouchPoint.parse_medialist(Jsoup.parse(html));
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getTitle());
            assertNotNull(item.getDeadline());
            if (item.getStatus().contains("ist möglich")) assertNotNull(item.getProlongData());
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/touchpoint/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<ReservedItem> media = TouchPoint.parse_reslist(Jsoup.parse(html));
        assertTrue(media.size() > 0);
    }
}
