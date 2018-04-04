package de.geeksfactory.opacclient.apis;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import de.geeksfactory.opacclient.objects.Account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class TouchPointTest extends BaseHtmlTest {
    private TouchPoint touchPoint;
    private Account account;

    @Before
    public void setUp() {
        touchPoint = spy(TouchPoint.class);
        touchPoint.opac_url = "http://opac.example.com";

        account = new Account();
        account.setName("username");
        account.setName("password");
    }

    @Test
    public void testLoginError() throws IOException {
        doReturn(readResource("/touchpoint/login/loginPage.html"))
                .when(touchPoint).httpGet(eq(touchPoint.opac_url + "/login.do"), anyString());
        doReturn(readResource("/touchpoint/login/errorMessage.html"))
                .when(touchPoint)
                .httpPost(eq(touchPoint.opac_url + "/login.do"), any(UrlEncodedFormEntity.class),
                        anyString());

        try {
            touchPoint.login(account);
        } catch (OpacApi.OpacErrorException e) {
            assertEquals(
                    "Anmeldung fehlgeschlagen, ein Fehler ist aufgetreten. 0000000test: " +
                            "Benutzernummer nicht korrekt oder keine Benutzerdaten vorhanden.",
                    e.getMessage());
            return;
        }
        fail("no OpacErrorException thrown");
    }

    @Test
    public void testLoginSuccess() throws OpacApi.OpacErrorException, IOException {
        doReturn(readResource("/touchpoint/login/loginPage.html"))
                .when(touchPoint).httpGet(eq(touchPoint.opac_url + "/login.do"), anyString());
        doReturn(readResource("/touchpoint/login/loggedIn.html"))
                .when(touchPoint)
                .httpPost(eq(touchPoint.opac_url + "/login.do"), any(UrlEncodedFormEntity.class),
                        anyString());


        TouchPoint.LoginResponse login = touchPoint.login(account);
        assertTrue(login.success);
        assertNull(login.warning);
    }

    @Test
    public void testLoginMessage() throws OpacApi.OpacErrorException, IOException {
        doReturn(readResource("/touchpoint/login/loginPage.html"))
                .when(touchPoint).httpGet(eq(touchPoint.opac_url + "/login.do"), anyString());
        doReturn(readResource("/touchpoint/login/alertMessage.html"))
                .when(touchPoint)
                .httpPost(eq(touchPoint.opac_url + "/login.do"), any(UrlEncodedFormEntity.class),
                        anyString());
        doReturn(readResource("/touchpoint/login/loggedIn.html"))
                .when(touchPoint)
                .httpGet(eq(touchPoint.opac_url + "/login.do?methodToCall=done"), anyString());


        TouchPoint.LoginResponse login = touchPoint.login(account);
        assertTrue(login.success);
        assertNull(login.warning);
    }

    @Test
    public void testLoginWarning() throws OpacApi.OpacErrorException, IOException {
        doReturn(readResource("/touchpoint/login/loginPage.html"))
                .when(touchPoint).httpGet(eq(touchPoint.opac_url + "/login.do"), anyString());
        doReturn(readResource("/touchpoint/login/warningMessage.html"))
                .when(touchPoint)
                .httpPost(eq(touchPoint.opac_url + "/login.do"), any(UrlEncodedFormEntity.class),
                        anyString());
        doReturn(readResource("/touchpoint/login/loggedIn.html"))
                .when(touchPoint)
                .httpGet(eq(touchPoint.opac_url + "/login.do?methodToCall=done"), anyString());


        TouchPoint.LoginResponse login = touchPoint.login(account);
        assertTrue(login.success);
        assertEquals("Nutzungseinschränkung", login.warning);
    }
}
