package de.geeksfactory.opacclient.utils;

import org.acra.ACRA;

import de.geeksfactory.opacclient.BuildConfig;

/**
 * Helper class to delegate errors to ACRA in the release version and rethrow them in the debug version
 */
public class ErrorReporter {
    public static void handleException(Throwable e) {
        if (BuildConfig.DEBUG) {
            throw new RuntimeException(e);
        } else {
            ACRA.getErrorReporter().handleException(e);
        }
    }
}
