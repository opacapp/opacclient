package de.geeksfactory.opacclient.tests;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

import java.util.Arrays;

import de.geeksfactory.opacclient.apis.BaseApi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UtilTest {

    @Test
    public void testBuildHttpGetParamsList() throws Exception {
        assertThat(BaseApi.buildHttpGetParams(Arrays.<NameValuePair>asList(
                        new BasicNameValuePair("foo", "bar"),
                        new BasicNameValuePair("space", " "),
                        new BasicNameValuePair("percent", "%")
                )),
                is("?foo=bar&space=+&percent=%25"));
    }
}
