package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.LentItem;
import okhttp3.RequestBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class WebOpacNetAccountTest extends BaseHtmlTest {
    private WebOpacNet api;

    @Before
    public void setUp() throws IOException, OpacApi.OpacErrorException {
        api = spy(WebOpacNet.class);
        api.sessionId = "xyz";
        api.stringProvider = new DummyStringProvider();
    }

    @Test
    public void testParseMediaList() throws JSONException {
        String json = readResource("/webopac.net/account.json");
        AccountData data = new AccountData(0);
        WebOpacNet.parseAccount(new JSONObject(json), data);
        assertEquals(4, data.getLent().size());
        assertEquals(1, data.getReservations().size());
        for (LentItem item : data.getLent()) {
            assertNotNull(item.getTitle());
            assertNotNull(item.getDeadline());
            assertNotNull(item.getMediaType());
            assertContainsData(item.getCover());
        }
    }

    @Test
    public void testProlongImpossible() throws IOException {
        String json = readResource("/webopac.net/prolong_impossible.json");
        doReturn(json).when(api).httpPost(anyString(), any(RequestBody.class), anyString());

        OpacApi.ProlongResult result = api.prolong("abc", new Account(), 0, null);
        assertEquals(OpacApi.MultiStepResult.Status.ERROR, result.status);
        assertEquals(
                "Diese Ausleihe darf erst ab 7 Tage vor Ablauf der Ausleihfrist verlängert werden.",
                result.message);
    }

    @Test
    public void testProlongSuccess() throws IOException {
        String json = readResource("/webopac.net/prolong_possible.json");
        String json2 = readResource("/webopac.net/prolong_success.json");
        doReturn(json).doReturn(json2).when(api)
                      .httpPost(anyString(), any(RequestBody.class), anyString());

        OpacApi.ProlongResult result = api.prolong("abc", new Account(), 0, null);
        assertEquals(OpacApi.MultiStepResult.Status.OK, result.status);
    }

    @Test
    public void testProlongFee() throws IOException {
        String json = readResource("/webopac.net/prolong_fee.json");
        String json2 = readResource("/webopac.net/prolong_success.json");
        doReturn(json).doReturn(json2).when(api)
                      .httpPost(anyString(), any(RequestBody.class), anyString());

        OpacApi.ProlongResult result = api.prolong("abc", new Account(), 0, null);
        assertEquals(OpacApi.MultiStepResult.Status.CONFIRMATION_NEEDED, result.status);
        assertEquals("fee_confirmation 1.00", result.message);

        result = api.prolong("abc", new Account(), OpacApi.MultiStepResult.ACTION_CONFIRMATION,
                null);
        assertEquals(OpacApi.MultiStepResult.Status.OK, result.status);
    }

    @Test
    public void testProlongAllImpossible() throws IOException {
        String json = readResource("/webopac.net/prolong_all_impossible.json");
        doReturn(json).when(api).httpPost(anyString(), any(RequestBody.class), anyString());

        OpacApi.ProlongAllResult result = api.prolongAll(new Account(), 0, null);
        assertEquals(OpacApi.MultiStepResult.Status.ERROR, result.status);
        assertEquals(
                "Die Medien können nicht verlängert werden.",
                result.message);
    }

}
