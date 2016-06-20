package de.geeksfactory.opacclient.utils;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;

public class DebugTools {
    public static void init(Application app) {
        Stetho.initializeWithDefaults(app);
    }

    public static OkHttpClient.Builder prepareHttpClient(OkHttpClient.Builder builder) {
        return builder.addNetworkInterceptor(new StethoInterceptor());
    }
}
