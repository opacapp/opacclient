package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class ApacheBaseApiTest {
    @Test
    public void cleanUrlShouldHandleMultipleEqualsSigns() {
        BaseApi baseApi = Mockito.mock(BaseApi.class, Mockito.CALLS_REAL_METHODS);
        String url = "http://www.example.com/file?param1=value=1&param=value2";
        assertEquals("http://www.example.com/file?param1=value%3D1&param=value2",
                baseApi.cleanUrl(url));
    }

    @Test
    public void testBuildHttpGetParamsList() throws Exception {
        assertThat(ApacheBaseApi.buildHttpGetParams(Arrays.<NameValuePair>asList(
                new BasicNameValuePair("foo", "bar"),
                new BasicNameValuePair("space", " "),
                new BasicNameValuePair("percent", "%")
                )),
                is("?foo=bar&space=+&percent=%25"));
    }
}