/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package de.geeksfactory.opacclient.networking;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.Asserts;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.KeyStore;

public class HTTPClient {

    public static HttpClient getNewHttpClient(boolean customssl) {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setRedirectStrategy(new HTTPClient.CustomRedirectStrategy());
        builder.setUserAgent("OpacApp/Test");
        if (customssl) {
            try {
                final KeyStore trustStore = KeyStore.getInstance("BKS");
                InputStream in;
                try {
                    in = Files.newInputStream(Paths.get("../res/raw/ssl_trust_store.bks"));
                } catch (NoSuchFileException e) {
                    in = Files.newInputStream(
                            Paths.get("opacapp/src/main/res/raw/ssl_trust_store.bks"));
                }
                try {
                    trustStore.load(in,
                            "ro5eivoijeeGohsh0daequoo5Zeepaen".toCharArray());
                } finally {
                    in.close();
                }

                SSLConnectionSocketFactory sf =
                        AdditionalKeyStoresSSLSocketFactory.create(trustStore);

                Registry<ConnectionSocketFactory> registry =
                        RegistryBuilder.<ConnectionSocketFactory>create().register("http",
                                PlainConnectionSocketFactory.getSocketFactory()).register(
                                "https", sf).build();

                HttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager(registry);
                builder.setConnectionManager(ccm);

                return builder.build();
            } catch (Exception e) {
                e.printStackTrace();
                return builder.build();
            }
        } else {
            return builder.build();
        }
    }

    public static class CustomRedirectStrategy extends LaxRedirectStrategy {


        public CustomRedirectStrategy() {
            super();
        }

        @Override
        public URI getLocationURI(final HttpRequest request, final HttpResponse response,
                final HttpContext context) throws ProtocolException {
            Args.notNull(request, "HTTP request");
            Args.notNull(response, "HTTP response");
            Args.notNull(context, "HTTP context");

            final HttpClientContext clientContext = HttpClientContext.adapt(context);

            //get the location header to find out where to redirect to
            final Header locationHeader = response.getFirstHeader("location");
            if (locationHeader == null) {
                // got a redirect response, but no location header
                throw new ProtocolException(
                        "Received redirect response " + response.getStatusLine()
                                + " but no location header");
            }
            final String location = locationHeader.getValue().replaceAll(" ", "%20");

            final RequestConfig config = clientContext.getRequestConfig();

            URI uri = createLocationURI(location);

            // rfc2616 demands the location value be a complete URI
            // Location       = "Location" ":" absoluteURI
            try {
                if (!uri.isAbsolute()) {
                    if (!config.isRelativeRedirectsAllowed()) {
                        throw new ProtocolException("Relative redirect location '"
                                + uri + "' not allowed");
                    }
                    // Adjust location URI
                    final HttpHost target = clientContext.getTargetHost();
                    Asserts.notNull(target, "Target host");
                    final URI requestURI = new URI(request.getRequestLine().getUri());
                    final URI absoluteRequestURI = URIUtils.rewriteURI(requestURI, target, false);
                    uri = URIUtils.resolve(absoluteRequestURI, uri);
                }
            } catch (final URISyntaxException ex) {
                throw new ProtocolException(ex.getMessage(), ex);
            }

            RedirectLocations redirectLocations = (RedirectLocations) clientContext.getAttribute(
                    HttpClientContext.REDIRECT_LOCATIONS);
            if (redirectLocations == null) {
                redirectLocations = new RedirectLocations();
                context.setAttribute(HttpClientContext.REDIRECT_LOCATIONS, redirectLocations);
            }
            if (!config.isCircularRedirectsAllowed()) {
                if (redirectLocations.contains(uri)) {
                    throw new CircularRedirectException("Circular redirect to '" + uri + "'");
                }
            }
            redirectLocations.add(uri);
            return uri;
        }
    }
}
