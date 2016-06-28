package de.geeksfactory.opacclient.utils;

import android.app.Application;

import okhttp3.OkHttpClient;

public class DebugTools {
    public static void init(Application app) {
        // this is only used in the debug version of the app
    }

    public static OkHttpClient.Builder prepareHttpClient(OkHttpClient.Builder builder) {
        return builder;
    }
}
