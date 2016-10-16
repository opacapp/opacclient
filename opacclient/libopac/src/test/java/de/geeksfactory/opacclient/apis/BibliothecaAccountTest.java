package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.reporting.Report;
import de.geeksfactory.opacclient.reporting.ReportHandler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BibliothecaAccountTest extends BaseHtmlTest {
    private String file;
    private ReportHandler reportHandler;

    public BibliothecaAccountTest(String file) {
        this.file = file;
        reportHandler = new ReportHandler() {
            @Override
            public void sendReport(Report report) {
                throw new RuntimeException("send report: " + report.toString());
            }
        };
    }

    private static final String[] FILES =
            new String[]{"gladbeck.html", "marl.htm", "halle.html", "albstadt.html", "bernau.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseMediaList()
            throws OpacApi.OpacErrorException, JSONException, NotReachableException {
        String html = readResource("/bibliotheca/account/" + file);
        if (html == null) return; // we may not have all files for all libraries
        AccountData data = Bibliotheca.parse_account(new Account(), Jsoup.parse(html),
                new JSONObject(), reportHandler,
                new JSONObject(readResource("/bibliotheca/headers_lent.json")),
                new JSONObject(readResource("/bibliotheca/headers_reservations.json")));
        assertTrue(data.getLent().size() > 0);
        for (LentItem item : data.getLent()) {
            assertContainsData(item.getTitle());
            assertNullOrNotEmpty(item.getAuthor());
            assertNotNull(item.getProlongData());
            assertNotNull(item.getDeadline());
        }
    }

    @Test
    public void testParseReservationList()
            throws OpacApi.OpacErrorException, JSONException, NotReachableException {
        String html = readResource("/bibliotheca/account/" + file);
        if (html == null) return; // we may not have all files for all libraries
        if (file.equals("gladbeck.html") || file.equals("halle.html") ||
                file.equals("albstadt.html") || file.equals("bernau.html"))
            return;
        AccountData data = Bibliotheca.parse_account(new Account(), Jsoup.parse(html),
                new JSONObject(), reportHandler,
                new JSONObject(readResource("/bibliotheca/headers_lent.json")),
                new JSONObject(readResource("/bibliotheca/headers_reservations.json")));
        assertTrue(data.getReservations().size() > 0);
        for (ReservedItem item : data.getReservations()) {
            assertContainsData(item.getTitle());
            assertNullOrNotEmpty(item.getAuthor());
        }
    }
}
