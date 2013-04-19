package de.geeksfactory.opacclient.networking;

import java.io.InputStream;
import java.security.KeyStore;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;

public class HTTPClient {

	public static DefaultHttpClient getNewHttpClient() {
		DefaultHttpClient hc = null;
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
		HttpProtocolParams.setUserAgent(hc.getParams(), "OpacApp/"+OpacClient.versionName+" (Android)");
		return hc;
	}

}
