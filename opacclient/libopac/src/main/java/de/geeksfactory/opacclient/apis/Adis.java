package de.geeksfactory.opacclient.apis;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult.Status;
import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.HttpUtils;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class Adis extends ApacheBaseApi implements OpacApi {

    private static final Logger LOGGER = Logger.getLogger(Adis.class.getName());

    protected static HashMap<String, MediaType> types = new HashMap<>();
    protected static HashSet<String> ignoredFieldNames = new HashSet<>();

    static {
        types.put("Buch", MediaType.BOOK);
        types.put("Band", MediaType.BOOK);
        types.put("DVD-ROM", MediaType.CD_SOFTWARE);
        types.put("CD-ROM", MediaType.CD_SOFTWARE);
        types.put("Medienkombination", MediaType.PACKAGE);
        types.put("DVD-Video", MediaType.DVD);
        types.put("DVD", MediaType.DVD);
        types.put("Noten", MediaType.SCORE_MUSIC);
        types.put("Konsolenspiel", MediaType.GAME_CONSOLE);
        types.put("Spielkonsole", MediaType.GAME_CONSOLE);
        types.put("CD", MediaType.CD);
        types.put("Zeitschrift", MediaType.MAGAZINE);
        types.put("Zeitschriftenheft", MediaType.MAGAZINE);
        types.put("Zeitung", MediaType.NEWSPAPER);
        types.put("Beitrag E-Book", MediaType.EBOOK);
        types.put("Elektronische Ressource", MediaType.EBOOK);
        types.put("E-Book", MediaType.EBOOK);
        types.put("Karte", MediaType.MAP);
        types.put("E-Ressource", MediaType.EBOOK);
        types.put("Munzinger", MediaType.EBOOK);
        types.put("E-Audio", MediaType.EAUDIO);
        types.put("Blu-Ray", MediaType.BLURAY);

        // TODO: The following fields from Berlin make no sense and don't work
        // when they are displayed alone.
        // We can only include them if we automatically deselect the "Verbund"
        // checkbox
        // when one of these dropdowns has a value other than "".
        ignoredFieldNames.add("oder Bezirk");
        ignoredFieldNames.add("oder Bibliothek");
    }

    protected String opac_url = "";
    protected JSONObject data;
    protected Library library;
    protected int s_requestCount = 0;
    protected String s_service;
    protected String s_hrefFormatSearch;
    protected String s_hrefFormatAccount;
    protected String s_sid;
    protected List<String> s_exts;
    protected String s_alink;
    protected List<NameValuePair> s_pageform;
    protected int s_lastpage;
    protected Document s_reusedoc;

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

    protected Document getSearchPage() throws IOException {

        final String methodName = "getSearchPage";
        logInfo("%s > requestCount = %d", methodName, s_requestCount);

        String url = String.format(s_hrefFormatSearch, opac_url, s_sid, s_requestCount);
        Document doc = htmlGet(url);

        logInfo("%s < requestCount = %d", methodName, s_requestCount);
        return doc;
    }

    protected void updateFormatSearch(Document doc) {
        for (Element navitem : doc.select(".search-adv-a a")) {
            if (navitem.text().contains("Erweiterte Suche")) {
                String href = navitem.attr("href");
                s_exts = getQueryParams(href).get("sp");
                s_hrefFormatSearch = getQueryParamFormat(href);
                logInfo("%s - s_hrefFormatSearch = %s", "updateFormatSearch", s_hrefFormatSearch);
                break;
            }
        }
    }

    protected static String getQueryParamFormat(final String href) {

        String[] urlParts = href.split("\\?");
        if (urlParts.length <= 1) {
            return null;
        }

        String query = urlParts[1];

        Pattern patRequestCount = Pattern.compile(".*requestCount=([0-9]+)");
        Matcher matcher = patRequestCount.matcher(query);
        if (!matcher.find()) {
            // requestCount kommt nicht vor
            return null;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("%s;jsessionid=%s?");    // opac_url, s_sid

        String format = query;
        // bereits vorkommende % escapen/verdoppeln
        format = format.replaceAll("%", "%%");
        // value von requestCount durch Platzhalter ersetzen
        format = format.replaceFirst("requestCount=([0-9]+)", "requestCount=%s");
        sb.append(format);
        format = sb.toString();

        return format;
    }

    protected int getRequestCount(Document doc) {
        Pattern patRequestCount = Pattern.compile(".*requestCount=([0-9]+)");
        final Elements a1 = doc.select("a");
        for (Element a : a1) {
            final String href = a.attr("href");
            Matcher objid_matcher = patRequestCount.matcher(href);
            if (objid_matcher.matches()) {
                return Integer.parseInt(objid_matcher.group(1));
            }
        }

        logWarning("requestCount nicht gefunden");
        return -1;
    }

    protected void updateRequestCount(final String methodName, Document doc) {
        int requestCount = getRequestCount(doc);
        if (requestCount >= 0) {
            if (s_requestCount == requestCount) {
                logWarning("requestCount didnot change");
//                s_requestCount++;
            } else {
                s_requestCount = requestCount;
            }
        }
    }

    protected Document htmlExecute(final String methodName, HttpUriRequest request)
            throws IOException {

        HttpResponse response;
        final String url = request.getURI().toURL().toString();
        try {
            response = http_client.execute(request);
        } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
            LOGGER.log(Level.SEVERE, url, e);
            throw new SSLSecurityException(e.getMessage());
        } catch (javax.net.ssl.SSLException e) {
            // Can be "Not trusted server certificate" or can be a
            // aborted/interrupted handshake/connection
            if (e.getMessage().contains("timed out")
                    || e.getMessage().contains("reset by")) {
                e.printStackTrace();
                throw new NotReachableException(e.getMessage());
            } else {
                throw new SSLSecurityException(e.getMessage());
            }
        } catch (InterruptedIOException e) {
            e.printStackTrace();
            throw new NotReachableException(e.getMessage());
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Request aborted")) {
                e.printStackTrace();
                throw new NotReachableException(e.getMessage());
            } else {
                throw e;
            }
        }

        if (response.getStatusLine().getStatusCode() >= 400) {
            throw new NotReachableException(response.getStatusLine().getReasonPhrase());
        }
        String html = convertStreamToString(response.getEntity().getContent(),
                getDefaultEncoding());

        logFine("%s - html = %s", methodName, html);

        HttpUtils.consume(response.getEntity());
        Document doc = Jsoup.parse(html);

        if (LOGGER.isLoggable(Level.INFO)) {
            Elements h1s = doc.select("h1");
            if (h1s.size() > 0) {
                logInfo("%s - h1: %s", methodName, h1s.first().text());
            }
        }

        updateFormatSearch(doc);
        updateRequestCount(methodName, doc);

        doc.setBaseUri(url);
        logInfo("%s < requestCount = %d, url  = %s", methodName, s_requestCount, url);
        return doc;

    }

    public Document htmlGet(String url) throws
            IOException {

        final String methodName = "htmlGet";
        logInfo("%s > requestCount = %d, url  = %s", methodName, s_requestCount, url);

        if (!url.contains("requestCount") && s_requestCount >= 0) {
            logFine("%s - !url.contains(\"requestCount\")", methodName);
            url = url + (url.contains("?") ? "&" : "?") + "requestCount="
                    + s_requestCount;
            logInfo("%s   requestCount = %d, url  = %s", methodName, s_requestCount, url);
        }

        HttpGet httpget = new HttpGet(cleanUrl(url));

        return htmlExecute(methodName, httpget);
    }

    public Document htmlPost(String url, List<NameValuePair> data)
            throws IOException {

        final String methodName = "htmlPost";
        logInfo("%s > requestCount = %d, url  = %s", methodName, s_requestCount, url);

        HttpPost httppost = new HttpPost(cleanUrl(url));

        boolean rcf = false;
        for (NameValuePair nv : data) {

            // nur gesetzte Werte loggen
            if ((nv.getValue() != null) && (nv.getValue().length() > 0)) {
                logInfo("%s - %s = %s", methodName, nv.getName(), nv.getValue());
            }

            if (nv.getName().equals("requestCount")) {
                rcf = true;
                if (!LOGGER.isLoggable(Level.INFO)) {
                    // falls nicht geloggt wird,
                    // kann die Schleife hier abgebrochen werden
                    break;
                }
            }
        }
        if (!rcf) {
            data.add(new BasicNameValuePair("requestCount", s_requestCount + ""));
        }

        httppost.setEntity(new UrlEncodedFormEntity(data, getDefaultEncoding()));

        return htmlExecute(methodName, httppost);
    }

    @Override
    public void start() throws IOException {
        final String methodName = "start";
        try {
            s_requestCount = -1;
            logInfo("%s > htmlGet - s_requestCount = %d", methodName, s_requestCount);
            Document doc = htmlGet(opac_url + "?"
                    + data.getString("startparams"));
            logInfo("%s < htmlGet - s_requestCount = %d", methodName, s_requestCount);

            Pattern padSid = Pattern
                    .compile(".*;jsessionid=([0-9A-Fa-f]+)[^0-9A-Fa-f].*");
            for (Element navitem : doc.select("#unav li a, .tree_ul li a")) {
                // Düsseldorf uses a custom layout where the navbar is .tree_ul
                String href = navitem.attr("href");
                if (href.contains("service=")) {
                    s_service = getQueryParams(href).get("service").get(0);
                }
                if (navitem.text().contains("Erweiterte Suche")) {
                    s_exts = getQueryParams(href).get("sp");
                    s_hrefFormatSearch = getQueryParamFormat(href);
                }
                Matcher objid_matcher = padSid.matcher(href);
                if (objid_matcher.matches()) {
                    s_sid = objid_matcher.group(1);
                }
            }
            for (Element navitem : doc.select(".cssmenu_button a")) {
                if (navitem.text().contains("Mein Konto")) {
                    String href = navitem.attr("href");
                    s_hrefFormatAccount = getQueryParamFormat(href);
                }
            }
            if (s_exts == null) {
                for (Element navitem : doc.select(".search-adv-a a")) {
                    if (navitem.text().contains("Erweiterte Suche")) {
                        String href = navitem.attr("href");
                        s_exts = getQueryParams(href).get("sp");
                    }
                }
                if (s_exts == null) {
                    s_exts = Collections.singletonList("SS6");
                }
            }
            for (Element navitem : doc.select(".search-adv-a a")) {
                if (navitem.text().contains("Erweiterte Suche")) {
                    String href = navitem.attr("href");
                    s_hrefFormatSearch = getQueryParamFormat(href);
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        super.start();
    }

    @Override
    protected String getDefaultEncoding() {
        return "UTF-8";
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, OpacErrorException {

        final String methodName = "search";
        if (LOGGER.isLoggable(Level.FINE)) {
            for (SearchQuery query : queries) {
                if ((query.getValue() != null) && (query.getValue().length() > 0)) {
                    logFine("%s - SearchQuery: %s = %s", methodName, query.getKey(),
                            query.getValue());
                }
            }
        }

//        if (!initialised) {
        start();    // notwendig wenn man vom Accoun kommt
//        }
        // TODO: There are also libraries with a different search form,
        // s_exts=SS2 instead of s_exts=SS6
        // e.g. munich. Treat them differently!

        Document doc = getSearchPage();

        int dropdownTextCount = 0;
        int totalCount = 0;
        for (SearchQuery query : queries) {
            if (!query.getValue().equals("")) {
                totalCount++;

                if (query.getSearchField() instanceof DropdownSearchField) {
                    doc.select("select#" + query.getKey())
                       .val(query.getValue());
                    continue;
                }

                if (query.getSearchField() instanceof TextSearchField &&
                        query.getSearchField().getData() != null &&
                        !query.getSearchField().getData().optBoolean("selectable", true) &&
                        doc.select("#" + query.getKey()).size() > 0) {
                    doc.select("#" + query.getKey())
                       .val(query.getValue());
                    continue;
                }

                dropdownTextCount++;

                if (s_exts.get(0).equals("SS2")
                        || (query.getSearchField().getData() != null && !query
                        .getSearchField().getData()
                        .optBoolean("selectable", true))) {
                    doc.select("input#" + query.getKey()).val(query.getValue());
                } else {
                    if (doc.select("select#SUCH01_1").size() == 0 &&
                            doc.select("input[fld=FELD01_" + dropdownTextCount + "]").size() > 0) {
                        // Hack needed for Nürnberg
                        doc.select("input[fld=FELD01_" + dropdownTextCount + "]").first()
                           .previousElementSibling().val(query.getKey());
                        doc.select("input[fld=FELD01_" + dropdownTextCount + "]")
                           .val(query.getValue());
                    } else {
                        doc.select("select#SUCH01_" + dropdownTextCount).val(query.getKey());
                        doc.select("input#FELD01_" + dropdownTextCount).val(query.getValue());
                    }
                }

                if (dropdownTextCount > 4) {
                    throw new OpacErrorException(stringProvider.getQuantityString(
                            StringProvider.LIMITED_NUM_OF_CRITERIA, 4, 4));
                }
            }
        }

        List<NameValuePair> nvpairs = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                nvpairs.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
            addIfSubmitWithFocus(nvpairs, input, "Suche starten");
        }
        nvpairs.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        nvpairs.add(new BasicNameValuePair("$Toolbar_0.y", "1"));

        if (totalCount == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }

        Document docresults = htmlPost(opac_url + ";jsessionid=" + s_sid,
                nvpairs);

        return parse_search_wrapped(docresults, 1);
    }

    public class SingleResultFound extends Exception {
    }

    protected SearchRequestResult parse_search_wrapped(Document doc, int page)
            throws IOException, OpacErrorException {
        try {
            return parse_search(doc, page);
        } catch (SingleResultFound e) {
            // Zurück zur Trefferliste
            List<NameValuePair> nvpairs = new ArrayList<>();
            for (Element input : doc.select("input, select")) {
                if (!"image".equals(input.attr("type"))
                        && !"submit".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))) {
                    nvpairs.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
            nvpairs.add(new BasicNameValuePair("$Toolbar_1.x", "1"));
            nvpairs.add(new BasicNameValuePair("$Toolbar_1.y", "1"));
            // Stuttgart
            nvpairs.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
            nvpairs.add(new BasicNameValuePair("$Toolbar_0.y", "1"));

            doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);

            try {
                return parse_search(doc, page);
            } catch (SingleResultFound e1) {
                throw new NotReachableException();
            }
        }
    }

    private SearchRequestResult parse_search(Document doc, int page)
            throws OpacErrorException, SingleResultFound {

        if (doc.select(".message h1").size() > 0
                && doc.select("#right #R06").size() == 0) {
            throw new OpacErrorException(doc.select(".message h1").text());
        }
        if (doc.select("#OPACLI").text().contains("nicht gefunden")) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_RESULTS));
        }

        int total_result_count = -1;
        List<SearchResult> results = new ArrayList<>();

        if (doc.select("#R06").size() > 0) {
            Pattern patNum = Pattern
                    .compile(".*Treffer: .* von ([0-9]+)[^0-9]*");
            Matcher matcher = patNum.matcher(doc.select("#R06").text()
                                                .trim());
            if (matcher.matches()) {
                total_result_count = Integer.parseInt(matcher.group(1));
            } else if (doc.select("#R06").text().trim().endsWith("Treffer: 1")) {
                total_result_count = 1;
            }
        }

        if (doc.select("#R03").size() == 1
                && doc.select("#R03").text().trim()
                      .endsWith("Treffer: 1")) {
            throw new SingleResultFound();
        }

        Pattern patId = Pattern
                .compile("javascript:.*htmlOnLink\\('([0-9A-Za-z]+)'\\)");

        int nr = 1;

        String selector_row, selector_link, selector_img, selector_num, selector_text;
        if (doc.select("table.rTable_table tbody").size() > 0) {
            selector_row = "table.rTable_table tbody tr";
            selector_link = ".rTable_td_text a";
            selector_text = ".rList_name";
            selector_img = ".rTable_td_img img, .rTable_td_text img";
            selector_num = "tr td:first-child";
        } else {
            // New version, e.g. Berlin
            selector_row = ".rList li.rList_li_even, .rList li.rList_li_odd";
            selector_link = ".rList_titel a";
            selector_text = ".rList_name";
            selector_img =
                    ".rlist_icon img, .rList_titel img, .rList_medium .icon, .rList_availability " +
                            ".icon, .rList_img img";
            selector_num = ".rList_num";
        }
        for (Element tr : doc.select(selector_row)) {
            SearchResult res = new SearchResult();

            Element innerele = tr.select(selector_link).first();
            innerele.select("img").remove();
            String descr = innerele.html();

            for (Element n : tr.select(selector_text)) {
                String t = n.text().replace("\u00a0", " ").trim();
                if (t.length() > 0) {
                    descr += "<br />" + t.trim();
                }
            }

            res.setInnerhtml(descr);

            try {
                res.setNr(Integer.parseInt(tr.select(selector_num).text().trim()));
            } catch (NumberFormatException e) {
                res.setNr(nr);
            }

            Matcher matcher = patId.matcher(tr.select(selector_link).first().attr("href"));
            if (matcher.matches()) {
                res.setId(matcher.group(1));
            }

            for (Element img : tr.select(selector_img)) {
                String ttext = img.attr("title");
                String src = img.attr("abs:src");
                if (types.containsKey(ttext)) {
                    res.setType(types.get(ttext));
                } else if (ttext.contains("+")
                        && types.containsKey(ttext.split("\\+")[0].trim())) {
                    res.setType(types.get(ttext.split("\\+")[0].trim()));
                } else if (ttext.matches(".*ist verf.+gbar") ||
                        ttext.contains("is available") ||
                        img.attr("href").contains("verfu_ja")) {
                    res.setStatus(SearchResult.Status.GREEN);
                } else if (ttext.matches(".*nicht verf.+gbar") ||
                        ttext.contains("not available") ||
                        img.attr("href").contains("verfu_nein")) {
                    res.setStatus(SearchResult.Status.RED);
                }
            }

            results.add(res);
            nr++;
        }

        updatePageform(doc);
        s_lastpage = page;

        return new SearchRequestResult(results, total_result_count, page);
    }

    @Override
    public void init(Library library, HttpClientFactory httpClientFactory) {
        super.init(library, httpClientFactory);
        this.library = library;
        this.data = library.getData();
        try {
            this.opac_url = data.getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        SearchRequestResult res = null;
        while (page != s_lastpage) {
            List<NameValuePair> nvpairs = s_pageform;
            int i = 0;
            List<Integer> indexes = new ArrayList<>();
            for (NameValuePair np : nvpairs) {
                if (np.getName().contains("$Toolbar_")) {
                    indexes.add(i);
                }
                i++;
            }
            for (int j = indexes.size() - 1; j >= 0; j--) {
                nvpairs.remove((int) indexes.get(j));
            }
            int p;
            if (page > s_lastpage) {
                nvpairs.add(new BasicNameValuePair("$Toolbar_5.x", "1"));
                nvpairs.add(new BasicNameValuePair("$Toolbar_5.y", "1"));
                p = s_lastpage + 1;
            } else {
                nvpairs.add(new BasicNameValuePair("$Toolbar_4.x", "1"));
                nvpairs.add(new BasicNameValuePair("$Toolbar_4.y", "1"));
                p = s_lastpage - 1;
            }

            Document docresults = htmlPost(opac_url + ";jsessionid=" + s_sid,
                    nvpairs);
            res = parse_search_wrapped(docresults, p);
        }
        return res;
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {

        Document doc;
        List<NameValuePair> nvpairs;

        if (id == null && s_reusedoc != null) {
            doc = s_reusedoc;
        } else {
            nvpairs = s_pageform;
            int i = 0;
            List<Integer> indexes = new ArrayList<>();
            for (NameValuePair np : nvpairs) {
                if (np.getName().contains("$Toolbar_")
                        || np.getName().contains("selected")) {
                    indexes.add(i);
                }
                i++;
            }
            for (int j = indexes.size() - 1; j >= 0; j--) {
                nvpairs.remove((int) indexes.get(j));
            }
            nvpairs.add(new BasicNameValuePair("selected", "ZTEXT       " + id));
            doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);

            List<NameValuePair> form = new ArrayList<>();
            for (Element input : doc.select("input, select")) {
                if (!"image".equals(input.attr("type"))
                        && !"submit".equals(input.attr("type"))
                        && !"checkbox".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))
                        && !"selected".equals(input.attr("name"))) {
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
            form.add(new BasicNameValuePair("selected", "ZTEXT       " + id));
            doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
            // Yep, two times.
        }

        // Reset
        updatePageform(doc);
        nvpairs = s_pageform;
        nvpairs.add(new BasicNameValuePair("$Toolbar_1.x", "1"));
        nvpairs.add(new BasicNameValuePair("$Toolbar_1.y", "1"));
        parse_search_wrapped(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs), 1);
        nvpairs = s_pageform;
        nvpairs.add(new BasicNameValuePair("$Toolbar_3.x", "1"));
        nvpairs.add(new BasicNameValuePair("$Toolbar_3.y", "1"));
        parse_search_wrapped(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs), 1);

        return parseResult(id, doc);
    }

    DetailedItem parseResult(String id, Document doc)
            throws IOException, OpacErrorException {
        List<NameValuePair> nvpairs;
        DetailedItem res = new DetailedItem();

        if (doc.select("#R001 img").size() == 1) {
            String cover_url = doc.select("#R001 img").first().absUrl("src");
            if (!cover_url.endsWith("erne.gif")) {
                // If there is no cover, the first image usually is the "n Stars" rating badge
                res.setCover(cover_url);
            }
        }

        for (Element tr : doc.select("#R06 .aDISListe table tbody tr")) {
            if (tr.children().size() < 2) {
                continue;
            }
            String title = tr.child(0).text().trim();
            String value = tr.child(1).text().trim();
            if (value.contains("hier klicken") || value.startsWith("zur ") ||
                    title.contains("URL")) {
                res.addDetail(new Detail(title, tr.child(1).select("a").first().absUrl("href")));
            } else {
                res.addDetail(new Detail(title, value));
            }

            if (title.contains("Titel") && res.getTitle() == null) {
                res.setTitle(value.split("[:/;]")[0].trim());
            }
        }

        if (res.getTitle() == null) {
            for (Detail d : res.getDetails()) {
                if (d.getDesc().contains("Gesamtwerk")) {
                    res.setTitle(d.getContent());
                    break;
                }
            }
        }

        if (doc.select(
                "input[value*=Reservieren], input[value*=Vormerken], " +
                        "input[value*=Einzelbestellung]")
               .size() > 0 && id != null) {
            res.setReservable(true);
            res.setReservation_info(id);
        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        if (doc.select("#R08 table.rTable_table, #R09 table.rTable_table").size() > 0) {
            Element table = doc.select("#R08 table.rTable_table, #R09 table.rTable_table").first();
            Map<Integer, String> colmap = new HashMap<>();
            int i = 0;
            for (Element th : table.select("thead tr th")) {
                String head = th.text().trim();
                if (head.contains("Bibliothek") || head.contains("Library")) {
                    colmap.put(i, "branch");
                } else if (head.contains("Standort") || head.contains("Location")) {
                    colmap.put(i, "location");
                } else if (head.contains("Signatur") || head.contains("Call number")) {
                    colmap.put(i, "signature");
                } else if (head.contains("URL")) {
                    colmap.put(i, "url");
                } else if (head.contains("Status") || head.contains("Hinweis")
                        || head.contains("Leihfrist") || head.matches(".*Verf.+gbarkeit.*")) {
                    colmap.put(i, "status");
                }
                i++;
            }

            for (Element tr : table.select("tbody tr")) {
                Copy copy = new Copy();
                for (Entry<Integer, String> entry : colmap.entrySet()) {
                    if (entry.getValue().equals("status")) {
                        String status = tr.child(entry.getKey()).text().trim();
                        String currentStatus =
                                copy.getStatus() != null ? copy.getStatus() + " - " : "";
                        if (status.contains(" am: ")) {
                            copy.setStatus(currentStatus + status.split("-")[0]);
                            try {
                                copy.setReturnDate(fmt.parseLocalDate(status.split(": ")[1]));
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                        } else {
                            copy.setStatus(currentStatus + status);
                        }
                    } else {
                        copy.set(entry.getValue(), tr.child(entry.getKey()).text().trim());
                    }
                }
                res.addCopy(copy);
            }
        }

        res.setId(""); // null would be overridden by the UI, because there _is_
        // an id,< we just can not use it.
        return res;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException,
            OpacErrorException {
        if (s_reusedoc != null) {
            return getResultById(null, null);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {

        Document doc;
        List<NameValuePair> nvpairs;
        ReservationResult res = null;

        if (selection != null && selection.equals("")) {
            selection = null;
        }

        if (s_pageform == null) {
            return new ReservationResult(Status.ERROR);
        }

        // Load details
        nvpairs = s_pageform;
        int i = 0;
        List<Integer> indexes = new ArrayList<>();
        for (NameValuePair np : nvpairs) {
            if (np.getName().contains("$Toolbar_")
                    || np.getName().contains("selected")) {
                indexes.add(i);
            }
            i++;
        }
        for (int j = indexes.size() - 1; j >= 0; j--) {
            nvpairs.remove((int) indexes.get(j));
        }

        // 1. Details auswählen
        nvpairs.add(new BasicNameValuePair("selected", "ZTEXT       "
                + item.getReservation_info()));
//        htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs); // Yep, two
        // times. Really?

        // 2. Reservieren
        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && (!"submit".equals(input.attr("type"))
                    || input.val().contains("Reservieren")
                    || input.val().contains("Einzelbestellung")
                    || input.val().contains("Vormerken"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {

                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));

//                addIfSubmitWithFocus(form, input, "Einzelbestellung");
                // Bei Einzelbestellung (StB Suttgart) noch focus setzen
                if (input.val().contains("Einzelbestellung")) {
                    String fld = input.attr("fld");
                    boolean withFocus = false;
                    if (withFocus && (fld != null)) {
                        form.add(new BasicNameValuePair("focus", fld));
                    }
                }
            }
        }
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        // 3. je nachdem
        if (doc.select(".message h1").size() > 0) {
            String msg = doc.select(".message h1").text().trim();
            res = new ReservationResult(MultiStepResult.Status.ERROR, msg);
            form = new ArrayList<>();
            for (Element input : doc.select("input")) {
                if (!"image".equals(input.attr("type"))
                        && !"checkbox".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))) {
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
            doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
        } else {
            // 3. Anmelden
            try {
                doc = handleLoginForm(doc, account);
            } catch (OpacErrorException e1) {
                return new ReservationResult(MultiStepResult.Status.ERROR,
                        e1.getMessage());
            }

            if (useraction == 0 && selection == null) {
                res = new ReservationResult(
                        MultiStepResult.Status.CONFIRMATION_NEEDED);
                List<String[]> details = new ArrayList<>();
                details.add(new String[]{doc.select("#F23").text()});
                res.setDetails(details);
            } else if (doc.select("#AUSGAB_1").size() > 0 &&
                    (selection == null || "confirmed".equals(selection))) {
                List<Map<String, String>> sel = new ArrayList<>();
                for (Element opt : doc.select("#AUSGAB_1 option")) {
                    if (opt.text().trim().length() > 0) {
                        Map<String, String> selopt = new HashMap<>();
                        selopt.put("key", opt.val());
                        selopt.put("value", opt.text());
                        sel.add(selopt);
                    }
                }
                res = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED, doc.select(
                        "#AUSGAB_1").first().parent().select("span").text());
                res.setSelection(sel);
            } else if (doc.select("#FSET01 select[name=select$0]").size() > 0 &&
                    (selection == null || !selection.contains("_SEP_"))) {
                // Munich: "Benachrichtigung mit E-Mail"
                List<Map<String, String>> sel = new ArrayList<>();
                for (Element opt : doc.select("select[name=select$0] option")) {
                    if (opt.text().trim().length() > 0) {
                        Map<String, String> selopt = new HashMap<>();
                        selopt.put("value", opt.text());
                        if (selection != null) {
                            selopt.put("key", opt.val() + "_SEP_" + selection);
                        } else {
                            selopt.put("key", opt.val());
                        }
                        sel.add(selopt);
                    }
                }
                res = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED, doc.select(
                        "#FSET01 select[name=select$0]").first().parent().select("span").text());
                res.setSelection(sel);
            } else if (selection != null || doc.select("#AUSGAB_1").size() == 0) {
                if (doc.select("#AUSGAB_1").size() > 0 && selection != null) {
                    if (selection.contains("_SEP_")) {
                        doc.select("#AUSGAB_1").attr("value", selection.split("_SEP_")[1]);
                    } else {
                        doc.select("#AUSGAB_1").attr("value", selection);
                    }
                }
                if (doc.select("#FSET01 select[name=select$0]").size() > 0 && selection != null) {
                    if (selection.contains("_SEP_")) {
                        doc.select("#FSET01 select[name=select$0]")
                           .attr("value", selection.split("_SEP_")[0]);
                    } else {
                        doc.select("#FSET01 select[name=select$0]").attr("value", selection);
                    }
                }
                if (doc.select("#BENJN_1").size() > 0) {
                    // Notification not requested because some libraries notify by snail mail
                    // and take a fee for it (Example: Stuttgart_Uni)
                    doc.select("#BENJN_1").attr("value", "Nein");
                }
                if (doc.select(".message h1").size() > 0) {
                    String msg = doc.select(".message h1").text().trim();
                    form = new ArrayList<>();
                    for (Element input : doc.select("input")) {
                        if (!"image".equals(input.attr("type"))
                                && !"checkbox".equals(input.attr("type"))
                                && !"".equals(input.attr("name"))) {
                            form.add(new BasicNameValuePair(input.attr("name"),
                                    input.attr("value")));
                        }
                    }
                    doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
                    if (!msg.contains("Reservation ist erfolgt")) {
                        res = new ReservationResult(
                                MultiStepResult.Status.ERROR, msg);
                    } else {
                        res = new ReservationResult(MultiStepResult.Status.OK,
                                msg);
                    }
                } else {
                    form = getPageform(doc);
                    form.add(new BasicNameValuePair("textButton",
                            "Reservation abschicken"));
                    res = new ReservationResult(MultiStepResult.Status.OK);
                    doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

                    if (doc.select("input[name=textButton]").attr("value")
                           .contains("kostenpflichtig bestellen")) {
                        // Munich
                        form = getPageform(doc);
                        form.add(new BasicNameValuePair("textButton",
                                doc.select("input[name=textButton]").first().attr("value")));
                        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
                    }

                    if (doc.select(".message h1").size() > 0) {
                        String msg = doc.select(".message h1").text().trim();
                        form = new ArrayList<>();
                        for (Element input : doc.select("input")) {
                            if (!"image".equals(input.attr("type"))
                                    && !"checkbox".equals(input.attr("type"))
                                    && !"".equals(input.attr("name"))) {
                                form.add(new BasicNameValuePair(input
                                        .attr("name"), input.attr("value")));
                            }
                        }
                        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
                        if (!msg.contains("Reservation ist erfolgt")) {
                            res = new ReservationResult(
                                    MultiStepResult.Status.ERROR, msg);
                        } else {
                            res = new ReservationResult(
                                    MultiStepResult.Status.OK, msg);
                        }
                    } else if (doc.select("#R01").text()
                                  .contains("Informationen zu Ihrer Reservation")) {
                        String msg = doc.select("#OPACLI").text().trim();
                        form = new ArrayList<>();
                        for (Element input : doc.select("input")) {
                            if (!"image".equals(input.attr("type"))
                                    && !"checkbox".equals(input.attr("type"))
                                    && !"".equals(input.attr("name"))) {
                                form.add(new BasicNameValuePair(input
                                        .attr("name"), input.attr("value")));
                            }
                        }
                        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
                        if (!msg.contains("Reservation ist erfolgt")) {
                            res = new ReservationResult(
                                    MultiStepResult.Status.ERROR, msg);
                        } else {
                            res = new ReservationResult(
                                    MultiStepResult.Status.OK, msg);
                        }
                    }
                }
            }
        }

        if (res == null
                || res.getStatus() == MultiStepResult.Status.SELECTION_NEEDED
                || res.getStatus() == MultiStepResult.Status.CONFIRMATION_NEEDED) {
            form = getPageform(doc);
            Element button = doc.select("input[value=Abbrechen], input[value=Zurück]").first();
            form.add(new BasicNameValuePair(button.attr("name"), button.attr("value")));
            doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
        }

        // Reset
        updatePageform(doc);
        try {
            nvpairs = s_pageform;
            nvpairs.add(new BasicNameValuePair("$Toolbar_1.x", "1"));
            nvpairs.add(new BasicNameValuePair("$Toolbar_1.y", "1"));
            parse_search_wrapped(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs),
                    1);
            nvpairs = s_pageform;
            nvpairs.add(new BasicNameValuePair("$Toolbar_3.x", "1"));
            nvpairs.add(new BasicNameValuePair("$Toolbar_3.y", "1"));
            parse_search_wrapped(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs),
                    1);
        } catch (OpacErrorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return res;
    }

    void updatePageform(Document doc) {
        s_pageform = getPageform(doc);
    }

    /**
     * Sammelt alle name-value-Paare zu input- oder select-Elementen
     * in der Seite ein, die nicht vom type image, submit, checkbox sind
     * oder deren name leer ist
     *
     * @param doc das html-Document mit der FORM
     * @return Liste der NameValuePair's
     */
    protected List<NameValuePair> getPageform(Document doc) {
        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
        return form;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {

        final String methodName = "prolong";

        Document doc = htmlGetAusleihen(account);
        if (doc == null) {
            return new ProlongResult(Status.ERROR);
        }

        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            logInfo("%s - type=%s %s=%s", methodName, input.attr("type"),
                    input.attr("type"), input.attr("value"));
            // erstes if passt zu getPageForm
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
            if ("submit".equals(input.attr("type"))
                    && "Markierte Titel verlängern".equals(input.attr("value"))) {

                logInfo("%s - type=submit %s=%s, fld=%s", methodName, input.attr("type"),
                        input.attr("value"), input.attr("fld"));

                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
                form.add(new BasicNameValuePair("foucs", input
                        .attr("fld")));
            }
        }
        for (Element tr : doc.select(".rTable_div tr")) {
            if (tr.select("input").attr("name").equals(media.split("\\|")[0])) {
                boolean disabled = tr.select("input").hasAttr("disabled");
                try {
                    disabled = (
                            disabled
                                    || tr.child(4).text().matches(".*nicht verl.+ngerbar.*")
                                    ||
                                    tr.child(4).text().matches(".*Verl.+ngerung nicht m.+glich.*")
                    );
                } catch (Exception e) {
                }

                if (disabled) {
                    form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
                    form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
                    htmlPost(opac_url + ";jsessionid=" + s_sid, form);
                    return new ProlongResult(Status.ERROR, tr.child(4).text().trim());
                }
            }
        }
        form.add(new BasicNameValuePair(media.split("\\|")[0], "on"));
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        form = getPageform(doc);
        form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
        htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        return new ProlongResult(Status.OK);
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {

        final String methodName = "prolongAll";

        Document doc = htmlGetAusleihen(account);
        if (doc == null) {
            return new ProlongAllResult(Status.ERROR);
        }

        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            // erstes if passt zu getPageForm
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
            if ("checkbox".equals(input.attr("type"))
                    && !input.hasAttr("disabled")) {
                form.add(new BasicNameValuePair(input.attr("name"), "on"));
            }
            if ("submit".equals(input.attr("type"))
                    && "Markierte Titel verlängern".equals(input.attr("value"))) {

                logInfo("%s - type=submit %s=%s, fld=%s", methodName, input.attr("type"),
                        input.attr("value"), input.attr("fld"));

                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
                form.add(new BasicNameValuePair("foucs", input
                        .attr("fld")));
            }
        }
        form.add(new BasicNameValuePair("textButton$1",
                "Markierte Titel verlängern"));
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        List<Map<String, String>> result = new ArrayList<>();
        for (Element tr : doc.select(".rTable_div tbody tr")) {
            Map<String, String> line = new HashMap<>();
            line.put(ProlongAllResult.KEY_LINE_TITLE,
                    tr.child(3).text().split("[:/;]")[0].trim());
            line.put(ProlongAllResult.KEY_LINE_NEW_RETURNDATE, tr.child(1)
                                                                 .text());
            line.put(ProlongAllResult.KEY_LINE_MESSAGE, tr.child(4).text());
            result.add(line);
        }

        form = getPageform(doc);
        form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
        htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        return new ProlongAllResult(Status.OK, result);
    }

    private Document htmlGetAusleihen(Account account) throws IOException {
        start();

        Document doc = null;
        try {
            doc = htmlGetWithHandleLogin(account);
        } catch (OpacErrorException e) {
            return null;
        }

        for (Element tr : doc.select(".rTable_div tr")) {
            if (tr.select("a").size() == 1) {
                if (tr.select("a").first().absUrl("href")
                      .contains("sp=SZA")) {
                    String alink = tr.select("a").first().absUrl("href");
                    return htmlGet(alink);
                }
            }
        }
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {

        start();

        Document doc = null;
        try {
            doc = htmlGetWithHandleLogin(account);
        } catch (OpacErrorException e) {
            return new CancelResult(Status.ERROR, e.getMessage());
        }

        String rlink = null;
        rlink = media.split("\\|")[1].replace("requestCount=", "fooo=");

        // Link auf Seite mit Vormerkungen ermitteln
        for (Element tr : doc.select(".rTable_div tr")) {
            String url = media.split("\\|")[1].toUpperCase(Locale.GERMAN);
            String sp = "SZM";
            if (url.contains("SP=")) {
                Map<String, String> qp = getQueryParamsFirst(url);
                if (qp.containsKey("SP")) {
                    sp = qp.get("SP");
                }
            }
            if (tr.select("a").size() == 1) {
                if ((tr.text().contains("Reservationen")
                        || tr.text().contains("Vormerkung")
                        || tr.text().contains("Bestellung"))
                        && !tr.child(0).text().trim().equals("")
                        && tr.select("a").first().attr("href")
                             .toUpperCase(Locale.GERMAN)
                             .contains("SP=" + sp)) {
                    rlink = tr.select("a").first().absUrl("href");
                }
            }
        }
        if (rlink == null) {
            return new CancelResult(Status.ERROR);
        }

        // Seite mit Vormerkungen aufrufen
        doc = htmlGet(rlink);

        // Standard (hidden) inputs setzen
//        List<NameValuePair> form = getPageform(doc);
        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {

                if ("submit".equals(input.attr("type"))) {
                    addIfSubmitWithFocus(form, input, "Markierte Titel löschen"); // Stuttgart
                } else {
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
        }
        // Checkbox setzen
        form.add(new BasicNameValuePair(media.split("\\|")[0], "on"));
        // Button bedienen (wäre für Stuttgar nicht notwendig)
        form.add(new BasicNameValuePair("textButton$0",
                "Markierte Titel löschen"));
        // Aufrufen
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        // Wir kommen wieder auf der Seite mit den Vormerkungen raus
        // daher zuück zur Konto-Übersichts-Seite
        form = getPageform(doc);
        form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
        htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        return new CancelResult(Status.OK);
    }

    protected Document htmlGetWithHandleLogin(Account account) throws IOException,
            OpacErrorException {
        String url = String.format(s_hrefFormatAccount, opac_url, s_sid, s_requestCount);
        Document doc = htmlGet(url);
        return doc = handleLoginForm(doc, account);

    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        start();

        Document doc = htmlGetWithHandleLogin(account);

        boolean split_title_author = true;
        if (doc.head().html().contains("VOEBB")) {
            split_title_author = false;
        }

        AccountData adata = new AccountData(account.getId());
        for (Element tr : doc.select(".aDISListe tr")) {
            if (tr.child(0).text().matches(".*F.+llige Geb.+hren.*")) {
                adata.setPendingFees(tr.child(1).text().trim());
            }
            if (tr.child(0).text().matches(".*Ausweis g.+ltig bis.*")) {
                adata.setValidUntil(tr.child(1).text().trim());
            }
        }
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        AccountPageReturn apr = parseAccount(doc);
        List<LentItem> lent = new ArrayList<>();
        if (apr.alink != null) {
            // Ausleihen-Seite aufrufen
            Document adoc = htmlGet(apr.alink);
            s_alink = apr.alink;
            List<NameValuePair> form = new ArrayList<>();
            String prolongTest = null;
            for (Element input : adoc.select("input, select")) {
                if (!"image".equals(input.attr("type"))
                        && !"submit".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))) {
                    if (input.attr("type").equals("checkbox")
                            && !input.hasAttr("value")) {
                        input.val("on");
                    }
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                } else if (input.val().matches(".+verl.+ngerbar.+")) {
                    prolongTest = input.attr("name");
                }
            }
            if (prolongTest != null) {
                form.add(new BasicNameValuePair(prolongTest,
                        "Markierte Titel verlängerbar?"));
                Document adoc_new = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
                if (adoc_new.select(".message h1").size() == 0) {
                    adoc = adoc_new;
                }
            }
            parseMediaList(adoc, apr.alink, split_title_author, lent);
            assert (lent.size() == apr.anum);

            // Zurück auf Mein-Konto-Seite mit Abbrechen
            htmlPostCancel(adoc);
        } else {
            assert (apr.anum == 0);
        }
        adata.setLent(lent);

        // ReseverdItem auf den diversen Reservation-Übersichtseiten einsammeln
        List<ReservedItem> res = new ArrayList<>();
        for (String[] rlink : apr.rlinks) {
            // Reservations-Seite aufrufen
            Document rdoc = htmlGet(rlink[1]);

            // Reservations-Seite parsen
            boolean error = parseReservationList(rdoc, rlink, split_title_author, res, fmt);

            if (error) {
                // Maybe we should send a bug report here, but using ACRA breaks
                // the unit tests
                adata.setWarning("Beim Abrufen der Reservationen ist ein Problem aufgetreten");
            }

            // Zurück auf Mein-Konto-Übersichtsseite
            htmlPostCancel(rdoc);
        }

        assert (res.size() == apr.rnum);

        adata.setReservations(res);

        return adata;
    }

    protected Document htmlPostCancel(Document doc) throws IOException {
        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            // if passt zu getPageform
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
            addIfSubmitWithFocus(form, input, "Abbrechen");
        }

        form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));

        return htmlPost(opac_url + ";jsessionid=" + s_sid, form);
    }

    /**
     * Ergänzt zur form ein NameValuePair mit "name" und "value" des Elements input.
     * falls es sich vom type "submit" ist und ( text==null oder input den Text text enthält)
     *
     * @param form  Liste von NameValuePair's
     * @param input das input-Element
     * @param text  für den Test von input.val().contains
     * @return true falls ein NameValuePair in form ergänzt wurde, sonst false
     */
    protected boolean addIfSubmitWithFocus(List<NameValuePair> form, Element input, String text) {
        if (!"submit".equals(input.attr("type"))) {
            return false;
        }

        if ((text == null) || input.val().contains(text)) {
            form.add(new BasicNameValuePair(input.attr("name"), input
                    .attr("value")));
            String fld = input.attr("fld");
            boolean withFocus = false;
            // Test ohne focus-fld als NameValuePair
            if (withFocus && (fld != null) && (fld.length() > 0)) {

                // ev. vorhandenen focus-Wert überschreiben
                int i = 0;
                for (i = 0; i < form.size(); i++) {
                    NameValuePair pair = form.get(i);
                    if (pair.getName().equals("focus")) {
                        // bereits vorhanden
                        break;
                    }
                }
                if (i < form.size()) {
                    // replace
                    form.set(i, new BasicNameValuePair("focus", fld));
                } else {
                    // add
                    form.add(new BasicNameValuePair("focus", fld));
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Class for return-Info from parseAccount-method
     */
    class AccountPageReturn {

        /**
         * für Link auf die Ausleih-Seite
         */
        String alink;

        /**
         * Anzahl Ausleihen laut Kontoseite
         */
        int anum;

        /**
         * für Links auf Reservations-Seiten
         */
        List<String[]> rlinks;

        /**
         * Anzahl Vormerkungen laut Kontoseite
         */
        int rnum;

        AccountPageReturn() {
            rlinks = new ArrayList<String[]>();
        }
    }

    /**
     * Parses die Account-Page, collects Links und num-Information
     *
     * @param doc the Accout-Page
     * @return Links and num's
     */
    AccountPageReturn parseAccount(Document doc) {

        // Mein-Konto-/Übersichtseite nach Links durchsuchen und einsammeln
        AccountPageReturn apr = new AccountPageReturn();
        List<LentItem> lent = new ArrayList<>();
        for (Element tr : doc.select(".rTable_div tr")) {
            Elements as = tr.select("a");
            if (as.size() == 1) {
                String href = as.first().absUrl("href");
                if (href.contains("sp=SZA")) {
                    // Link auf die Ausleihen-Seite
                    apr.alink = href;
                    apr.anum = Integer.parseInt(tr.child(0).text().trim());
                } else if ((tr.text().contains("Reservationen")
                        || tr.text().contains("Vormerkung")
                        || tr.text().contains("Fernleihbestellung")
                        || tr.text().contains("Bereitstellung")
                        || tr.text().contains("Bestellw")
                        || tr.text().contains("Magazin"))
                        && !tr.child(0).text().trim().equals("")) {

                    // Link eine Reservations-Sseite
                    apr.rlinks.add(new String[]{
                            tr.select("a").text(),
                            href,
                    });
                    apr.rnum += Integer.parseInt(tr.child(0).text().trim());
                } else if (href.contains("sp=SZM")) {
                    // auch Link auf Reservations-Seite,
                    // in StB Stuttgart = tr.text().contains("Vormerkung")
                    apr.rlinks.add(new String[]{
                            as.first().text(),
                            href,
                    });
                    apr.rnum += Integer.parseInt(tr.child(0).text().trim());
                }
            }
        }
        return apr;
    }

    /**
     * Parses the (lent-) Medialist-Page
     *
     * @param doc                the (lent-) Medialist-page as Document
     * @param link               link/href for Medialist-page
     * @param split_title_author switch whether to split title and author
     * @param lent               (initialized) list of LentItem's, for adding the found media
     */
    static void parseMediaList(Document doc, final String link, boolean split_title_author,
            List<LentItem> lent) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        for (Element tr : doc.select(".rTable_div tbody tr")) {
            LentItem item = new LentItem();
            String text = tr.child(3).html();
            // <br/> durch # ersetzen, html-parsen und text() geben lassen
            // (?i) --> matching case-insensitiv:
            text = Jsoup.parse(text.replaceAll("(?i)<br[^>]*>", "#")).text();
            if (text.contains(" / ")) {
                // Format "Titel / Autor #Sig#Nr", z.B. normale Ausleihe in Berlin
                String[] split = text.split("[/#\n]");
                String title = split[0];
                //Is always the last one...
                String id = split[split.length - 1];
                item.setId(id);
                if (split_title_author) {
                    title = title.replaceFirst("([^:;\n]+)[:;\n](.*)$", "$1");
                }
                item.setTitle(title.trim());
                if (split.length > 1) {
                    item.setAuthor(split[1].replaceFirst("([^:;\n]+)[:;\n](.*)$", "$1").trim());
                }
            } else {
                // Format "Autor: Titel - Verlag - ISBN:... #Nummer", z.B. Fernleihe in Berlin
                String[] split = text.split("#");
                String[] aut_tit = split[0].split(": ");
                item.setAuthor(aut_tit[0].replaceFirst("([^:;\n]+)[:;\n](.*)$", "$1").trim());
                if (aut_tit.length > 1) {
                    item.setTitle(
                            aut_tit[1].replaceFirst("([^:;\n]+)[:;\n](.*)$", "$1").trim());
                }
                //Is always the last one...
                String id = split[split.length - 1];
                item.setId(id);
            }
            String date = tr.child(1).text().trim();
            if (date.contains("-")) {
                // Nürnberg: "29.03.2016 - 26.04.2016"
                // for beginning and end date in one field
                date = date.split("-")[1].trim();
            }
            try {
                item.setDeadline(fmt.parseLocalDate(date));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            item.setHomeBranch(tr.child(2).text().trim());
            if (tr.select("input[type=checkbox]").hasAttr("disabled")) {
                item.setRenewable(false);
            } else {
                try {
                    item.setRenewable(
                            !tr.child(4).text().matches(".*nicht verl.+ngerbar.*")
                                    &&
                                    !tr.child(4).text().matches(".*Verl.+ngerung nicht m.+glich.*")
                    );
                } catch (Exception e) {

                }
                item.setProlongData(
                        tr.select("input[type=checkbox]").attr("name") + "|" + link);
            }

            lent.add(item);
        }
    }

    /**
     * Parses a reservation-Page
     *
     * @param rdoc               the reservation-page to parse as Document
     * @param rlink              links/href for all reservation-pages
     * @param split_title_author switch whether to split title and author
     * @param res                list of ReservedItem's, for adding the found media
     * @param fmt                formatter to parse the expirationdate
     * @return true if a media-row doesnot have enough columns, otherwise false
     */
    boolean parseReservationList(Document rdoc, String[] rlink,
            boolean split_title_author, List<ReservedItem> res, DateTimeFormatter fmt) {
        boolean error = false;
        boolean interlib = rdoc.html().contains("Ihre Fernleih-Bestellung");
        boolean stacks = rdoc.html().contains("aus dem Magazin");
        boolean provision = rdoc.html().contains("Ihre Bereitstellung");

        Map<String, Integer> colmap = new HashMap<>();
        colmap.put("title", 2);
        colmap.put("branch", 1);
        colmap.put("expirationdate", 0);
        int i = 0;
        for (Element th : rdoc.select(".rTable_div thead tr th")) {
            if (th.text().contains("Bis")) {
                colmap.put("expirationdate", i);
            }
            if (th.text().contains("Ausgabeort")) {
                colmap.put("branch", i);
            }
            if (th.text().contains("Titel")) {
                colmap.put("title", i);
            }
            if (th.text().contains("Hinweis")) {
                colmap.put("status", i);
            }
            i++;

        }
        for (Element tr : rdoc.select(".rTable_div tbody tr")) {
            if (tr.children().size() >= colmap.size()) {
                ReservedItem item = new ReservedItem();
                String text = tr.child(colmap.get("title")).html();
                // <br/> durch ; ersetzen, html-parsen und text() geben lassen
                // (?i) --> matching case-insensitiv:
                text = Jsoup.parse(text.replaceAll("(?i)<br[^>]*>", ";")).text();
                if (split_title_author) {
                    String[] split = text.split("[:/;\n]");
                    item.setTitle(split[0].replaceFirst("([^:;\n]+)[:;\n](.*)$", "$1").trim());
                    if (split.length > 1) {
                        item.setAuthor(
                                split[1].replaceFirst("([^:;\n]+)[:;\n](.*)$", "$1").trim());
                    }
                } else {
                    item.setTitle(text);
                }

                String branch = tr.child(colmap.get("branch")).text().trim();
                if (interlib) {
                    branch = stringProvider
                            .getFormattedString(StringProvider.INTERLIB_BRANCH, branch);
                } else if (stacks) {
                    branch = stringProvider
                            .getFormattedString(StringProvider.STACKS_BRANCH, branch);
                } else if (provision) {
                    branch = stringProvider
                            .getFormattedString(StringProvider.PROVISION_BRANCH, branch);
                }
                item.setBranch(branch);

                if (rlink[0].contains("Abholbereit")) {
                    // Abholbereite Bestellungen
                    item.setStatus("bereit");
                    if (tr.child(0).text().trim().length() >= 10) {
                        item.setExpirationDate(fmt.parseLocalDate(
                                tr.child(colmap.get("expirationdate")).text().trim()
                                  .substring(0, 10)));
                    }
                } else {
                    // Nicht abholbereite
                    if (tr.select("input[type=checkbox]").size() > 0
                            && (rlink[1].toUpperCase(Locale.GERMAN).contains(
                            "SP=SZM") || rlink[1].toUpperCase(
                            Locale.GERMAN).contains("SP=SZW") || rlink[1].toUpperCase(
                            Locale.GERMAN).contains("SP=SZB"))) {
                        item.setCancelData(
                                tr.select("input[type=checkbox]").attr("name") + "|" +
                                        rlink[1]);
                    }
                }
                res.add(item);
            } else {
                // This is a strange bug where sometimes there is only three
                // columns
                error = true;
            }
        }
        return error;
    }

    protected Document handleLoginForm(Document doc, Account account)
            throws IOException, OpacErrorException {
        return handleLoginForm(doc, account, new ArrayList<NameValuePair>());
    }

    protected Document handleLoginForm(Document doc, Account account, List<NameValuePair> form)
            throws IOException, OpacErrorException {

        if (doc.select("#LPASSW_1").size() == 0) {
            return doc;
        }

        doc.select("#LPASSW_1").val(account.getPassword());

        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                if (input.attr("id").equals("L#AUSW_1")
                        || input.attr("fld").equals("L#AUSW_1")
                        || input.attr("id").equals("IDENT_1")
                        || input.attr("id").equals("LMATNR_1")) {
                    input.attr("value", account.getName());
                }
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
        Element inputSend = doc.select("input[type=submit]").first();
        addIfSubmitWithFocus(form, inputSend, null);

        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        if (doc.select(".message h1").size() > 0) {
            String msg = doc.select(".message h1").text().trim();
            form = new ArrayList<>();
            for (Element input : doc.select("input")) {
                if (!"image".equals(input.attr("type"))
                        && !"checkbox".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))) {
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
            doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
            if (!msg.contains("Sie sind angemeldet") && !msg.contains("jetzt angemeldet")) {
                throw new OpacErrorException(msg);
            }
            return doc;
        } else {
            return doc;
        }
    }

    @Override
    public List<SearchField> parseSearchFields() throws IOException,
            JSONException {

        // in Stuttgart_StB besser immer start(), falls man schon als letztes die
        // SearchPage aufgerufen hat, dann kommt beim nächsten mal (mit requestCount+1)
        // eine SearchPage mit fast leere Branch-Select-Liste
        start();

        Document doc = getSearchPage();

        return parseSearchFields(doc);
    }

    static List<SearchField> parseSearchFields(Document doc) throws IOException,
            JSONException {

        List<SearchField> fields = new ArrayList<>();
        // dropdown to select which field you want to search in
        Elements searchoptions = doc.select("#SUCH01_1 option");
        if (searchoptions.size() == 0 && doc.select("input[fld=FELD01_1]").size() > 0) {
            // Hack is needed in Nuernberg
            searchoptions = doc.select("input[fld=FELD01_1]").first().previousElementSibling()
                               .select("option");
        }
        for (Element opt : searchoptions) {

            // Damit doppelte Optionen nicht mehrfach auftauchen
            // (bei Stadtbücherei Stuttgart der Fall)
            boolean found = false;
            for (SearchField f : fields) {
                if (f.getDisplayName().equals(opt.text())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                TextSearchField field = new TextSearchField();
                field.setId(opt.attr("value"));
                field.setDisplayName(opt.text());
                field.setHint("");
                fields.add(field);
            }
        }

        // Save data so that the search() function knows that this
        // is not a selectable search field
        JSONObject selectableData = new JSONObject();
        selectableData.put("selectable", false);

        for (Element row : doc.select("div[id~=F\\d+|R15]")) {
            if (row.select("input[type=text]").size() == 1
                    && row.select("input, select").first().tagName()
                          .equals("input")) {
                // A single text search field
                Element input = row.select("input[type=text]").first();
                TextSearchField field = new TextSearchField();
                field.setId(input.attr("id"));
                field.setDisplayName(row.select("label").first().text());
                field.setHint("");
                field.setData(selectableData);
                fields.add(field);
            } else if (row.select("select").size() == 1
                    && row.select("input[type=text]").size() == 0) {
                // Things like language, media type, etc.
                Element select = row.select("select").first();
                DropdownSearchField field = new DropdownSearchField();
                field.setId(select.id());
                field.setDisplayName(row.select("label").first().text());
                List<Map<String, String>> values = new ArrayList<>();
                for (Element opt : select.select("option")) {
                    field.addDropdownValue(opt.attr("value"), opt.text());
                }
                fields.add(field);
            } else if (row.select("select").size() == 0
                    && row.select("input[type=text]").size() == 3
                    && row.select("label").size() == 3) {
                // Three text inputs.
                // Year single/from/to or things like Band-/Heft-/Satznummer
                String name1 = row.select("label").get(0).text();
                String name2 = row.select("label").get(1).text();
                String name3 = row.select("label").get(2).text();
                Element input1 = row.select("input[type=text]").get(0);
                Element input2 = row.select("input[type=text]").get(1);
                Element input3 = row.select("input[type=text]").get(2);

                if (name2.contains("von") && name3.contains("bis")) {
                    TextSearchField field1 = new TextSearchField();
                    field1.setId(input1.id());
                    field1.setDisplayName(name1);
                    field1.setHint("");
                    field1.setData(selectableData);
                    fields.add(field1);

                    TextSearchField field2 = new TextSearchField();
                    field2.setId(input2.id());
                    field2.setDisplayName(name2.replace("von", "").trim());
                    field2.setHint("von");
                    field2.setData(selectableData);
                    fields.add(field2);

                    TextSearchField field3 = new TextSearchField();
                    field3.setId(input3.id());
                    field3.setDisplayName(name3.replace("bis", "").trim());
                    field3.setHint("bis");
                    field3.setHalfWidth(true);
                    field3.setData(selectableData);
                    fields.add(field3);
                } else {
                    TextSearchField field1 = new TextSearchField();
                    field1.setId(input1.id());
                    field1.setDisplayName(name1);
                    field1.setHint("");
                    field1.setData(selectableData);
                    fields.add(field1);

                    TextSearchField field2 = new TextSearchField();
                    field2.setId(input2.id());
                    field2.setDisplayName(name2);
                    field2.setHint("");
                    field2.setData(selectableData);
                    fields.add(field2);

                    TextSearchField field3 = new TextSearchField();
                    field3.setId(input3.id());
                    field3.setDisplayName(name3);
                    field3.setHint("");
                    field3.setData(selectableData);
                    fields.add(field3);
                }
            }
        }

        for (Iterator<SearchField> iterator = fields.iterator(); iterator
                .hasNext(); ) {
            SearchField field = iterator.next();
            if (ignoredFieldNames.contains(field.getDisplayName())) {
                iterator.remove();
            }
        }

        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ACCOUNT_PROLONG_ALL
                | SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_WARN_RESERVATION_FEES;
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        start();

        Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
                + s_service + "&sp=SBK");
        handleLoginForm(doc, account);
    }

    @Override
    public void setLanguage(String language) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    private static void logFine(String format, Object... args) {
        if (!LOGGER.isLoggable(Level.FINE)) {
            return;
        }
        String msg = String.format(format, args);
        LOGGER.log(Level.FINE, msg);
    }

    private static void logInfo(String format, Object... args) {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }
        String msg = String.format(format, args);
        LOGGER.log(Level.INFO, msg);
    }

    private static void logWarning(String format, Object... args) {
        if (!LOGGER.isLoggable(Level.WARNING)) {
            return;
        }
        String msg = String.format(format, args);
        LOGGER.log(Level.INFO, msg);
    }

    private static void logStart(String methodName) {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }
        String msg = String.format("Start %s", methodName);
        LOGGER.log(Level.INFO, msg);
    }

    private static void logEnde(String methodName) {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }
        String msg = String.format("Ende  %s", methodName);
        LOGGER.log(Level.INFO, msg);
    }

}
