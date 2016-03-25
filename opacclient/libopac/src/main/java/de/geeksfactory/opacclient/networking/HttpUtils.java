package de.geeksfactory.opacclient.networking;

import org.apache.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;

public class HttpUtils {
    /**
     * Re-implement this here as it is in different classes on Android and non-Android due to the
     * Android backporting process of HttpClient 4
     */

    public static void consume(HttpEntity entity) throws IOException {
        if (entity == null) {
            return;
        }
        if (entity.isStreaming()) {
            final InputStream instream = entity.getContent();
            if (instream != null) {
                instream.close();
            }
        }
    }
}
