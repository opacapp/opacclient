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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.HttpUtils;
import de.geeksfactory.opacclient.networking.NotReachableException;
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
import de.geeksfactory.opacclient.objects.Volume;
import de.geeksfactory.opacclient.reporting.Report;
import de.geeksfactory.opacclient.reporting.ReportHandler;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.utils.JsonKeyIterator;
import okhttp3.FormBody;

/**
 * OpacApi implementation for Bibliotheca Web Opacs, originally developed by BOND, now owned by
 * OCLC. Known to work well with Web Opac versions from 2.6, maybe older, to 2.8
 */
public class Bibliotheca extends OkHttpBaseApi {

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
    public List<SearchField> parseSearchFields() throws IOException,
            JSONException {
        if (!initialised) {
            start();
        }

        List<SearchField> fields = new ArrayList<>();
        // Read branches and media types
        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("link_profis.x", "0");
        formData.add("link_profis.y", "1");
        String html = httpPost(opac_url + "/index.asp",
                formData.build(), getDefaultEncoding());
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
                SearchField field = createSearchField(name, hint, inputs.get(0));
                Elements radios = fieldElem.select("input[type=radio]");
                if (field instanceof TextSearchField && radios.size() > 0) {
                    TextSearchField tf = (TextSearchField) field;
                    if (radios.get(0).attr("value").equals("stich")) {
                        tf.setFreeSearch(true);
                        if (fieldElem.select("label[for=stichtit_sich]").size() > 0) {
                            tf.setHint(fieldElem.select("label[for=stichtit_sich]").text().trim());
                        }
                        JSONObject addData = new JSONObject();
                        JSONObject params = new JSONObject();
                        params.put("stichtit", "stich");
                        addData.put("additional_params", params);
                        tf.setData(addData);
                    }
                    if (radios.size() == 2 && radios.get(1).attr("value").equals("titel")) {
                        TextSearchField tf2 = new TextSearchField();
                        tf2.setId(tf.getId());
                        if (fieldElem.select("label[for=stichtit_titel]").size() > 0) {
                            tf2.setDisplayName(
                                    fieldElem.select("label[for=stichtit_titel]").text().trim());
                        }
                        JSONObject addData = new JSONObject();
                        JSONObject params = new JSONObject();
                        params.put("stichtit", "titel");
                        addData.put("additional_params", params);
                        tf2.setData(addData);
                        fields.add(tf2);
                    }
                }

                fields.add(field);
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

        DropdownSearchField orderField = new DropdownSearchField("orderselect",
                stringProvider.getString(StringProvider.ORDER), false,
                null);
        orderField.addDropdownValue("1", stringProvider.getString(StringProvider.ORDER_DEFAULT));
        orderField.addDropdownValue("2:desc", stringProvider.getString(StringProvider.ORDER_YEAR_DESC));
        orderField.addDropdownValue("2:asc", stringProvider.getString(StringProvider.ORDER_YEAR_ASC));
        orderField.addDropdownValue("3:desc", stringProvider.getString(StringProvider.ORDER_CATEGORY_DESC));
        orderField.addDropdownValue("3:asc", stringProvider.getString(StringProvider.ORDER_CATEGORY_ASC));
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
            for (Element option : input.select("option")) {
                field.addDropdownValue(option.attr("value"), option.text());
            }
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
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);
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

        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        boolean stichtitSet = false;

