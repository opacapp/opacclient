/*
 * Copyright (C) 2015 by Rüdiger Wurth, Raphael Michel and contributors under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.apis;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
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
import de.geeksfactory.opacclient.objects.SearchResult.Status;
import de.geeksfactory.opacclient.reporting.Report;
import de.geeksfactory.opacclient.reporting.ReportHandler;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * @author Ruediger Wurth
 */
public class BiBer1992 extends ApacheBaseApi {

    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();

    static {
    }

    // we have to limit num of results because PUSH attribute SHOW=20 does not work:
    // number of results is always 50 which is too much
    final private int numOfResultsPerPage = 20;
    protected boolean newStyleReservations = false;
    private String opacUrl = "";
    private String opacDir = "opac"; // sometimes also "opax"
    private String opacSuffix = ".C"; // sometimes also ".S"
    private JSONObject data;

    // private int m_resultcount = 10;
    // private long logged_in;
    // private Account logged_in_as;
    private List<NameValuePair> nameValuePairs = new ArrayList<>(2);

    /*
     * ----- media types ----- Example Wuerzburg: <td ...><input type="checkbox"
     * name="MT" value="1" ...></td> <td ...><img src="../image/spacer.gif.S"
     * title="Buch"><br>Buch</td>
     *
     * Example Friedrichshafen: <td ...><input type="checkbox" name="MS"
     * value="1" ...></td> <td ...><img src="../image/spacer.gif.S"
     * title="Buch"><br>Buch</td>
     *
     * Example Offenburg: <input type="radio" name="MT" checked
     * value="MTYP0">Alles&nbsp;&nbsp; <input type="radio" name="MT"
     * value="MTYP10">Belletristik&nbsp;&nbsp; Unfortunately Biber miss the end
     * tag </input>, so opt.text() does not work! (at least Offenburg)
     *
     * Example Essen, Aschaffenburg: <input type="radio" name="MT" checked
     * value="MTYP0"><img src="../image/all.gif.S" title="Alles"> <input
     * type="radio" name="MT" value="MTYP7"><img src="../image/cdrom.gif.S"
     * title="CD-ROM">
     *
     * ----- Branches ----- Example Essen,Erkrath: no closing </option> !!!
     * cannot be parsed by Jsoup, so not supported <select name="AORT"> <option
     * value="ZWST1">Altendorf </select>
     *
     * Example Hagen, Würzburg, Friedrichshafen: <select name="ZW" class="sel1">
     * <option selected value="ZWST0">Alle Bibliotheksorte</option> </select>
     */
    @Override
    public List<SearchField> parseSearchFields() throws IOException {
        List<SearchField> fields = new ArrayList<>();

        HttpGet httpget;
        if (opacDir.contains("opax")) {
            httpget = new HttpGet(opacUrl + "/" + opacDir
                    + "/de/qsel.html.S");
        } else {
            httpget = new HttpGet(opacUrl + "/" + opacDir
                    + "/de/qsel_main.S");
        }

        HttpResponse response = http_client.execute(httpget);

        if (response.getStatusLine().getStatusCode() == 500) {
            throw new NotReachableException(response.getStatusLine().getReasonPhrase());
        }
        String html = convertStreamToString(response.getEntity().getContent());
        HttpUtils.consume(response.getEntity());

        Document doc = Jsoup.parse(html);

        // get text fields
        Elements text_opts = doc.select("form select[name=REG1] option");
        for (Element opt : text_opts) {
            TextSearchField field = new TextSearchField();
            field.setId(opt.attr("value"));
            field.setDisplayName(opt.text());
            field.setHint("");
            fields.add(field);
        }

        // get media types
        Elements mt_opts = doc.select("form input[name~=(MT|MS)]");
        if (mt_opts.size() > 0) {
            DropdownSearchField mtDropdown = new DropdownSearchField();
            mtDropdown.setId(mt_opts.get(0).attr("name"));
            mtDropdown.setDisplayName("Medientyp");
            for (Element opt : mt_opts) {
                if (!opt.val().equals("")) {
                    String text = opt.text();
                    if (text.length() == 0) {
                        // text is empty, check layouts:
                        // Essen: <input name="MT"><img title="mediatype">
                        // Schaffenb: <input name="MT"><img alt="mediatype">
                        Element img = opt.nextElementSibling();
                        if (img != null && img.tagName().equals("img")) {
                            text = img.attr("title");
                            if (text.equals("")) {
                                text = img.attr("alt");
                            }
                        }
                    }
                    if (text.length() == 0) {
                        // text is still empty, check table layout, Example
                        // Friedrichshafen
                        // <td><input name="MT"></td> <td><img
                        // title="mediatype"></td>
                        Element td1 = opt.parent();
                        Element td2 = td1.nextElementSibling();
                        if (td2 != null) {
                            Elements td2Children = td2.select("img[title]");
                            if (td2Children.size() > 0) {
                                text = td2Children.get(0).attr("title");
                            }
                        }
                    }
                    if (text.length() == 0) {
                        // text is still empty, check images in label layout, Example
                        // Wiedenst
                        // <input type="radio" name="MT" id="MTYP1" value="MTYP1">
                        // <label for="MTYP1"><img src="http://www.wiedenest.de/bib/image/books
                        // .png" alt="Bücher" title="Bücher"></label>
                        Element label = opt.nextElementSibling();
                        if (label != null) {
                            Elements td2Children = label.select("img[title]");
                            if (td2Children.size() > 0) {
                                text = td2Children.get(0).attr("title");
                            }
                        }
                    }
                    if (text.length() == 0) {
                        // text is still empty: missing end tag like Offenburg
                        text = parse_option_regex(opt);
                    }
                    mtDropdown.addDropdownValue(opt.val(), text);
                }
            }
            fields.add(mtDropdown);
        }

        // get branches
        Elements br_opts = doc.select("form select[name=ZW] option");
        if (br_opts.size() > 0) {
            DropdownSearchField brDropdown = new DropdownSearchField();
            brDropdown.setId(br_opts.get(0).parent().attr("name"));
            brDropdown.setDisplayName(br_opts.get(0).parent().parent()
                                             .previousElementSibling().text().replace("\u00a0", "")
                                             .replace("?", "").trim());
            for (Element opt : br_opts) {
                brDropdown.addDropdownValue(opt.val(), opt.text());
            }
            fields.add(brDropdown);
        }

        Elements sort_opts = doc.select("form select[name=SORTX] option");
        if (sort_opts.size() > 0) {
            DropdownSearchField sortDropdown = new DropdownSearchField();
            sortDropdown.setId(sort_opts.get(0).parent().attr("name"));
            sortDropdown.setDisplayName(sort_opts.get(0).parent().parent()
                                                 .previousElementSibling().text()
                                                 .replace("\u00a0", "")
                                                 .replace("?", "").trim());
            for (Element opt : sort_opts) {
                sortDropdown.addDropdownValue(opt.val(), opt.text());
            }
            fields.add(sortDropdown);
        }

        return fields;
    }

