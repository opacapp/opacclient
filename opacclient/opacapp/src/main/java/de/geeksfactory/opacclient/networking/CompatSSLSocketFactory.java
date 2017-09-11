package de.geeksfactory.opacclient.networking;

import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class CompatSSLSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory internalSSLSocketFactory;

    public CompatSSLSocketFactory(SSLContext context)
            throws KeyManagementException, NoSuchAlgorithmException {
        context.init(null, null, null);
        internalSSLSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return modifySocket(internalSSLSocketFactory.createSocket(), null);
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        return modifySocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose),
                host);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return modifySocket(internalSSLSocketFactory.createSocket(host, port), host);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        return modifySocket(
                internalSSLSocketFactory.createSocket(host, port, localHost, localPort),
                host);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return modifySocket(internalSSLSocketFactory.createSocket(host, port), host.getHostName());
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        return modifySocket(
                internalSSLSocketFactory.createSocket(address, port, localAddress, localPort),
                address.getHostName());
    }

    protected Socket modifySocket(Socket socket, String hostname) {
        if (socket != null && (socket instanceof SSLSocket)) {
            ((SSLSocket) socket).setEnabledProtocols(((SSLSocket) socket).getSupportedProtocols());
            ((SSLSocket) socket)
                    .setEnabledCipherSuites(((SSLSocket) socket).getEnabledCipherSuites());

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // No documented SNI support on Android <4.2, trying with reflection
                try {
                    java.lang.reflect.Method setHostnameMethod =
                            socket.getClass().getMethod("setHostname", String.class);
                    setHostnameMethod.invoke(socket, hostname);
                } catch (Exception e) {
                }
            }

        }
        return socket;
    }
}