package de.geeksfactory.opacclient.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtils {
    public static Bitmap bitmapFromBytes(byte[] b) {
        if (b == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }
}
