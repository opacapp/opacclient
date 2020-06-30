package de.geeksfactory.opacclient.utils;

import de.geeksfactory.opacclient.BuildConfig;
import io.sentry.core.Sentry;

/**
 * Helper class to delegate errors to Sentry in the release version and rethrow them in the debug
 * version
 */
public class ErrorReporter {
    public static void handleException(Throwable e) {
        if (BuildConfig.DEBUG) {
            throw new RuntimeException(e);
        } else {
            Sentry.captureException(e);
        }
    }
}
