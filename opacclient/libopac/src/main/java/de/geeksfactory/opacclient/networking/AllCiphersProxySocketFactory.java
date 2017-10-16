package de.geeksfactory.opacclient.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class AllCiphersProxySocketFactory extends SSLSocketFactory {
    SSLSocketFactory parent;

    public AllCiphersProxySocketFactory(SSLSocketFactory parent) {
        this.parent = parent;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return parent.getSupportedCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return parent.getSupportedCipherSuites();
    }

    private Socket modify(SSLSocket sslsock) {
        sslsock.setEnabledCipherSuites(sslsock.getSupportedCipherSuites());
        return sslsock;
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        final SSLSocket sslsock = (SSLSocket) parent.createSocket(socket, s, i, b);
        return modify(sslsock);
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        final SSLSocket sslsock = (SSLSocket) parent.createSocket(s, i);
        return modify(sslsock);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1)
            throws IOException, UnknownHostException {
        final SSLSocket sslsock = (SSLSocket) parent.createSocket(s, i, inetAddress, i1);
        return modify(sslsock);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        final SSLSocket sslsock = (SSLSocket) parent.createSocket(inetAddress, i);
        return modify(sslsock);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1)
            throws IOException {
        final SSLSocket sslsock = (SSLSocket) parent.createSocket(inetAddress, i, inetAddress1, i1);
        return modify(sslsock);
    }
}