    private static MediaType getMediaTypeFromImageFilename(SearchResult sr, String imagename,
            JSONObject data) {
        String[] fparts1 = imagename.split("/"); // "images/31.gif.S"
        String[] fparts2 = fparts1[fparts1.length - 1].split("\\."); // "31.gif.S"
        String lookup = fparts2[0]; // "31"

        if (imagename.contains("amazon")) {
            if (sr != null) sr.setCover(imagename);
            return null;
        }

        if (data.has("mediatypes")) {
            try {
                String typeStr = data.getJSONObject("mediatypes").getString(
                        lookup);
                return MediaType.valueOf(typeStr);
            } catch (Exception e) {
                if (defaulttypes.containsKey(lookup)) {
                    return defaulttypes.get(lookup);
                }
            }
        } else {
            if (defaulttypes.containsKey(lookup)) {
                return defaulttypes.get(lookup);
            }
        }
        return null;
    }

    /*
     * Parser for non XML compliant html part: (the crazy way) Get text from
     * <input> without end tag </input>
     *
     * Example Offenburg: <input type="radio" name="MT"
     * value="MTYP10">Belletristik&nbsp;&nbsp; Regex1: value="MTYP10".*?>([^<]+)
     */
    private String parse_option_regex(Element inputTag) {
        String optStr = inputTag.val();
        String html = inputTag.parent().html();
        String result = optStr;

        String regex1 = "value=\"" + optStr + "\".*?>([^<]+)";
        String[] regexList = new String[]{regex1};

        for (String regex : regexList) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                result = matcher.group(1);
                result = result.replaceAll("&nbsp;", " ").trim();
                break;
            }
        }

        return result;
    }

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);

        data = lib.getData();

        try {
            opacUrl = data.getString("baseurl");
            opacDir = data.getString("opacdir");
            opacSuffix = data.optString("opacsuffix", ".C");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queryList)
            throws IOException {

        if (!initialised) {
            start();
        }

        nameValuePairs.clear();
        int count = 1;
        for (SearchQuery query : queryList) {
            if ((query.getSearchField() instanceof TextSearchField || query
                    .getSearchField() instanceof BarcodeSearchField)
                    && !query.getValue().equals("")) {
                nameValuePairs.add(new BasicNameValuePair("CNN" + count,
                        "AND"));
                nameValuePairs.add(new BasicNameValuePair("FLD" + count,
                        query.getValue()));
                nameValuePairs.add(new BasicNameValuePair("REG" + count,
                        query.getKey()));
                count++;
            } else if (query.getSearchField() instanceof DropdownSearchField) {
                nameValuePairs.add(new BasicNameValuePair(query.getKey(),
                        query.getValue()));
            }
        }

        nameValuePairs.add(new BasicNameValuePair("FUNC", "qsel"));
        nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
        nameValuePairs.add(new BasicNameValuePair("SHOW", "20")); // but
        // result
        // gives 50
        nameValuePairs.add(new BasicNameValuePair("SHOWSTAT", "N"));
        nameValuePairs.add(new BasicNameValuePair("FROMPOS", "1"));

        return searchGetPage(1);
    }

    /*
     * (non-Javadoc)
     *
     * @see OpacApi#searchGetPage(int)
     */
    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException {

        int startNum = (page - 1) * numOfResultsPerPage + 1;

        // remove last element = "FROMPOS", and add a new one
        nameValuePairs.remove(nameValuePairs.size() - 1);
        nameValuePairs.add(new BasicNameValuePair("FROMPOS", String
                .valueOf(startNum)));

        String html = httpPost(opacUrl + "/" + opacDir + "/query" + opacSuffix,
                new UrlEncodedFormEntity(nameValuePairs),
                getDefaultEncoding());
        return parse_search(html, page);
    }

    /*
     * result table format: JSON "rows_per_hit" = 1: One <tr> per hit JSON
     * "rows_per_hit" = 2: Two <tr> per hit (default) <form> <table> <tr
     * valign="top"> <td class="td3" ...><a href=...><img ...></a></td> (row is
     * optional, only in some bibs) <td class="td2" ...><input ...></td> <td
     * width="34%">TITEL</td> <td width="34%">&nbsp;</td> <td width="6%"
     * align="center">2009</td> <td width="*" align="left">DVD0 Seew</td> </tr>
     * <tr valign="top"> <td class="td3" ...>&nbsp;...</td> <td class="td2"
     * ...>&nbsp;...</td> <td colspan="4" ...><font size="-1"><font
     * class="p1">Erwachsenenbibliothek</font></font><div
     * class="hr4"></div></td> </tr>
     */
    private SearchRequestResult parse_search(String html, int page) {
        List<SearchResult> results = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        if (doc.select("h3").text().contains("Es wurde nichts gefunden")) {
            return new SearchRequestResult(results, 0, page);
        }

        Elements trList = doc.select("form table tr[valign]"); // <tr
        // valign="top">
        if (trList.size() == 0) { // Schwieberdingen
            trList = doc.select("table:has(input[type=checkbox]) tr");
        }
        Elements elem;
        int rows_per_hit = 2;
        if (trList.size() == 1
                || (trList.size() > 1
                && trList.get(0).select("input[type=checkbox]").size() > 0 && trList
                .get(1).select("input[type=checkbox]").size() > 0)) {
            rows_per_hit = 1;
        }

        try {
            rows_per_hit = data.getInt("rows_per_hit");
        } catch (JSONException e) {
        }

        // Overall search results
        // are very differently layouted, but have always the text:
        // "....Treffer Gesamt (nnn)"
        int results_total;
        Pattern pattern = Pattern.compile("Treffer Gesamt \\(([0-9]+)\\)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            results_total = Integer.parseInt(matcher.group(1));
        } else {
            results_total = -1;
        }

        // limit to 20 entries
        int numOfEntries = trList.size() / rows_per_hit; // two rows per entry
        if (numOfEntries > numOfResultsPerPage) {
            numOfEntries = numOfResultsPerPage;
        }

        for (int i = 0; i < numOfEntries; i++) {
            Element tr = trList.get(i * rows_per_hit);
            SearchResult sr = new SearchResult();

            // ID as href tag
            elem = tr.select("td a");
            if (elem.size() > 0 && !elem.get(0).attr("href").contains("ISBN")) {
                // Exclude the cover links in Ludwigsburg as they lead to a page that misses the
                // reservation button
                String hrefID = elem.get(0).attr("href");
                sr.setId(hrefID);
            } else {
                // no ID as href found, look for the ID in the input form
                elem = tr.select("td input");
                if (elem.size() > 0) {
                    String nameID = elem.get(0).attr("name").trim();
                    String hrefID = "/" + opacDir
                            + "/ftitle" + opacSuffix + "?LANG=de&FUNC=full&" + nameID + "=YES";
                    sr.setId(hrefID);
                }
            }

            // media type
            elem = tr.select("td img");
            if (elem.size() > 0) {
                sr.setType(getMediaTypeFromImageFilename(sr, elem.get(0).attr("src"), data));
            }

            // description
            String desc = "";
            try {
                // array "searchtable" list the column numbers of the
                // description
                JSONArray searchtable = data.getJSONArray("searchtable");
                for (int j = 0; j < searchtable.length(); j++) {
                    int colNum = searchtable.getInt(j);
                    if (j > 0) {
                        desc = desc + "<br />";
                    }
                    String c = tr.child(colNum).html();
                    if (tr.child(colNum).childNodes().size() == 1 &&
                            tr.child(colNum).select("a[href*=ftitle.]").size() > 0) {
                        c = tr.select("a[href*=ftitle.]").text();
                    }
                    desc = desc + c;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // remove links "<a ...>...</a>
            // needed for Friedrichshafen: "Warenkorb", "Vormerkung"
            // Herford: "Medienkorb"
            desc = desc.replaceAll("<a .*?</a>", "");
            // remove newlines (useless in HTML)
            desc = desc.replaceAll("\\n", "");
            // remove hidden divs ("Titel übernommen!" in Wuerzburg)
            desc = desc.replaceAll("<div[^>]*style=\"display:none\">.*</div>", "");
            // remove all invalid HTML tags
            desc = desc.replaceAll("</?(tr|td|font|table|tbody|div)[^>]*>", "");
            // replace multiple line breaks by one
            desc = desc.replaceAll("(<br( /)?>\\s*)+", "<br>");
            sr.setInnerhtml(desc);

            if (tr.select("font.p04x09b").size() > 0
                    && tr.select("font.p02x09b").size() == 0) {
                sr.setStatus(Status.GREEN);
            } else if (tr.select("font.p04x09b").size() == 0
                    && tr.select("font.p02x09b").size() > 0) {
                sr.setStatus(Status.RED);
            } else if (tr.select("font.p04x09b").size() > 0
                    && tr.select("font.p02x09b").size() > 0) {
                sr.setStatus(Status.YELLOW);
            }

            // number
            sr.setNr(i / rows_per_hit);
            results.add(sr);
        }

        // m_resultcount = results.size();
        return new SearchRequestResult(results, results_total, page);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * OpacApi#getResultById(java.lang.String)
     */
    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException {
        if (!initialised) {
            start();
        }

        if (!id.contains("ftitle")) {
            id = "ftitle" + opacSuffix + "?LANG=de&FUNC=full&" + id + "=YES";
        }
        // normally full path like
        // "/opac/ftitle.C?LANG=de&FUNC=full&331313252=YES"
        // but sometimes (Wuerzburg) "ftitle.C?LANG=de&FUNC=full&331313252=YES"
        // and sometimes (Hagen) absolute URL including opac_url
        if (id.startsWith(opacUrl)) {
            id = id.substring(opacUrl.length());
        } else if (!id.startsWith("/")) {
            id = "/" + opacDir + "/" + id;
        }


        HttpGet httpget = new HttpGet(opacUrl + id);

        HttpResponse response = http_client.execute(httpget);

        String html = convertStreamToString(response.getEntity().getContent());
        HttpUtils.consume(response.getEntity());

        return parse_result(html);
    }

    /*
     * (non-Javadoc)
     *
     * @see OpacApi#getResult(int)
     */
    @Override
    public DetailedItem getResult(int position) throws IOException {
        // not needed, normall all search results should have an ID,
        // so getResultById() is called
        return null;
    }

    /*
     * Two-column table inside of a form 1st column is category, e.g.
     * "Verfasser" 2nd column is content, e.g. "Bach, Johann Sebastian" In some
     * rows, the 1st column is empty, then 2nd column is continued text from row
     * above.
     *
     * Some libraries have a second section for the copies in stock (Exemplare).
     * This 2nd section has reverse layout.
     *
     * |-------------------| | Subject | Content | |-------------------| |
     * Subject | Content | |-------------------| | | Content |
     * |-------------------| | Subject | Content |
     * |-------------------------------------------------| | | Site | Signatur|
     * ID | State | |-------------------------------------------------| | |
     * Content | Content | Content | Content |
     * |-------------------------------------------------|
     */
    private DetailedItem parse_result(String html) {
        DetailedItem item = new DetailedItem();

        Document document = Jsoup.parse(html);

        Elements rows = document.select("html body form table tr");
        // Elements rows = document.select("html body div form table tr");

        // Element rowReverseSubject = null;
        Detail detail = null;

        // prepare copiestable
        Copy copy_last_content = null;
        int copy_row = 0;

        String[] copy_keys = new String[]{"barcode",
                "branch",
                "department",
                "location",
                "status",
                "returndate",
                "reservations"
        };
        int[] copy_map = new int[]{3, 1, -1, 1, 4, -1, -1};

        try {
            JSONObject map = data.getJSONObject("copiestable");
            for (int i = 0; i < copy_keys.length; i++) {
                if (map.has(copy_keys[i])) {
                    copy_map[i] = map.getInt(copy_keys[i]);
                }
            }
        } catch (Exception e) {
            // "copiestable" is optional
        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        // go through all rows
        for (Element row : rows) {
            Elements columns = row.children();

            if (columns.size() == 2) {
                // HTML tag "&nbsp;" is encoded as 0xA0
                String firstColumn = columns.get(0).text()
                                            .replace("\u00a0", " ").trim();
                String secondColumn = columns.get(1).text()
                                             .replace("\u00a0", " ").trim();

                if (firstColumn.length() > 0) {
                    // 1st column is category
                    if (firstColumn.equalsIgnoreCase("titel")) {
                        detail = null;
                        item.setTitle(secondColumn);
                    } else {

                        if (secondColumn.contains("hier klicken")
                                && columns.get(1).select("a").size() > 0) {
                            secondColumn += " "
                                    + columns.get(1).select("a").first()
                                             .attr("href");
                        }

                        detail = new Detail(firstColumn, secondColumn);
                        item.getDetails().add(detail);
                    }
                } else {
                    // 1st column is empty, so it is an extension to last
                    // category
                    if (detail != null) {
                        String content = detail.getContent() + "\n"
                                + secondColumn;
                        detail.setContent(content);
                    } else {
                        // detail==0, so it's the first row
                        // check if there is an amazon image
                        if (columns.get(0).select("a img[src]").size() > 0) {
                            item.setCover(columns.get(0).select("a img")
                                                 .first().attr("src"));
                        }

                    }
                }
            } else if (columns.size() > 3) {
                // This is the second section: the copies in stock ("Exemplare")
                // With reverse layout: first row is headline, skipped via
                // (copy_row > 0)
                if (copy_row > 0) {
                    Copy copy = new Copy();
                    for (int j = 0; j < copy_keys.length; j++) {
                        int col = copy_map[j];
                        if (col > -1) {
                            String text = "";
                            if (copy_keys[j].equals("branch")) {
                                // for "Standort" only use ownText() to suppress
                                // Link "Wegweiser"
                                text = columns.get(col).ownText()
                                              .replace("\u00a0", " ").trim();
                            }
                            if (text.length() == 0) {
                                // text of children
                                text = columns.get(col).text()
                                              .replace("\u00a0", " ").trim();
                            }
                            if (text.length() == 0) {
                                // empty table cell, take the one above
                                // this is sometimes the case for "Standort"
                                if (copy_keys[j].equals("status")) {
                                    // but do it not for Status
                                    text = " ";
                                } else {
                                    if (copy_last_content != null) {
                                        text = copy_last_content
                                                .get(copy_keys[j]);
                                    } else {
                                        text = "";
                                    }
                                }
                            }
                            if (copy_keys[j].equals("reservations")) {
                                text = text.replace("Vorgemerkt: ", "")
                                           .replace("Vorbestellt: ", "");
                            }
                            try {
                                copy.set(copy_keys[j], text, fmt);
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (copy.getBranch() != null
                            && copy.getLocation() != null
                            && copy.getLocation().equals(copy.getBranch())) {
                        copy.setLocation(null);
                    }
                    item.addCopy(copy);
                    copy_last_content = copy;
                }// ignore 1st row
                copy_row++;

            }// if columns.size
        }// for rows

        item.setReservable(true); // We cannot check if media is reservable

        if (opacDir.contains("opax")) {
            if (document.select("input[type=checkbox]").size() > 0) {
                item.setReservation_info(document
                        .select("input[type=checkbox]").first().attr("name"));
            } else if (document.select("a[href^=reserv" + opacSuffix + "]").size() > 0) {
                String href = document.select("a[href^=reserv" + opacSuffix + "]").first()
                                      .attr("href");
                item.setReservation_info(href.substring(href.indexOf("resF_")));
            } else {
                item.setReservable(false);
            }
        } else {
            item.setReservation_info(document.select("input[name=ID]").attr(
                    "value"));
        }
        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * OpacApi#reservation(java.lang.String,
     * de.geeksfactory.opacclient.objects.Account, int, java.lang.String)
     */
    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        String resinfo = item.getReservation_info();
        if (selection == null || selection.equals("confirmed")) {
            // STEP 1: Check if reservable and select branch ("ID1")

            // Differences between opax and opac
            String func = opacDir.contains("opax") ? "sigl" : "resF";
            String id = opacDir.contains("opax") ? (resinfo.contains("resF") ? resinfo
                    .substring(5) + "=" + resinfo
                    : resinfo + "=resF_" + resinfo)
                    : "ID=" + resinfo;

            String html = httpGet(opacUrl + "/" + opacDir
                            + "/reserv" + opacSuffix + "?LANG=de&FUNC=" + func + "&" + id,
                    getDefaultEncoding());
            Document doc = Jsoup.parse(html);
            newStyleReservations = doc
                    .select("input[name=" + resinfo.replace("resF_", "") + "]")
                    .val().length() > 4;
            Elements optionsElements = doc.select("select[name=ID1] option");
            if (optionsElements.size() > 0) {
                List<Map<String, String>> options = new ArrayList<>();
                for (Element option : optionsElements) {
                    if ("0".equals(option.attr("value"))) {
                        continue;
                    }
                    Map<String, String> selopt = new HashMap<>();
                    selopt.put("key", option.attr("value") + ":" + option.text());
                    selopt.put("value", option.text());
                    options.add(selopt);
                }
                if (options.size() > 1) {
                    ReservationResult res = new ReservationResult(
                            MultiStepResult.Status.SELECTION_NEEDED);
                    res.setActionIdentifier(ReservationResult.ACTION_BRANCH);
                    res.setSelection(options);
                    return res;
                } else {
                    return reservation(item, account, useraction, options.get(0).get("key"));
                }
            } else {
                ReservationResult res = new ReservationResult(
                        MultiStepResult.Status.ERROR);
                res.setMessage("Dieses Medium ist nicht reservierbar.");
                return res;
            }
        } else {
            // STEP 2: Reserve
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
            nameValuePairs.add(new BasicNameValuePair("BENUTZER", account
                    .getName()));
            nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
                    .getPassword()));
            nameValuePairs.add(new BasicNameValuePair("FUNC", "vors"));
            if (opacDir.contains("opax")) {
                nameValuePairs.add(new BasicNameValuePair(resinfo.replace(
                        "resF_", ""), "vors"
                        + (newStyleReservations ? resinfo.replace("resF_", "")
                        : "")));
            }
            if (newStyleReservations) {
                nameValuePairs.add(new BasicNameValuePair("ID11", selection
                        .split(":")[1]));
            }
            nameValuePairs.add(new BasicNameValuePair("ID1", selection
                    .split(":")[0]));

            String html = httpPost(opacUrl + "/" + opacDir
                            + "/setreserv" + opacSuffix, new UrlEncodedFormEntity(nameValuePairs),
                    getDefaultEncoding());

            Document doc = Jsoup.parse(html);
            if (doc.select(".tab21 .p44b, .p2").text().contains("eingetragen")) {
                return new ReservationResult(MultiStepResult.Status.OK);
            } else {
                ReservationResult res = new ReservationResult(
                        MultiStepResult.Status.ERROR);
                if (doc.select(".p1, .p22b").size() > 0) {
                    res.setMessage(doc.select(".p1, .p22b").text());
                }
                return res;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * OpacApi#prolong(de.geeksfactory.opacclient
     * .objects.Account, java.lang.String)
     *
     * Offenburg, prolong negative result: <table border="1" width="100%"> <tr>
     * <th ...>Nr</th> <th ...>Signatur / Kurztitel</th> <th
     * ...>F&auml;llig</th> <th ...>Status</th> </tr> <tr> <td
     * ...>101103778</td> <td ...>Hyde / Hyde, Anthony: Der Mann aus </td> <td
     * ...>09.04.2013</td> <td ...><font class="p1">verl&auml;ngerbar ab
     * 03.04.13, nicht verl&auml;ngert</font> <br>Bitte wenden Sie sich an Ihre
     * Bibliothek!</td> </tr> </table>
     *
     * Offenburg, prolong positive result: TO BE DESCRIBED
     */
    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String Selection) throws IOException {

        String command;

        // prolong media via http POST
        // Offenburg: URL is .../opac/verl.C
        // Hagen: URL is .../opax/renewmedia.C
        if (opacDir.contains("opax")) {
            command = "/renewmedia" + opacSuffix;
        } else {
            command = "/verl" + opacSuffix;
        }

        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair(media, "YES"));
        nameValuePairs
                .add(new BasicNameValuePair("BENUTZER", account.getName()));
        nameValuePairs.add(new BasicNameValuePair("FUNC", "verl"));
        nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
        nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
                .getPassword()));

        String html = httpPost(opacUrl + "/" + opacDir + command,
                new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());
        if (html.contains("no such key")) {
            html = httpPost(
                    opacUrl + "/" + opacDir + command.replace(".C", ".S"),
                    new UrlEncodedFormEntity(nameValuePairs),
                    getDefaultEncoding());
        }

        Document doc = Jsoup.parse(html);

        // Check result:
        // First we look for a cell with text "Status"
        // and store the column number
        // Then we look in the rows below at this column if
        // we find any text. Stop at first text we find.
        // This text must start with "verl�ngert"
        Elements rowElements = doc.select("table tr");

        int statusCol = -1; // Status column not yet found

        // rows loop
        for (int i = 0; i < rowElements.size(); i++) {
            Element tr = rowElements.get(i);
            Elements tdList = tr.children(); // <th> or <td>

            // columns loop
            for (int j = 0; j < tdList.size(); j++) {
                String cellText = tdList.get(j).text().trim();

                if (statusCol < 0) {
                    // we look for cell with text "Status"
                    if (cellText.equals("Status")) {
                        statusCol = j;
                        break; // next row
                    }
                } else {
                    // we look only at Status column
                    // In "Hagen", there are some extra empty rows below
                    if ((j == statusCol) && (cellText.length() > 0)) {
                        // Status found
                        if (cellText.matches("verl.ngert.*")) {
                            return new ProlongResult(MultiStepResult.Status.OK);
                        } else {
                            return new ProlongResult(
                                    MultiStepResult.Status.ERROR, cellText);
                        }
                    }
                }
            }// for columns
        }// for rows

        return new ProlongResult(MultiStepResult.Status.ERROR, "unknown result");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * OpacApi#cancel(de.geeksfactory.opacclient
     * .objects.Account, java.lang.String)
     */
    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
        nameValuePairs.add(new BasicNameValuePair("FUNC", "vorl"));
        if (opacDir.contains("opax")) {
            nameValuePairs.add(new BasicNameValuePair("BENUTZER", account
                    .getName()));
            nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
                    .getPassword()));
        }
        nameValuePairs.add(new BasicNameValuePair(media, "YES"));

        String action =
                opacDir.contains("opax") ? "/delreserv" + opacSuffix : "/vorml" + opacSuffix;

        String html = httpPost(opacUrl + "/" + opacDir + action,
                new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());

        Document doc = Jsoup.parse(html);
        if (doc.select(".tab21 .p44b, .p2").text().contains("Vormerkung wurde")) {
            return new CancelResult(MultiStepResult.Status.OK);
        } else {
            return new CancelResult(MultiStepResult.Status.ERROR);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * OpacApi#account(de.geeksfactory.opacclient
     * .objects.Account)
     *
     * POST-format: BENUTZER xxxxxxxxx FUNC medk LANG de PASSWORD ddmmyyyy
     */
    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {

        AccountData res = new AccountData(account.getId());

        // get media
        List<LentItem> media = accountGetMedia(account, res);
        res.setLent(media);

        // get reservations
        List<ReservedItem> reservations = accountGetReservations(account);
        res.setReservations(reservations);

        return res;
    }

    private List<LentItem> accountGetMedia(Account account, AccountData res) throws IOException, JSONException,
            OpacErrorException {
        // get media list via http POST
        Document doc = accountHttpPost(account, "medk");

        return parseMediaList(res, account, doc, data, reportHandler,
                loadJsonResource("/biber1992/headers_lent.json"));
    }

    static List<LentItem> parseMediaList(AccountData res, Account account, Document doc,
            JSONObject data, ReportHandler reportHandler, JSONObject headers_lent)
            throws JSONException {
        List<LentItem> media = new ArrayList<>();
        if (doc == null) {
            return media;
        }

        if (doc.select("form[name=medkl] table").size() == 0){
            return new ArrayList<LentItem>();
        }

        // parse result list
        Map<String, Integer> copymap = new HashMap<>();
        Map<String, Integer> colspanmap = new HashMap<>();
        Elements headerCells = doc.select("form[name=medkl] table tr:has(th)").last().select("th");
        JSONArray headersList = new JSONArray();
        JSONArray unknownHeaders = new JSONArray();
        int j = 0;
        for (Element headerCell : headerCells) {
            String header = headerCell.text();

            String colspan_str = headerCell.attr("colspan");
            int colspan = 1;
            if (!colspan_str.equals("")) {
                colspan = Integer.valueOf(colspan_str);
            }

            headersList.put(header);
            if (headers_lent.has(header)) {
                if (!headers_lent.isNull(header)) {
                    copymap.put(headers_lent.getString(header), j);
                    colspanmap.put(headers_lent.getString(header), colspan);
                }
            } else {
                unknownHeaders.put(header);
            }

            j += colspan;
        }

        if (unknownHeaders.length() > 0) {
            // send report
            JSONObject reportData = new JSONObject();
            reportData.put("headers", headersList);
            reportData.put("unknown_headers", unknownHeaders);
            Report report = new Report(account.getLibrary(), "biber1992", "unknown header - lent",
                    DateTime.now(), reportData);
            reportHandler.sendReport(report);

            // fallback to JSON
            JSONObject accounttable = data.getJSONObject("accounttable");
            copymap = jsonToMap(accounttable);
        }

        Pattern expire = Pattern.compile("Ausweisg.ltigkeit: ([0-9.]+)");
        Pattern fees = Pattern.compile("([0-9,.]+) .");
        for (Element td : doc.select(".td01x09n")) {
            String text = td.text().trim();
            if (expire.matcher(text).matches()) {
                res.setValidUntil(expire.matcher(text).replaceAll("$1"));
            } else if (fees.matcher(text).matches()) {
                res.setPendingFees(text);
            }
        }
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        Elements rowElements = doc.select("form[name=medkl] table tr");

        // rows: skip 1st row -> title row
        for (int i = 1; i < rowElements.size(); i++) {
            Element tr = rowElements.get(i);
            if (tr.child(0).tagName().equals("th")) {
                continue;
            }
            LentItem item = new LentItem();

            Elements mediatypeImg = tr.select("td img");
            if (mediatypeImg.size() > 0) {
                item.setMediaType(getMediaTypeFromImageFilename(
                        null, mediatypeImg.get(0).attr("src"), data));
            }

            Pattern itemIdPat = Pattern
                    .compile("javascript:(?:smAcc|smMedk)\\('[a-z]+','[a-z]+','([A-Za-z0-9]+)'\\)");
            // columns: all elements of one media
            for (Map.Entry<String, Integer> entry : copymap.entrySet()) {
                String key = entry.getKey();
                int index = entry.getValue();
                if (tr.child(index).select("a").size() == 1) {
                    Matcher matcher = itemIdPat.matcher(tr.child(index)
                                                          .select("a").attr("href"));
                    if (matcher.find()) item.setId(matcher.group(1));
                }

                String value = tr.child(index).text().trim().replace("\u00A0", "");
                if (colspanmap.get(key) > 1) {
                    for (int k = 1; k < colspanmap.get(key); k++) {
                        value = value + " " + tr.child(index + k).text().trim().replace("\u00A0", "");
                    }
                    value = value.trim();
                }

                switch (key) {
                    case "author+title":
                        item.setTitle(findTitleAndAuthor(value)[0]);
                        item.setAuthor(findTitleAndAuthor(value)[1]);
                        continue;
                    case "returndate":
                        try {
                            value = fmt.parseLocalDate(value).toString();
                        } catch (IllegalArgumentException e1) {
                            e1.printStackTrace();
                        }
                        break;
                    case "renewals_number":
                    case "status":
                        if (value != null && value.length() != 0) {
                            if (item.getStatus() == null) {
                                item.setStatus(value);
                            } else {
                                item.setStatus(item.getStatus() + ", " + value);
                            }
                        }
                        continue;
                }

                if (value != null && value.length() != 0) item.set(key, value);
            }

            if (tr.select("input[type=checkbox][value=YES]").size() > 0) {
                item.setProlongData(tr.select("input[type=checkbox][value=YES]").attr("name"));
            }

            media.add(item);
        }
        return media;
    }

    private List<ReservedItem> accountGetReservations(Account account)
            throws IOException, JSONException, OpacErrorException {
        // get reservations list via http POST
        Document doc = accountHttpPost(account, "vorm");

        return parseResList(account, doc, data, reportHandler,
                loadJsonResource("/biber1992/headers_reservations.json"));
    }

    static List<ReservedItem> parseResList(Account account, Document doc, JSONObject data,
            ReportHandler reportHandler, JSONObject headers_reservations)
            throws JSONException {
        List<ReservedItem> reservations = new ArrayList<>();
        if (doc == null) {
            // error message as html result
            return reservations;
        }
        if (doc.select("form[name=vorml] table").size() == 0){
            return new ArrayList<ReservedItem>();
        }

        // parse result list
        Map<String, Integer> copymap = new HashMap<>();
        Elements headerCells = doc.select("form[name=vorml] table tr:has(th)").last().select("th");
        JSONArray headersList = new JSONArray();
        JSONArray unknownHeaders = new JSONArray();
        int j = 0;
        for (Element headerCell : headerCells) {
            String header = headerCell.text();
            headersList.put(header);
            if (headers_reservations.has(header)) {
                if (!headers_reservations.isNull(header)) {
                    copymap.put(headers_reservations.getString(header), j);
                }
            } else {
                unknownHeaders.put(header);
            }

            String colspan = headerCell.attr("colspan");
            j += !colspan.equals("") ? Integer.valueOf(colspan) : 1;
        }

        if (unknownHeaders.length() > 0) {
            // send report
            JSONObject reportData = new JSONObject();
            reportData.put("headers", headersList);
            reportData.put("unknown_headers", unknownHeaders);
            Report report =
                    new Report(account.getLibrary(), "biber1992", "unknown header - reservations",
                            DateTime.now(), reportData);
            reportHandler.sendReport(report);

            // fallback to JSON
            JSONObject reservationtable;
            if (data.has("reservationtable")) {
                reservationtable = data.getJSONObject("reservationtable");
            } else {
                // reservations not specifically supported, let's just try it
                // with default values but fail silently
                reservationtable = new JSONObject();
                reservationtable.put("author", 3);
                reservationtable.put("availability", 6);
                reservationtable.put("branch", -1);
                reservationtable.put("cancelurl", -1);
                reservationtable.put("expirationdate", 5);
                reservationtable.put("title", 3);
            }
            copymap = jsonToMap(reservationtable);
        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        Elements rowElements = doc.select("form[name=vorml] table tr");

        // rows: skip 1st row -> title row
        for (int i = 1; i < rowElements.size(); i++) {
            Element tr = rowElements.get(i);
            if (tr.child(0).tagName().equals("th")) {
                continue;
            }
            ReservedItem item = new ReservedItem();

            item.setCancelData(tr.select("input[type=checkbox]").attr("name"));

            Elements mediatypeImg = tr.select("td img");
            if (mediatypeImg.size() > 0) {
                item.setMediaType(getMediaTypeFromImageFilename(
                        null, mediatypeImg.get(0).attr("src"), data));
            }

            // columns: all elements of one media
            for (Map.Entry<String, Integer> entry : copymap.entrySet()) {
                String key = entry.getKey();
                int index = entry.getValue();
                String value = tr.child(index).text().trim();

                switch (key) {
                    case "author+title":
                        item.setTitle(findTitleAndAuthor(value)[0]);
                        item.setAuthor(findTitleAndAuthor(value)[1]);
                        continue;
                    case "availability":
                        try {
                            value = fmt.parseLocalDate(value).toString();
                        } catch (IllegalArgumentException e1) {
                            key = "status";
                        }
                        break;
                    case "expirationdate":
                        try {
                            value = fmt.parseLocalDate(value).toString();
                        } catch (IllegalArgumentException e1) {
                            key = "status";
                        }
                        break;
                }

                if (value != null && value.length() != 0) {
                    item.set(key, value);
                }
            }
            reservations.add(item);
        }
        return reservations;
    }

    /*
     * BiBer returns titles, authors and call numbers all in one field and cuts them of after a
     * fixed length. The exact formats differ greatly.
     *
     * Examples:
     *
     * Author: Title
     * Callnumber / Title
     * Callnumber / Author: Title
     *
     * Note that magazine titles might contain slashes as well, e.g. "Android Welt 3/15 Mai-Juni"
     */
    private static Pattern PATTERN_TITLE_AUTHOR =
            Pattern.compile("(?:" +                     // Start matching the call number
                    "[^/]+" +                           // The call number itself
                    // A slash is only considered a separator between call number if it
                    // isn't surrounded by digits (e.g. 2/12 in a magazine title)
                    "(?<![0-9])/(?![0-9]{2})" +
                    ")?" +                              // Signature is optional

                    "(?:" +                             // Start matching the author
                    "([^:]+)" +                         // The author itself
                    // The author is separated form the title by a colon
                    ":" +
                    ")?" +                              // Author is optional

                    // Everything else is considered to be part of the title
                    "(.*)");

    public static String[] findTitleAndAuthor(String value) {
        Matcher m = PATTERN_TITLE_AUTHOR.matcher(value);
        if (m.matches()) {
            return new String[]{m.group(2) != null ? m.group(2).trim() : null,
                    m.group(1) != null ? m.group(1).trim() : null};
        } else {
            return new String[]{null, null};
        }
    }

    private Document accountHttpPost(Account account, String func)
            throws IOException, OpacErrorException {
        // get media list via http POST
        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("FUNC", func));
        nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
        nameValuePairs
                .add(new BasicNameValuePair("BENUTZER", account.getName()));
        nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
                .getPassword()));

        String html = httpPost(opacUrl + "/" + opacDir + "/user.C",
                new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());

        Document doc = Jsoup.parse(html);

        // Error recognition
        // <title>OPAC Fehler</title>
        if (doc.title().contains("Fehler")
                || (doc.select("h2").size() > 0 && doc.select("h2").text()
                                                      .contains("Fehler"))) {
            String errText = "unknown error";
            Elements elTable = doc.select("table");
            if (elTable.size() > 0) {
                errText = elTable.get(0).text();
            }
            throw new OpacErrorException(errText);
        }
        if (doc.select("tr td font[color=red]").size() == 1) {
            // Jena: Onleihe advertisement recognized as error message
            if (!doc.select("tr td font[color=red]").text()
                    .contains("Ausleihe per Download rund um die Uhr")) {
                throw new OpacErrorException(doc.select("font[color=red]").text());
            }
        }
        if (doc.text().contains("No html file set")
                || doc.text().contains("Der BIBDIA Server konnte den Auftrag nicht")
                || doc.text().contains("Fehler in der Ausf")) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.WRONG_LOGIN_DATA));
        }

        return doc;
    }

    @Override
    public String getShareUrl(String id, String title) {
        // id is normally full path like
        // "/opac/ftitle.C?LANG=de&FUNC=full&331313252=YES"
        // but sometimes (Wuerzburg) "ftitle.C?LANG=de&FUNC=full&331313252=YES"
        if (!id.startsWith("/")) {
            id = "/" + opacDir + "/" + id;
        }

        return opacUrl + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        Document doc = accountHttpPost(account, "medk");
        if (doc == null) {
            throw new NotReachableException("Account document was null");
        }
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
