package de.geeksfactory.opacclient.networking;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
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

/**
 * Utility to create a new HTTP client.
 */
public class HttpClientFactory {

    public String user_agent;
    public String ssl_store_path = "../res/raw/ssl_trust_store.bks";
    private KeyStore trust_store;

    /**
     * Initialize a new client factory.
     *
     * @param user_agent The User-Agent header to be sent.
     */
    public HttpClientFactory(String user_agent) {
        this.user_agent = user_agent;
    }

    /**
     * Initialize a new client factory with an additional key store to trust for SSL connections.
     *
     * @param user_agent     The User-Agent header to be sent.
     * @param ssl_store_path The path to the .bks store
     */
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
            in = new FileInputStream("../opacapp/src/main/res/raw/ssl_trust_store.bks");
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

    /**
     * Create a new HttpClient.
     *
     * @param tls_only If this is true, only TLS v1 and newer will be used, SSLv3 will be disabled.
     *                 We highly recommend to set this to true, if possible. This is currently a
     *                 no-op on the default implementation and only used in the Android
     *                 implementation!
     */
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

        if (customssl && ssl_store_path != null) {
            try {
                if (trust_store == null) {
                    trust_store = getKeyStore();
                }
                SSLConnectionSocketFactory sf =
                        AdditionalKeyStoresSSLSocketFactory.create(trust_store,
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
        public HttpUriRequest getRedirect(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws ProtocolException {
            URI uri = getLocationURI(request, response, context);
            final String method = request.getRequestLine().getMethod();
            String original_scheme = "http";
            String original_host = "";

            if (request instanceof HttpRequestWrapper) {
                HttpRequest original = ((HttpRequestWrapper) request).getOriginal();
                if (original instanceof HttpRequestBase) {
                    original_scheme = ((HttpRequestBase) original).getURI().getScheme();
                    original_host = ((HttpRequestBase) original).getURI().getHost();
                }
            } else if (request instanceof HttpRequestBase) {
                if (((HttpRequestBase) request).getURI().getScheme() != null) {
                    original_scheme = ((HttpRequestBase) request).getURI().getScheme();
                    original_host = ((HttpRequestBase) request).getURI().getHost();
                }
            }
            // Strict Transport Security for redirects, required for misconfigured webservers like Erlangen
            if ("https".equals(original_scheme) && uri.getScheme().equals("http") && uri.getHost().equals(original_host)) {
                try {
                    uri = new URI(uri.toString().replace("http://", "https://"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
                return new HttpHead(uri);
            } else if (method.equalsIgnoreCase(HttpGet.METHOD_NAME)) {
                return new HttpGet(uri);
            } else {
                final int status = response.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_TEMPORARY_REDIRECT) {
                    return RequestBuilder.copy(request).setUri(uri).build();
                } else {
                    return new HttpGet(uri);
                }
            }
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
