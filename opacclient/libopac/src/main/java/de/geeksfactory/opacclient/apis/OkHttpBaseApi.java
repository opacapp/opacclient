package de.geeksfactory.opacclient.apis;

import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.CoverHolder;
import de.geeksfactory.opacclient.objects.Library;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSource;

public abstract class OkHttpBaseApi extends BaseApi {
    public static final MediaType MEDIA_TYPE_JSON =
            MediaType.parse("application/json; charset=utf-8");

    public OkHttpClient http_client;
    public HttpClientFactory http_client_factory;
    protected boolean httpLoggingEnabled = true;

    /**
     * Initializes HTTP client and String Provider
     */
    @Override
    public void init(Library library, HttpClientFactory http_client_factory) {
        this.http_client_factory = http_client_factory;
        http_client = http_client_factory.getNewOkHttpClient(
                library.getData().optBoolean("customssl", false),
                library.getData().optBoolean("customssl_tls_only", true),
                library.getData().optBoolean("customssl_all_ciphersuites", false)
        );
        http_client.dispatcher().setMaxRequestsPerHost(10);
        this.library = library;
        stringProvider = new DummyStringProvider();
    }

    private String readBody(Response response, String encoding) throws IOException {
        ResponseBody body = response.body();
        BufferedSource source = body.source();

        MediaType contentType = body.contentType();

        try {
            Charset charset = Util.bomAwareCharset(source,
                    contentType != null ? contentType.charset(Charset.forName(encoding)) :
                            Charset.forName(encoding));
            return source.readString(charset);
        } finally {
            Util.closeQuietly(source);
        }
    }

    /**
     * Perform a HTTP GET request to a given URL
     *
     * @param url           URL to fetch
     * @param encoding      Expected encoding of the response body
     * @param ignore_errors If true, status codes above 400 do not raise an exception
     * @return Answer content
     * @throws NotReachableException Thrown when server returns a HTTP status code greater or equal
     *                               than 400.
     */
    public String httpGet(String url, String encoding, boolean ignore_errors) throws
            IOException {
        Request request = new Request.Builder()
                .url(cleanUrl(url))
                .header("Accept", "*/*")
                .header("User-Agent", getUserAgent())
                .build();

        try {
            Response response = http_client.newCall(request).execute();

            if (!ignore_errors && response.code() >= 400) {
                throw new NotReachableException(response.message());
            }

            return readBody(response, encoding);
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
    }

    public String httpGet(String url, String encoding)
            throws IOException {
        return httpGet(url, encoding, false);
    }

    @Deprecated
    public String httpGet(String url) throws
            IOException {
        return httpGet(url, getDefaultEncoding(), false);
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

        Request request = new Request.Builder()
                .url(cleanUrl(item.getCover()))
                .header("Accept", "*/*")
                .header("User-Agent", getUserAgent())
                .build();
        try {
            Response response = http_client.newCall(request).execute();

            if (response.code() >= 400) {
                return;
            }

            item.setCoverBitmap(response.body().bytes());
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
     * @return Answer content
     * @throws NotReachableException Thrown when server returns a HTTP status code greater or equal
     *                               than 400.
     */
    public String httpPost(String url, RequestBody data, String encoding, boolean ignore_errors)
            throws IOException {
        Request request = new Request.Builder()
                .url(cleanUrl(url))
                .header("Accept", "*/*")
                .header("User-Agent", getUserAgent())
                .post(data)
                .build();

        try {
            Response response = http_client.newCall(request).execute();

            if (!ignore_errors && response.code() >= 400) {
                throw new NotReachableException(response.message());
            }

            return readBody(response, encoding);
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
    }

    public void asyncPost(String url, RequestBody data, Callback callback)
            throws IOException {
        Request request = new Request.Builder()
                .url(cleanUrl(url))
                .header("Accept", "*/*")
                .header("User-Agent", getUserAgent())
                .post(data)
                .build();

        http_client.newCall(request).enqueue(callback);
    }

    public void asyncGet(String url, Callback callback)
            throws IOException {
        Request request = new Request.Builder()
                .url(cleanUrl(url))
                .header("Accept", "*/*")
                .header("User-Agent", getUserAgent())
                .build();

        http_client.newCall(request).enqueue(callback);
    }

    public void asyncHead(String url, Callback callback)
            throws IOException {
        Request request = new Request.Builder()
                .url(cleanUrl(url))
                .header("Accept", "*/*")
                .header("User-Agent", getUserAgent())
                .build();

        http_client.newCall(request).enqueue(callback);
    }

    public void asyncWait() {
        long started = System.currentTimeMillis();
        while (http_client.dispatcher().runningCallsCount() > 0 || http_client.dispatcher().queuedCallsCount() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // pass
            }
            if (System.currentTimeMillis() - started > 1000 * 90) {
                // Timeout
                http_client.dispatcher().cancelAll();
                break;
            }
        }
    }

    protected void logHttpError(Throwable e) {
        if (httpLoggingEnabled) {
            e.printStackTrace();
        }
    }


    public String httpPost(String url, RequestBody data,
            String encoding) throws IOException {
        return httpPost(url, data, encoding, false);
    }

    @Deprecated
    public String httpPost(String url, RequestBody data)
            throws IOException {
        return httpPost(url, data, getDefaultEncoding(), false);
    }

    public void setHttpLoggingEnabled(boolean httpLoggingEnabled) {
        this.httpLoggingEnabled = httpLoggingEnabled;
    }

    protected String getUserAgent() {
        if (library.getData().optBoolean("disguise", false)) {
            return "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, " +
                    "like Gecko) Chrome/43.0.2357.130 Safari/537.36\t";
        } else {
            return http_client_factory.user_agent;
        }
    }
}
