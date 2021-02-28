package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApacheBaseApiTest {

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