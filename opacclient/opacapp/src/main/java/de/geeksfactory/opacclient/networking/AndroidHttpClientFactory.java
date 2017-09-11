/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.geeksfactory.opacclient.networking;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;

public class AndroidHttpClientFactory extends HttpClientFactory {

    public AndroidHttpClientFactory() {
        super("OpacApp/" + OpacClient.versionName);
    }

    @Override
    public KeyStore getKeyStore()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore trustStore = KeyStore.getInstance("BKS");
        final InputStream in = OpacClient.context.getResources().openRawResource(
                R.raw.ssl_trust_store);
        try {
            trustStore.load(in, "ro5eivoijeeGohsh0daequoo5Zeepaen".toCharArray());
        } finally {
            in.close();
        }
        return trustStore;
    }

    @Override
    protected Class<?> getSocketFactoryClass(boolean tls_only, boolean allCipherSuites) {
        if (tls_only) {
            if (allCipherSuites) {
                return TlsSniSocketFactoryWithAllCipherSuites.class;
            } else {
                return TlsSniSocketFactory.class;
            }
        } else {
            return TlsSniSocketFactoryWithSSL3.class;
        }
    }
}