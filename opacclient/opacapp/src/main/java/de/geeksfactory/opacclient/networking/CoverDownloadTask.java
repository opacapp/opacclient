package de.geeksfactory.opacclient.networking;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashSet;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.apis.ApacheBaseApi;
import de.geeksfactory.opacclient.apis.OkHttpBaseApi;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.CoverHolder;
import de.geeksfactory.opacclient.utils.Base64;
import de.geeksfactory.opacclient.utils.ISBNTools;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class CoverDownloadTask extends AsyncTask<Void, Integer, CoverHolder> {
    protected static HashSet<String> rejectImages = new HashSet<>();
    protected int width = 0;
    protected int height = 0;

    static {
        rejectImages.add(
                "R0lGODlhOwBLAIAAALy8vf///yH5BAEAAAEALAAAAAA7AEsAAAL/jI+py+0Po5y0" +
                        "2ouz3rz7D2rASJbmiYYGyralGrhyqrbTW4+rGeEhmeA5fCCg4sQgfowLFkLpYTaE" +
                        "O10OIJFCO9KhtYq9Zr+xbpTsDYNh5iR5y2k33/JNPUhHn9UP7T3zd+Cnx0U4xwdn" +
                        "Z3iUx7e0iIcYeDFZJgkJiCnYyKZZ9VRZUTnouDd2WVqYegjqaTHKebUa6SSLKdOJ" +
                        "5GYDY0nVWtvrqxSa61PciytMwbss+uvMjBxNXW19jZ29bHVJu/MNvqmTCK4WhvbF" +
                        "bS65EnPqXiaIJ26Eg/6HVW8+327fHg9kVpBw5xylc6eu3jeBTwh28bewIJh807RZ" +
                        "vIgxo8aNRxw7ZlNXbt04RvT+lXQjL57KciT/nRuY5iW8YzJPQjx5xKVCeCoNurTE" +
                        "0+QukBNZAsu3ECbKnhIBBnwaMWFBVx6rWr2KdUIBADs=");
        rejectImages.add(
                "R0lGODlhOwBLAIABALy8vf///yH+EUNyZWF0ZWQgd2l0aCBHSU1QACH5BAEKAAEA" +
                        "LAAAAAA7AEsAAAJHjI+py+0Po5y02ouz3rz7D4biSJbmiabqyrbuC8fyTNf2jef6z" +
                        "vf+DwwKh8Si8YhMKpfMpvMJjUqn1Kr1is1qt9yu9wsO6woAOw==");
        rejectImages.add(
                "R0lGODlhOwBLANUtAAEBAYmXsWd6mt3h6fXd38zMzNJobvru77zE0tyJjhISEkVchLu7uyQ" +
                        "/beu8vhMwYrkUHchGTpmZme7u7kVFRe7w9L4lLaqqqiMjIzVNeVZrj1ZWVjQ0NMM1PY" +
                        "iIiGdnZ/DNz9d5fs1XXt3d3czT3XiJpeGannh4eKu1x5qmvOarrrQDDQIhV////wAAA" +
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                        "ACH5BAEAAC0ALAAAAAA7AEsAAAb/wJZwSCwaj8ikcslsOp/QqHRKrVqv2Kx2y+16v+C" +
                        "weEwum8/oJIuVKaLWAaiAFZ/Hr2sWiaiBy+ktdlh5JUMVeXdOghUDFYNrDUNvfkIBDy" +
                        "wPiRUalxkZgIItA31sey1rApcNKUdrl6YLmIAtJWufs7EPsX6LDSwNvg8Dp5C+ekZrf" +
                        "YWHLH1xA2vDCGsV08It06CAtQtCfRrEAkKfiUNr05EpzYIBLOJCsSi14PDacZ8IQtAP" +
                        "xKahRNHwxRM0Jw+ifwQBGTTIbFigWQBZDGiX62GdXwsyZpTnbogziy06aczYUMg/cxK" +
                        "hJQOprpu3BgjePHBU4dLFOLFYiWrQJppJ/4goh93K14tNgFyOfGUoYexmi0kCmHb0Cb" +
                        "KIz3b8qg4w9ivfzjw2q6rLo8ER1ZNCEHitZmoAAodCSCAwRUTuALfD8BpSC1ftEL1pA" +
                        "gseTLgwGgIiIKxYYSHEASqLCWwhoBhChA6LI0BeIVmLgRUdHrdwsNjBlMhbIqwIQUTE" +
                        "igQtULf4DLsFCNWMay9OzNjEEMSLO4Bw8nn18CKyabegzBjz69iLLVhYPPzAdOkrIHR" +
                        "ecsB58BDHkz//rLmFiezQDQjBDDvEivKuRTxRYcD7Ct/iYU83LcSECujHKYcZfwSgx8" +
                        "QBBIg2Gm4H5AfddkM4uNiEEyqYxGesDdHgCqRxJv+Ea7ApdtwBCSSwYWfKKdZBBCyya" +
                        "CES52k3BAiRUdfCAYrBppp6LaiAnoOq+bacBR00YV12BiRQnGaqNeYcbD6uIEIIirHm" +
                        "YJQGhDAdjwe6NiEEjtmmWHTPmUchjw6aOaEIL3LngAPHEfEmgg5sd8CcQ7z5W50avgm" +
                        "hYYAGKuighBZq6KGIclEAABIgsWijSEywAQAAYMCAFxIAUECkHkwQKQYYXMCAAgB4cU" +
                        "KpFADAwKQAXNDCBwDASuoGRMC6aQsecFAAqx+0AACtHmjKqquTKsBBEhQYq0ABHyhwA" +
                        "QcKtEABBhwwOikRClBQRLIXwHoBtS1k2+yz0Vb7wa1HYEC26QUjAHBCC6e2gEGqtMI6" +
                        "RLvvClHAoh60wIC1CmSa6bvxAqBtEhMAUO0GmVJKacKT9jvtEIv22kLC67aQKQOnckB" +
                        "Bww7zq8S/F6SaqQejUkCyquHSOoQCy46Q7MAjKICBxpQycHLKF7CcRKYF9PxBtZWOcD" +
                        "IAIyTc7xCjUmqptE6P4K/BFxONwQjBeorwrQxIva8QExQQthAMaE0EA+i28DXTZq89d" +
                        "qJwxy333HTXbffdeOcdRhAAOw==");
        rejectImages.add("R0lGODlhUAB4AIAAAP///wAAACH5BAEAAAAALAAAAABQAHgAAAJxhI+py+" +
                "0Po5y02ouz3rz7D4biSJbmiabqyrbuC8fyTNf2jef6zvf+DwwKh8Si8YhMKpfMpvMJj" +
                "Uqn1Kr1is1qt9yu9wsOi8fksvmMTqvX7Lb7DY/L5/S6/Y7P6/f8vv8PGCg4SFhoeIiY" +
                "qLjI2OjYUAAAOw==");
    }

    protected CoverHolder item;
    protected Context context;

    public CoverDownloadTask(Context context, CoverHolder item) {
        this.item = item;
        this.context = context;
    }

    protected byte[] getImageApacheClient(HttpClient httpClient) throws IOException {
        HttpGet httpget = new HttpGet(ISBNTools.getBestSizeCoverUrl(item.getCover(),
                width, height));
        HttpResponse response;

        response = httpClient.execute(httpget);

        HttpEntity entity = response.getEntity();
        if (response.getStatusLine().getStatusCode() >= 400) {
            HttpUtils.consume(entity);
            return null;
        }
        return EntityUtils.toByteArray(entity);
    }


    protected byte[] getImageOkHttpClient(OkHttpClient httpClient) throws IOException {
        Request request = new Request.Builder()
                .url(ISBNTools.getBestSizeCoverUrl(item.getCover(), width, height))
                .header("Accept", "*/*")
                .build();
        Response response = httpClient.newCall(request).execute();

        if (response.code() >= 400) {
            response.close();
            return null;
        }

        return response.body().bytes();
    }

    protected byte[] getImage() throws IOException {
        try {
            OpacApi api = ((OpacClient) context.getApplicationContext()).getApi();
            if (api instanceof ApacheBaseApi) {
                return getImageApacheClient(((ApacheBaseApi) api).http_client);
            } else if (api instanceof OkHttpBaseApi) {
                return getImageOkHttpClient(((OkHttpBaseApi) api).http_client);
            }
        } catch (OpacClient.LibraryRemovedException e) {
        }
        HttpClient httpClient;
        httpClient =
                new AndroidHttpClientFactory().getNewApacheHttpClient(false, true, false, false);
        return getImageApacheClient(httpClient);
    }

    @Override
    protected CoverHolder doInBackground(Void... voids) {

        if (item.getCover() != null && item.getCoverBitmap() == null) {
            try {
                if (width == 0 && height == 0) {
                    // Use default
                    float density = context.getResources().getDisplayMetrics().density;
                    width = height = (int) density * 56;
                }


                try {
                    byte[] bytes = getImage();
                    if (bytes == null || rejectImages.contains(Base64.encodeBytes(bytes))) {
                        // OPACs like VuFind have a 'cover proxy' that returns a simple GIF with
                        // the text 'no image available' if no cover was found. We don't want to
                        // display this image but the media type,
                        // so we detect it. We do this here
                        // instead of in the API implementation because only this way it can be
                        // done asynchronously.
                        item.setCover(null);
                    } else {
                        if (bytes.length > 64) {
                            item.setCoverBitmap(bytes);
                        } else {
                            // When images embedded from Amazon aren't available, a
                            // 1x1
                            // pixel image is returned (iOPAC)
                            item.setCover(null);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (OutOfMemoryError e) {
                item.setCoverBitmap(null);
                item.setCover(null);
                Log.i("CoverDownloadTask", "OutOfMemoryError");
                return item;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return item;
    }

    protected abstract void onPostExecute(CoverHolder result);
}
