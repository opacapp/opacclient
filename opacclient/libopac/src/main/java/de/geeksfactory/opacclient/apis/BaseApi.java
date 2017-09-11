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
package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.reporting.ReportHandler;
import de.geeksfactory.opacclient.searchfields.MeaningDetector;
import de.geeksfactory.opacclient.searchfields.MeaningDetectorImpl;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;

/**
 * Abstract Base class for OpacApi implementations providing some helper methods for HTTP
 */
public abstract class BaseApi implements OpacApi {

    protected Library library;
    protected StringProvider stringProvider;
    protected Set<String> supportedLanguages;
    protected boolean initialised;
    protected ReportHandler reportHandler;

    /**
     * Keywords to do a free search. Some APIs do support this, some don't. If supported, it must at
     * least search in title and author field, but should also search abstract and other things.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_FREE = "free";

    /**
     * Item title to search for. Doesn't have to be the full title, can also be a substring to be
     * searched.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_TITLE = "titel";

    /**
     * Author name to search for.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_AUTHOR = "verfasser";

    /**
     * "Keyword A". Most libraries require very special input in this field. May be only shown if
     * "advanced fields" is set in user preferences.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_KEYWORDA = "schlag_a";

    /**
     * "Keyword B". Most libraries require very special input in this field. May be only shown if
     * "advanced fields" is set in user preferences. Can only be set, if
     * <code>KEY_SEARCH_QUERY_KEYWORDA</code> is set as well.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_KEYWORDB = "schlag_b";

    /**
     * Library branch to search in. The user is able to select from multiple options, generated from
     * the MetaData you store in the MetaDataSource you get in {@link #init(Library,
     * HttpClientFactory)}.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_BRANCH = "zweigstelle";

    /**
     * "Home" library branch. Some library systems require this information at search request time
     * to determine where book reservations should be placed. If in doubt, don't use. Behaves
     * similar to <code>KEY_SEARCH_QUERY_BRANCH</code> .
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_HOME_BRANCH = "homebranch";

    /**
     * An ISBN / EAN code to search for. We cannot promise whether it comes with spaces or hyphens
     * in between but it most likely won't. If it makes a difference to you, eliminate everything
     * except numbers and X. We also cannot say whether a ISBN10 or a ISBN13 is supplied - if
     * relevant, check in your {@link #search(List)} implementation.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_ISBN = "isbn";

    /**
     * Year of publication. Your API can either support this or both the
     * <code>KEY_SEARCH_QUERY_YEAR_RANGE_*</code> fields (or none of them).
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_YEAR = "jahr";

    /**
     * End of range, if year of publication can be specified as a range. Can not be combined with
     * <code>KEY_SEARCH_QUERY_YEAR</code> but has to be combined with
     * <code>KEY_SEARCH_QUERY_YEAR_RANGE_END</code>.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_YEAR_RANGE_START = "jahr_von";

    /**
     * Start of range, if year of publication can be specified as a range. Can not be combined with
     * <code>KEY_SEARCH_QUERY_YEAR</code> but has to be combined with
     * <code>KEY_SEARCH_QUERY_YEAR_RANGE_START</code>.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_YEAR_RANGE_END = "jahr_bis";

    /**
     * Systematic identification, used in some libraries. Rarely in use. May be only shown if
     * "advanced fields" is set in user preferences.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_SYSTEM = "systematik";

    /**
     * Some libraries support a special "audience" field with specified values. Rarely in use. May
     * be only shown if "advanced fields" is set in user preferences.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_AUDIENCE = "interessenkreis";

    /**
     * The "publisher" search field
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_PUBLISHER = "verlag";

    /**
     * Item category (like "book" or "CD"). The user is able to select from multiple options,
     * generated from the MetaData you store in the MetaDataSource you get in {@link #init(Library,
     * HttpClientFactory)}.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_CATEGORY = "mediengruppe";

    /**
     * Unique item identifier. In most libraries, every single book has a unique number, most of the
     * time printed on the in form of a barcode, sometimes encoded in a NFC chip.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_BARCODE = "barcode";

    /**
     * Item location in library. Currently not in use.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_LOCATION = "location";

    /**
     * Restrict search to digital media.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_DIGITAL = "digital";

    /**
     * Restrict search to available media.
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_AVAILABLE = "available";

    /**
     * Sort search results in a specific order
     *
     * This is only used internally to construct search fields.
     */
    protected static final String KEY_SEARCH_QUERY_ORDER = "order";

    /**
     * Reads content from an InputStream into a string
     *
     * @param is       InputStream to read from
     * @param encoding the encoding to use
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

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
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

    /**
     * Reads content from an InputStream into a string, using the default {@code ISO-8859-1}
     * encoding
     *
     * @param is InputStream to read from
     * @return String content of the InputStream
     */
    protected static String convertStreamToString(InputStream is)
            throws IOException {
        return convertStreamToString(is, "ISO-8859-1");
    }

