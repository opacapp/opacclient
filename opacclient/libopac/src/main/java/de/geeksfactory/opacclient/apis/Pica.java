/**
 * Copyright (C) 2013 by Johan von Forstner under the MIT license:
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
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.utils.ISBNTools;

/**
 * @author Johan von Forstner, 16.09.2013
 */

public abstract class Pica extends ApacheBaseApi implements OpacApi {

    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();
    protected static HashMap<String, String> languageCodes = new HashMap<>();

    static {
        defaulttypes.put("book", MediaType.BOOK);
        defaulttypes.put("article", MediaType.BOOK);
        defaulttypes.put("binary", MediaType.EBOOK);
        defaulttypes.put("periodical", MediaType.MAGAZINE);
        defaulttypes.put("onlineper", MediaType.EBOOK);
        defaulttypes.put("letter", MediaType.UNKNOWN);
        defaulttypes.put("handwriting", MediaType.UNKNOWN);
        defaulttypes.put("map", MediaType.MAP);
        defaulttypes.put("picture", MediaType.ART);
        defaulttypes.put("audiovisual", MediaType.MOVIE);
        defaulttypes.put("score", MediaType.SCORE_MUSIC);
        defaulttypes.put("sound", MediaType.CD_MUSIC);
        defaulttypes.put("software", MediaType.CD_SOFTWARE);
        defaulttypes.put("microfilm", MediaType.UNKNOWN);
        defaulttypes.put("empty", MediaType.UNKNOWN);

        languageCodes.put("de", "DU");
        languageCodes.put("en", "EN");
        languageCodes.put("nl", "NE");
        languageCodes.put("fr", "FR");
    }

