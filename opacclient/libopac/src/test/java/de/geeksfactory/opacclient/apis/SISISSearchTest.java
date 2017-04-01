package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Volume;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SISISSearchTest extends BaseHtmlTest {
    private String file;

    public SISISSearchTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"berlin_htw.html"};

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
            throws OpacApi.OpacErrorException, JSONException, IOException {
        String html1 = readResource("/sisis/result_detail/" + file.replace(".html", "_1.html"));
        String html2 = readResource("/sisis/result_detail/" + file.replace(".html", "_2.html"));
        String html3 = readResource("/sisis/result_detail/" + file.replace(".html", "_3.html"));
        String coverJs = readResource("/sisis/result_detail/" + file.replace(".html", ".js"));
        if (html1 == null || html2 == null || html3 == null) {
            return; // we may not have all files for all libraries
        }
        DetailedItem result = SISIS.parseDetail(html1, html2, html3, coverJs, new JSONObject(),
                new DummyStringProvider());
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

        if (file.equals("berlin_htw.html")) {
            assertTrue(result.getDetails().contains(new Detail("Signatur:", "15/2322")));
            assertNotNull(result.getCover());
        }
    }

    private String getDetailTitle(String file) {
        switch (file) {
            case "berlin_htw.html":
                return "Agile business intelligence";
        }
        return null;
    }
}
