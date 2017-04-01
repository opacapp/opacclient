package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BibliothecaSearchTest extends BaseHtmlTest {
    private String file;

    public BibliothecaSearchTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"mannheim.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseSearch()
            throws OpacApi.OpacErrorException, JSONException, NotReachableException {
        String html = readResource("/bibliotheca/resultlist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        int page = 1;
        SearchRequestResult result = Bibliotheca.parseSearch(html, page, getData(file));
        assertTrue(result.getPage_count() > 0 || result.getTotal_result_count() > 0);
        assertTrue(result.getPage_index() == page);
        for (SearchResult item : result.getResults()) {
            assertNotNull(item.getId());
            assertNotNull(item.getType());
        }
        SearchResult firstItem = result.getResults().get(0);
        assertEquals(firstItem.getInnerhtml(), getFirstResultHtml(file));
    }

    @Test
    public void testParseResult()
            throws OpacApi.OpacErrorException, JSONException, NotReachableException {
        String html = readResource("/bibliotheca/result_detail/" + file);
        if (html == null) return; // we may not have all files for all libraries
        DetailedItem result = Bibliotheca.parseResult(html, getData(file));
        for (Copy copy : result.getCopies()) {
            assertContainsData(copy.getStatus());
            assertNullOrNotEmpty(copy.getBarcode());
            assertNullOrNotEmpty(copy.getBranch());
            assertNullOrNotEmpty(copy.getDepartment());
            assertNullOrNotEmpty(copy.getLocation());
            assertNullOrNotEmpty(copy.getReservations());
            assertNullOrNotEmpty(copy.getShelfmark());
            assertNullOrNotEmpty(copy.getUrl());
            if (copy.getStatus().equals("Entliehen")) assertNotNull(copy.getReturnDate());
        }
        assertContainsData(result.getReservation_info());
        assertEquals(result.getTitle(), getDetailTitle(file));
    }

    private JSONObject getData(String file) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject copiestable = new JSONObject();
        switch (file) {
            case "mannheim.html":
                copiestable.put("barcode", 0);
                copiestable.put("branch", 1);
                copiestable.put("department", 2);
                copiestable.put("location", 3);
                copiestable.put("reservations", 6);
                copiestable.put("returndate", 5);
                copiestable.put("status", 4);
                break;
        }
        json.put("copiestable", copiestable);
        return json;
    }

    private String getFirstResultHtml(String file) {
        switch (file) {
            case "mannheim.html":
                return "Anelli, Melissa:<br>Das Phänomen <b><b>Harry</b></b> <b><b>Potter</b></b>" +
                        " : alles über einen jungen Zauberer, seine Fans und eine magische " +
                        "Erfolgsgeschichte / Melissa Anelli - 2009";

        }
        return null;
    }

    private String getDetailTitle(String file) {
        switch (file) {
            case "mannheim.html":
                return "Harry Potter und der Stein der Weisen";
        }
        return null;
    }
}
