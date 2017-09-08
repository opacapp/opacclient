package de.geeksfactory.opacclient.apis;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.HttpUtils;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.CoverHolder;
import de.geeksfactory.opacclient.objects.Library;

public abstract class ApacheBaseApi extends BaseApi {
    public HttpClient http_client;
    protected boolean httpLoggingEnabled = true;

    /**
     * Initializes HTTP client and String Provider
     */
    @Override
    public void init(Library library, HttpClientFactory http_client_factory) {
        http_client = http_client_factory.getNewApacheHttpClient(
                library.getData().optBoolean("customssl", false),
                library.getData().optBoolean("customssl_tls_only", true),
                library.getData().optBoolean("customssl_all_ciphersuites", false),
                library.getData().optBoolean("disguise", false));
        this.library = library;
        stringProvider = new DummyStringProvider();
    }

    /**
     * Perform a HTTP GET request to a given URL
     *
     * @param url           URL to fetch
     * @param encoding      Expected encoding of the response body
     * @param ignore_errors If true, status codes above 400 do not raise an exception
     * @param cookieStore   If set, the given cookieStore is used instead of the built-in one.
     * @return Answer content
     * @throws NotReachableException Thrown when server returns a HTTP status code greater or equal
     *                               than 400.
     */
    public String httpGet(String url, String encoding, boolean ignore_errors,
            CookieStore cookieStore) throws
            IOException {

        HttpGet httpget = new HttpGet(cleanUrl(url));
        HttpResponse response;
        String html;
        httpget.setHeader("Accept", "*/*");

        try {
            if (cookieStore != null) {
                // Create local HTTP context
                HttpContext localContext = new BasicHttpContext();
                // Bind custom cookie store to the local context
                localContext.setAttribute(ClientContext.COOKIE_STORE,
                        cookieStore);

                response = http_client.execute(httpget, localContext);
            } else {
                response = http_client.execute(httpget);
            }

            if (!ignore_errors && response.getStatusLine().getStatusCode() >= 400) {
                HttpUtils.consume(response.getEntity());
                throw new NotReachableException(response.getStatusLine().getReasonPhrase());
            }

            html = convertStreamToString(response.getEntity().getContent(),
                    encoding);
            HttpUtils.consume(response.getEntity());
        } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
            logHttpError(e);
            throw new SSLSecurityException(e.getMessage());
        } catch (javax.net.ssl.SSLException e) {
            // Can be "Not trusted server certificate" or can be a
            // aborted/interrupted handshake/connection
            if (e.getMessage().contains("timed out")
                    || e.getMessage().contains("reset by")) {
                logHttpError(e);
                throw new NotReachableException(e.getMessage());
            } else {
                logHttpError(e);
                throw new SSLSecurityException(e.getMessage());
            }
        } catch (InterruptedIOException e) {
            logHttpError(e);
            throw new NotReachableException(e.getMessage());
        } catch (UnknownHostException e) {
            throw new NotReachableException(e.getMessage());
        } catch (IOException e) {
            if (e.getMessage() != null
                    && e.getMessage().contains("Request aborted")) {
                logHttpError(e);
                throw new NotReachableException(e.getMessage());
            } else {
                throw e;
            }
        }
        return html;
    }

    public String httpGet(String url, String encoding, boolean ignore_errors)
            throws IOException {
        return httpGet(url, encoding, ignore_errors, null);
    }

    public String httpGet(String url, String encoding)
            throws IOException {
        return httpGet(url, encoding, false, null);
    }

    @Deprecated
    public String httpGet(String url) throws
            IOException {
        return httpGet(url, getDefaultEncoding(), false, null);
    }

    /**
     * Downloads a cover to a CoverHolder. You only need to use this if the covers are only
     * available with e.g. Session cookies. Otherwise, it is sufficient to specify the URL of the
     * cover.
     *
     * @param item CoverHolder to download the cover for
     */
    protected void downloadCover(CoverHolder item) {
        if (item.getCover() == null) {
            return;
        }
        HttpGet httpget = new HttpGet(cleanUrl(item.getCover()));
        HttpResponse response;

        try {
            response = http_client.execute(httpget);

            if (response.getStatusLine().getStatusCode() >= 400) {
                return;
            }
            HttpEntity entity = response.getEntity();
            byte[] bytes = EntityUtils.toByteArray(entity);

            item.setCoverBitmap(bytes);

        } catch (IOException e) {
            logHttpError(e);
        }
    }

    /**
     * Perform a HTTP POST request to a given URL
     *
     * @param url           URL to fetch
     * @param data          POST data to send
     * @param encoding      Expected encoding of the response body
     * @param ignore_errors If true, status codes above 400 do not raise an exception
     * @param cookieStore   If set, the given cookieStore is used instead of the built-in one.
     * @return Answer content
     * @throws NotReachableException Thrown when server returns a HTTP status code greater or equal
     *                               than 400.
     */
    public String httpPost(String url, HttpEntity data,
            String encoding, boolean ignore_errors, CookieStore cookieStore)
            throws IOException {
        HttpPost httppost = new HttpPost(cleanUrl(url));
        httppost.setEntity(data);
        httppost.setHeader("Accept", "*/*");

        HttpResponse response;
        String html;
        try {
            if (cookieStore != null) {
                // Create local HTTP context
                HttpContext localContext = new BasicHttpContext();
                // Bind custom cookie store to the local context
                localContext.setAttribute(ClientContext.COOKIE_STORE,
                        cookieStore);

                response = http_client.execute(httppost, localContext);
            } else {
                response = http_client.execute(httppost);
            }

            if (!ignore_errors && response.getStatusLine().getStatusCode() >= 400) {
                throw new NotReachableException(response.getStatusLine().getReasonPhrase());
            }
            html = convertStreamToString(response.getEntity().getContent(),
                    encoding);
            HttpUtils.consume(response.getEntity());
        } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
            logHttpError(e);
            throw new SSLSecurityException(e.getMessage());
        } catch (javax.net.ssl.SSLException e) {
            // Can be "Not trusted server certificate" or can be a
            // aborted/interrupted handshake/connection
            if (e.getMessage().contains("timed out")
                    || e.getMessage().contains("reset by")) {
                logHttpError(e);
                throw new NotReachableException(e.getMessage());
            } else {
                logHttpError(e);
                throw new SSLSecurityException(e.getMessage());
            }
        } catch (InterruptedIOException e) {
            logHttpError(e);
            throw new NotReachableException(e.getMessage());
        } catch (UnknownHostException e) {
            throw new NotReachableException(e.getMessage());
        } catch (IOException e) {
            if (e.getMessage() != null
                    && e.getMessage().contains("Request aborted")) {
                logHttpError(e);
                throw new NotReachableException(e.getMessage());
            } else {
                throw e;
            }
        }
        return html;
    }

    protected void logHttpError(Throwable e) {
        if (httpLoggingEnabled) {
            e.printStackTrace();
        }
    }

    public String httpPost(String url, HttpEntity data,
            String encoding, boolean ignore_errors)
            throws IOException {
        return httpPost(url, data, encoding, ignore_errors, null);
    }

    public String httpPost(String url, HttpEntity data,
            String encoding) throws IOException {
        return httpPost(url, data, encoding, false, null);
    }

    @Deprecated
    public String httpPost(String url, HttpEntity data)
            throws IOException {
        return httpPost(url, data, getDefaultEncoding(), false, null);
    }


    public static String buildHttpGetParams(List<NameValuePair> params)
            throws UnsupportedEncodingException {
        try {
            return new URIBuilder().addParameters(params).build().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void setHttpLoggingEnabled(boolean httpLoggingEnabled) {
        this.httpLoggingEnabled = httpLoggingEnabled;
    }
}
