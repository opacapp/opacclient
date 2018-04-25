package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.LentItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WebOpacNetAccountTest extends BaseHtmlTest {

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

}