    /**
     * Converts a {@link List} of {@link SearchQuery}s to {@link Map} of their keys and values. Can
     * be used to convert old implementations using {@code search(Map<String, String>)} to the new
     * SearchField API
     *
     * @param queryList List of search queries
     * @return Map of their keys and values
     */
    protected static Map<String, String> searchQueryListToMap(
            List<SearchQuery> queryList) {
        Map<String, String> queryMap = new HashMap<>();
        for (SearchQuery query : queryList) {
            queryMap.put(query.getKey(), query.getValue());
        }
        return queryMap;
    }

    /*
     * Gets all values of all query parameters in an URL.
     */
    public static Map<String, List<String>> getQueryParams(String url) {
        try {
            Map<String, List<String>> params = new HashMap<>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }

                    List<String> values = params.get(key);
                    if (values == null) {
                        values = new ArrayList<>();
                        params.put(key, values);
                    }
                    values.add(value);
                }
            }

            return params;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    /*
     * Gets the value for every query parameter in the URL. If a parameter name
     * occurs twice or more, only the first occurrence is interpreted by this
     * method
     */
    public static Map<String, String> getQueryParamsFirst(String url) {
        try {
            Map<String, String> params = new HashMap<>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }

                    String values = params.get(key);
                    if (values == null) {
                        params.put(key, value);
                    }
                }
            }

            return params;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Initializes HTTP client and String Provider
     */
    @Override
    public void init(Library library, HttpClientFactory http_client_factory) {
        this.library = library;
        stringProvider = new DummyStringProvider();
    }

    public void start() throws IOException {
        supportedLanguages = getSupportedLanguages();
        initialised = true;
    }

    protected String getDefaultEncoding() {
        return "ISO-8859-1";
    }

    protected boolean shouldUseMeaningDetector() {
        return true;
    }

    @Override
    public SearchRequestResult volumeSearch(Map<String, String> query)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public void setStringProvider(StringProvider stringProvider) {
        this.stringProvider = stringProvider;
    }

    @Override
    public List<SearchField> getSearchFields()
            throws JSONException, OpacErrorException, IOException {
        List<SearchField> fields = parseSearchFields();
        if (shouldUseMeaningDetector()) {
            MeaningDetector md = new MeaningDetectorImpl(library);
            for (int i = 0; i < fields.size(); i++) {
                fields.set(i, md.detectMeaning(fields.get(i)));
            }
            Collections.sort(fields, new SearchField.OrderComparator());
        }
        return fields;
    }

    public abstract List<SearchField> parseSearchFields() throws IOException, OpacErrorException,
            JSONException;

    public void setReportHandler(ReportHandler reportHandler) {
        this.reportHandler = reportHandler;
    }


    /**
     * Converts a {@link JSONObject} that contains only integer values into a {@link Map}.
     *
     * @param json a JSON object
     * @return a Map
     */
    protected static Map<String, Integer> jsonToMap(JSONObject json) {
        Map<String, Integer> map = new HashMap<>();
        Iterator keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                int value = json.getInt(key);
                if (value >= 0) map.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return map;
    }


    /**
     * Loads a resource file in JSON format to a {@link JSONObject}. Returns null if an error
     * occurred.
     *
     * @param filename the file name, relative to the resources directory, starting with a slash
     * @return the loaded JSON object
     */
    protected JSONObject loadJsonResource(String filename) {
        InputStream is = getClass().getResourceAsStream(filename);
        if (is == null) return null;
        try {
            return new JSONObject(convertStreamToString(is));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Cleans the parameters of a URL by parsing it manually and reformatting it using {@link
     * URLEncodedUtils#format(java.util.List, String)}
     *
     * @param myURL the URL to clean
     * @return cleaned URL
     */
    public static String cleanUrl(String myURL) {
        String[] parts = myURL.split("\\?");
        String url = parts[0];
        try {
            if (parts.length > 1) {
                url += "?";
                List<NameValuePair> params = new ArrayList<>();
                String[] pairs = parts[1].split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=");
                    if (kv.length > 1) {
                        StringBuilder join = new StringBuilder();
                        for (int i = 1; i < kv.length; i++) {
                            if (i > 1) join.append("=");
                            join.append(kv[i]);
                        }
                        params.add(new BasicNameValuePair(URLDecoder.decode(
                                kv[0], "UTF-8"), URLDecoder.decode(join.toString(),
                                "UTF-8")));
                    } else {
                        params.add(new BasicNameValuePair(URLDecoder.decode(
                                kv[0], "UTF-8"), ""));
                    }
                }
                url += URLEncodedUtils.format(params, "UTF-8");
            }
            return url;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return myURL;
        }
    }
}
