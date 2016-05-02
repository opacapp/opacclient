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

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BibliothecaAccountTest extends BaseAccountTest {
    private String file;

    public BibliothecaAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"gladbeck.html", "marl.htm", "halle.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    private JSONObject getData(String file) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject accounttable = new JSONObject();
        JSONObject reservationtable = new JSONObject();
        if (file.equals("gladbeck.html")) {
            accounttable.put("author", 0);
            accounttable.put("barcode", 3);
            accounttable.put("homebranch", -1);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 4);
            accounttable.put("returndate", 2);
            accounttable.put("status", -1);
            accounttable.put("title", 1);
            reservationtable.put("author", 0);
            reservationtable.put("availability", 2);
            reservationtable.put("branch", -1);
            reservationtable.put("cancelurl", 3);
            reservationtable.put("expirationdate", -1);
            reservationtable.put("title", 1);
        } else if (file.equals("marl.htm")) {
            accounttable.put("author", 0);
            accounttable.put("barcode", 3);
            accounttable.put("homebranch", -1);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 4);
            accounttable.put("returndate", 2);
            accounttable.put("status", -1);
            accounttable.put("title", 1);
            reservationtable.put("author", 0);
            reservationtable.put("availability", 2);
            reservationtable.put("branch", -1);
            reservationtable.put("cancelurl", 3);
            reservationtable.put("expirationdate", -1);
            reservationtable.put("title", 1);
        } else if (file.equals("halle.html")) {
            accounttable.put("author", 1);
            accounttable.put("barcode", -1);
            accounttable.put("homebranch", 0);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 6);
            accounttable.put("returndate", 3);
            accounttable.put("status", 5);
            accounttable.put("title", 2);
            reservationtable.put("author", 1);
            reservationtable.put("availability", -1);
            reservationtable.put("branch", 0);
            reservationtable.put("cancelurl", 3);
            reservationtable.put("expirationdate", -1);
            reservationtable.put("title", 2);
        }
        json.put("accounttable", accounttable);
        json.put("reservationtable", reservationtable);
        return json;
    }

    @Test
    public void testParseMediaList()
            throws OpacApi.OpacErrorException, JSONException, NotReachableException {
        String html = readResource("/bibliotheca/" + file);
        if (html == null) return; // we may not have all files for all libraries
        AccountData data = Bibliotheca.parse_account(new Account(), Jsoup.parse(html),
                getData(file));
        assertTrue(data.getLent().size() > 0);
        for (LentItem item : data.getLent()) {
            assertNotNull(item.getTitle());
            assertNotNull(item.getAuthor());
            assertNotNull(item.getProlongData());
            assertNotNull(item.getDeadline());
        }
    }

    @Test
    public void testParseReservationList()
            throws OpacApi.OpacErrorException, JSONException, NotReachableException {
        String html = readResource("/bibliotheca/" + file);
        if (html == null) return; // we may not have all files for all libraries
        if (file.equals("gladbeck.html") || file.equals("halle.html"))
            return;
        AccountData data = Bibliotheca.parse_account(new Account(), Jsoup.parse(html),
                getData(file));
        assertTrue(data.getReservations().size() > 0);
        for (ReservedItem item : data.getReservations()) {
            assertNotNull(item.getTitle());
            assertNotNull(item.getAuthor());
        }
    }
}
