package de.geeksfactory.opacclient.apis;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.objects.LentItem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SISISAccountTest extends BaseHtmlTest {
    private String file;

    public SISISAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"dresden.html", "dresden2.html", "witten.html", "erfurt.html"};

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
        String html = readResource("/sisis/medialist/" + file);
        if (html == null) return; // we may not have all files for all libraries

        List<LentItem> media = new ArrayList<LentItem>();
        Document doc = Jsoup.parse(html);
        SISIS.parse_medialist(media, doc, 0, new JSONObject());
        if (!file.equals("dresden.html")) {
            assertTrue(media.size() > 0);
        }
        // The dresden file is actually empty, so we can't even assert this; but before 4.5.9.
        // The test is still useful: Before 4.5.10 we actually had a bug with empty accounts.
        for (LentItem item : media) {
            assertContainsData(item.getTitle());
            assertNotNull(item.getDeadline());
            assertContainsData(item.getBarcode());
        }

        Map<String, Integer> links = SISIS.getAccountPageLinks(doc);
        if (file.equals("erfurt.html")) {
            // here we have two pages
            assertTrue(links.size() == 1);
            assertTrue(links.get(
                    "https://opac.erfurt.de/webOPACClient/userAccount" +
                            ".do?methodToCall=pos&accountTyp=AUSLEIHEN&anzPos=11")
                            .equals(11));
        }
    }
}
