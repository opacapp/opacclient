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

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpUtils;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.utils.JsonKeyIterator;

/**
 * OpacApi implementation for Bibliotheca Web Opacs, originally developed by BOND, now owned by
 * OCLC. Known to work well with Web Opac versions from 2.6, maybe older, to 2.8
 */
public class Bibliotheca extends BaseApi {

    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();

    static {
        defaulttypes.put("mbuchs", MediaType.BOOK);
        defaulttypes.put("cdkl", MediaType.CD);
        defaulttypes.put("cd", MediaType.CD);
        defaulttypes.put("cdromkl", MediaType.CD_SOFTWARE);
        defaulttypes.put("mcdroms", MediaType.CD);
        defaulttypes.put("ekl", MediaType.EBOOK);
        defaulttypes.put("emedium", MediaType.EBOOK);
        defaulttypes.put("monleihe", MediaType.EBOOK);
        defaulttypes.put("mdivis", MediaType.EBOOK);
        defaulttypes.put("mbmonos", MediaType.PACKAGE_BOOKS);
        defaulttypes.put("mbuechers", MediaType.PACKAGE_BOOKS);
        defaulttypes.put("mdvds", MediaType.DVD);
        defaulttypes.put("mdvd", MediaType.DVD);
        defaulttypes.put("blu-ray--disc_s_35x35", MediaType.BLURAY);
        defaulttypes.put("mblurays", MediaType.BLURAY);
        defaulttypes.put("mfilms", MediaType.MOVIE);
        defaulttypes.put("mvideos", MediaType.MOVIE);
        defaulttypes.put("mhoerbuchs", MediaType.AUDIOBOOK);
        defaulttypes.put("mmusikcds", MediaType.CD_MUSIC);
        defaulttypes.put("mcdns", MediaType.CD_MUSIC);
        defaulttypes.put("mnoten1s", MediaType.SCORE_MUSIC);
        defaulttypes.put("munselbs", MediaType.UNKNOWN);
        defaulttypes.put("mztgs", MediaType.NEWSPAPER);
        defaulttypes.put("zeitung", MediaType.NEWSPAPER);
        defaulttypes.put("spielekl", MediaType.BOARDGAME);
        defaulttypes.put("mspiels", MediaType.BOARDGAME);
        defaulttypes.put("tafelkl", MediaType.SCHOOL_VERSION);
        defaulttypes.put("spiel_konsol", MediaType.GAME_CONSOLE);
        defaulttypes.put("wii", MediaType.GAME_CONSOLE);
    }

    protected final long SESSION_LIFETIME = 1000 * 60 * 3;
    protected String opac_url = "";
    protected JSONObject data;
    protected Library library;
    protected long logged_in;
    protected Account logged_in_as;
    protected String _res_target = "vorbesttranskonto";
    protected String branch_inputfield = "zstauswahl";

    @Override
    public List<SearchField> getSearchFields() throws IOException,
            JSONException {
        if (!initialised) {
            start();
        }

        List<SearchField> fields = new ArrayList<>();
        // Read branches and media types
        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("link_profis.x", "0"));
        nameValuePairs.add(new BasicNameValuePair("link_profis.y", "1"));
        String html = httpPost(opac_url + "/index.asp",
                new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        Elements fieldElems = doc.select(".suchfeldinhalt");
        for (Element fieldElem : fieldElems) {
            String name = fieldElem.select(".suchfeld_inhalt_titel label")
                                   .text();
            String hint = "";
            if (fieldElem.select(".suchfeld_inhalt_input").size() > 0) {
                List<TextNode> textNodes = fieldElem
                        .select(".suchfeld_inhalt_input").first().textNodes();
                if (textNodes.size() > 0) {
                    for (TextNode node : textNodes) {
                        String text = node.getWholeText().replace("\n", "");
                        if (!text.equals("")) {
                            hint = node.getWholeText().replace("\n", "");
                            break;
                        }
                    }
                }
            }

            Elements inputs = fieldElem
                    .select(".suchfeld_inhalt_input input[type=text], " +
                            ".suchfeld_inhalt_input select");
            if (inputs.size() == 1) {
                fields.add(createSearchField(name, hint, inputs.get(0)));
            } else if (inputs.size() == 2
                    && inputs.select("input[type=text]").size() == 2) {
                // Two text fields, e.g. year from/to or two keywords
                fields.add(createSearchField(name, hint, inputs.get(0)));
                TextSearchField secondField = (TextSearchField) createSearchField(
                        name, hint, inputs.get(1));
                secondField.setHalfWidth(true);
                fields.add(secondField);
            } else if (inputs.size() == 2
                    && inputs.get(0).tagName().equals("select")
                    && inputs.get(1).tagName().equals("input")
                    && inputs.get(0).attr("name").equals("feld1")) {
                // A dropdown to select from different search field types.
                // Break it down into single text fields.
                for (Element option : inputs.get(0).select("option")) {
                    TextSearchField field = new TextSearchField();
                    field.setHint(hint);
                    field.setDisplayName(option.text());
                    field.setId(inputs.get(1).attr("name") + "$"
                            + option.attr("value"));

                    JSONObject data = new JSONObject();
                    JSONObject params = new JSONObject();
                    params.put(inputs.get(0).attr("name"), option.attr("value"));
                    data.put("additional_params", params);
                    field.setData(data);

                    fields.add(field);
                }
            }
        }

        List<Map<String, String>> dropdownValues = new ArrayList<>();
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("key", "1");
        valueMap.put("value",
                stringProvider.getString(StringProvider.ORDER_DEFAULT));
        dropdownValues.add(valueMap);
        valueMap = new HashMap<>();
        valueMap.put("key", "2:desc");
        valueMap.put("value",
                stringProvider.getString(StringProvider.ORDER_YEAR_DESC));
        dropdownValues.add(valueMap);
        valueMap = new HashMap<>();
        valueMap.put("key", "2:asc");
        valueMap.put("value",
                stringProvider.getString(StringProvider.ORDER_YEAR_ASC));
        dropdownValues.add(valueMap);
        valueMap = new HashMap<>();
        valueMap.put("key", "3:desc");
        valueMap.put("value",
                stringProvider.getString(StringProvider.ORDER_CATEGORY_DESC));
        dropdownValues.add(valueMap);
        valueMap = new HashMap<>();
        valueMap.put("key", "3:asc");
        valueMap.put("value",
                stringProvider.getString(StringProvider.ORDER_CATEGORY_ASC));
        dropdownValues.add(valueMap);
        DropdownSearchField orderField = new DropdownSearchField("orderselect",
                stringProvider.getString(StringProvider.ORDER), false,
                dropdownValues);
        orderField.setMeaning(Meaning.ORDER);
        fields.add(orderField);

        return fields;
    }

