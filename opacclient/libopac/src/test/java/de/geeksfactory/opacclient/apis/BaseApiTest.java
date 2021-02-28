package de.geeksfactory.opacclient.apis;

import org.junit.Test;

import static de.geeksfactory.opacclient.apis.BaseApi.cleanUrl;
import static org.junit.Assert.assertEquals;

public class BaseApiTest {
    @Test
    public void cleanUrlShouldHandleMultipleEqualsSigns() {
        String url = "http://www.example.com/file?param1=value=1&param=value2";
        assertEquals("http://www.example.com/file?param1=value%3D1&param=value2",
                cleanUrl(url));
    }

    @Test
    public void cleanUrlShouldHandleKeyOnlyParameters() {
        String url = "http://www.example.com/file?param1&param2=value2";
        assertEquals("http://www.example.com/file?param1&param2=value2",
                cleanUrl(url));
    }
}
