package de.geeksfactory.opacclient.networking;

import javax.net.ssl.SSLContext;

public class TlsSniSocketFactoryWithSSL3 extends TlsSniSocketFactory {

    public TlsSniSocketFactoryWithSSL3(final SSLContext sslContext) {
        super(sslContext);
        this.tls_only = false;
    }
}