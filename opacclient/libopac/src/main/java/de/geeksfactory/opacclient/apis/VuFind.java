package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class VuFind extends BaseApi {
    protected static HashMap<String, String> languageCodes = new HashMap<>();
    protected static HashMap<String, SearchResult.MediaType> mediaTypeSelectors = new HashMap<>();

    static {
        languageCodes.put("de", "de");
        languageCodes.put("en", "en");
        languageCodes.put("el", "el");
        languageCodes.put("es", "es");
        languageCodes.put("it", "it");
        languageCodes.put("fr", "fr");
        languageCodes.put("da", "da");

        mediaTypeSelectors
                .put(".cd, .audio, .musicrecording, .record, .soundrecordingmedium, " +
                                ".soundrecording",
                        SearchResult.MediaType.CD_MUSIC);
        mediaTypeSelectors.put(".audiotape, .cassette, .soundcassette",
                SearchResult.MediaType.AUDIO_CASSETTE);
        mediaTypeSelectors.put(".dvdaudio, .sounddisc", SearchResult.MediaType.CD_MUSIC);
        mediaTypeSelectors.put(".dvd, .dvdvideo", SearchResult.MediaType.DVD);
        mediaTypeSelectors.put(".blueraydisc, .bluraydisc", SearchResult.MediaType.BLURAY);
        mediaTypeSelectors.put(".ebook", SearchResult.MediaType.EBOOK);
        mediaTypeSelectors.put(".map, .globe, .atlas", SearchResult.MediaType.MAP);
        mediaTypeSelectors
                .put(".slide, .photo, .artprint, .collage, .drawing, .flashcard, .painting, " +
                                ".photonegative, .placard, .print, .sensorimage, .transparency",
                        SearchResult.MediaType.ART);
        mediaTypeSelectors.put(
                ".microfilm, .video, .videodisc, .vhs, .video, .videotape, .videocassette, " +
                        ".videocartridge, .audiovisualmedia, .filmstrip, .motionpicture, " +
                        ".videoreel",
                SearchResult.MediaType.MOVIE);
        mediaTypeSelectors.put(".kit, .sets", SearchResult.MediaType.PACKAGE);
        mediaTypeSelectors.put(".musicalscore, .notatedmusic, .electronicmusicalscore",
                SearchResult.MediaType.SCORE_MUSIC);
        mediaTypeSelectors.put(".manuscript, .book, .articles", SearchResult.MediaType.BOOK);
        mediaTypeSelectors
                .put(".journal, .journalnewspaper, .serial", SearchResult.MediaType.MAGAZINE);
        mediaTypeSelectors.put(".newspaper, .newspaperarticle", SearchResult.MediaType.NEWSPAPER);
        mediaTypeSelectors.put(".software, .cdrom, .chipcartridge, .disccartridge, .dvdrom",
                SearchResult.MediaType.CD_SOFTWARE);
        mediaTypeSelectors.put(".newspaper", SearchResult.MediaType.NEWSPAPER);
        mediaTypeSelectors
                .put(".electronicnewspaper, .electronic, .electronicarticle, " +
                                "electronicresourcedatacarrier, .electronicresourceremoteaccess, " +
                                ".electronicserial, .electronicjournal, .electronicthesis",
                        SearchResult.MediaType.EDOC);
        mediaTypeSelectors.put(".newspaper", SearchResult.MediaType.NEWSPAPER);
        mediaTypeSelectors.put(".newspaper", SearchResult.MediaType.NEWSPAPER);
        mediaTypeSelectors.put(".unknown", SearchResult.MediaType.UNKNOWN);

    }

    protected String languageCode = "en";
    protected String opac_url = "";
    protected JSONObject data;
    protected List<SearchQuery> last_query;

    @Override
    public void init(Library lib) {
        super.init(lib);

        this.library = lib;
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<NameValuePair> buildSearchParams(List<SearchQuery> query) {
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("sort", "relevance"));
        params.add(new BasicNameValuePair("join", "AND"));

        for (SearchQuery singleQuery : query) {
            if (singleQuery.getValue().equals("")) continue;
            if (singleQuery.getKey().contains("filter[]")) {
                params.add(new BasicNameValuePair("filter[]", singleQuery.getValue()));
            } else {
                params.add(new BasicNameValuePair("type0[]", singleQuery.getKey()));
                params.add(new BasicNameValuePair("bool0[]", "AND"));
                params.add(new BasicNameValuePair("lookfor0[]", singleQuery.getValue()));
            }
        }
        return params;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        if (!initialised) start();
        last_query = query;
        String html = httpGet(opac_url + "/Search/Results" +
                        buildHttpGetParams(buildSearchParams(query)),
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        return parse_search(doc, 1);
    }

    protected SearchRequestResult parse_search(Document doc, int page) throws OpacErrorException {
        doc.setBaseUri(opac_url + "/Search/Results");

        if (doc.select("p.error, p.errorMsg, .alert-error").size() > 0) {
            throw new OpacErrorException(doc.select("p.error, p.errorMsg, .alert-error").text());
        } else if (doc.select("div.result").size() == 0 && doc.select(".main p").size() > 0) {
            throw new OpacErrorException(doc.select(".main p").first().text());
        }

        int rescount = -1;
        if (doc.select(".resulthead").size() == 1) {
            rescount = Integer.parseInt(
                    doc.select(".resulthead strong").get(2).text());
        }
        List<SearchResult> reslist = new ArrayList<>();

        for (Element row : doc.select("div.result")) {
            SearchResult res = new SearchResult();
            Element z3988el = null;
            if (row.select("span.Z3988").size() == 1) {
                z3988el = row.select("span.3988").first();
            } else if (row.parent().tagName().equals("li") &&
                    row.parent().select("span.Z3988").size() > 0) {
                z3988el = row.parent().select("span.3988").first();
            }
            if (z3988el != null) {
                List<NameValuePair> z3988data;
                try {
                    StringBuilder description = new StringBuilder();
                    z3988data = URLEncodedUtils.parse(new URI("http://dummy/?"
                            + z3988el.select("span.Z3988").attr("title")), "UTF-8");
                    for (NameValuePair nv : z3988data) {
                        if (nv.getValue() != null) {
                            if (!nv.getValue().trim().equals("")) {
                                if (nv.getName().equals("rft.btitle")) {
                                    description.append("<b>").append(nv.getValue()).append("</b>");
                                } else if (nv.getName().equals("rft.atitle")) {
                                    description.append("<b>").append(nv.getValue()).append("</b>");
                                } else if (nv.getName().equals("rft.au")) {
                                    description.append("<br />").append(nv.getValue());
                                } else if (nv.getName().equals("rft.date")) {
                                    description.append("<br />").append(nv.getValue());
                                }
                            }
                        }
                    }
                    res.setInnerhtml(description.toString());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                res.setInnerhtml(row.select("a.title").text());
            }

            if (row.hasClass("available") || row.hasClass("internet")) {
                res.setStatus(SearchResult.Status.GREEN);
            } else if (row.hasClass("reservable")) {
                res.setStatus(SearchResult.Status.YELLOW);
            } else if (row.hasClass("not-available")) {
                res.setStatus(SearchResult.Status.RED);
            } else if (row.select(".status.available").size() > 0) {
                res.setStatus(SearchResult.Status.GREEN);
            } else if (row.select(".status .label-success").size() > 0) {
                res.setStatus(SearchResult.Status.GREEN);
            } else if (row.select(".status .label-important").size() > 0) {
                res.setStatus(SearchResult.Status.RED);
            } else if (row.select(".status.checkedout").size() > 0) {
                res.setStatus(SearchResult.Status.RED);
            }

            for (Map.Entry<String, SearchResult.MediaType> entry : mediaTypeSelectors.entrySet()) {
                if (row.select(entry.getKey()).size() > 0) {
                    res.setType(entry.getValue());
                    break;
                }
            }

            for (Element img : row.select("img")) {
                String src = img.absUrl("src");
                if (src.contains("over")) {
                    if (!src.contains("Unavailable")) {
                        res.setCover(src);
                    }
                    break;
                }
            }

            res.setPage(page);
            String href = row.select("a.title").first().absUrl("href");
            try {
                URL idurl = new URL(href);
                res.setId(idurl.getPath().replace("/Record/", ""));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            reslist.add(res);
        }

        return new SearchRequestResult(reslist, rescount, page);
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        List<NameValuePair> params = buildSearchParams(last_query);
        params.add(new BasicNameValuePair("page", String.valueOf(page)));
        String html = httpGet(opac_url + "/Search/Results" +
                        buildHttpGetParams(params),
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        return parse_search(doc, page);
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        if (!initialised) start();
        String url = opac_url + "/Record/" + id;
        String html = httpGet(url, getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(url);
        return parse_detail(id, doc);
    }

    protected DetailledItem parse_detail(String id, Document doc) throws OpacErrorException {
        if (doc.select("p.error, p.errorMsg, .alert-error").size() > 0) {
            throw new OpacErrorException(doc.select("p.error, p.errorMsg, .alert-error").text());
        }

        DetailledItem res = new DetailledItem();
        res.setId(id);

        Elements title = doc.select(".record h1, .record [itemprop=name], .record [property=name]");
        if (title.size() > 0) {
            res.setTitle(title.text());
        }
        for (Element img : doc.select(".record img, #cover img")) {
            String src = img.absUrl("src");
            if (src.contains("over")) {
                if (!src.contains("Unavailable")) {
                    res.setCover(src);
                }
                break;
            }
        }
        for (Element tr : doc.select(".record table").first().select("tr")) {
            String text = tr.child(1).text();
            if (tr.child(1).select("a").size() > 0) {
                String href = tr.child(1).select("a").attr("href");
                if (!href.startsWith("/") || !text.contains(opac_url)) {
                    text += " " + href;
                }
            }
            res.addDetail(new Detail(tr.child(0).text(), text));
        }

        try {
            parse_copies(res, doc);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return res;
    }

    protected void parse_copies(DetailledItem res, Document doc) throws JSONException {
        if ("doublestacked".equals(data.optString("copystyle"))) {
            // e.g. http://vopac.nlg.gr/Record/393668/Holdings#tabnav
            // for Athens_GreekNationalLibrary
            Element container = doc.select(".tab-container").first();
            String branch = "";
            for (Element child : container.children()) {
                if (child.tagName().equals("h5")) {
                    branch = child.text();
                } else if (child.tagName().equals("table")) {
                    int i = 0;
                    String callNumber = "";
                    for (Element row : child.select("tr")) {
                        if (i == 0) {
                            callNumber = row.child(1).text();
                        } else {
                            Map<String, String> copy = new HashMap<>();
                            copy.put(DetailledItem.KEY_COPY_BRANCH, branch);
                            copy.put(DetailledItem.KEY_COPY_SHELFMARK, callNumber);
                            copy.put(DetailledItem.KEY_COPY_BARCODE, row.child(0).text());
                            copy.put(DetailledItem.KEY_COPY_STATUS, row.child(1).text());
                            res.addCopy(copy);
                        }
                        i++;
                    }
                }
            }

        } else if ("stackedtable".equals(data.optString("copystyle"))) {
            // e.g. http://search.lib.auth.gr/Record/376356
            // or https://katalog.ub.uni-leipzig.de/Record/0000196115
            Element container = doc.select(".recordsubcontent").first();
            String branch = "";
            JSONObject copytable = data.getJSONObject("copytable");
            for (Element child : container.children()) {
                if (child.tagName().equals("div")) {
                    child = child.child(0);
                }
                if (child.tagName().equals("h3")) {
                    branch = child.text();
                } else if (child.tagName().equals("table")) {
                    if (child.select("caption").size() > 0) {
                        // Leipzig_Uni
                        branch = child.select("caption").first().ownText();
                    }
                    int i = 0;
                    String callNumber = null;
                    if ("headrow".equals(copytable.optString("signature"))) {
                        callNumber = child.select("tr").get(0).child(1).text();
                    }
                    for (Element row : child.select("tr")) {
                        if (i < copytable.optInt("_offset", 0)) {
                            i++;
                            continue;
                        }
                        Map<String, String> copy = new HashMap<>();
                        if (callNumber != null) {
                            copy.put(DetailledItem.KEY_COPY_SHELFMARK, callNumber);
                        }
                        copy.put(DetailledItem.KEY_COPY_BRANCH, branch);
                        Iterator<?> keys = copytable.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            if (key.startsWith("_")) continue;
                            if (copytable.optString(key, "").contains("/")) {
                                // Leipzig_Uni
                                String[] splitted = copytable.getString(key).split("/");
                                int col = Integer.parseInt(splitted[0]);
                                int line = Integer.parseInt(splitted[1]);
                                int j = 0;
                                for (Node node : row.child(col).childNodes()) {
                                    if (node instanceof Element) {
                                        if (((Element) node).tagName().equals("br")) {
                                            j++;
                                        } else if (j == line) {
                                            copy.put(key, ((Element) node).text());
                                        }
                                    } else if (node instanceof TextNode && j == line &&
                                            !((TextNode) node).text().trim().equals("")) {
                                        copy.put(key, ((TextNode) node).text());
                                    }
                                }
                            } else {
                                // Thessaloniki_University
                                if (copytable.optInt(key, -1) == -1) continue;
                                String value = row.child(copytable.getInt(key)).text();
                                copy.put(key, value);
                            }
                        }
                        res.addCopy(copy);
                        i++;
                    }
                }
            }
        }
    }

    @Override
    public DetailledItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    public void start() throws IOException {
        super.start();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("mylang", languageCode));
        httpPost(opac_url + "/Search/Advanced", new UrlEncodedFormEntity(params),
                getDefaultEncoding());
    }

    @Override
    public List<SearchField> getSearchFields()
            throws IOException, OpacErrorException, JSONException {
        start();
        String html = httpGet(opac_url + "/Search/Advanced?mylang = " + languageCode,
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        List<SearchField> fields = new ArrayList<>();

        Elements options = doc.select("select#search_type0_0 option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.val());
            field.setHint("");
            field.setData(new JSONObject());
            field.getData().put("meaning", option.val());
            fields.add(field);
        }
        if (fields.size() == 0) {
            // Weird JavaScript, e.g. view-source:http://vopac.nlg.gr/Search/Advanced
            Pattern pattern_key = Pattern
                    .compile("searchFields\\[\"([^\"]+)\"\\] = \"([^\"]+)\";");
            for (Element script : doc.select("script")) {
                if (!script.html().contains("searchFields")) continue;
                for (String line : script.html().split("\n")) {
                    Matcher matcher = pattern_key.matcher(line);
                    if (matcher.find()) {
                        TextSearchField field = new TextSearchField();
                        field.setDisplayName(matcher.group(2));
                        field.setId(matcher.group(1));
                        field.setHint("");
                        field.setData(new JSONObject());
                        field.getData().put("meaning", field.getId());
                        fields.add(field);
                    }
                }
            }
        }

        Elements selects = doc.select("select");
        for (Element select : selects) {
            if (!select.attr("name").equals("filter[]")) continue;
            DropdownSearchField field = new DropdownSearchField();
            if (select.parent().select("label").size() > 0) {
                field.setDisplayName(select.parent().select("label").first()
                                           .text());
            }
            field.setId(select.attr("name") + select.attr("id"));
            List<Map<String, String>> dropdownOptions = new ArrayList<>();
            String meaning = select.attr("id");
            field.addDropdownValue("", "");
            for (Element option : select.select("option")) {
                if (option.val().contains(":")) {
                    meaning = option.val().split(":")[0];
                }
                field.addDropdownValue(option.val(), option.text());
            }
            field.setData(new JSONObject());
            field.getData().put("meaning", meaning);
            fields.add(field);
        }

        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return opac_url + "/Record/" + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        Set<String> langs = new HashSet<>();
        String html = httpGet(opac_url + "/Search/Advanced",
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        if (doc.select("select[name=mylang]").size() > 0) {
            for (Element opt : doc.select("select[name=mylang] option")) {
                if (languageCodes.containsValue(opt.val())) {
                    for (Map.Entry<String, String> lc : languageCodes.entrySet()) {
                        if (lc.getValue().equals(opt.val())) {
                            langs.add(lc.getKey());
                            break;
                        }
                    }
                } else {
                    langs.add(opt.val());
                }
            }
        }
        return langs;
    }

    protected String getDefaultEncoding() {
        return "UTF-8";
    }

    @Override
    public void setLanguage(String language) {
        languageCode = languageCodes.containsKey(language) ? languageCodes.get(language) : language;
    }

    @Override
    public boolean isAccountSupported(Library library) {
        return false;
    }

    @Override
    public ReservationResult reservation(DetailledItem item, Account account,
            int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        return null;
    }

    @Override
    public void checkAccountData(Account account)
            throws IOException, JSONException, OpacErrorException {
    }
}
