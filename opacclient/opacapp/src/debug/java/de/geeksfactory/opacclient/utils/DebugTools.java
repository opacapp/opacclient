package de.geeksfactory.opacclient.utils;

import android.app.Application;

import com.facebook.stetho.Stetho;

public class DebugTools {
    public static void init(Application app) {
        Stetho.initializeWithDefaults(app);
    }
}
