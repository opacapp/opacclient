package de.geeksfactory.opacclient.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;

public class GooglePlayTools {

    public static void updateSecurityProvider(Context appContext) {
        ProviderInstaller.installIfNeededAsync(appContext, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {
                Log.d("OpacApp", "Google Play security provider installed.");
            }

            @Override
            public void onProviderInstallFailed(int i, Intent intent) {
                Log.d("OpacApp", String.format("Google Play security provider failed to install. Error code: %d", i));
            }
        });
    }
}