        int ifeldCount = 0;
        String order = "asc";
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
                formData.add("orderselect", query.getValue().split(":")[0]);
                order = query.getValue().split(":")[1];
            } else {
                formData.add(key, query.getValue());
            }
            if (query.getSearchField().getData() != null) {
                JSONObject data = query.getSearchField().getData();
                if (data.has("additional_params")) {
                    JSONObject params = data.getJSONObject("additional_params");
                    Iterator<String> keys = new JsonKeyIterator(params);
                    while (keys.hasNext()) {
                        String additionalKey = keys.next();
                        if (additionalKey.equals("stichtit")) {
                            if (stichtitSet) {
                                throw new OpacErrorException(
                                        stringProvider.getString(
                                                StringProvider.COMBINATION_NOT_SUPPORTED));
                            }
                            stichtitSet = true;
                        }
                        formData.add(additionalKey, params.getString(additionalKey));
                    }
                }
            }
        }

        if (!stichtitSet) {
            formData.add("stichtit", "stich");
        }

        formData.add("suche_starten.x", "1");
        formData.add("suche_starten.y", "1");

        if (data.has("db")) {
            try {
                formData.add("dbase", data.getString("db"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String html = httpPost(opac_url + "/index.asp", formData.build(), getDefaultEncoding());
        if (html.contains("<a href=\"index.asp?order=" + order + "\">")) {
            html = httpGet(opac_url + "/index.asp?order=" + order, getDefaultEncoding());
        }
        return parseSearch(html, 1, data);
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException {
        if (!initialised) {
            start();
        }

        String html = httpGet(opac_url + "/index.asp?scrollAction=" + page,
                getDefaultEncoding());
        return parseSearch(html, page, data);
    }

    public static SearchRequestResult parseSearch(String html, int page, JSONObject data) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(data.optString("baseurl"));
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
    public DetailedItem getResultById(String a, String homebranch)
            throws IOException {
        if (!initialised) {
            start();
        }
        String html = httpGet(opac_url + "/index.asp?MedienNr=" + a,
                getDefaultEncoding());
        DetailedItem result = parseResult(html, data);
        if (result.getId() == null) {
            result.setId(a);
        }
        return result;
    }

    @Override
    public DetailedItem getResult(int nr) throws IOException {
        String html = httpGet(opac_url + "/index.asp?detmediennr=" + nr,
                getDefaultEncoding());

        return parseResult(html, data);
    }

    static DetailedItem parseResult(String html, JSONObject data) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(data.optString("baseurl"));

        DetailedItem result = new DetailedItem();

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
                        copymap.put("branch", i);
                    } else if (head.equals("Abteilung")) {
                        copymap.put("department", i);
                    } else if (head.equals("Bereich")
                            || head.equals("Standort")) {
                        copymap.put("location", i);
                    } else if (head.equals("Signatur")) {
                        copymap.put("signature", i);
                    } else if (head.equals("Barcode")
                            || head.equals("Medien-Nummer")) {
                        copymap.put("barcode", i);
                    } else if (head.equals("Status")) {
                        copymap.put("status", i);
                    } else if (head.equals("Frist")
                            || head.matches("Verf.+gbar")) {
                        copymap.put("returndate", i);
                    } else if (head.equals("Vorbestellungen")
                            || head.equals("Reservierungen")) {
                        copymap.put("reservations", i);
                    }
                }
            }
            Elements exemplartrs = doc
                    .select(".exemplartab .tabExemplar, .exemplartab .tabExemplar_");
            DateTimeFormatter
                    fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
            for (int i = 0; i < exemplartrs.size(); i++) {
                Element tr = exemplartrs.get(i);

                Copy copy = new Copy();

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
                        try {
                            copy.set(key, tr.child(index).text(), fmt);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }

                result.addCopy(copy);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Elements bandtrs = doc.select("table .tabBand a");
            for (int i = 0; i < bandtrs.size(); i++) {
                Element tr = bandtrs.get(i);

                Volume volume = new Volume();
                volume.setId(tr.attr("href").split("=")[1]);
                volume.setTitle(tr.text());
                result.addVolume(volume);
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
    public ReservationResult reservation(DetailedItem item, Account acc,
            int useraction, String selection) throws IOException {
        String reservation_info = item.getReservation_info();

        Document doc = null;

        if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("make_allvl", "Bestaetigung");
            formData.add("target", "makevorbest");
            httpPost(opac_url + "/index.asp", formData.build(), getDefaultEncoding());
            return new ReservationResult(MultiStepResult.Status.OK);
        } else if (selection == null || useraction == 0) {
            String html = httpGet(opac_url + "/" + reservation_info,
                    getDefaultEncoding());
            doc = Jsoup.parse(html);

            if (doc.select("input[name=AUSWEIS]").size() > 0) {
                // Needs login
                FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
                formData.add("AUSWEIS", acc.getName());
                formData.add("PWD", acc.getPassword());
                if (data.has("db")) {
                    try {
                        formData.add("vkontodb", data.getString("db"));
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                formData.add("B1", "weiter");
                formData.add("target", doc.select("input[name=target]").val());
                formData.add("type", "VT2");
                html = httpPost(opac_url + "/index.asp",
                        formData.build(),
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
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add(branch_inputfield, selection);
            formData.add("button2", "weiter");
            formData.add("target", _res_target);
            String html = httpPost(opac_url + "/index.asp",
                    formData.build(),
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
                Pattern p = Pattern.compile("geb.hr", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
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

                if (doc.select("#vorbest").size() > 0 &&
                        doc.select("#vorbest").val().contains("(")) {
                    // Erlangen uses "Kostenpflichtige Vorbestellung (1 Euro)"
                    // as the label of its reservation button
                    details.add(new String[]{doc.select("#vorbest").val().trim()});
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
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("target", "make_vl");
            formData.add("verlaengern", "Bestätigung");
            httpPost(opac_url + "/index.asp", formData.build(), getDefaultEncoding());

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
                    FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
                    formData.add("target", "make_vl");
                    formData.add("verlaengern", "Bestätigung");
                    httpPost(opac_url + "/index.asp", formData.build(), getDefaultEncoding());

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
                FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
                formData.add("target", "make_allvl_flag");
                formData.add("make_allvl", "Bestaetigung");
                httpPost(opac_url + "/index.asp", formData.build(), getDefaultEncoding());
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

        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("target", "delvorbest");
        formData.add("vorbdelbest", "Bestätigung");
        httpPost(opac_url + "/index.asp", formData.build(), getDefaultEncoding());
        return new CancelResult(MultiStepResult.Status.OK);
    }

    @Override
    public AccountData account(Account acc) throws IOException,
            JSONException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        List<NameValuePair> nameValuePairs;
        String html = httpGet(opac_url + "/index.asp?kontofenster=start",
                "ISO-8859-1");
        Document doc = Jsoup.parse(html);
        if (doc.select("input[name=AUSWEIS]").size() > 0) {
            // Login vonnöten
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("AUSWEIS", acc.getName());
            formData.add("PWD", acc.getPassword());
            if (data.has("db")) {
                formData.add("vkontodb", data.getString("db"));
            }
            formData.add("B1", "weiter");
            formData.add("kontofenster", "true");
            formData.add("target", "konto");
            formData.add("type", "K");
            html = httpPost(opac_url + "/index.asp", formData.build(), "ISO-8859-1", true);
            doc = Jsoup.parse(html);
        }
        if (doc.getElementsByClass("kontomeldung").size() == 1) {
            throw new OpacErrorException(doc.getElementsByClass("kontomeldung")
                                            .get(0).text());
        }
        logged_in_as = acc;
        logged_in = System.currentTimeMillis();
        return parse_account(acc, doc, data, reportHandler,
                loadJsonResource("/bibliotheca/headers_lent.json"),
                loadJsonResource("/bibliotheca/headers_reservations.json"));
    }

    public static AccountData parse_account(Account acc, Document doc, JSONObject data,
            ReportHandler reportHandler, JSONObject headers_lent, JSONObject headers_reservations)
            throws JSONException, NotReachableException {
        if (doc.select(".kontozeile_center table").size() == 0) {
            throw new NotReachableException();
        }

        Map<String, Integer> copymap = new HashMap<>();
        Elements headerCells = doc.select(".kontozeile_center table").get(0)
                                  .select("tr.exemplarmenubar").get(0).children();
        JSONArray headersList = new JSONArray();
        JSONArray unknownHeaders = new JSONArray();
        int i = 0;
        for (Element headerCell : headerCells) {
            String header = headerCell.text();
            headersList.put(header);
            if (headers_lent.has(header)) {
                if (!headers_lent.isNull(header)) copymap.put(headers_lent.getString(header), i);
            } else {
                unknownHeaders.put(header);
            }
            i++;
        }

        if (unknownHeaders.length() > 0) {
            // send report
            JSONObject reportData = new JSONObject();
            reportData.put("headers", headersList);
            reportData.put("unknown_headers", unknownHeaders);
            Report report = new Report(acc.getLibrary(), "bibliotheca", "unknown header - lent",
                    DateTime.now(), reportData);
            reportHandler.sendReport(report);

            // fallback to JSON
            JSONObject accounttable = data.getJSONObject("accounttable");
            copymap = jsonToMap(accounttable);
        }

        List<LentItem> media = new ArrayList<>();

        Elements exemplartrs = doc.select(".kontozeile_center table").get(0)
                                  .select("tr.tabKonto");

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        DateTimeFormatter fmt2 = DateTimeFormat.forPattern("d/M/yyyy").withLocale(Locale.GERMAN);

        for (Element tr : exemplartrs) {
            LentItem item = new LentItem();

            for (Entry<String, Integer> entry : copymap.entrySet()) {
                String key = entry.getKey();
                int index = entry.getValue();
                if (key.equals("prolongurl")) {
                    if (tr.child(index).children().size() > 0) {
                        item.setProlongData(tr.child(index).child(0).attr("href"));
                        item.setRenewable(
                                !tr.child(index).child(0).attr("href").contains("vermsg"));
                    }
                } else if (key.equals("returndate")) {
                    try {
                        item.setDeadline(fmt.parseLocalDate(tr.child(index).text()));
                    } catch (IllegalArgumentException e1) {
                        try {
                            item.setDeadline(fmt2.parseLocalDate(tr.child(index).text()));
                        } catch (IllegalArgumentException e2) {
                            e2.printStackTrace();
                        }
                    }
                } else {
                    item.set(key, tr.child(index).text());
                }
            }

            media.add(item);
        }

        copymap = new HashMap<>();
        headerCells = doc.select(".kontozeile_center table").get(1)
                         .select("tr.exemplarmenubar").get(0).children();
        headersList = new JSONArray();
        unknownHeaders = new JSONArray();
        i = 0;
        for (Element headerCell : headerCells) {
            String header = headerCell.text();
            headersList.put(header);
            if (headers_reservations.has(header)) {
                if (!headers_reservations.isNull(header)) {
                    copymap.put(headers_reservations.getString(header), i);
                }
            } else {
                unknownHeaders.put(header);
            }
            i++;
        }

        if (unknownHeaders.length() > 0) {
            // send report
            JSONObject reportData = new JSONObject();
            reportData.put("headers", headersList);
            reportData.put("unknown_headers", unknownHeaders);
            Report report =
                    new Report(acc.getLibrary(), "bibliotheca", "unknown header - reservations",
                            DateTime.now(), reportData);
            reportHandler.sendReport(report);

            // fallback to JSON
            JSONObject reservationtable = data.getJSONObject("reservationtable");
            copymap = jsonToMap(reservationtable);
        }

        List<ReservedItem> reservations = new ArrayList<>();
        exemplartrs = doc.select(".kontozeile_center table").get(1)
                         .select("tr.tabKonto");
        for (Element tr : exemplartrs) {
            ReservedItem item = new ReservedItem();

            for (Entry<String, Integer> entry : copymap.entrySet()) {
                String key = entry.getKey();
                int index = entry.getValue();
                if (key.equals("cancelurl")) {
                    if (tr.child(index).children().size() > 0) {
                        item.setCancelData(tr.child(index).child(0).attr("href"));
                    }
                } else if (key.equals("availability")) {
                    try {
                        item.setReadyDate(fmt.parseLocalDate(tr.child(index).text()));
                    } catch (IllegalArgumentException e1) {
                        try {
                            item.setReadyDate(fmt2.parseLocalDate(tr.child(index).text()));
                        } catch (IllegalArgumentException e2) {
                            e2.printStackTrace();
                        }
                    }
                } else if (key.equals("expirationdate")) {
                    try {
                        item.setExpirationDate(fmt.parseLocalDate(tr.child(index).text()));
                    } catch (IllegalArgumentException e1) {
                        try {
                            item.setExpirationDate(fmt2.parseLocalDate(tr.child(index).text()));
                        } catch (IllegalArgumentException e2) {
                            item.setStatus(tr.child(index).text());
                        }
                    }
                } else {
                    item.set(key, tr.child(index).text());
                }
            }

            reservations.add(item);
        }

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
    public String getShareUrl(String id, String title) {
        try {
            // our server does not like getting a "+" as the encoding for a space, so we replace it
            // with %20.
            return "https://info.opacapp.net/bibproxy/:" +
                    URLEncoder.encode(library.getIdent(), "UTF-8").replace("+", "%20") + ":"
                    + id + ":" + URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return null;
        }
    }

    @Override
    public int getSupportFlags() {
        int flags = SUPPORT_FLAG_CHANGE_ACCOUNT | SUPPORT_FLAG_WARN_RESERVATION_FEES;
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
        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("AUSWEIS", acc.getName());
        formData.add("PWD", acc.getPassword());
        if (data.has("db")) {
            formData.add("vkontodb", data.getString("db"));
        }
        formData.add("B1", "weiter");
        formData.add("target", "konto");
        formData.add("type", "K");
        String html = httpPost(opac_url + "/index.asp", formData.build(), "ISO-8859-1");
        Document doc = Jsoup.parse(html);
        if (doc.select(".kontomeldung").size() > 0) {
            throw new OpacErrorException(doc.select(".kontomeldung").text());
        }
    }

    /*
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
    }*/

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
