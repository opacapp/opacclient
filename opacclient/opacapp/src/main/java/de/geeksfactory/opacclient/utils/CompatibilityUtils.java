package de.geeksfactory.opacclient.utils;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

/**
 * Utility methods for compatibility between Android versions
 */
public class CompatibilityUtils {
    /**
     * @return {@link Intent#FLAG_ACTIVITY_NEW_DOCUMENT} on API 21 and above and {@link Intent#FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET} on other versions
     */
    @TargetApi(21)
    public static int getNewDocumentIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        } else {
            //noinspection deprecation
            return Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
    }
}
