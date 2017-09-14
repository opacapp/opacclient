package de.geeksfactory.opacclient.apis;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

    protected static HashMap<String, MediaType> types = new HashMap<>();
    protected static HashSet<String> ignoredFieldNames = new HashSet<>();

    static {
        types.put("Buch", MediaType.BOOK);
        types.put("Band", MediaType.BOOK);
        types.put("DVD-ROM", MediaType.CD_SOFTWARE);
        types.put("CD-ROM", MediaType.CD_SOFTWARE);
        types.put("Medienkombination", MediaType.PACKAGE);
        types.put("DVD-Video", MediaType.DVD);
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
    protected String s_sid;
    protected String s_exts;
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

    public Document htmlGet(String url) throws
            IOException {

        if (!url.contains("requestCount") && s_requestCount >= 0) {
            url = url + (url.contains("?") ? "&" : "?") + "requestCount="
                    + s_requestCount;
        }

        HttpGet httpget = new HttpGet(cleanUrl(url));
        HttpResponse response;

        try {
            response = http_client.execute(httpget);
        } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
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
        HttpUtils.consume(response.getEntity());
        Document doc = Jsoup.parse(html);
        Pattern patRequestCount = Pattern.compile("requestCount=([0-9]+)");
        for (Element a : doc.select("a")) {
            Matcher objid_matcher = patRequestCount.matcher(a.attr("href"));
            if (objid_matcher.matches()) {
                s_requestCount = Integer.parseInt(objid_matcher.group(1));
            }
        }
        doc.setBaseUri(url);
        return doc;
    }

    public Document htmlPost(String url, List<NameValuePair> data)
            throws IOException {
        HttpPost httppost = new HttpPost(cleanUrl(url));

        boolean rcf = false;
        for (NameValuePair nv : data) {
            if (nv.getName().equals("requestCount")) {
                rcf = true;
                break;
            }
        }
        if (!rcf) {
            data.add(new BasicNameValuePair("requestCount", s_requestCount + ""));
        }

        httppost.setEntity(new UrlEncodedFormEntity(data, getDefaultEncoding()));
        HttpResponse response;

        try {
            response = http_client.execute(httppost);

        } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
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
        HttpUtils.consume(response.getEntity());
        Document doc = Jsoup.parse(html);
        Pattern patRequestCount = Pattern
                .compile(".*requestCount=([0-9]+)[^0-9].*");
        for (Element a : doc.select("a")) {
            Matcher objid_matcher = patRequestCount.matcher(a.attr("href"));
            if (objid_matcher.matches()) {
                s_requestCount = Integer.parseInt(objid_matcher.group(1));
            }
        }
        doc.setBaseUri(url);
        return doc;
    }

    @Override
    public void start() throws IOException {
        try {
            s_requestCount = -1;
            Document doc = htmlGet(opac_url + "?"
                    + data.getString("startparams"));

            Pattern padSid = Pattern
                    .compile(".*;jsessionid=([0-9A-Fa-f]+)[^0-9A-Fa-f].*");
            for (Element navitem : doc.select("#unav li a, .tree_ul li a")) {
                // Düsseldorf uses a custom layout where the navbar is .tree_ul
                if (navitem.attr("href").contains("service=")) {
                    s_service = getQueryParams(navitem.attr("href")).get(
                            "service").get(0);
                }
                if (navitem.text().contains("Erweiterte Suche")) {
                    s_exts = getQueryParams(navitem.attr("href")).get("sp")
                                                                 .get(0);
                }
                Matcher objid_matcher = padSid.matcher(navitem.attr("href"));
                if (objid_matcher.matches()) {
                    s_sid = objid_matcher.group(1);
                }
            }
            if (s_exts == null) {
                s_exts = "SS6";
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
        start();
        // TODO: There are also libraries with a different search form,
        // s_exts=SS2 instead of s_exts=SS6
        // e.g. munich. Treat them differently!
        Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
                + s_service + "&sp=" + s_exts);

        int dropdownTextCount = 0;
        int totalCount = 0;
        List<NameValuePair> nvpairs = new ArrayList<>();
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

                if (s_exts.equals("SS2")
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

        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                nvpairs.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
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

    protected SearchRequestResult parse_search_wrapped(Document doc, int page) throws IOException, OpacErrorException {
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

            doc  = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);

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
            selector_img = ".rlist_icon img, .rList_titel img, .rList_medium .icon, .rList_availability .icon, .rList_img img";
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

        if (doc.select("input[value*=Reservieren], input[value*=Vormerken]")
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
        nvpairs.add(new BasicNameValuePair("selected", "ZTEXT       "
                + item.getReservation_info()));
        htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs); // Yep, two
        // times.

        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && (!"submit".equals(input.attr("type"))
                    || input.val().contains("Reservieren") || input
                    .val().contains("Vormerken"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
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
            } else if (doc.select("#AUSGAB_1").size() > 0 && (selection == null || "confirmed".equals(selection))) {
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
                    form = new ArrayList<>();
                    for (Element input : doc.select("input, select")) {
                        if (!"image".equals(input.attr("type"))
                                && !"submit".equals(input.attr("type"))
                                && !"checkbox".equals(input.attr("type"))
                                && !"".equals(input.attr("name"))) {
                            form.add(new BasicNameValuePair(input.attr("name"),
                                    input.attr("value")));
                        }
                    }
                    form.add(new BasicNameValuePair("textButton",
                            "Reservation abschicken"));
                    res = new ReservationResult(MultiStepResult.Status.OK);
                    doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

                    if (doc.select("input[name=textButton]").attr("value")
                           .contains("kostenpflichtig bestellen")) {
                        // Munich
                        form = new ArrayList<>();
                        for (Element input : doc.select("input, select")) {
                            if (!"image".equals(input.attr("type"))
                                    && !"submit".equals(input.attr("type"))
                                    && !"checkbox".equals(input.attr("type"))
                                    && !"".equals(input.attr("name"))) {
                                form.add(new BasicNameValuePair(input.attr("name"),
                                        input.attr("value")));
                            }
                        }
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
            form = new ArrayList<>();
            for (Element input : doc.select("input, select")) {
                if (!"image".equals(input.attr("type"))
                        && !"submit".equals(input.attr("type"))
                        && !"checkbox".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))) {
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
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
        s_pageform = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                s_pageform.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        String alink = null;
        Document doc;

        start();
        doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
                + s_service + "&sp=SBK");
        try {
            doc = handleLoginForm(doc, account);
        } catch (OpacErrorException e) {
            return new ProlongResult(Status.ERROR, e.getMessage());
        }
        for (Element tr : doc.select(".rTable_div tr")) {
            if (tr.select("a").size() == 1) {
                if (tr.select("a").first().absUrl("href")
                      .contains("sp=SZA")) {
                    alink = tr.select("a").first().absUrl("href");
                }
            }
        }
        if (alink == null) {
            return new ProlongResult(Status.ERROR);
        }

        doc = htmlGet(alink);

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
        for (Element tr : doc.select(".rTable_div tr")) {
            if (tr.select("input").attr("name").equals(media.split("\\|")[0])) {
                boolean disabled = tr.select("input").hasAttr("disabled");
                try {
                    disabled = (
                            disabled
                                    || tr.child(4).text().matches(".*nicht verl.+ngerbar.*")
                                    || tr.child(4).text().matches(".*Verl.+ngerung nicht m.+glich.*")
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
        form.add(new BasicNameValuePair("textButton$1",
                "Markierte Titel verlängern"));
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
        form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
        htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        return new ProlongResult(Status.OK);
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        String alink = null;
        Document doc;
        start();
        doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
                + s_service + "&sp=SBK");
        try {
            doc = handleLoginForm(doc, account);
        } catch (OpacErrorException e) {
            return new ProlongAllResult(Status.ERROR, e.getMessage());
        }
        for (Element tr : doc.select(".rTable_div tr")) {
            if (tr.select("a").size() == 1) {
                if (tr.select("a").first().absUrl("href")
                      .contains("sp=SZA")) {
                    alink = tr.select("a").first().absUrl("href");
                }
            }
        }
        if (alink == null) {
            return new ProlongAllResult(Status.ERROR);
        }

        doc = htmlGet(alink);

        List<NameValuePair> form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
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

        form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
        form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
        htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        return new ProlongAllResult(Status.OK, result);
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        String rlink = null;
        Document doc;
        rlink = media.split("\\|")[1].replace("requestCount=", "fooo=");
        start();
        doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
                + s_service + "&sp=SBK");
        try {
            doc = handleLoginForm(doc, account);
        } catch (OpacErrorException e) {
            return new CancelResult(Status.ERROR, e.getMessage());
        }
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
                if ((tr.text().contains("Reservationen") || tr.text().contains("Vormerkung") ||
                        tr.text().contains("Bestellung"))
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

        doc = htmlGet(rlink);

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
        form.add(new BasicNameValuePair(media.split("\\|")[0], "on"));
        form.add(new BasicNameValuePair("textButton$0",
                "Markierte Titel löschen"));
        doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        form = new ArrayList<>();
        for (Element input : doc.select("input, select")) {
            if (!"image".equals(input.attr("type"))
                    && !"submit".equals(input.attr("type"))
                    && !"checkbox".equals(input.attr("type"))
                    && !"".equals(input.attr("name"))) {
                form.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
        form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
        form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
        htmlPost(opac_url + ";jsessionid=" + s_sid, form);

        return new CancelResult(Status.OK);
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        start();

        Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
                + s_service + "&sp=SBK");
        doc = handleLoginForm(doc, account);

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

        // Ausleihen
        String alink = null;
        int anum = 0;
        List<LentItem> lent = new ArrayList<>();
        for (Element tr : doc.select(".rTable_div tr")) {
            if (tr.select("a").size() == 1) {
                if (tr.select("a").first().absUrl("href").contains("sp=SZA")) {
                    alink = tr.select("a").first().absUrl("href");
                    anum = Integer.parseInt(tr.child(0).text().trim());
                }
            }
        }
        if (alink != null) {
            Document adoc = htmlGet(alink);
            s_alink = alink;
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
            parseMediaList(adoc, alink, lent, split_title_author);
            assert (lent.size() == anum);
            form = new ArrayList<>();
            for (Element input : adoc.select("input, select")) {
                if (!"image".equals(input.attr("type"))
                        && !"submit".equals(input.attr("type"))
                        && !"checkbox".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))) {
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
            form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
            form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
            doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
        } else {
            assert (anum == 0);
        }

        adata.setLent(lent);

        List<String[]> rlinks = new ArrayList<>();
        int rnum = 0;
        List<ReservedItem> res = new ArrayList<>();
        for (Element tr : doc.select(".rTable_div tr")) {
            if (tr.select("a").size() == 1) {
                if ((tr.text().contains("Reservationen")
                        || tr.text().contains("Vormerkung")
                        || tr.text().contains("Fernleihbestellung")
                        || tr.text().contains("Bereitstellung")
                        || tr.text().contains("Bestellw")
                        || tr.text().contains("Magazin"))
                        && !tr.child(0).text().trim().equals("")) {
                    rlinks.add(new String[]{
                            tr.select("a").text(),
                            tr.select("a").first().absUrl("href"),
                    });
                    rnum += Integer.parseInt(tr.child(0).text().trim());
                }
            }
        }
        for (String[] rlink : rlinks) {
            Document rdoc = htmlGet(rlink[1]);
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
            if (error) {
                // Maybe we should send a bug report here, but using ACRA breaks
                // the unit tests
                adata.setWarning("Beim Abrufen der Reservationen ist ein Problem aufgetreten");
            }

            List<NameValuePair> form = new ArrayList<>();
            for (Element input : rdoc.select("input, select")) {
                if (!"image".equals(input.attr("type"))
                        && !"submit".equals(input.attr("type"))
                        && !"checkbox".equals(input.attr("type"))
                        && !"".equals(input.attr("name"))) {
                    form.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
            form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
            form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
            htmlPost(opac_url + ";jsessionid=" + s_sid, form);
        }

        assert (res.size() == rnum);

        adata.setReservations(res);

        return adata;
    }

    static void parseMediaList(Document adoc, String alink, List<LentItem> lent,
            boolean split_title_author) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        for (Element tr : adoc.select(".rTable_div tbody tr")) {
            LentItem item = new LentItem();
            String text = Jsoup.parse(tr.child(3).html().replaceAll("(?i)<br[^>]*>", "#"))
                               .text();
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
                            && !tr.child(4).text().matches(".*Verl.+ngerung nicht m.+glich.*")
                    );
                } catch (Exception e) {

                }
                item.setProlongData(
                        tr.select("input[type=checkbox]").attr("name") + "|" + alink);
            }

            lent.add(item);
        }
    }

    protected Document handleLoginForm(Document doc, Account account)
            throws IOException, OpacErrorException {

        if (doc.select("#LPASSW_1").size() == 0) {
            return doc;
        }

        doc.select("#LPASSW_1").val(account.getPassword());

        List<NameValuePair> form = new ArrayList<>();
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
        form.add(new BasicNameValuePair(inputSend.attr("name"), inputSend
                .attr("value")));

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
        if (!initialised) {
            start();
        }

        Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
                + s_service + "&sp=" + s_exts);

        List<SearchField> fields = new ArrayList<>();
        // dropdown to select which field you want to search in
        Elements searchoptions = doc.select("#SUCH01_1 option");
        if (searchoptions.size() == 0 && doc.select("input[fld=FELD01_1]").size() > 0) {
            // Hack is needed in Nuernberg
            searchoptions = doc.select("input[fld=FELD01_1]").first().previousElementSibling()
                               .select("option");
        }
        for (Element opt : searchoptions) {
            TextSearchField field = new TextSearchField();
            field.setId(opt.attr("value"));
            field.setDisplayName(opt.text());
            field.setHint("");
            fields.add(field);
        }

        // Save data so that the search() function knows that this
        // is not a selectable search field
        JSONObject selectableData = new JSONObject();
        selectableData.put("selectable", false);

        for (Element row : doc.select("div[id~=F\\d+]")) {
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

}
