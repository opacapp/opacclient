package de.geeksfactory.opacclient.apis;

import org.json.JSONObject;
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
public class IOpacAccountTest extends BaseHtmlTest {
    private String file;

    public IOpacAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"heide.html"};

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
        String html = readResource("/iopac/" + file);
        List<LentItem> media = new ArrayList<>();
        IOpac.parseMediaList(media, Jsoup.parse(html), new JSONObject());
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getTitle());
            assertNotNull(item.getDeadline());
        }

        if (file.equals("heide.html")) {
            assertEquals("0x verl., 1 x reserv.", media.get(1).getStatus());
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/iopac/" + file);
        List<ReservedItem> media = new ArrayList<>();
        IOpac.parseResList(media, Jsoup.parse(html), new JSONObject());
    }
}
