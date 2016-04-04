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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class HttpClientFactory {

    public String user_agent;
    public String ssl_store_path = "../res/raw/ssl_trust_store.bks";
    private KeyStore trust_store;

    public HttpClientFactory(String user_agent) {
        this.user_agent = user_agent;
    }

    public HttpClientFactory(String user_agent, String ssl_store_path) {
        this.user_agent = user_agent;
        this.ssl_store_path = ssl_store_path;
    }

    protected KeyStore getKeyStore()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore trustStore = KeyStore.getInstance("BKS");
        InputStream in;
        try {
            in = new FileInputStream(ssl_store_path);
        } catch (FileNotFoundException e) {
            in = new FileInputStream("opacapp/src/main/res/raw/ssl_trust_store.bks");
        }
        try {
            trustStore.load(in,
                    "ro5eivoijeeGohsh0daequoo5Zeepaen".toCharArray());
        } finally {
            in.close();
        }
        return trustStore;
    }

    protected Class<?> getSocketFactoryClass(boolean tls_only) {
        return null;
    }

    public HttpClient getNewApacheHttpClient(boolean customssl, boolean tls_only,
            boolean disguise_app) {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setRedirectStrategy(new CustomRedirectStrategy());
        if (disguise_app) {
            builder.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, " +
                    "like Gecko) Chrome/43.0.2357.130 Safari/537.36\t");
        } else {
            builder.setUserAgent(user_agent);
        }

        if (customssl) {
            try {
                if (trust_store == null) {
                    trust_store = getKeyStore();
                }
                SSLConnectionSocketFactory sf =
                        AdditionalKeyStoresSSLSocketFactory.create(trust_store, true,
                                getSocketFactoryClass(tls_only));

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
