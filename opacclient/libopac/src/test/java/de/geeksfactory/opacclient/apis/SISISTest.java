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

    @Test
    public void testParseCoverJs() {
        String lampertheim = " var bookInfo = JSON.parse" +
                "('{\"ISBN:9783455010312\":{\"bib_key\":\"ISBN:9783455010312\"," +
                "\"info_url\":\"https://books.google" +
                ".com/books?id=q76xAgAACAAJ\\u0026source=gbs_ViewAPI\"," +
                "\"preview_url\":\"https://books.google" +
                ".com/books?id=q76xAgAACAAJ\\u0026source=gbs_ViewAPI\"," +
                "\"thumbnail_url\":\"https://books.google" +
                ".com/books/content?id=q76xAgAACAAJ\\u0026printsec=frontcover\\u0026img=1" +
                "\\u0026zoom=5\",\"preview\":\"noview\",\"embeddable\":false," +
                "\"can_download_pdf\":false,\"can_download_epub\":false," +
                "\"is_pdf_drm_enabled\":false,\"is_epub_drm_enabled\":false}}');\n" +
                "      var book = bookInfo[Object.keys(bookInfo)[0]]; \n" +
                "      \n" +
                "      \n" +
                "      var imgTag = '<img style=\"margin: 5px; border: 0px solid #666; ' + size +" +
                " '\" border=0 src=\"' + book.thumbnail_url + '\">';";
        String url = SISIS.parseCoverJs(lampertheim, "https://katalog.lampertheim.de");
        assertEquals("https://books.google.com/books/content?id=q76xAgAACAAJ&printsec" +
                "=frontcover&img=1&zoom=5", url);

        String wuppertal = "      var imgSrc = 'showMVBCover" +
                ".do?token=2aa75c57-40a7-4c99-b501-d49b39ada7a9';\n" +
                "      var imgTag = '<img style=\"margin: 10px; \" border=0 src=\"' + imgSrc + " +
                "'\"/>';\n" +
                "   var detailUrl = \"http://www.buchhandel.de/buch/9783551551931\";   \n" +
                "      var imgLink= '<a href=' + detailUrl + ' target=\"cover\">' + imgTag + " +
                "'</a>';\n" +
                "      $(\"div#-1_5\").html(imgLink);\n" +
                "    ";
        url = SISIS.parseCoverJs(wuppertal, "http://webopac.wuppertal.de");
        assertEquals(
                "http://webopac.wuppertal.de/showMVBCover.do?token=2aa75c57-40a7-4c99-b501-d49b39ada7a9",
                url);
    }
}
