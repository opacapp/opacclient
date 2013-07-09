package de.geeksfactory.opacclient.networking;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Library;

public class HTTPClient {

	public static DefaultHttpClient getNewHttpClient(Library library) {
		DefaultHttpClient hc = null;
		if (library.getData().has("customssl")) {
			try {
				final KeyStore trustStore = KeyStore.getInstance("BKS");

				final InputStream in = OpacClient.context.getResources()
						.openRawResource(R.raw.ssl_trust_store);
				try {
					trustStore.load(in,
							"ro5eivoijeeGohsh0daequoo5Zeepaen".toCharArray());
				} finally {
					in.close();
				}

				SSLSocketFactory sf = new AdditionalKeyStoresSSLSocketFactory(
						trustStore);

				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

				SchemeRegistry registry = new SchemeRegistry();
				registry.register(new Scheme("http", PlainSocketFactory
						.getSocketFactory(), 80));
				registry.register(new Scheme("https", sf, 443));

				ClientConnectionManager ccm = new ThreadSafeClientConnManager(
						params, registry);

				hc = new DefaultHttpClient(ccm, params);
			} catch (Exception e) {
				e.printStackTrace();
				hc = new DefaultHttpClient();
			}
		} else {
			hc = new DefaultHttpClient();
		}
		RedirectHandler customRedirectHandler = new HTTPClient.CustomRedirectHandler();
		hc.setRedirectHandler(customRedirectHandler);
		HttpProtocolParams.setUserAgent(hc.getParams(), "OpacApp/"
				+ OpacClient.versionName);
		return hc;
	}

	public static class CustomRedirectHandler extends DefaultRedirectHandler {

		private static final String REDIRECT_LOCATIONS = "http.protocol.redirect-locations";

		public CustomRedirectHandler() {
			super();
		}

		public URI getLocationURI(final HttpResponse response,
				final HttpContext context) throws ProtocolException {
			if (response == null) {
				throw new IllegalArgumentException(
						"HTTP response may not be null");
			}

			// get the location header to find out where to redirect to
			Header locationHeader = response.getFirstHeader("location");
			if (locationHeader == null) {
				// got a redirect response, but no location header
				throw new ProtocolException("Received redirect response "
						+ response.getStatusLine() + " but no location header");
			}

			String location = locationHeader.getValue().replaceAll(" ", "%20");

			URI uri;
			try {
				uri = new URI(location);
			} catch (URISyntaxException ex) {
				throw new ProtocolException(
						"Invalid redirect URI: " + location, ex);
			}

			HttpParams params = response.getParams();
			// rfc2616 demands the location value be a complete URI
			// Location = "Location" ":" absoluteURI
			if (!uri.isAbsolute()) {
				if (params
						.isParameterTrue(ClientPNames.REJECT_RELATIVE_REDIRECT)) {
					throw new ProtocolException("Relative redirect location '"
							+ uri + "' not allowed");
				}
				// Adjust location URI
				HttpHost target = (HttpHost) context
						.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
				if (target == null) {
					throw new IllegalStateException(
							"Target host not available "
									+ "in the HTTP context");
				}

				HttpRequest request = (HttpRequest) context
						.getAttribute(ExecutionContext.HTTP_REQUEST);

				try {
					URI requestURI = new URI(request.getRequestLine().getUri());
					URI absoluteRequestURI = URIUtils.rewriteURI(requestURI,
							target, true);
					uri = URIUtils.resolve(absoluteRequestURI, uri);
				} catch (URISyntaxException ex) {
					throw new ProtocolException(ex.getMessage(), ex);
				}
			}

			if (params.isParameterFalse(ClientPNames.ALLOW_CIRCULAR_REDIRECTS)) {

				RedirectLocations redirectLocations = (RedirectLocations) context
						.getAttribute(REDIRECT_LOCATIONS);

				if (redirectLocations == null) {
					redirectLocations = new RedirectLocations();
					context.setAttribute(REDIRECT_LOCATIONS, redirectLocations);
				}

				URI redirectURI;
				if (uri.getFragment() != null) {
					try {
						HttpHost target = new HttpHost(uri.getHost(),
								uri.getPort(), uri.getScheme());
						redirectURI = URIUtils.rewriteURI(uri, target, true);
					} catch (URISyntaxException ex) {
						throw new ProtocolException(ex.getMessage(), ex);
					}
				} else {
					redirectURI = uri;
				}

				if (redirectLocations.contains(redirectURI)) {
					throw new CircularRedirectException(
							"Circular redirect to '" + redirectURI + "'");
				} else {
					redirectLocations.add(redirectURI);
				}
			}

			return uri;
		}
	}
}
