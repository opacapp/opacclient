package de.geeksfactory.opacclient.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtils {
    public static Bitmap bitmapFromBytes(byte[] b) {
        try {
            if (b == null) {
                return null;
            }
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }
}
