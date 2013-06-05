package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.networking.HTTPClient;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * Abstract Base class for OpacApi implementations providing some helper methods
 * for HTTP
 */
public abstract class BaseApi implements OpacApi {

	protected DefaultHttpClient http_client;

	/**
	 * Initializes HTTP client
	 */
	public void init(MetaDataSource metadata, Library library) {
		http_client = HTTPClient.getNewHttpClient(library);
	}

	/**
	 * Perform a HTTP GET request to a given URL
	 * 
	 * @param url
	 *            URL to fetch
	 * @return Answer content
	 * @throws NotReachableException
	 *             Thrown when server returns a HTTP status code greater or
	 *             equal than 400.
	 */
	protected String httpGet(String url, String encoding)
			throws ClientProtocolException, IOException {
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = http_client.execute(httpget);
		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent(),
				encoding);
		response.getEntity().consumeContent();
		return html;
	}

	protected String httpGet(String url) throws ClientProtocolException,
			IOException {
		return httpGet(url, "ISO-8859-1");
	}

	/**
	 * Perform a HTTP POST request to a given URL
	 * 
	 * @param url
	 *            URL to fetch
	 * @param data
	 *            POST data to send
	 * @return Answer content
	 * @throws NotReachableException
	 *             Thrown when server returns a HTTP status code greater or
	 *             equal than 400.
	 */
	protected String httpPost(String url, UrlEncodedFormEntity data,
			String encoding) throws ClientProtocolException, IOException {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(data);
		HttpResponse response = http_client.execute(httppost);
		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent(), encoding);
		response.getEntity().consumeContent();
		return html;
	}

	protected String httpPost(String url, UrlEncodedFormEntity data)
			throws ClientProtocolException, IOException {
		return httpPost(url, data, "ISO-8859-1");
	}

	/**
	 * Reads content from an InputStream into a string
	 * 
	 * @param is
	 *            InputStream to read from
	 * @return String content of the InputStream
	 */
	protected static String convertStreamToString(InputStream is,
			String encoding) throws IOException {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, encoding));
		} catch (UnsupportedEncodingException e1) {
			reader = new BufferedReader(new InputStreamReader(is));
		}
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append((line + "\n"));
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	protected static String convertStreamToString(InputStream is)
			throws IOException {
		return convertStreamToString(is, "ISO-8859-1");
	}
}
