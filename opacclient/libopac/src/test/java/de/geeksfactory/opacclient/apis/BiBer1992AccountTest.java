package de.geeksfactory.opacclient.apis;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.reporting.Report;
import de.geeksfactory.opacclient.reporting.ReportHandler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BiBer1992AccountTest extends BaseHtmlTest {
    private String file;
    private ReportHandler reportHandler;

    public BiBer1992AccountTest(String file) {
        this.file = file;
        reportHandler = new ReportHandler() {
            @Override
            public void sendReport(Report report) {
                throw new RuntimeException("send report: " + report.toString());
            }
        };
    }

    private static final String[] FILES =
            new String[]{"gelsenkirchen.htm", "freising.html", "herford.htm", "erkrath_opac.html",
                    "erkrath_opax.html", "nuertingen.html", "jena.html", "bochum.html"};

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
        String html = readResource("/biber1992/medialist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<LentItem> media = BiBer1992
                .parseMediaList(new AccountData(0), new Account(), Jsoup.parse(html),
                        new JSONObject(), reportHandler,
                        new JSONObject(readResource("/biber1992/headers_lent.json")));
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getDeadline());
            assertTrue(item.getDeadline().isAfter(new LocalDate(2010, 1, 1))); // sensible dates
            assertNotNull(item.getId());
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException, JSONException {
        String html = readResource("/biber1992/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<ReservedItem> media = BiBer1992.parseResList(new Account(), Jsoup.parse(html),
                new JSONObject(), reportHandler,
                new JSONObject(readResource("/biber1992/headers_reservations.json")));
        assertTrue(media.size() > 0);
    }
}
