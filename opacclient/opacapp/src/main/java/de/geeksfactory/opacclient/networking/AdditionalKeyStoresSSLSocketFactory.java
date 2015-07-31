package de.geeksfactory.opacclient.networking;

//Based on
//http://stackoverflow.com/a/6378872/336784
//and
//http://download.oracle.com/javase/1.5.0/docs/guide/security/jsse/SSERefGuide.html#X509TrustManager
//and
//https://github.com/nelenkov/custom-cert-https

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifierHC4;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * Allows you to trust certificates from additional KeyStores in addition to the default KeyStore
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class AdditionalKeyStoresSSLSocketFactory {

    final static String TAG = "opacclient.tls";

    public static ConnectionSocketFactory create(KeyStore keyStore)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new AdditionalKeyStoresTrustManager(keyStore)},
                null);
        return new TlsSniSocketFactory(sslContext);
    }

    public static class TlsSniSocketFactory extends SSLConnectionSocketFactory {
        private javax.net.ssl.SSLSocketFactory socketfactory;
        private final X509HostnameVerifier hostnameVerifier;

        public TlsSniSocketFactory(final SSLContext sslContext) {
            super(sslContext, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            socketfactory = sslContext.getSocketFactory();
            hostnameVerifier = BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
        }

        @TargetApi(17)
        public Socket createLayeredSocket(
                final Socket socket,
                final String target,
                final int port,
                final HttpContext context) throws IOException {
            final SSLSocket sslsock = (SSLSocket) this.socketfactory.createSocket(
                    socket,
                    target,
                    port,
                    true);
            // If supported protocols are not explicitly set, remove all SSL protocol versions
            final String[] allProtocols = sslsock.getEnabledProtocols();
            final List<String> enabledProtocols = new ArrayList<String>(allProtocols.length);
            for (String protocol: allProtocols) {
                if (!protocol.startsWith("SSL")) {
                    enabledProtocols.add(protocol);
                }
            }
            if (!enabledProtocols.isEmpty()) {
                sslsock.setEnabledProtocols(
                        enabledProtocols.toArray(new String[enabledProtocols.size()]));
            }

            Log.d(TAG, "Enabled protocols: " + Arrays.asList(sslsock.getEnabledProtocols()));
            Log.d(TAG, "Enabled cipher suites:" + Arrays.asList(sslsock.getEnabledCipherSuites()));
            prepareSocket(sslsock);

            // Android specific code to enable SNI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Log.d(TAG, "Enabling SNI for " + target);
                try {
                    Method method = sslsock.getClass().getMethod("setHostname", String.class);
                    method.invoke(sslsock, target);
                } catch (Exception ex) {
                    Log.d(TAG, "SNI configuration failed", ex);
                }
            } else {
                Log.d(TAG, "No documented SNI support on Android <4.2, trying with reflection");
                try {
                    java.lang.reflect.Method setHostnameMethod = sslsock.getClass().getMethod("setHostname", String.class);
                    setHostnameMethod.invoke(sslsock, target);
                } catch (Exception e) {
                    Log.w(TAG, "SNI not useable", e);
                }
            }

            Log.d(TAG, "Starting handshake");
            sslsock.startHandshake();
            verifyHostname(sslsock, target);
            return sslsock;
        }

        private void verifyHostname(final SSLSocket sslsock, final String hostname) throws IOException {
            try {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    try {
                        final SSLSession session = sslsock.getSession();
                        Log.d(TAG, "Secure session established");
                        Log.d(TAG, " negotiated protocol: " + session.getProtocol());
                        Log.d(TAG, " negotiated cipher suite: " + session.getCipherSuite());

                        final Certificate[] certs = session.getPeerCertificates();
                        final X509Certificate x509 = (X509Certificate) certs[0];
                        final X500Principal peer = x509.getSubjectX500Principal();

                        Log.d(TAG, " peer principal: " + peer.toString());
                        final Collection<List<?>> altNames1 = x509.getSubjectAlternativeNames();
                        if (altNames1 != null) {
                            final List<String> altNames = new ArrayList<String>();
                            for (final List<?> aC : altNames1) {
                                if (!aC.isEmpty()) {
                                    altNames.add((String) aC.get(1));
                                }
                            }
                            Log.d(TAG, " peer alternative names: " + altNames);
                        }

                        final X500Principal issuer = x509.getIssuerX500Principal();
                        Log.d(TAG, " issuer principal: " + issuer.toString());
                        final Collection<List<?>> altNames2 = x509.getIssuerAlternativeNames();
                        if (altNames2 != null) {
                            final List<String> altNames = new ArrayList<String>();
                            for (final List<?> aC : altNames2) {
                                if (!aC.isEmpty()) {
                                    altNames.add((String) aC.get(1));
                                }
                            }
                            Log.d(TAG, " issuer alternative names: " + altNames);
                        }
                    } catch (Exception ignore) {
                    }
                }

                this.hostnameVerifier.verify(hostname, sslsock);
                // verifyHostName() didn't blowup - good!
            } catch (final IOException iox) {
                // close the socket before re-throwing the exception
                try { sslsock.close(); } catch (final Exception x) { /*ignore*/ }
                throw iox;
            }
        }
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
                localTrustManager.checkClientTrusted(
                        new X509Certificate[]{chain[0]}, authType);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(
                        new X509Certificate[]{chain[0]}, authType);
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