    private SearchField createSearchField(String name, String hint,
            Element input) {
        if (input.tagName().equals("input")
                && input.attr("type").equals("text")) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(name);
            field.setHint(hint);
            field.setId(input.attr("name"));
            return field;
        } else if (input.tagName().equals("select")) {
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName(name);
            field.setId(input.attr("name"));
            List<Map<String, String>> options = new ArrayList<>();
            for (Element option : input.select("option")) {
                Map<String, String> map = new HashMap<>();
                map.put("key", option.attr("value"));
                map.put("value", option.text());
                options.add(map);
            }
            field.setDropdownValues(options);
            return field;
        } else {
            return null;
        }
    }

    @Override
    public void start() throws
            IOException {
        String db = "";
        if (data.has("db")) {
            try {
                db = "&db=" + data.getString("db");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        httpGet(opac_url + "/woload.asp?lkz=1&nextpage=" + db,
                getDefaultEncoding());
        super.start();
    }

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

    @Override
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, JSONException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("stichtit", "stich"));

        int ifeldCount = 0;
        for (SearchQuery query : queries) {
            if (query.getValue().equals("")) {
                continue;
            }
            String key = query.getKey();
            if (key.contains("$")) {
                key = key.substring(0, key.indexOf("$"));
            }
            if (key.contains("ifeld")) {
                ifeldCount++;
                if (ifeldCount > 1) {
                    throw new OpacErrorException(
                            stringProvider
                                    .getString(StringProvider.COMBINATION_NOT_SUPPORTED));
                }
            }
            if (key.equals("orderselect") && query.getValue().contains(":")) {
                nameValuePairs.add(new BasicNameValuePair("orderselect", query
                        .getValue().split(":")[0]));
                nameValuePairs.add(new BasicNameValuePair("order", query
                        .getValue().split(":")[1]));
            } else {
                nameValuePairs
                        .add(new BasicNameValuePair(key, query.getValue()));
            }
            if (query.getSearchField().getData() != null) {
                JSONObject data = query.getSearchField().getData();
                if (data.has("additional_params")) {
                    JSONObject params = data.getJSONObject("additional_params");
                    Iterator<String> keys = new JsonKeyIterator(params);
                    while (keys.hasNext()) {
                        String additionalKey = keys.next();
                        nameValuePairs
                                .add(new BasicNameValuePair(additionalKey,
                                        params.getString(additionalKey)));
                    }
                }
            }
        }

        nameValuePairs.add(new BasicNameValuePair("suche_starten.x", "1"));
        nameValuePairs.add(new BasicNameValuePair("suche_starten.y", "1"));

        if (data.has("db")) {
            try {
                nameValuePairs.add(new BasicNameValuePair("dbase", data
                        .getString("db")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String html = httpPost(opac_url + "/index.asp",
                new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());
        return parse_search(html, 1);
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException {
        if (!initialised) {
            start();
        }

        String html = httpGet(opac_url + "/index.asp?scrollAction=" + page,
                getDefaultEncoding());
        return parse_search(html, page);
    }

    protected SearchRequestResult parse_search(String html, int page) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);
        Elements table = doc
                .select(".resulttab tr.result_trefferX, .resulttab tr.result_treffer");
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            Element tr = table.get(i);
            SearchResult sr = new SearchResult();
            int contentindex = 1;
            if (tr.select("td a img").size() > 0) {
                String[] fparts = tr.select("td a img").get(0).attr("src")
                                    .split("/");
                String fname = fparts[fparts.length - 1];
                if (data.has("mediatypes")) {
                    try {
                        sr.setType(MediaType.valueOf(data.getJSONObject(
                                "mediatypes").getString(fname)));
                    } catch (JSONException | IllegalArgumentException e) {
                        sr.setType(defaulttypes.get(fname
                                .toLowerCase(Locale.GERMAN).replace(".jpg", "")
                                .replace(".gif", "").replace(".png", "")));
                    }
                } else {
                    sr.setType(defaulttypes.get(fname
                            .toLowerCase(Locale.GERMAN).replace(".jpg", "")
                            .replace(".gif", "").replace(".png", "")));
                }
            } else {
                if (tr.children().size() == 3) {
                    contentindex = 2;
                }
            }
            sr.setInnerhtml(tr.child(contentindex).child(0).html());

            sr.setNr(i);
            Element link = tr.child(contentindex).select("a").first();
            try {
                if (link != null && link.attr("href").contains("detmediennr")) {
                    Map<String, String> params = getQueryParamsFirst(link
                            .attr("abs:href"));
                    String nr = params.get("detmediennr");
                    if (Integer.parseInt(nr) > i + 1) {
                        // Seems to be an ID…
                        if (params.get("detDB") != null) {
                            sr.setId("&detmediennr=" + nr + "&detDB="
                                    + params.get("detDB"));
                        } else {
                            sr.setId("&detmediennr=" + nr);
                        }
                    }
                }
            } catch (Exception e) {
            }
            try {
                if (tr.child(1).childNode(0) instanceof Comment) {
                    Comment c = (Comment) tr.child(1).childNode(0);
                    String comment = c.getData().trim();
                    String id = comment.split(": ")[1];
                    sr.setId(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            results.add(sr);
        }
        int results_total = -1;
        if (doc.select(".result_gefunden").size() > 0) {
            try {
                results_total = Integer.parseInt(doc.select(".result_gefunden")
                                                    .text().trim()
                                                    .replaceAll(".*[^0-9]+([0-9]+).*", "$1"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                results_total = -1;
            }
        }
        return new SearchRequestResult(results, results_total, page);
    }

    @Override
    public DetailledItem getResultById(String a, String homebranch)
            throws IOException {
        if (!initialised) {
            start();
        }
        String html = httpGet(opac_url + "/index.asp?MedienNr=" + a,
                getDefaultEncoding());
        DetailledItem result = parse_result(html);
        if (result.getId() == null) {
            result.setId(a);
        }
        return result;
    }

    @Override
    public DetailledItem getResult(int nr) throws IOException {
        String html = httpGet(opac_url + "/index.asp?detmediennr=" + nr,
                getDefaultEncoding());

        return parse_result(html);
    }

    protected DetailledItem parse_result(String html) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);

        DetailledItem result = new DetailledItem();

        if (doc.select(".detail_cover img").size() == 1) {
            result.setCover(doc.select(".detail_cover img").get(0).attr("src"));
        }

        result.setTitle(doc.select(".detail_titel").text());

        Elements detailtrs = doc.select(".detailzeile table tr");
        for (int i = 0; i < detailtrs.size(); i++) {
            Element tr = detailtrs.get(i);
            if (tr.child(0).hasClass("detail_feld")) {
                String title = tr.child(0).text();
                String content = tr.child(1).text();
                if (title.equals("Gesamtwerk:") || title.equals("Erschienen in:")) {
                    try {
                        if (tr.child(1).select("a").size() > 0) {
                            Element link = tr.child(1).select("a").first();
                            List<NameValuePair> query = URLEncodedUtils.parse(
                                    new URI(link.absUrl("href")), "UTF-8");
                            for (NameValuePair q : query) {
                                if (q.getName().equals("MedienNr")) {
                                    result.setCollectionId(q.getValue());
                                }
                            }
                        }
                    } catch (URISyntaxException e) {
                    }
                } else {

                    if (content.contains("hier klicken")
                            && tr.child(1).select("a").size() > 0) {
                        content += " "
                                + tr.child(1).select("a").first().attr("href");
                    }

                    result.addDetail(new Detail(title, content));
                }
            }
        }

        Elements detailcenterlinks = doc
                .select(".detailzeile_center a.detail_link");
        for (int i = 0; i < detailcenterlinks.size(); i++) {
            Element a = detailcenterlinks.get(i);
            result.addDetail(new Detail(a.text().trim(), a.absUrl("href")));
        }

        try {
            JSONObject copymap = new JSONObject();
            if (data.has("copiestable")) {
                copymap = data.getJSONObject("copiestable");
            } else {
                Elements ths = doc.select(".exemplartab .exemplarmenubar th");
                for (int i = 0; i < ths.size(); i++) {
                    Element th = ths.get(i);
                    String head = th.text().trim();
                    if (head.equals("Zweigstelle")) {
                        copymap.put(DetailledItem.KEY_COPY_BRANCH, i);
                    } else if (head.equals("Abteilung")) {
                        copymap.put(DetailledItem.KEY_COPY_DEPARTMENT, i);
                    } else if (head.equals("Bereich")) {
                        copymap.put(DetailledItem.KEY_COPY_LOCATION, i);
                    } else if (head.equals("Standort")) {
                        copymap.put(DetailledItem.KEY_COPY_LOCATION, i);
                    } else if (head.equals("Signatur")) {
                        copymap.put(DetailledItem.KEY_COPY_SHELFMARK, i);
                    } else if (head.equals("Barcode")
                            || head.equals("Medien-Nummer")) {
                        copymap.put(DetailledItem.KEY_COPY_BARCODE, i);
                    } else if (head.equals("Status")) {
                        copymap.put(DetailledItem.KEY_COPY_STATUS, i);
                    } else if (head.equals("Frist")
                            || head.matches("Verf.+gbar")) {
                        copymap.put(DetailledItem.KEY_COPY_RETURN, i);
                    } else if (head.equals("Vorbestellungen")
                            || head.equals("Reservierungen")) {
                        copymap.put(DetailledItem.KEY_COPY_RESERVATIONS, i);
                    }
                }
            }
            Elements exemplartrs = doc
                    .select(".exemplartab .tabExemplar, .exemplartab .tabExemplar_");
            for (int i = 0; i < exemplartrs.size(); i++) {
                Element tr = exemplartrs.get(i);

                Map<String, String> e = new HashMap<>();

                Iterator<?> keys = copymap.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    int index;
                    try {
                        index = copymap.has(key) ? copymap.getInt(key) : -1;
                    } catch (JSONException e1) {
                        index = -1;
                    }
                    if (index >= 0) {
                        e.put(key, tr.child(index).text());
                    }
                }

                result.addCopy(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Elements bandtrs = doc.select("table .tabBand a");
            for (int i = 0; i < bandtrs.size(); i++) {
                Element tr = bandtrs.get(i);

                Map<String, String> e = new HashMap<>();
                e.put(DetailledItem.KEY_CHILD_ID, tr.attr("href").split("=")[1]);
                e.put(DetailledItem.KEY_CHILD_TITLE, tr.text());
                result.addVolume(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (doc.select(".detail_vorbest a").size() == 1) {
            result.setReservable(true);
            result.setReservation_info(doc.select(".detail_vorbest a").attr(
                    "href"));
        }
        return result;
    }

    @Override
    public ReservationResult reservation(DetailledItem item, Account acc,
            int useraction, String selection) throws IOException {
        String reservation_info = item.getReservation_info();

        Document doc = null;

        if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("make_allvl",
                    "Bestaetigung"));
            nameValuePairs.add(new BasicNameValuePair("target", "makevorbest"));
            httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
                    nameValuePairs), getDefaultEncoding());
            return new ReservationResult(MultiStepResult.Status.OK);
        } else if (selection == null || useraction == 0) {
            String html = httpGet(opac_url + "/" + reservation_info,
                    getDefaultEncoding());
            doc = Jsoup.parse(html);

            if (doc.select("input[name=AUSWEIS]").size() > 0) {
                // Needs login
                List<NameValuePair> nameValuePairs = new ArrayList<>(
                        2);
                nameValuePairs.add(new BasicNameValuePair("AUSWEIS", acc
                        .getName()));
                nameValuePairs.add(new BasicNameValuePair("PWD", acc
                        .getPassword()));
                if (data.has("db")) {
                    try {
                        nameValuePairs.add(new BasicNameValuePair("vkontodb",
                                data.getString("db")));
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
                nameValuePairs.add(new BasicNameValuePair("target", doc.select(
                        "input[name=target]").val()));
                nameValuePairs.add(new BasicNameValuePair("type", "VT2"));
                html = httpPost(opac_url + "/index.asp",
                        new UrlEncodedFormEntity(nameValuePairs),
                        getDefaultEncoding());
                doc = Jsoup.parse(html);
            }
            if (doc.select("select[name=" + branch_inputfield + "]").size() == 0) {
                if (doc.select("select[name=VZST]").size() > 0) {
                    branch_inputfield = "VZST";
                }
            }
            if (doc.select("select[name=" + branch_inputfield + "]").size() > 0) {
                List<Map<String, String>> branches = new ArrayList<>();
                for (Element option : doc
                        .select("select[name=" + branch_inputfield + "]")
                        .first().children()) {
                    String value = option.text().trim();
                    String key;
                    if (option.hasAttr("value")) {
                        key = option.attr("value");
                    } else {
                        key = value;
                    }
                    Map<String, String> selopt = new HashMap<>();
                    selopt.put("key", key);
                    selopt.put("value", value);
                    branches.add(selopt);
                }
                _res_target = doc.select("input[name=target]").attr("value");
                ReservationResult result = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED);
                result.setActionIdentifier(ReservationResult.ACTION_BRANCH);
                result.setSelection(branches);
                return result;
            }
        } else if (useraction == ReservationResult.ACTION_BRANCH) {
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair(branch_inputfield,
                    selection));
            nameValuePairs.add(new BasicNameValuePair("button2", "weiter"));
            nameValuePairs.add(new BasicNameValuePair("target", _res_target));
            String html = httpPost(opac_url + "/index.asp",
                    new UrlEncodedFormEntity(nameValuePairs),
                    getDefaultEncoding());
            doc = Jsoup.parse(html);
        }

        if (doc == null) {
            return new ReservationResult(MultiStepResult.Status.ERROR);
        }

        if (doc.select("input[name=target]").size() > 0) {
            if (doc.select("input[name=target]").attr("value")
                   .equals("makevorbest")) {
                List<String[]> details = new ArrayList<>();

                if (doc.getElementsByClass("kontomeldung").size() == 1) {
                    details.add(new String[]{doc
                            .getElementsByClass("kontomeldung").get(0).text()
                            .trim()});
                }
                Pattern p = Pattern.compile("geb.hr", Pattern.MULTILINE);
                for (Element div : doc.select(".kontozeile_center")) {
                    for (String text : Jsoup
                            .parse(div.html().replaceAll("(?i)<br[^>]*>",
                                    "br2n")).text().split("br2n")) {
                        if (p.matcher(text).find()
                                && !text.contains("usstehend")
                                && text.contains("orbestellung")) {
                            details.add(new String[]{text.trim()});
                        }
                    }
                }

                for (Element row : doc.select(".kontozeile_center table tr")) {
                    if (row.select(".konto_feld").size() == 1
                            && row.select(".konto_feldinhalt").size() == 1) {
                        details.add(new String[]{
                                row.select(".konto_feld").text().trim(),
                                row.select(".konto_feldinhalt").text().trim()});
                    }
                }
                ReservationResult result = new ReservationResult(
                        MultiStepResult.Status.CONFIRMATION_NEEDED);
                result.setDetails(details);
                return result;
            }
        }

        if (doc.getElementsByClass("kontomeldung").size() == 1) {
            return new ReservationResult(MultiStepResult.Status.ERROR, doc
                    .getElementsByClass("kontomeldung").get(0).text());
        }

        return new ReservationResult(MultiStepResult.Status.ERROR,
                stringProvider.getString(StringProvider.UNKNOWN_ERROR));
    }

    @Override
    public ProlongResult prolong(String a, Account account, int useraction,
            String selection) throws IOException {
        if (!initialised) {
            start();
        }
        if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
                || logged_in_as == null) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ProlongResult(
                        MultiStepResult.Status.ERROR,
                        stringProvider
                                .getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        } else if (logged_in_as.getId() != account.getId()) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ProlongResult(
                        MultiStepResult.Status.ERROR,
                        stringProvider
                                .getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        }

        if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("target", "make_vl"));
            nameValuePairs.add(new BasicNameValuePair("verlaengern",
                    "Bestätigung"));
            httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
                    nameValuePairs), getDefaultEncoding());

            return new ProlongResult(MultiStepResult.Status.OK);
        } else {

            String html = httpGet(opac_url + "/" + a, getDefaultEncoding());
            Document doc = Jsoup.parse(html);

            if (doc.getElementsByClass("kontomeldung").size() == 1) {
                return new ProlongResult(MultiStepResult.Status.ERROR, doc
                        .getElementsByClass("kontomeldung").get(0).text());
            }
            if (doc.select("#verlaengern").size() == 1) {
                if (doc.select(".kontozeile_center table").size() == 1) {
                    Element table = doc.select(".kontozeile_center table")
                                       .first();
                    ProlongResult res = new ProlongResult(
                            MultiStepResult.Status.CONFIRMATION_NEEDED);
                    List<String[]> details = new ArrayList<>();

                    for (Element row : table.select("tr")) {
                        if (row.select(".konto_feld").size() == 1
                                && row.select(".konto_feldinhalt").size() == 1) {
                            details.add(new String[]{
                                    row.select(".konto_feld").text().trim(),
                                    row.select(".konto_feldinhalt").text()
                                       .trim()});
                        }
                    }
                    res.setDetails(details);
                    return res;
                } else {
                    List<NameValuePair> nameValuePairs = new ArrayList<>(
                            2);
                    nameValuePairs.add(new BasicNameValuePair("target",
                            "make_vl"));
                    nameValuePairs.add(new BasicNameValuePair("verlaengern",
                            "Bestätigung"));
                    httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
                            nameValuePairs), getDefaultEncoding());

                    return new ProlongResult(MultiStepResult.Status.OK);
                }
            }
        }
        return new ProlongResult(MultiStepResult.Status.ERROR, "??");
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {

        if (!initialised) {
            start();
        }
        if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
                || logged_in_as == null) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ProlongAllResult(MultiStepResult.Status.ERROR,
                        stringProvider
                                .getString(StringProvider.CONNECTION_ERROR));
            } catch (OpacErrorException e) {
                return new ProlongAllResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        } else if (logged_in_as.getId() != account.getId()) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ProlongAllResult(MultiStepResult.Status.ERROR,
                        stringProvider
                                .getString(StringProvider.CONNECTION_ERROR));
            } catch (OpacErrorException e) {
                return new ProlongAllResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        }
        String html = httpGet(opac_url + "/index.asp?target=alleverl",
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        if (doc.getElementsByClass("kontomeldung").size() == 1) {
            String err = doc.getElementsByClass("kontomeldung").get(0).text();
            return new ProlongAllResult(MultiStepResult.Status.ERROR, err);
        }

        if (doc.select(".kontozeile table").size() == 1) {
            Map<Integer, String> colmap = new HashMap<>();
            List<Map<String, String>> result = new ArrayList<>();
            for (Element tr : doc.select(".kontozeile table tr")) {
                if (tr.select(".tabHeaderKonto").size() > 0) {
                    int i = 0;
                    for (Element th : tr.select("th")) {
                        if (th.text().contains("Verfasser")) {
                            colmap.put(i,
                                    OpacApi.ProlongAllResult.KEY_LINE_AUTHOR);
                        } else if (th.text().contains("Titel")) {
                            colmap.put(i,
                                    OpacApi.ProlongAllResult.KEY_LINE_TITLE);
                        } else if (th.text().contains("Neue")) {
                            colmap.put(
                                    i,
                                    OpacApi.ProlongAllResult.KEY_LINE_NEW_RETURNDATE);
                        } else if (th.text().contains("Frist")) {
                            colmap.put(
                                    i,
                                    OpacApi.ProlongAllResult.KEY_LINE_OLD_RETURNDATE);
                        } else if (th.text().contains("Status")) {
                            colmap.put(i,
                                    OpacApi.ProlongAllResult.KEY_LINE_MESSAGE);
                        }
                        i++;
                    }
                } else {
                    Map<String, String> line = new HashMap<>();
                    for (Entry<Integer, String> entry : colmap.entrySet()) {
                        line.put(entry.getValue(), tr.child(entry.getKey())
                                                     .text().trim());
                    }
                    result.add(line);
                }
            }

            if (doc.select("input#make_allvl").size() > 0) {
                List<NameValuePair> nameValuePairs = new ArrayList<>(
                        2);
                nameValuePairs.add(new BasicNameValuePair("target",
                        "make_allvl_flag"));
                nameValuePairs.add(new BasicNameValuePair("make_allvl",
                        "Bestaetigung"));
                httpPost(opac_url + "/index.asp",
                        new UrlEncodedFormEntity(nameValuePairs),
                        getDefaultEncoding());
            }

            return new ProlongAllResult(MultiStepResult.Status.OK, result);
        }

        return new ProlongAllResult(MultiStepResult.Status.ERROR,
                stringProvider.getString(StringProvider.INTERNAL_ERROR));
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        if (!initialised) {
            start();
        }
        if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
                || logged_in_as == null) {
            try {
                account(account);
            } catch (JSONException e) {
                return new CancelResult(
                        MultiStepResult.Status.ERROR,
                        stringProvider
                                .getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
            }
        } else if (logged_in_as.getId() != account.getId()) {
            try {
                account(account);
            } catch (JSONException e) {
                return new CancelResult(
                        MultiStepResult.Status.ERROR,
                        stringProvider
                                .getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
            }
        }
        httpGet(opac_url + "/" + media, getDefaultEncoding());

        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("target", "delvorbest"));
        nameValuePairs
                .add(new BasicNameValuePair("vorbdelbest", "Bestätigung"));
        httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
                nameValuePairs), getDefaultEncoding());
        return new CancelResult(MultiStepResult.Status.OK);
    }

    @Override
    public AccountData account(Account acc) throws IOException,
            JSONException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        if (acc.getName() == null || acc.getName().equals("null")) {
            return null;
        }

        List<NameValuePair> nameValuePairs;
        // nameValuePairs.add(new BasicNameValuePair("link_konto.x", "0"));
        // nameValuePairs.add(new BasicNameValuePair("link_konto.y", "0"));
        // String html = httpPost(opac_url + "/index.asp",
        // new UrlEncodedFormEntity(nameValuePairs), "ISO-8859-1");
        String html = httpGet(opac_url + "/index.asp?kontofenster=start",
                "ISO-8859-1");
        Document doc = Jsoup.parse(html);

        if (doc.select("input[name=AUSWEIS]").size() > 0) {
            // Login vonnöten
            nameValuePairs = new ArrayList<>();
            nameValuePairs
                    .add(new BasicNameValuePair("AUSWEIS", acc.getName()));
            nameValuePairs
                    .add(new BasicNameValuePair("PWD", acc.getPassword()));
            if (data.has("db")) {
                nameValuePairs.add(new BasicNameValuePair("vkontodb", data
                        .getString("db")));
            }
            nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
            nameValuePairs.add(new BasicNameValuePair("kontofenster", "true"));
            nameValuePairs.add(new BasicNameValuePair("target", "konto"));
            nameValuePairs.add(new BasicNameValuePair("type", "K"));
            html = httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
                    nameValuePairs), "ISO-8859-1", true);
            doc = Jsoup.parse(html);
        }
        // } else if (response.getStatusLine().getStatusCode() == 302) {
        // Already logged in
        // html = httpGet(opac_url + "/index.asp?target=konto",
        // "ISO-8859-1",
        // true);
        // } else if (response.getStatusLine().getStatusCode() >= 400) {
        // throw new NotReachableException();
        // }

        if (doc.getElementsByClass("kontomeldung").size() == 1) {
            throw new OpacErrorException(doc.getElementsByClass("kontomeldung")
                                            .get(0).text());
        }

        logged_in = System.currentTimeMillis();
        logged_in_as = acc;

        JSONObject copymap = data.getJSONObject("accounttable");

        List<Map<String, String>> media = new ArrayList<>();

        if (doc.select(".kontozeile_center table").size() == 0) {
            return null;
        }

        Elements exemplartrs = doc.select(".kontozeile_center table").get(0)
                                  .select("tr.tabKonto");

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

        for (int i = 0; i < exemplartrs.size(); i++) {
            Element tr = exemplartrs.get(i);
            Map<String, String> e = new HashMap<>();

            Iterator<?> keys = copymap.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                int index;
                try {
                    index = copymap.has(key) ? copymap.getInt(key) : -1;
                } catch (JSONException e1) {
                    index = -1;
                }
                if (index >= 0) {
                    if (key.equals(AccountData.KEY_LENT_LINK)) {
                        if (tr.child(index).children().size() > 0) {
                            e.put(key, tr.child(index).child(0).attr("href"));
                            e.put(AccountData.KEY_LENT_RENEWABLE,
                                    tr.child(index).child(0).attr("href")
                                      .contains("vermsg") ? "N" : "Y");
                        }
                    } else {
                        e.put(key, tr.child(index).text());
                    }
                }
            }

            if (e.containsKey(AccountData.KEY_LENT_DEADLINE)) {
                try {
                    e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String
                            .valueOf(sdf.parse(
                                    e.get(AccountData.KEY_LENT_DEADLINE))
                                        .getTime()));
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
            }
            media.add(e);
        }
        assert (doc.select(".kontozeile_center table").get(0).select("tr")
                   .size() > 0);
        assert (exemplartrs.size() == media.size());

        copymap = data.getJSONObject("reservationtable");

        List<Map<String, String>> reservations = new ArrayList<>();
        exemplartrs = doc.select(".kontozeile_center table").get(1)
                         .select("tr.tabKonto");
        for (int i = 0; i < exemplartrs.size(); i++) {
            Element tr = exemplartrs.get(i);
            Map<String, String> e = new HashMap<>();

            Iterator<?> keys = copymap.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                int index;
                try {
                    index = copymap.has(key) ? copymap.getInt(key) : -1;
                } catch (JSONException e1) {
                    index = -1;
                }
                if (index >= 0) {
                    if (key.equals(AccountData.KEY_RESERVATION_CANCEL)) {
                        if (tr.child(index).children().size() > 0) {
                            e.put(key, tr.child(index).child(0).attr("href"));
                        }
                    } else {
                        e.put(key, tr.child(index).text());
                    }
                }
            }

            reservations.add(e);
        }
        assert (doc.select(".kontozeile_center table").get(1).select("tr")
                   .size() > 0);
        assert (exemplartrs.size() == reservations.size());

        AccountData res = new AccountData(acc.getId());

        for (Element row : doc.select(".kontozeile_center, div[align=center]")) {
            String text = row.text().trim();
            if (text.matches(
                    ".*Ausstehende Geb.+hren:[^0-9]+([0-9.,]+)[^0-9€A-Z]*(€|EUR|CHF|Fr.).*")) {
                text = text
                        .replaceAll(
                                ".*Ausstehende Geb.+hren:[^0-9]+([0-9.," +
                                        "]+)[^0-9€A-Z]*(€|EUR|CHF|Fr.).*",
                                "$1 $2");
                res.setPendingFees(text);
            }
            if (text.matches("Ihr Ausweis ist g.ltig bis:.*")) {
                text = text.replaceAll(
                        "Ihr Ausweis ist g.ltig bis:[^A-Za-z0-9]+", "");
                res.setValidUntil(text);
            } else if (text.matches("Ausweis g.ltig bis:.*")) {
                text = text.replaceAll("Ausweis g.ltig bis:[^A-Za-z0-9]+", "");
                res.setValidUntil(text);
            }
        }

        res.setLent(media);
        res.setReservations(reservations);
        return res;
    }

    @Override
    public boolean isAccountSupported(Library library) {
        return !library.getData().isNull("accounttable");
    }

    @Override
    public boolean isAccountExtendable() {
        return true;
    }

    @Override
    public String getAccountExtendableInfo(Account acc)
            throws IOException {
        if (!initialised) {
            start();
        }

        String html = "";

        if (acc.getName() == null || acc.getName().equals("null")) {
            return null;
        }

        // Needs login
        HttpPost httppost = new HttpPost(opac_url + "/index.asp");
        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("link_konto.x", "0"));
        nameValuePairs.add(new BasicNameValuePair("link_konto.y", "0"));
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = http_client.execute(httppost);

        if (response.getStatusLine().getStatusCode() == 200) {
            // Needs login
            HttpUtils.consume(response.getEntity());
            nameValuePairs = new ArrayList<>(2);
            nameValuePairs
                    .add(new BasicNameValuePair("AUSWEIS", acc.getName()));
            nameValuePairs
                    .add(new BasicNameValuePair("PWD", acc.getPassword()));
            nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
            nameValuePairs.add(new BasicNameValuePair("target", "konto"));
            nameValuePairs.add(new BasicNameValuePair("type", "K"));
            html = httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
                    nameValuePairs), getDefaultEncoding());
        } else if (response.getStatusLine().getStatusCode() == 302) {
            // Already logged in
            HttpUtils.consume(response.getEntity());
            html = httpGet(opac_url + "/index.asp?target=konto",
                    getDefaultEncoding());
        } else if (response.getStatusLine().getStatusCode() == 500) {
            throw new NotReachableException();
        }

        return html;

    }

    @Override
    public String getShareUrl(String id, String title) {
        try {
            return "http://opacapp.de/:" + library.getIdent() + ":" + id + ":"
                    + URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "http://opacapp.de/:" + library.getIdent() + ":" + id + ":"
                    + title;
        }
    }

    @Override
    public int getSupportFlags() {
        int flags = SUPPORT_FLAG_ACCOUNT_EXTENDABLE
                | SUPPORT_FLAG_CHANGE_ACCOUNT;
        flags |= SUPPORT_FLAG_ENDLESS_SCROLLING;
        if (!data.has("disableProlongAll")) {
            flags |= SUPPORT_FLAG_ACCOUNT_PROLONG_ALL;
        }
        return flags;
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void checkAccountData(Account acc) throws IOException,
            JSONException, OpacErrorException {
        start();
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("AUSWEIS", acc.getName()));
        nameValuePairs.add(new BasicNameValuePair("PWD", acc.getPassword()));
        if (data.has("db")) {
            nameValuePairs.add(new BasicNameValuePair("vkontodb", data
                    .getString("db")));
        }
        nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
        nameValuePairs.add(new BasicNameValuePair("target", "konto"));
        nameValuePairs.add(new BasicNameValuePair("type", "K"));
        String html = httpPostWithRedirect(opac_url + "/index.asp",
                new UrlEncodedFormEntity(nameValuePairs), "ISO-8859-1");
        Document doc = Jsoup.parse(html);
        if (doc.select(".kontomeldung").size() > 0) {
            throw new OpacErrorException(doc.select(".kontomeldung").text());
        }
    }

    private String httpPostWithRedirect(String url, UrlEncodedFormEntity data,
            String encoding)
            throws IOException {
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(data);
        HttpResponse response = http_client.execute(httppost);
        if (response.getStatusLine().getStatusCode() == 302) {
            HttpGet httpget = new HttpGet(url.substring(0,
                    url.lastIndexOf("/") + 1)
                    + response.getFirstHeader("Location").getValue());
            HttpUtils.consume(response.getEntity());
            response = http_client.execute(httpget);
        }
        return convertStreamToString(response.getEntity().getContent(),
                encoding);
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
