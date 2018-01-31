package de.geeksfactory.opacclient.apis;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class AdisAccountTest extends BaseHtmlTest {
    private String file;

    public AdisAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"tuebingen.html", "stuttgart.html", "muenchen.html"};

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
        if (html == null) return; // we may not have all files for all libraries
        List<LentItem> media = new ArrayList<>();
        Adis.parseMediaList(Jsoup.parse(html), "", media, false);
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getDeadline());
            assertTrue(item.getDeadline().isAfter(new LocalDate(2010, 1, 1))); // sensible dates
            assertNotNull(item.getId());
        }
    }

    @Test
    public void testParseReservationList() throws OpacApi.OpacErrorException, JSONException {
        String html = readResource("/adis/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<ReservedItem> res = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        String[] rlink = new String[]{"Vormerkungen zeigen oder löschen",
                "https://opac.sbs.stuttgart.de/aDISWeb/app;" +
                        "jsessionid=98AAE50B33FC5A0C191319D406D1564E?service=direct/1/POOLM02Q" +
                        "@@@@@@@@_4B032E00_349DAD80/Tabelle_Z1LW01.cellInternalLink" +
                        ".directlink&sp=SRGLINK_3&sp=SZM&requestCount=2"};
        Adis.parseReservationList(Jsoup.parse(html), rlink, true, res, fmt,
                new DummyStringProvider());
        assertTrue(res.size() > 0);
        for (ReservedItem item : res) {
            assertNotNull(item.getTitle());
        }
    }
}
