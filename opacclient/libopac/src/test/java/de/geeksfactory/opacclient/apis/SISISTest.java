package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import de.geeksfactory.opacclient.objects.LentItem;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class SISISTest extends BaseHtmlTest {
    private SISIS sisis;

    @Before
    public void setUp() throws JSONException {
        sisis = spy(SISIS.class);
        sisis.opac_url = "http://opac-url.de";
        sisis.data = new JSONObject("{\"baseurl\":\"" + sisis.opac_url + "\"}");
    }

    @Test
    public void testLoadPages() throws IOException {
        // tests that links to other pages are also found when they are not visible from the
        // first page
        // (link to page 4 is usually not yet visible on page 1)

        String html = readResource("/sisis/medialist/erfurt.html");

        doReturn(readResource("/sisis/medialist/erfurt_page2.html"))
                .when(sisis).httpGet(
                eq("https://opac.erfurt.de/webOPACClient/userAccount" +
                        ".do?methodToCall=pos&accountTyp=AUSLEIHEN&anzPos=11"),
                anyString());
        doReturn(readResource("/sisis/medialist/erfurt_page3.html"))
                .when(sisis).httpGet(
                eq("https://opac.erfurt.de/webOPACClient/userAccount" +
                        ".do?methodToCall=pos&accountTyp=AUSLEIHEN&anzPos=21"),
                anyString());

        ArrayList<LentItem> media = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        SISIS.parse_medialist(media, doc, 1, sisis.data);
        sisis.loadPages(media, doc, SISIS::parse_medialist);

        assertEquals(30, media.size());
    }
}
