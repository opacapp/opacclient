package de.geeksfactory.opacclient.networking;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtilsHC4;

import java.io.IOException;

/**
 * Provides wrappers around methods that need to be called with *HC4 classes on Android
 */
public class HttpUtils {
    public static void consume(HttpEntity entity) throws IOException {
        EntityUtilsHC4.consume(entity);
    }
}
