package de.geeksfactory.opacclient.networking;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.auth.x500.X500Principal;

public class TlsSniSocketFactory extends SSLConnectionSocketFactory {
    final static String TAG = "opacclient.tls";
    private javax.net.ssl.SSLSocketFactory socketfactory;
    private final X509HostnameVerifier hostnameVerifier;
    protected boolean tls_only = true;
    public boolean allCipherSuites = false;

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
        final String[] allProtocols = sslsock.getSupportedProtocols();
        final List<String> enabledProtocols = new ArrayList<String>(allProtocols.length);
        for (String protocol : allProtocols) {
            if (!protocol.startsWith("SSL") || !tls_only) {
                enabledProtocols.add(protocol);
            }
        }
        if (!enabledProtocols.isEmpty()) {
            sslsock.setEnabledProtocols(
                    enabledProtocols.toArray(new String[enabledProtocols.size()]));
        }

        if (allCipherSuites) {
            sslsock.setEnabledCipherSuites(sslsock.getSupportedCipherSuites());
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
                java.lang.reflect.Method setHostnameMethod =
                        sslsock.getClass().getMethod("setHostname", String.class);
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

    private void verifyHostname(final SSLSocket sslsock, final String hostname)
            throws IOException {
        try {
            if (hostname.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                // In very rare cases, we have endpoints with SSL enabled behind an IP address.
                // Getting SSL certificates for an IP address is hard, so they always have
                // invalid certificates. There is no way we could do a proper host name check
                // here, so we skip it and still have more security than with SSL disabled.
                // Note that this only applies to endpoints where an IP address is stored in
                // *our* configuration, so this does not allow any new attacks on all other
                // endpoints.
                return;
            }
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
            try {
                sslsock.close();
            } catch (final Exception x) { /*ignore*/ }
            throw iox;
        }
    }
}