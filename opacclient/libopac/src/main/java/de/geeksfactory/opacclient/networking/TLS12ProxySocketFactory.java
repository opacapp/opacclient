package de.geeksfactory.opacclient.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class TLS12ProxySocketFactory extends SSLSocketFactory {
    private SSLSocketFactory parent;

    public TLS12ProxySocketFactory(SSLSocketFactory sf) {
        this.parent = sf;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return parent.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return parent.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return enableTls12(parent.createSocket(socket, s, i, b));
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return enableTls12(parent.createSocket(s, i));
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1)
            throws IOException, UnknownHostException {
        return enableTls12(parent.createSocket(s, i, inetAddress, i1));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return enableTls12(parent.createSocket(inetAddress, i));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1)
            throws IOException {
        return enableTls12(parent.createSocket(inetAddress, i, inetAddress1, i1));
    }

    private Socket enableTls12(Socket socket) {
        if (socket != null && socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnabledProtocols(((SSLSocket) socket).getSupportedProtocols());
        }
        return socket;
    }
}
