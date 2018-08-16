package de.geeksfactory.opacclient.ui.vectordrawable;

import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;

/**
 * Copied from AOSP code
 */
public class DrawableUtils {
    private static final String VECTOR_DRAWABLE_CLAZZ_NAME
            = "android.graphics.drawable.VectorDrawable";

    /**
     * Attempt the fix any issues in the given drawable, usually caused by platform bugs in the
     * implementation. This method should be call after retrieval from {@link
     * android.content.res.Resources} or a {@link android.content.res.TypedArray}.
     */
    static void fixDrawable(@NonNull final Drawable drawable) {
        if (Build.VERSION.SDK_INT == 21
                && VECTOR_DRAWABLE_CLAZZ_NAME.equals(drawable.getClass().getName())) {
            fixVectorDrawableTinting(drawable);
        }
    }

    /**
     * VectorDrawable has an issue on API 21 where it sometimes doesn't create its tint filter.
     * Fixed by toggling it's state to force a filter creation.
     */
    private static void fixVectorDrawableTinting(final Drawable drawable) {
        final int[] originalState = drawable.getState();
        if (originalState == null || originalState.length == 0) {
            // The drawable doesn't have a state, so set it to be checked
            drawable.setState(ThemeUtils.CHECKED_STATE_SET);
        } else {
            // Else the drawable does have a state, so clear it
            drawable.setState(ThemeUtils.EMPTY_STATE_SET);
        }
        // Now set the original state
        drawable.setState(originalState);
    }
}
