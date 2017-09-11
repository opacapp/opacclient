package de.geeksfactory.opacclient.networking;

//Based on
//http://stackoverflow.com/a/6378872/336784
//and
//http://download.oracle.com/javase/1.5.0/docs/guide/security/jsse/SSERefGuide.html#X509TrustManager
//and
//https://github.com/nelenkov/custom-cert-https

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Allows you to trust certificates from additional KeyStores in addition to the default KeyStore
 */
public class AdditionalKeyStoresSSLSocketFactory {

    /**
     * Creates a customized keystore
     *
     * @param socketFactory The class that should be used to instantiate a new socket factory, must
     *                      be a subclass of {@link SSLConnectionSocketFactory}.
     * @return a new {@link SSLConnectionSocketFactory}
     */
    public static SSLConnectionSocketFactory create(Class<?> socketFactory,
            X509TrustManager trustManager)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        if (socketFactory != null) {
            try {
                return (SSLConnectionSocketFactory) socketFactory
                        .getDeclaredConstructor(SSLContext.class).newInstance
                                (sslContext);
            } catch (Exception e) {
                // Fall back to default
                e.printStackTrace();
            }
        }
        return new SSLConnectionSocketFactory(sslContext);
    }

    /**
     * Creates a customized keystore for OkHttp
     *
     * @return a new {@link SSLConnectionSocketFactory}
     */
    public static SSLSocketFactory createForOkHttp(X509TrustManager trustManager)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        return sslContext.getSocketFactory();
    }

    public static class AdditionalKeyStoresTrustManager implements
            X509TrustManager {

        private X509TrustManager defaultTrustManager;
        private X509TrustManager localTrustManager;
        private X509Certificate[] acceptedIssuers;

        public AdditionalKeyStoresTrustManager(KeyStore localKeyStore) {
            try {
                TrustManagerFactory tmf = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);

                defaultTrustManager = findX509TrustManager(tmf);
                if (defaultTrustManager == null) {
                    throw new IllegalStateException(
                            "Couldn't find X509TrustManager");
                }

                localTrustManager = new LocalStoreX509TrustManager(
                        localKeyStore);

                List<X509Certificate> allIssuers = new ArrayList<>();
                Collections.addAll(allIssuers, defaultTrustManager
                        .getAcceptedIssuers());
                Collections.addAll(allIssuers, localTrustManager
                        .getAcceptedIssuers());
                acceptedIssuers = allIssuers
                        .toArray(new X509Certificate[allIssuers.size()]);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }

        }

        static X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
            TrustManager tms[] = tmf.getTrustManagers();
            for (TrustManager tm : tms) {
                if (tm instanceof X509TrustManager) {
                    return (X509TrustManager) tm;
                }
            }

            return null;
        }

        public static String getThumbPrint(X509Certificate cert)
                throws NoSuchAlgorithmException, CertificateEncodingException {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            return hexify(digest);

        }

        public static String hexify(byte bytes[]) {
            char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                    '9', 'a', 'b', 'c', 'd', 'e', 'f'};

            StringBuilder buf = new StringBuilder(bytes.length * 2);

            for (byte aByte : bytes) {
                buf.append(hexDigits[(aByte & 0xf0) >> 4]);
                buf.append(hexDigits[aByte & 0x0f]);
            }

            return buf.toString();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                defaultTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkClientTrusted(chain, authType);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return acceptedIssuers;
        }

        static class LocalStoreX509TrustManager implements X509TrustManager {

            private X509TrustManager trustManager;

            LocalStoreX509TrustManager(KeyStore localTrustStore) {
                try {
                    TrustManagerFactory tmf = TrustManagerFactory
                            .getInstance(TrustManagerFactory
                                    .getDefaultAlgorithm());
                    tmf.init(localTrustStore);

                    trustManager = findX509TrustManager(tmf);
                    if (trustManager == null) {
                        throw new IllegalStateException(
                                "Couldn't find X509TrustManager");
                    }
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
                trustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
                trustManager.checkServerTrusted(chain, authType);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return trustManager.getAcceptedIssuers();
            }
        }
    }

}