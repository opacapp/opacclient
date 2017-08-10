package de.geeksfactory.opacclient.networking;

import javax.net.ssl.SSLContext;

/**
 * Created by johan_000 on 03.08.2017.
 */

class TlsSniSocketFactoryWithAllCipherSuites extends TlsSniSocketFactory {
    public TlsSniSocketFactoryWithAllCipherSuites(SSLContext sslContext) {
        super(sslContext);
        this.allCipherSuites = true;
    }
}
