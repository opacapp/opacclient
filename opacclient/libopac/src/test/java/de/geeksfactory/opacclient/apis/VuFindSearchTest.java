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
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Volume;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class VuFindSearchTest extends BaseHtmlTest {
    private String file;

    public VuFindSearchTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"muenster_volumes.html", "muenster_copies.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseDetail()
            throws OpacApi.OpacErrorException, JSONException, NotReachableException {
        String html = readResource("/vufind/result_detail/" + file);
        if (html == null) return; // we may not have all files for all libraries
        DetailedItem result = VuFind.parseDetail("0", Jsoup.parse(html), getData(file));
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
        for (Volume volume : result.getVolumes()) {
            assertContainsData(volume.getId());
            assertContainsData(volume.getTitle());
        }
        assertEquals(result.getTitle(), getDetailTitle(file));
    }

    private JSONObject getData(String file) throws JSONException {
        switch (file) {
            case "muenster_volumes.html":
            case "muenster_copies.html":
                return new JSONObject("{\n" +
                        "        \"copystyle\": \"stackedtable\",\n" +
                        "        \"copytable\": {\n" +
                        "            \"_offset\": 1,\n" +
                        "            \"branch\": 0,\n" +
                        "            \"department\": 1,\n" +
                        "            \"location\": 3,\n" +
                        "            \"status\": 4\n" +
                        "        }\n" +
                        "    }");
        }
        return null;
    }

    private String getDetailTitle(String file) {
        switch (file) {
            case "muenster_volumes.html":
                return "brand eins : 2014 ; Wirtschaftsmagazin";
            case "muenster_copies.html":
                return "Brand : Roman";
        }
        return null;
    }
}
