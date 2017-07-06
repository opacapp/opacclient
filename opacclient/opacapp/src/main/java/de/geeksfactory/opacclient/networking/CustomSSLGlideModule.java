package de.geeksfactory.opacclient.networking;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import okhttp3.OkHttpClient;

public class CustomSSLGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        try {
            KeyStore keyStore = new AndroidHttpClientFactory().getKeyStore();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            AdditionalKeyStoresSSLSocketFactory.AdditionalKeyStoresTrustManager
                    trustManager =
                    new AdditionalKeyStoresSSLSocketFactory.AdditionalKeyStoresTrustManager(
                            keyStore);
            sslContext.init(null, new TrustManager[]{trustManager}, null);

            OkHttpClient client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .build();
            OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory(client);
            glide.register(GlideUrl.class, InputStream.class, factory);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                CertificateException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

    }
}
