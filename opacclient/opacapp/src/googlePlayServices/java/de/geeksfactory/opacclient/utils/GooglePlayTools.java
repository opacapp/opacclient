package de.geeksfactory.opacclient.utils;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.security.ProviderInstaller;

public class GooglePlayTools {

    public static void updateSecurityProvider(Context appContext) {
        ProviderInstaller.installIfNeededAsync(appContext, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {

            }

            @Override
            public void onProviderInstallFailed(int i, Intent intent) {

            }
        });
    }
}