    protected String opac_url = "";
    protected String https_url = "";
    protected JSONObject data;
    protected Library library;
    protected int resultcount = 10;
    protected String reusehtml;
    protected Integer searchSet;
    protected String db;
    protected String pwEncoded;
    protected String languageCode;
    protected CookieStore cookieStore = new BasicCookieStore();
    protected String lor_reservations;

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);

        this.library = lib;
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
            this.db = data.getString("db");
            if (library.isAccountSupported()) {
                if (data.has("httpsbaseurl")) {
                    this.https_url = data.getString("httpsbaseurl");
                } else {
                    this.https_url = this.opac_url;
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected int addParameters(SearchQuery query, List<NameValuePair> params,
            int index) throws JSONException {
        if (query.getValue().equals("") || query.getValue().equals("false")) {
            return index;
        }
        if (query.getSearchField() instanceof TextSearchField
                || query.getSearchField() instanceof BarcodeSearchField) {
            if (query.getSearchField().getData().getBoolean("ADI")) {
                params.add(new BasicNameValuePair(query.getKey(), query
                        .getValue()));
            } else {
                if (index == 0) {
                    params.add(new BasicNameValuePair("ACT" + index, "SRCH"));
                } else {
                    params.add(new BasicNameValuePair("ACT" + index, "*"));
                }

                params.add(new BasicNameValuePair("IKT" + index, query.getKey()));
                params.add(new BasicNameValuePair("TRM" + index, query
                        .getValue()));
                return index + 1;
            }
        } else if (query.getSearchField() instanceof CheckboxSearchField) {
            boolean checked = Boolean.valueOf(query.getValue());
            if (checked) {
                params.add(new BasicNameValuePair(query.getKey(), "Y"));
            }
        } else if (query.getSearchField() instanceof DropdownSearchField) {
            params.add(new BasicNameValuePair(query.getKey(), query.getValue()));
        }
        return index;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException,
            JSONException {
        if (!initialised) {
            start();
        }

        List<NameValuePair> params = new ArrayList<>();

        int index = 0;
        start();

        params.add(new BasicNameValuePair("ACT", "SRCHM"));
        params.add(new BasicNameValuePair("MATCFILTER", "Y"));
        params.add(new BasicNameValuePair("MATCSET", "Y"));
        params.add(new BasicNameValuePair("NOSCAN", "Y"));
        params.add(new BasicNameValuePair("PARSE_MNEMONICS", "N"));
        params.add(new BasicNameValuePair("PARSE_OPWORDS", "N"));
        params.add(new BasicNameValuePair("PARSE_OLDSETS", "N"));

        for (SearchQuery singleQuery : query) {
            index = addParameters(singleQuery, params, index);
        }

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }
        if (index > 4) {
            throw new OpacErrorException(stringProvider.getQuantityString(
                    StringProvider.LIMITED_NUM_OF_CRITERIA, 4, 4));
        }

        String html = httpGet(
                opac_url + "/LNG=" + getLang() + "/DB=" + db
                        + "/SET=1/TTL=1/CMD?"
                        + URLEncodedUtils.format(params, getDefaultEncoding()),
                getDefaultEncoding(), false, cookieStore);

        return parse_search(html, 1);
    }

    public SearchRequestResult volumeSearch(Map<String, String> query)
            throws IOException, OpacErrorException {
        String html = httpGet(
                opac_url + "/LNG=" + getLang() + "/DB=" + db + "/SET=1/TTL=1"
                        + "/FAM?PPN=" + query.get("ppn"),
                getDefaultEncoding(), false, cookieStore);
        return parse_search(html, 1);
    }

    protected SearchRequestResult parse_search(String html, int page)
            throws OpacErrorException {
        Document doc = Jsoup.parse(html);

        updateSearchSetValue(doc);

        if (doc.select(".error").size() > 0) {
            String error = doc.select(".error").first().text().trim();
            if (error.equals("Es wurde nichts gefunden.")
                    || error.equals("Nothing has been found")
                    || error.equals("Er is niets gevonden.")
                    || error.equals("Rien n'a été trouvé.")) {
                // nothing found
                return new SearchRequestResult(new ArrayList<SearchResult>(),
                        0, 1, 1);
            } else {
                // error
                throw new OpacErrorException(error);
            }
        }

        reusehtml = html;

        int results_total;

        String resultnumstr = doc.select(".pages").first().text();
        Pattern p = Pattern.compile("[0-9]+$");
        Matcher m = p.matcher(resultnumstr);
        if (m.find()) {
            resultnumstr = m.group();
        }
        if (resultnumstr.contains("(")) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(
                    ".*\\(([0-9]+)\\).*", "$1"));
        } else if (resultnumstr.contains(": ")) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(
                    ".*: ([0-9]+)$", "$1"));
        } else {
            results_total = Integer.parseInt(resultnumstr);
        }

        List<SearchResult> results = new ArrayList<>();

        if (results_total == 1) {
            // Only one result
            DetailedItem singleResult = parse_result(html);
            SearchResult sr = new SearchResult();
            sr.setType(getMediaTypeInSingleResult(html));
            sr.setInnerhtml("<b>" + singleResult.getTitle() + "</b><br>"
                    + singleResult.getDetails().get(0).getContent());
            results.add(sr);
        }

        Elements table = doc
                .select("table[summary=hitlist] tbody tr[valign=top]");
        // identifier = null;

        Elements links = doc.select("table[summary=hitlist] a");
        boolean haslink = false;
        for (int i = 0; i < links.size(); i++) {
            Element node = links.get(i);
            if (node.hasAttr("href") & node.attr("href").contains("SHW?")
                    && !haslink) {
                haslink = true;
                try {
                    List<NameValuePair> anyurl = URLEncodedUtils.parse(new URI(
                            node.attr("href")), getDefaultEncoding());
                    for (NameValuePair nv : anyurl) {
                        if (nv.getName().equals("identifier")) {
                            // identifier = nv.getValue();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        for (int i = 0; i < table.size(); i++) {
            Element tr = table.get(i);
            SearchResult sr = new SearchResult();
            if (tr.select("td.hit img").size() > 0) {
                String[] fparts = tr.select("td img").get(0).attr("src")
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
            }
            Element middlething = tr.child(2);

            List<Node> children = middlething.childNodes();
            int childrennum = children.size();

            List<String[]> strings = new ArrayList<>();
            for (int ch = 0; ch < childrennum; ch++) {
                Node node = children.get(ch);
                if (node instanceof TextNode) {
                    String text = ((TextNode) node).text().trim();
                    if (text.length() > 3) {
                        strings.add(new String[]{"text", "", text});
                    }
                } else if (node instanceof Element) {

                    List<Node> subchildren = node.childNodes();
                    for (int j = 0; j < subchildren.size(); j++) {
                        Node subnode = subchildren.get(j);
                        if (subnode instanceof TextNode) {
                            String text = ((TextNode) subnode).text().trim();
                            if (text.length() > 3) {
                                strings.add(new String[]{
                                        ((Element) node).tag().getName(),
                                        "text", text,
                                        ((Element) node).className(),
                                        node.attr("style")});
                            }
                        } else if (subnode instanceof Element) {
                            String text = ((Element) subnode).text().trim();
                            if (text.length() > 3) {
                                strings.add(new String[]{
                                        ((Element) node).tag().getName(),
                                        ((Element) subnode).tag().getName(),
                                        text, ((Element) node).className(),
                                        node.attr("style")});
                            }
                        }
                    }
                }
            }

            StringBuilder description = new StringBuilder();

            int k = 0;
            for (String[] part : strings) {
                if (part[0].equals("a") && k == 0) {
                    description.append("<b>").append(part[2]).append("</b>");
                } else if (k < 3) {
                    description.append("<br />").append(part[2]);
                }
                k++;
            }
            sr.setInnerhtml(description.toString());

            sr.setNr(10 * (page - 1) + i);
            sr.setId(null);
            results.add(sr);
        }
        resultcount = results.size();
        return new SearchRequestResult(results, results_total, page);
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        String html = httpGet(opac_url + "/LNG=" + getLang() + "/DB=" + db
                        + "/SET=" + searchSet + "/TTL=1/NXT?FRST="
                        + (((page - 1) * resultcount) + 1), getDefaultEncoding(),
                false, cookieStore);
        return parse_search(html, page);
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException {
        return null;
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException {

        if (id == null && reusehtml != null) {
            return parse_result(reusehtml);
        }

        if (!initialised) {
            start();
        }

        if (id.startsWith("http")) {
            return parse_result(httpGet(id, getDefaultEncoding()));
        } else {
            try {
                return parse_result(httpGet(opac_url + "/LNG=" + getLang() + "/DB="
                        + data.getString("db") + "/PPNSET?PPN=" + id, getDefaultEncoding()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException {
        if (!initialised) {
            start();
        }
        String html = httpGet(opac_url + "/LNG=" + getLang() + "/DB=" + db
                        + "/LNG=" + getLang() + "/SET=" + searchSet
                        + "/TTL=1/SHW?FRST=" + (position + 1), getDefaultEncoding(),
                false, cookieStore);

        return parse_result(html);
    }

    protected DetailedItem parse_result(String html) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);

        DetailedItem result = new DetailedItem();
        for (Element a : doc.select("a[href*=PPN")) {
            Map<String, String> hrefq = getQueryParamsFirst(a
                    .absUrl("href"));
            String ppn = hrefq.get("PPN");
            result.setId(ppn);
            break;
        }

        // GET COVER
        if (doc.select("img[title=Titelbild]").size() > 0) {
            result.setCover(doc.select("img[title=Titelbild]").first().absUrl("src"));
        } else if (doc.select("td.preslabel:contains(ISBN) + td.presvalue").size() > 0) {
            Element isbnElement = doc.select(
                    "td.preslabel:contains(ISBN) + td.presvalue").first();
            String isbn = isbnElement.text().trim();
            if (!isbn.equals("")) {
                result.setCover(ISBNTools.getAmazonCoverURL(isbn, true));
            }
        }

        // GET TITLE AND SUBTITLE
        String titleAndSubtitle;
        Element titleAndSubtitleElem = null;
        String titleRegex = ".*(Titel|Aufsatz|Zeitschrift|Gesamttitel"
                + "|Title|Article|Periodical|Collective\\stitle"
                + "|Titre|Article|P.riodique|Titre\\sg.n.ral).*";
        String selector = "td.preslabel:matches(" + titleRegex + ") + td.presvalue";
        if (doc.select(selector).size() > 0) {
            titleAndSubtitleElem = doc.select(selector).first();
            titleAndSubtitle = titleAndSubtitleElem.text().trim();
            int slashPosition =
                    Math.min(titleAndSubtitle.indexOf("/"), titleAndSubtitle.indexOf(":"));
            String title;
            if (slashPosition > 0) {
                title = titleAndSubtitle.substring(0, slashPosition).trim();
                String subtitle = titleAndSubtitle.substring(slashPosition + 1).trim();
                result.addDetail(new Detail(stringProvider
                        .getString(StringProvider.SUBTITLE), subtitle));
            } else {
                title = titleAndSubtitle;
            }
            result.setTitle(title);
        } else {
            result.setTitle("");
        }

        // Details
        int line = 0;
        Elements lines = doc.select("td.preslabel + td.presvalue");
        if (titleAndSubtitleElem != null) {
            lines.remove(titleAndSubtitleElem);
        }
        for (Element element : lines) {
            Element titleElem = element.firstElementSibling();
            String detail = "";
            if (element.select("div").size() > 1 &&
                    element.select("div").text().equals(element.text())) {
                boolean first = true;
                for (Element div : element.select("div")) {
                    if (!div.text().replace("\u00a0", " ").trim().equals("")) {
                        if (!first) {
                            detail += "\n" + div.text().replace("\u00a0", " ").trim();
                        } else {
                            detail += div.text().replace("\u00a0", " ").trim();
                            first = false;
                        }
                    }
                }
            } else {
                detail = element.text().replace("\u00a0", " ").trim();
            }
            String title = titleElem.text().replace("\u00a0", " ").trim();

            if (element.select("hr").size() > 0 || element.text().trim().equals(""))
            // after the separator we get the copies
            {
                break;
            }

            if (detail.length() == 0 && title.length() == 0) {
                line++;
                continue;
            }
            if (title.contains(":")) {
                title = title.substring(0, title.indexOf(":")); // remove colon
            }
            result.addDetail(new Detail(title, detail));

            if (element.select("a").size() == 1 &&
                    !element.select("a").get(0).text().trim().equals("")) {
                String url = element.select("a").first().absUrl("href");
                if (!url.startsWith(opac_url)) {
                    result.addDetail(
                            new Detail(stringProvider.getString(StringProvider.LINK), url));
                }
            }

            line++;
        }
        line++; // next line after separator

        // Copies
        Copy copy = new Copy();
        String location = "";

        // reservation info will be stored as JSON
        JSONArray reservationInfo = new JSONArray();

        while (line < lines.size()) {
            Element element = lines.get(line);
            if (element.select("hr").size() == 0 && !element.text().trim().equals("")) {
                Element titleElem = element.firstElementSibling();
                String detail = element.text().trim();
                String title = titleElem.text().replace("\u00a0", " ").trim();

                if (detail.length() == 0 && title.length() == 0) {
                    line++;
                    continue;
                }

                if (title.contains("Standort")
                        || title.contains("Vorhanden in")
                        || title.contains("Location")) {
                    location += detail;
                } else if (title.contains("Sonderstandort")) {
                    location += " - " + detail;
                } else if (title.contains("Systemstelle")
                        || title.contains("Sachgebiete")
                        || title.contains("Subject")) {
                    copy.setDepartment(detail);
                } else if (title.contains("Fachnummer")
                        || title.contains("locationnumber")
                        || title.contains("Schlagwörter")) {
                    copy.setLocation(detail);
                } else if (title.contains("Signatur")
                        || title.contains("Shelf mark")) {
                    copy.setShelfmark(detail);
                } else if (title.contains("Anmerkung")) {
                    location += " (" + detail + ")";
                } else if (title.contains("Link")) {
                    result.addDetail(new Detail(title.replace(":", "").trim(), detail));
                } else if (title.contains("Status")
                        || title.contains("Ausleihinfo")
                        || title.contains("Ausleihstatus")
                        || title.contains("Request info")) {
                    // Find return date
                    Pattern pattern = Pattern
                            .compile("(till|bis) (\\d{2}-\\d{2}-\\d{4})");
                    Matcher matcher = pattern.matcher(detail);
                    if (matcher.find()) {
                        DateTimeFormatter fmt =
                                DateTimeFormat.forPattern("dd-MM-yyyy").withLocale(Locale.GERMAN);
                        try {
                            copy.setStatus(detail.substring(0, matcher.start() - 1).trim());
                            copy.setReturnDate(fmt.parseLocalDate(matcher.group(2)));
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            copy.setStatus(detail);
                        }
                    } else {
                        copy.setStatus(detail);
                    }
                    // Get reservation info
                    if (element.select("a:has(img[src*=inline_arrow])").size() > 0) {
                        Element a = element.select(
                                "a:has(img[src*=inline_arrow])").first();
                        boolean multipleCopies = a.text().matches(
                                ".*(Exemplare|Volume list).*");
                        JSONObject reservation = new JSONObject();
                        try {
                            reservation.put("multi", multipleCopies);
                            reservation.put("link", _extract_url(a));
                            reservation.put("desc", location);
                            reservationInfo.put(reservation);
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        result.setReservable(true);
                    }
                }
            } else {
                copy.setBranch(location);
                if (copy.notEmpty()) {
                    result.addCopy(copy);
                }
                location = "";
                copy = new Copy();
            }
            line++;
        }

        if (copy.notEmpty()) {
            copy.setBranch(location);
            result.addCopy(copy);
        }

        if (reservationInfo.length() == 0) {
            // No reservation info found yet, because we didn't find any copies.
            // If there is a reservation link somewhere in the rows we interpreted
            // as details, we still want to use it.
            if (doc.select("td a:has(img[src*=inline_arrow])").size() > 0) {
                Element a = doc.select(
                        "td a:has(img[src*=inline_arrow])").first();
                boolean multipleCopies = a.text().matches(
                        ".*(Exemplare|Volume list).*");
                JSONObject reservation = new JSONObject();
                try {
                    reservation.put("multi", multipleCopies);
                    reservation.put("link", _extract_url(a));
                    reservation.put("desc", location);
                    reservationInfo.put(reservation);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                result.setReservable(true);
            }
        }
        result.setReservation_info(reservationInfo.toString());

        // Volumes
        if (doc.select("a[href^=FAM?PPN=]").size() > 0) {
            String href = doc.select("a[href^=FAM?PPN=]").attr("href");
            String ppn = getQueryParamsFirst(href).get("PPN");
            Map<String, String> data = new HashMap<>();
            data.put("ppn", ppn);
            result.setVolumesearch(data);
        }

        return result;
    }

    private String _extract_url(Element link) {
        String javascriptUrl = link.absUrl("href");
        if (javascriptUrl.isEmpty()) {
            // absUrl does not work with javascript: links, obviously
            javascriptUrl = link.attr("href");
        }
        if (javascriptUrl.startsWith("javascript:")) {
            javascriptUrl = javascriptUrl.replaceAll("^javascript:PU\\('(.*)',(.*)\\)(.*)", "$1");
        }
        try {
            return new URL(new URL(opac_url), javascriptUrl).toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<SearchField> parseSearchFields() throws IOException, JSONException {
        if (!initialised) {
            start();
        }

        String html =
                httpGet(opac_url + "/LNG=" + getLang() + "/DB=" + db + "/ADVANCED_SEARCHFILTER",
                        getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        List<SearchField> fields = new ArrayList<>();

        Elements options = doc.select("select[name=IKT0] option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.attr("value"));
            field.setHint("");
            field.setData(new JSONObject("{\"ADI\": false}"));

            Pattern pattern = Pattern
                    .compile("(?: --- )?(\\[X?[A-Za-z]{2,3}:?\\]|\\(X?[A-Za-z]{2,3}:?\\))");
            Matcher matcher = pattern.matcher(field.getDisplayName());
            if (matcher.find()) {
                field.getData().put("meaning",
                        matcher.group(1).replace(":", "").toUpperCase());
                field.setDisplayName(matcher.replaceFirst("").trim());
            }

            fields.add(field);
        }

        Elements sort = doc.select("select[name=SRT]");
        if (sort.size() > 0) {
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName(sort.first().parent().parent()
                                     .select(".longval").first().text());
            field.setId("SRT");
            for (Element option : sort.select("option")) {
                field.addDropdownValue(option.attr("value"), option.text());
            }
            fields.add(field);
        }

        for (Element input : doc.select("input[type=text][name^=ADI]")) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(input.parent().parent().select(".longkey")
                                      .text());
            field.setId(input.attr("name"));
            field.setHint(input.parent().select("span").text());
            field.setData(new JSONObject("{\"ADI\": true}"));
            fields.add(field);
        }

        for (Element dropdown : doc.select("select[name^=ADI]")) {
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName(dropdown.parent().parent().select(".longkey")
                    .text());
            field.setId(dropdown.attr("name"));
            for (Element option : dropdown.select("option")) {
                field.addDropdownValue(option.attr("value"), option.text());
            }
            fields.add(field);
        }

        Elements fuzzy = doc.select("input[name=FUZZY]");
        if (fuzzy.size() > 0) {
            CheckboxSearchField field = new CheckboxSearchField();
            field.setDisplayName(fuzzy.first().parent().parent()
                                      .select(".longkey").first().text());
            field.setId("FUZZY");
            fields.add(field);
        }

        Elements mediatypes = doc.select("input[name=ADI_MAT]");
        if (mediatypes.size() > 0) {
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName("Materialart");
            field.setId("ADI_MAT");

            field.addDropdownValue("", "Alle");
            for (Element mt : mediatypes) {
                field.addDropdownValue(mt.attr("value"),
                        mt.parent().nextElementSibling().text().replace("\u00a0", ""));
            }
            fields.add(field);
        }

        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        if (id.startsWith("http")) {
            return id;
        } else {
            return opac_url + "/LNG=" + getLang() + "/DB=" + db + "/PPNSET?PPN=" + id;
        }
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
    }

    public void updateSearchSetValue(Document doc) {
        String url = doc.select("base").first().attr("href");
        Integer setPosition = url.indexOf("SET=") + 4;
        String searchSetString = url.substring(setPosition,
                url.indexOf("/", setPosition));
        searchSet = Integer.parseInt(searchSetString);
    }

    public MediaType getMediaTypeInSingleResult(String html) {
        Document doc = Jsoup.parse(html);
        MediaType mediatype = MediaType.UNKNOWN;

        if (doc.select("table[summary=presentation switch] img").size() > 0) {

            String[] fparts = doc
                    .select("table[summary=presentation switch] img").get(0)
                    .attr("src").split("/");
            String fname = fparts[fparts.length - 1];

            if (data.has("mediatypes")) {
                try {
                    mediatype = MediaType.valueOf(data.getJSONObject(
                            "mediatypes").getString(fname));
                } catch (JSONException e) {
                    mediatype = defaulttypes.get(fname
                            .toLowerCase(Locale.GERMAN).replace(".jpg", "")
                            .replace(".gif", "").replace(".png", ""));
                } catch (IllegalArgumentException e) {
                    mediatype = defaulttypes.get(fname
                            .toLowerCase(Locale.GERMAN).replace(".jpg", "")
                            .replace(".gif", "").replace(".png", ""));
                }
            } else {
                mediatype = defaulttypes.get(fname.toLowerCase(Locale.GERMAN)
                                                  .replace(".jpg", "").replace(".gif", "")
                                                  .replace(".png", ""));
            }
        }

        return mediatype;
    }

    @Override
    protected String getDefaultEncoding() {
        try {
            if (data.has("charset")) {
                return data.getString("charset");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "UTF-8";
    }

    @Override
    public void setLanguage(String language) {
        this.languageCode = language;
    }

    protected String getLang() {
        if (!initialised) {
            return null;
        }
        if (supportedLanguages.contains(languageCode)) {
            return languageCodes.get(languageCode);
        } else if (supportedLanguages.contains("en"))
        // Fall back to English if language not available
        {
            return languageCodes.get("en");
        } else if (supportedLanguages.contains("de"))
        // Fall back to German if English not available
        {
            return languageCodes.get("de");
        } else {
            return null;
        }
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        Set<String> langs = new HashSet<>();
        for (String lang : languageCodes.keySet()) {
            String html = httpGet(opac_url + "/DB=" + db + "/LNG="
                            + languageCodes.get(lang) + "/START_WELCOME",
                    getDefaultEncoding());
            if (!html.contains("MODE_START") && !html.contains("LABEL_")) {
                langs.add(lang);
            }
        }
        return langs;
    }
}
