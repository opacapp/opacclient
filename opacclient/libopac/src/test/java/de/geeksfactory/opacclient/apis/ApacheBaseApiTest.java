package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ApacheBaseApiTest {
    @Test
    public void cleanUrlShouldHandleMultipleEqualsSigns() throws Exception {
        String url = "http://www.example.com/file?param1=value=1&param=value2";
        assertEquals("http://www.example.com/file?param1=value%3D1&param=value2",
                ApacheBaseApi.cleanUrl(url));
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