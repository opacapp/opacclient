package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.objects.DetailedItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(Parameterized.class)
public class AdisReservationTest extends BaseHtmlTest {
    public static final String DIR = "/adis/reservation/";
    private String file;
    private Adis adis;
    private DetailedItem item;

    public AdisReservationTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"muenchen"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Before
    public void setUp() throws IOException, OpacApi.OpacErrorException {
        adis = spy(Adis.class);
        String html = readResource(DIR + file + "_detail.html");
        Document detailDoc = Jsoup.parse(html);
        item = adis.parseResult(getId(file), detailDoc);
        adis.updatePageform(detailDoc);
    }

    private String getId(String file) {
        switch (file) {
            case "muenchen":
                return "AK04076347";
            default:
                return null;
        }
    }

    @Test
    public void testReservation() throws JSONException, IOException, OpacApi.OpacErrorException {
        testReservationStep1();
        String selection = testReservationStep2();
        selection = testReservationStep3(selection);
        testReservationStep4(selection);
    }

    public void testReservationStep1() throws OpacApi.OpacErrorException, JSONException,
            IOException {
        String html = readResource(DIR + file + "_1.html");
        doReturn(Jsoup.parse(html)).when(adis).htmlPost(anyString(), any(List.class));
        OpacApi.ReservationResult result = adis.reservation(item, null, 0, null);
        assertEquals(result.status, OpacApi.MultiStepResult.Status.CONFIRMATION_NEEDED);
    }

    public String testReservationStep2() throws OpacApi.OpacErrorException, JSONException,
            IOException {
        String html = readResource(DIR + file + "_1.html");
        doReturn(Jsoup.parse(html)).when(adis).htmlPost(anyString(), any(List.class));
        OpacApi.ReservationResult result = adis.reservation(item, null, 0, "confirmed");
        assertEquals(result.status, OpacApi.MultiStepResult.Status.SELECTION_NEEDED);
        assertTrue(result.selection.size() > 0);
        assertEquals(result.message, "Ausgabeort");
        return result.selection.get(0).get("key");
    }

    public String testReservationStep3(String selection)
            throws OpacApi.OpacErrorException, JSONException,
            IOException {
        String html = readResource(DIR + file + "_1.html");
        doReturn(Jsoup.parse(html)).when(adis).htmlPost(anyString(), any(List.class));
        OpacApi.ReservationResult result = adis.reservation(item, null, 0, selection);
        assertEquals(result.status, OpacApi.MultiStepResult.Status.SELECTION_NEEDED);
        assertTrue(result.selection.size() == 2);
        assertEquals(result.message, "Benachrichting per Brief - kostenpflichtig"); // sic!
        assertEquals(result.selection.get(1).get("value"), "Nein");
        assertTrue(result.selection.get(1).get("key").contains("_SEP_"));
        return result.selection.get(1).get("key");
    }

    public void testReservationStep4(String selection)
            throws OpacApi.OpacErrorException, JSONException,
            IOException {
        String html = readResource(DIR + file + "_2.html");
        doReturn(Jsoup.parse(html)).when(adis).htmlPost(anyString(), any(List.class));
        OpacApi.ReservationResult result = adis.reservation(item, null, 0, selection);
        System.out.println(result.status);
    }
}
