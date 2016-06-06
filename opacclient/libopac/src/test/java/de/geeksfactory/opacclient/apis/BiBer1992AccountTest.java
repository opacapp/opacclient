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

import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BiBer1992AccountTest extends BaseAccountTest {
    private String file;

    public BiBer1992AccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"gelsenkirchen.htm", "freising.html", "herford.htm", "erkrath_opac.html",
                    "erkrath_opax.html"};

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
        JSONObject json = new JSONObject();
        JSONObject accounttable = new JSONObject();
        if (file.equals("gelsenkirchen.htm")) {
            accounttable.put("author", 7);
            accounttable.put("barcode", 4);
            accounttable.put("homebranch", -1);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 4);
            accounttable.put("returndate", 3);
            accounttable.put("status", 5);
            accounttable.put("title", 7);
        } else if (file.equals("freising.html")) {
            accounttable.put("author", 8);
            accounttable.put("barcode", 4);
            accounttable.put("homebranch", -1);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 4);
            accounttable.put("returndate", 3);
            accounttable.put("status", 6);
            accounttable.put("title", 8);
        } else if (file.equals("herford.htm")) {
            accounttable.put("author", 8);
            accounttable.put("barcode", 4);
            accounttable.put("homebranch", -1);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 1);
            accounttable.put("returndate", 3);
            accounttable.put("status", 9);
            accounttable.put("title", 8);
        } else if (file.equals("erkrath_opac.html")) {
            accounttable.put("author", 6);
            accounttable.put("barcode", 3);
            accounttable.put("homebranch", -1);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 3);
            accounttable.put("returndate", 2);
            accounttable.put("status", 4);
            accounttable.put("title", 6);
        } else if (file.equals("erkrath_opax.html")) {
            accounttable.put("author", 8);
            accounttable.put("barcode", 4);
            accounttable.put("homebranch", -1);
            accounttable.put("lendingbranch", -1);
            accounttable.put("prolongurl", 1);
            accounttable.put("returndate", 3);
            accounttable.put("status", 9);
            accounttable.put("title", 8);
        }
        json.put("accounttable", accounttable);
        if (html == null) return; // we may not have all files for all libraries
        List<LentItem> media = BiBer1992.parseMediaList(new AccountData(0), Jsoup.parse(html), json);
        assertTrue(media.size() > 0);
        for (LentItem item : media) {
            assertNotNull(item.getDeadline());
            assertNotNull(item.getId());
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException, JSONException {
        String html = readResource("/biber1992/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<ReservedItem> media = BiBer1992.parseResList(Jsoup.parse(html), new JSONObject());
        assertTrue(media.size() > 0);
    }
}
