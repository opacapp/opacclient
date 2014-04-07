package de.geeksfactory.opacclient.networking;

//Based on
//http://stackoverflow.com/a/6378872/336784
//and
//http://download.oracle.com/javase/1.5.0/docs/guide/security/jsse/SSERefGuide.html#X509TrustManager
//and
//https://github.com/nelenkov/custom-cert-https

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * Allows you to trust certificates from additional KeyStores in addition to the
 * default KeyStore
 */
public class AdditionalKeyStoresSSLSocketFactory extends SSLSocketFactory {
	protected SSLContext sslContext = SSLContext.getInstance("TLS");

	public AdditionalKeyStoresSSLSocketFactory(KeyStore keyStore)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException {
		super((String) null, (KeyStore) null, (String) null, (KeyStore) null, (SecureRandom) null, (HostNameResolver) null);
		sslContext.init(null,
				new TrustManager[] { new AdditionalKeyStoresTrustManager(
						keyStore) }, null);
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException {
		return sslContext.getSocketFactory().createSocket(socket, host, port,
				autoClose);
	}

	@Override
	public Socket createSocket() throws IOException {
		return sslContext.getSocketFactory().createSocket();
	}

	public static class AdditionalKeyStoresTrustManager implements
			X509TrustManager {

		static class LocalStoreX509TrustManager implements X509TrustManager {

			private X509TrustManager trustManager;

			LocalStoreX509TrustManager(KeyStore localTrustStore) {
				try {
					TrustManagerFactory tmf = TrustManagerFactory
							.getInstance(TrustManagerFactory
									.getDefaultAlgorithm());
					tmf.init(localTrustStore);

					trustManager = findX509TrustManager(tmf);
					if (trustManager == null) {
						throw new IllegalStateException(
								"Couldn't find X509TrustManager");
					}
				} catch (GeneralSecurityException e) {
					throw new RuntimeException(e);
				}

			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
				trustManager.checkClientTrusted(chain, authType);
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
				trustManager.checkServerTrusted(chain, authType);
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return trustManager.getAcceptedIssuers();
			}
		}

		static X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
			TrustManager tms[] = tmf.getTrustManagers();
			for (int i = 0; i < tms.length; i++) {
				if (tms[i] instanceof X509TrustManager) {
					return (X509TrustManager) tms[i];
				}
			}

			return null;
		}

		private X509TrustManager defaultTrustManager;
		private X509TrustManager localTrustManager;

		private X509Certificate[] acceptedIssuers;

		public AdditionalKeyStoresTrustManager(KeyStore localKeyStore) {
			try {
				TrustManagerFactory tmf = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore) null);

				defaultTrustManager = findX509TrustManager(tmf);
				if (defaultTrustManager == null) {
					throw new IllegalStateException(
							"Couldn't find X509TrustManager");
				}

				localTrustManager = new LocalStoreX509TrustManager(
						localKeyStore);

				List<X509Certificate> allIssuers = new ArrayList<X509Certificate>();
				for (X509Certificate cert : defaultTrustManager
						.getAcceptedIssuers()) {
					allIssuers.add(cert);
				}
				for (X509Certificate cert : localTrustManager
						.getAcceptedIssuers()) {
					allIssuers.add(cert);
				}
				acceptedIssuers = allIssuers
						.toArray(new X509Certificate[allIssuers.size()]);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}

		}

		public static String getThumbPrint(X509Certificate cert)
				throws NoSuchAlgorithmException, CertificateEncodingException {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] der = cert.getEncoded();
			md.update(der);
			byte[] digest = md.digest();
			return hexify(digest);

		}

		public static String hexify(byte bytes[]) {
			char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
					'9', 'a', 'b', 'c', 'd', 'e', 'f' };

			StringBuffer buf = new StringBuffer(bytes.length * 2);

			for (int i = 0; i < bytes.length; ++i) {
				buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
				buf.append(hexDigits[bytes[i] & 0x0f]);
			}

			return buf.toString();
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			try {
				defaultTrustManager.checkClientTrusted(chain, authType);
			} catch (CertificateException ce) {
				localTrustManager.checkClientTrusted(
						new X509Certificate[] { chain[0] }, authType);
			}
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			try {
				defaultTrustManager.checkServerTrusted(chain, authType);
			} catch (CertificateException ce) {
				localTrustManager.checkServerTrusted(
						new X509Certificate[] { chain[0] }, authType);
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return acceptedIssuers;
		}
	}

}