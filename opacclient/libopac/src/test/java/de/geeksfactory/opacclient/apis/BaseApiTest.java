package de.geeksfactory.opacclient.apis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaseApiTest {
    @Test
    public void cleanUrlShouldHandleMultipleEqualsSigns() throws Exception {
        String url = "http://www.example.com/file?param1=value=1&param=value2";
        assertEquals("http://www.example.com/file?param1=value%3D1&param=value2",
                BaseApi.cleanUrl(url));
    }
}