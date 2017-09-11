/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
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

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * API für Web-Opacs von Zones mit dem Hinweis "Zones.2.2.45.xx" oder "ZONES v1.8.1" im Footer.
 *
 * TODO: Kontofunktionen für Zones 1.8.1
 */
public class Zones extends ApacheBaseApi {

    private static HashMap<String, MediaType> defaulttypes = new HashMap<>();

    static {
        // Zones 2.2
        defaulttypes.put("Buch", MediaType.BOOK);
        defaulttypes.put("Buch/Druckschrift", MediaType.BOOK);
        defaulttypes.put("Buch Erwachsene", MediaType.BOOK);
        defaulttypes.put("Buch Kinder/Jugendliche", MediaType.BOOK);
        defaulttypes.put("Buch Kinder/Jugend", MediaType.BOOK);
        defaulttypes.put("Kinder-Buch", MediaType.BOOK);
        defaulttypes.put("DVD", MediaType.DVD);
        defaulttypes.put("Kinder-DVD", MediaType.DVD);
        defaulttypes.put("Konsolenspiele", MediaType.GAME_CONSOLE);
        defaulttypes.put("Blu-ray Disc", MediaType.BLURAY);
        defaulttypes.put("Compact Disc", MediaType.CD);
        defaulttypes.put("CD-ROM", MediaType.CD_SOFTWARE);
        defaulttypes.put("Kinder-CD", MediaType.CD_SOFTWARE);
        defaulttypes.put("Noten", MediaType.SCORE_MUSIC);
        defaulttypes.put("Note", MediaType.SCORE_MUSIC);
        defaulttypes.put("Zeitschrift, Heft", MediaType.MAGAZINE);
        defaulttypes.put("Zeitschrift", MediaType.MAGAZINE);
        defaulttypes.put("E-Book", MediaType.EBOOK);
        defaulttypes.put("CDROM", MediaType.CD_SOFTWARE);
        defaulttypes.put("E-Audio", MediaType.MP3);
        defaulttypes.put("CD", MediaType.CD);
        defaulttypes.put("eBook", MediaType.EBOOK);
        defaulttypes.put("ePaper", MediaType.EBOOK);
        defaulttypes.put("Plan, Karte", MediaType.MAP);

        // Zones 1.8.1 (.gif file names)
        defaulttypes.put("book", MediaType.BOOK);
        defaulttypes.put("cd", MediaType.CD);
        defaulttypes.put("video", MediaType.MOVIE);
        defaulttypes.put("serial", MediaType.MAGAZINE);
    }

    // Indicates whether the OPAC uses ZONES 1.8.1 (instead of 2.2)
    private boolean version18;
    private String opac_url = "";
    private JSONObject data;
    private int page;
    private String searchobj;
    private String accountobj;

    @Override
    public List<SearchField> parseSearchFields() throws
            IOException {
        if (!initialised) start();
        List<SearchField> fields = new ArrayList<>();
        String html = httpGet(
                opac_url
                        +
                        "/APS_ZONES?fn=AdvancedSearch&Style=Portal3&SubStyle=&Lang=GER" +
                        "&ResponseEncoding=utf-8",
                getDefaultEncoding());

        Document doc = Jsoup.parse(html);

        // find text fields
        Elements txt_opts = doc.select("#formSelectTerm_1 option");
        for (Element opt : txt_opts) {
            TextSearchField field = new TextSearchField();
            field.setId(opt.attr("value"));
            field.setHint("");
            field.setDisplayName(opt.text());
            fields.add(field);
        }

        // find filters
        String filtersQuery = version18 ? ".inSearchLimits .floatingBox" : ".TabRechAv .limitBlock";
        Elements filters = doc.select(filtersQuery);
        int i = 0;
        for (Element filter : filters) {
            DropdownSearchField dropdown = new DropdownSearchField();
            dropdown.addDropdownValue("", "Alle");
            // All dropdowns use "q.limits.limit" as URL param, but they must not have the same ID
            dropdown.setId("dropdown_" + i);

            if (version18) {
                dropdown.setDisplayName(filter.select("tr").get(0).text().trim());
                Elements opts = filter.select("tr").get(1).select("table td:has(input)");
                for (Element opt : opts) {
                    dropdown.addDropdownValue(opt.select("input").attr("value"), opt.text().trim());
                }
            } else {
                dropdown.setDisplayName(filter.parent().previousElementSibling().text().trim());
                Elements opts = filter.select(".limitChoice label");
                for (Element opt : opts) {
                    dropdown.addDropdownValue(opt.attr("for"), opt.text().trim());
                }
            }
            fields.add(dropdown);
            i++;
        }

        return fields;
    }

    @Override
    public void start() throws
            IOException {
        String html = httpGet(
                opac_url
                        +
                        "/APS_ZONES?fn=AdvancedSearch&Style=Portal3&SubStyle=&Lang=GER" +
                        "&ResponseEncoding=utf-8",
                getDefaultEncoding());

        Document doc = Jsoup.parse(html);

        searchobj = doc.select("#ExpertSearch").attr("action");
        version18 = doc.select(".poweredBy").text().contains("v1.8");
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

    private int addParameters(SearchQuery query, List<NameValuePair> params,
            int index) {
        if (query.getValue().equals("")) {
            return index;
        }

        if (query.getSearchField() instanceof TextSearchField) {
            if (index != 1) {
                params.add(new BasicNameValuePair(".form.t" + index + ".logic",
                        "and"));
            }
            params.add(new BasicNameValuePair("q.form.t" + index + ".term",
                    query.getKey()));
            params.add(new BasicNameValuePair("q.form.t" + index + ".expr",
                    query.getValue()));
            return index + 1;
        } else if (query.getSearchField() instanceof DropdownSearchField) {
            params.add(new BasicNameValuePair("q.limits.limit", query.getValue()));
        }
        return index;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, OpacErrorException {
        start();

        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("Style", version18 ? "Portal2" : "Portal3"));
        params.add(new BasicNameValuePair("SubStyle", ""));
        params.add(new BasicNameValuePair("Lang", "GER"));
        params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
        params.add(new BasicNameValuePair("Method", "QueryWithLimits"));
        params.add(new BasicNameValuePair("SearchType", "AdvancedSearch"));
        params.add(new BasicNameValuePair("TargetSearchType", "AdvancedSearch"));
        params.add(new BasicNameValuePair("DB", "SearchServer"));
        params.add(new BasicNameValuePair("q.PageSize", "10"));

        int index = 1;
        for (SearchQuery query : queries) {
            index = addParameters(query, params, index);
        }

        if (index > 3) {
            throw new OpacErrorException(stringProvider.getQuantityString(
                    StringProvider.LIMITED_NUM_OF_CRITERIA, 3, 3));
        } else if (index == 1) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }

        String html = httpGet(opac_url + "/" + searchobj + "?"
                + URLEncodedUtils.format(params, "UTF-8"), getDefaultEncoding());

        page = 1;

        return parse_search(html, page);
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("Style", version18 ? "Portal2" : "Portal3"));
        params.add(new BasicNameValuePair("SubStyle", ""));
        params.add(new BasicNameValuePair("Lang", "GER"));
        params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
        if (page > this.page) {
            params.add(new BasicNameValuePair("Method",
                    version18 ? "FetchIncrementalBrowseDown" : "PageDown"));
        } else {
            params.add(new BasicNameValuePair("Method",
                    version18 ? "FetchIncrementalBrowseUp" : "PageUp"));
        }
        params.add(new BasicNameValuePair("PageSize", "10"));

        String html = httpGet(opac_url + "/" + searchobj + "?"
                + URLEncodedUtils.format(params, "UTF-8"), getDefaultEncoding());
        this.page = page;

        return parse_search(html, page);
    }

    private SearchRequestResult parse_search(String html, int page)
            throws OpacErrorException {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url + "/APS_PRESENT_BIB");

        if (doc.select("#ErrorAdviceRow").size() > 0) {
            throw new OpacErrorException(doc.select("#ErrorAdviceRow").text()
                                            .trim());
        }

        int results_total = -1;

        String searchHitsQuery = version18 ? "td:containsOwn(Total)" : ".searchHits";
        if (doc.select(searchHitsQuery).size() > 0) {
            results_total = Integer.parseInt(doc.select(searchHitsQuery).first().text().trim()
                                                .replaceAll(".*\\(([0-9]+)\\).*", "$1"));
        } else if (doc.select("span:matches(\\[\\d+/\\d+\\])").size() > 0) {
            // Zones 1.8 - searchGetPage
            String text = doc.select("span:matches(\\[\\d+/\\d+\\])").text();
            Pattern pattern = Pattern.compile("\\[\\d+/(\\d+)\\]");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                results_total = Integer.parseInt(matcher.group(1));
            }
        }

        if (doc.select(".pageNavLink").size() > 0) {
            // Zones 2.2
            searchobj = doc.select(".pageNavLink").first().attr("href").split("\\?")[0];
        } else if (doc.select("div[targetObject]").size() > 0) {
            // Zones 1.8 - search
            searchobj = doc.select("div[targetObject]").attr("targetObject").split("\\?")[0];
        } else {
            // Zones 1.8 - searchGetPage

            // The page contains a data structure that at first glance seems to be JSON, but uses
            // "=" instead of ":". So we parse it using regex...
            Pattern pattern = Pattern.compile("targetObject = \"([^\\?]+)[^\"]+\"");
            Matcher matcher = pattern.matcher(doc.html());
            if (matcher.find()) {
                searchobj = matcher.group(1);
            }
        }

        Elements table = doc.select(
                "#BrowseList > tbody > tr," // Zones 2.2
                        + " .inRoundBox1" // Zones 1.8
        );
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            Element tr = table.get(i);
            SearchResult sr = new SearchResult();

            String typetext;
            if (version18) {
                String[] parts = tr.select("img[src^=IMG/MAT]").attr("src").split("/");
                typetext = parts[parts.length - 1].replace(".gif", "");
            } else {
                typetext = tr.select(".SummaryMaterialTypeField").text().replace("\n", " ").trim();
            }

            if (data.has("mediatypes")) {
                try {
                    sr.setType(MediaType.valueOf(data.getJSONObject(
                            "mediatypes").getString(typetext)));
                } catch (JSONException | IllegalArgumentException e) {
                    sr.setType(defaulttypes.get(typetext));
                }
            } else {
                sr.setType(defaulttypes.get(typetext));
            }

            String imgUrl = null;
            if (version18) {
                if (tr.select("a[title=Titelbild]").size() > 0) {
                    imgUrl = tr.select("a[title=Titelbild]").attr("href");
                } else if (tr.select("img[width=50]").size() > 0) {
                    // TODO: better way to select these cover images? (found in Hannover)
                    imgUrl = tr.select("img[width=50]").attr("src");
                }
            } else {
                if (tr.select(".SummaryImageCell img[id^=Bookcover]").size() > 0) {
                    imgUrl = tr.select(".SummaryImageCell img[id^=Bookcover]").first().attr("src");
                }
            }
            sr.setCover(imgUrl);

            if (version18) {
                if (tr.select("img[src$=oci_1.gif]").size() > 0) {
                    // probably can only appear when searching the catalog on a terminal in
                    // the library.
                    sr.setStatus(SearchResult.Status.GREEN);
                } else if (tr.select("img[src$=blob_amber.gif]").size() > 0) {
                    sr.setStatus(SearchResult.Status.YELLOW);
                }
            }

            String desc = "";
            String childrenQuery = version18 ? "table[cellpadding=1] tr" :
                    ".SummaryDataCell tr, .SummaryDataCellStripe tr";
            Elements children = tr.select(childrenQuery);
            int childrennum = children.size();
            boolean haslink = false;

            for (int ch = 0; ch < childrennum; ch++) {
                Element node = children.get(ch);
                if (getName(node).equals("Titel")) {
                    desc += "<b>" + getValue(node).trim() + "</b><br />";
                } else if (getName(node).equals("Verfasser") || getName(node).equals("Jahr")) {
                    desc += getValue(node).trim() + "<br />";
                }

                String linkSelector = version18 ? "a[href*=ShowStock], a[href*=APS_CAT_IDENTIFY]" :
                        ".SummaryFieldData a.SummaryFieldLink";
                if (node.select(linkSelector).size() > 0 && !haslink) {
                    String href = node.select(linkSelector).attr("abs:href");
                    Map<String, String> hrefq = getQueryParamsFirst(href);
                    if (hrefq.containsKey("no")) {
                        sr.setId(hrefq.get("no"));
                    } else if (hrefq.containsKey("Key")) {
                        sr.setId(hrefq.get("Key"));
                    }
                    haslink = true;
                }
            }
            if (desc.endsWith("<br />")) {
                desc = desc.substring(0, desc.length() - 6);
            }
            sr.setInnerhtml(desc);
            sr.setNr(i);

            results.add(sr);
        }

        return new SearchRequestResult(results, results_total, page);
    }

    private String getValue(Element node) {
        if (version18) {
            return node.child(4).text().trim();
        } else {
            return node.select(".SummaryFieldData").text();
        }
    }

    private String getName(Element node) {
        if (version18) {
            return node.child(0).text().trim();
        } else {
            return node.select(".SummaryFieldLegend").text();
        }
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException {

        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("Style", version18 ? "Portal2" : "Portal3"));
        params.add(new BasicNameValuePair("SubStyle", ""));
        params.add(new BasicNameValuePair("Lang", "GER"));
        params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
        params.add(new BasicNameValuePair("no", id));

        String html = httpGet(
                opac_url + "/APS_PRESENT_BIB?"
                        + URLEncodedUtils.format(params, "UTF-8"),
                getDefaultEncoding());

        return parse_result(id, html);
    }

    @Override
    public DetailedItem getResult(int nr) throws IOException {
        return null;
    }

    private DetailedItem parse_result(String id, String html) {
        Document doc = Jsoup.parse(html);

        DetailedItem result = new DetailedItem();
        result.setTitle("");
        boolean title_is_set = false;

        result.setId(id);

        String detailTrsQuery = version18 ? ".inRoundBox1 table table tr" :
                ".DetailDataCell table table:not(.inRecordHeader) tr";
        Elements detailtrs1 = doc.select(detailTrsQuery);
        for (int i = 0; i < detailtrs1.size(); i++) {
            Element tr = detailtrs1.get(i);
            int s = tr.children().size();
            if (tr.child(0).text().trim().equals("Titel") && !title_is_set) {
                result.setTitle(tr.child(s - 1).text().trim());
                title_is_set = true;
            } else if (s > 1) {
                Element valchild = tr.child(s - 1);
                if (valchild.select("table").isEmpty()) {
                    String val = valchild.text().trim();
                    if (val.length() > 0) {
                        result.addDetail(new Detail(tr.child(0).text().trim(), val));
                    }
                }
            }
        }

        for (Element a : doc.select("a.SummaryActionLink")) {
            if (a.text().contains("Vormerken")) {
                result.setReservable(true);
                result.setReservation_info(a.attr("href"));
            }
        }

        Elements detaildiv = doc.select("div.record-item-new");
        if (!detaildiv.isEmpty()) {
            for (int i = 0; i < detaildiv.size(); i++) {
                Element dd = detaildiv.get(i);
                String text = "";
                for (Node node : dd.childNodes()) {
                    if (node instanceof TextNode) {
                        String snip = ((TextNode) node).text();
                        if (snip.length() > 0) {
                            text += snip;
                        }
                    } else if (node instanceof Element) {
                        if (((Element) node).tagName().equals("br")) {
                            text += "\n";
                        } else {
                            String snip = ((Element) node).text().trim();
                            if (snip.length() > 0) {
                                text += snip;
                            }
                        }
                    }
                }
                result.addDetail(new Detail("", text));
            }
        }

        if (doc.select("span.z3988").size() > 0) {
            // Sometimes there is a <span class="Z3988"> item which provides
            // data in a standardized format.
            String z3988data = doc.select("span.z3988").first().attr("title")
                                  .trim();
            for (String pair : z3988data.split("&")) {
                String[] nv = pair.split("=", 2);
                if (nv.length == 2) {
                    if (!nv[1].trim().equals("")) {
                        if (nv[0].equals("rft.btitle")
                                && result.getTitle().length() == 0) {
                            result.setTitle(nv[1]);
                        } else if (nv[0].equals("rft.atitle")
                                && result.getTitle().length() == 0) {
                            result.setTitle(nv[1]);
                        } else if (nv[0].equals("rft.au")) {
                            result.addDetail(new Detail("Author", nv[1]));
                        }
                    }
                }
            }
        }

        // Cover
        if (doc.select(".BookCover, .LargeBookCover").size() > 0) {
            result.setCover(doc.select(".BookCover, .LargeBookCover").first().attr("src"));
        }

        Elements copydivs = doc.select("div[id^=stock_]");
        String pop = "";
        for (int i = 0; i < copydivs.size(); i++) {
            Element div = copydivs.get(i);

            if (div.attr("id").startsWith("stock_head")) {
                pop = div.text().trim();
                continue;
            }

            Copy copy = new Copy();
            DateTimeFormatter fmt =
                    DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

            // This is getting very ugly - check if it is valid for libraries which are not Hamburg.
            // Seems to also work in Kiel (Zones 1.8, checked 10.10.2015)
            int j = 0;
            for (Node node : div.childNodes()) {
                try {
                    if (node instanceof Element) {
                        if (((Element) node).tag().getName().equals("br")) {
                            copy.setBranch(pop);
                            result.addCopy(copy);
                            j = -1;
                        } else if (((Element) node).tag().getName().equals("b")
                                && j == 1) {
                            copy.setLocation(((Element) node).text());
                        } else if (((Element) node).tag().getName().equals("b")
                                && j > 1) {
                            copy.setStatus(((Element) node).text());
                        }
                        j++;
                    } else if (node instanceof TextNode) {
                        if (j == 0) {
                            copy.setDepartment(((TextNode) node).text());
                        }
                        if (j == 2) {
                            copy.setBarcode(((TextNode) node).getWholeText().trim()
                                                             .split("\n")[0].trim());
                        }
                        if (j == 6) {
                            String text = ((TextNode) node).text().trim();
                            String date = text.substring(text.length() - 10);
                            try {
                                copy.setReturnDate(fmt.parseLocalDate(date));
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                        }
                        j++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account acc,
            int useraction, String selection) throws IOException {
        String reservation_info = item.getReservation_info();
        String html = httpGet(opac_url + "/" + reservation_info,
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        if (html.contains("Geheimnummer")) {
            List<NameValuePair> params = new ArrayList<>();
            for (Element input : doc.select("#MainForm input")) {
                if (!input.attr("name").equals("BRWR")
                        && !input.attr("name").equals("PIN")) {
                    params.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }
            }
            params.add(new BasicNameValuePair("BRWR", acc.getName()));
            params.add(new BasicNameValuePair("PIN", acc.getPassword()));
            html = httpGet(
                    opac_url
                            + "/"
                            + doc.select("#MainForm").attr("action")
                            + "?"
                            + URLEncodedUtils.format(params,
                            getDefaultEncoding()), getDefaultEncoding());
            doc = Jsoup.parse(html);
        }

        if (useraction == ReservationResult.ACTION_BRANCH) {
            List<NameValuePair> params = new ArrayList<>();
            for (Element input : doc.select("#MainForm input")) {
                if (!input.attr("name").equals("Confirm")) {
                    params.add(new BasicNameValuePair(input.attr("name"), input
                            .attr("value")));
                }

            }
            params.add(new BasicNameValuePair(
                    "MakeResTypeDef.Reservation.RecipientLocn", selection));
            params.add(new BasicNameValuePair("Confirm", "1"));
            httpGet(
                    opac_url
                            + "/"
                            + doc.select("#MainForm").attr("action")
                            + "?"
                            + URLEncodedUtils.format(params,
                            getDefaultEncoding()), getDefaultEncoding());
            return new ReservationResult(MultiStepResult.Status.OK);
        }

        if (useraction == 0) {
            ReservationResult res = null;
            for (Node n : doc.select("#MainForm").first().childNodes()) {
                if (n instanceof TextNode) {
                    if (((TextNode) n).text().contains("Entgelt")) {
                        res = new ReservationResult(
                                ReservationResult.Status.CONFIRMATION_NEEDED);
                        List<String[]> details = new ArrayList<>();
                        details.add(new String[]{((TextNode) n).text().trim()});
                        res.setDetails(details);
                        res.setMessage(((TextNode) n).text().trim());
                        res.setActionIdentifier(MultiStepResult.ACTION_CONFIRMATION);
                    }
                }
            }
            if (res != null) {
                return res;
            }
        }
        if (doc.select("#MainForm select").size() > 0) {
            ReservationResult res = new ReservationResult(
                    ReservationResult.Status.SELECTION_NEEDED);
            List<Map<String, String>> sel = new ArrayList<>();
            for (Element opt : doc.select("#MainForm select option")) {
                Map<String, String> selopt = new HashMap<>();
                selopt.put("key", opt.attr("value"));
                selopt.put("value", opt.text().trim());
                sel.add(selopt);
            }
            res.setSelection(sel);
            res.setMessage("Bitte Zweigstelle auswählen");
            res.setActionIdentifier(ReservationResult.ACTION_BRANCH);
            return res;
        }

        return new ReservationResult(ReservationResult.Status.ERROR);
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String Selection) throws IOException {
        if (media.startsWith("cannotrenew|")) {
            String message = media.split("\\|")[1];
            return new ProlongResult(MultiStepResult.Status.ERROR, message);
        }
        if (accountobj == null) {
            try {
                login(account);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        }
        String html = httpGet(opac_url + "/" + media, getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        if ((html.contains("document.location.replace") || html
                .contains("Schnellsuche")) && useraction == 0) {
            try {
                login(account);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
            prolong(media, account, 1, null);
        }
        String dialog = doc.select("#SSRenewDlgContent").text();
        if (dialog.contains("erfolgreich")) {
            return new ProlongResult(MultiStepResult.Status.OK, dialog);
        } else {
            return new ProlongResult(MultiStepResult.Status.ERROR, dialog);
        }
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        throw new UnsupportedOperationException();
    }

    private Document login(Account acc) throws IOException, OpacErrorException {
        String html = httpGet(
                opac_url
                        +
                        "/APS_ZONES?fn=MyZone&Style=Portal3&SubStyle=&Lang=GER&ResponseEncoding" +
                        "=utf-8",
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url + "/APS_ZONES");
        if (doc.select(".AccountSummaryCounterLink").size() > 0) {
            return doc;
        }
        if (doc.select("#LoginForm").size() == 0) {
            throw new NotReachableException("Login form not found");
        }
        List<NameValuePair> params = new ArrayList<>();

        for (Element input : doc.select("#LoginForm input")) {
            if (!input.attr("name").equals("BRWR")
                    && !input.attr("name").equals("PIN")) {
                params.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
            }
        }
        params.add(new BasicNameValuePair("BRWR", acc.getName()));
        params.add(new BasicNameValuePair("PIN", acc.getPassword()));

        String loginHtml;
        try {
            loginHtml = httpPost(
                    doc.select("#LoginForm").get(0).absUrl("action"),
                    new UrlEncodedFormEntity(params), getDefaultEncoding());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (!loginHtml.contains("Kontostand")) {
            throw new OpacErrorException(stringProvider.getString(
                    StringProvider.LOGIN_FAILED));
        }

        Document doc2 = Jsoup.parse(loginHtml);
        Pattern objid_pat = Pattern.compile("Obj_([0-9]+)\\?.*");
        for (Element a : doc2.select("a")) {
            Matcher objid_matcher = objid_pat.matcher(a.attr("href"));
            if (objid_matcher.matches()) {
                accountobj = objid_matcher.group(1);
            }
        }

        return doc2;
    }

    @Override
    public AccountData account(Account acc) throws IOException,
            JSONException,
            OpacErrorException {
        Document login = login(acc);
        if (login == null) {
            return null;
        }

        AccountData res = new AccountData(acc.getId());

        AccountLinks accountLinks = new AccountLinks(login, res);
        String lentLink = accountLinks.getLentLink();
        String resLink = accountLinks.getResLink();
        if (lentLink == null) {
            return null;
        }

        List<LentItem> lentItems = new ArrayList<>();
        String lentUrl = opac_url + "/" + lentLink.replace("utf-8?Method", "utf-8&Method");
        String lentHtml = httpGet(lentUrl, getDefaultEncoding());
        Document lentDoc = Jsoup.parse(lentHtml);
        lentDoc.setBaseUri(lentUrl);
        loadMediaList(lentDoc, lentItems);
        res.setLent(lentItems);

        // In Koeln, the reservations link only doesn't show on the overview page
        if (resLink == null) {
            for (Element a : lentDoc.select("a.AccountMenuLink")) {
                if (a.text().contains("Vormerkungen")) {
                    resLink = a.attr("href");
                }
            }
        }

        List<ReservedItem> reservedItems = new ArrayList<>();
        String resUrl = opac_url + "/" + resLink;
        String resHtml = httpGet(resUrl, getDefaultEncoding());
        Document resDoc = Jsoup.parse(resHtml);
        resDoc.setBaseUri(resUrl);
        loadResList(resDoc, reservedItems);
        res.setReservations(reservedItems);

        return res;
    }

    private void loadMediaList(Document lentDoc, List<LentItem> items)
            throws IOException {
        items.addAll(parseMediaList(lentDoc));
        String nextPageUrl = findNextPageUrl(lentDoc);
        if (nextPageUrl != null) {
            Document doc = Jsoup.parse(httpGet(nextPageUrl, getDefaultEncoding()));
            doc.setBaseUri(lentDoc.baseUri());
            loadMediaList(doc, items);
        }
    }

    private void loadResList(Document resDoc, List<ReservedItem> items) throws IOException {
        items.addAll(parseResList(resDoc));
        String nextPageUrl = findNextPageUrl(resDoc);
        if (nextPageUrl != null) {
            Document doc = Jsoup.parse(httpGet(nextPageUrl, getDefaultEncoding()));
            doc.setBaseUri(resDoc.baseUri());
            loadResList(doc, items);
        }
    }

    static String findNextPageUrl(Document doc) {
        if (doc.select(".pageNavLink[title*=nächsten]").size() > 0) {
            Element link = doc.select(".pageNavLink[title*=nächsten]").first();
            return link.absUrl("href");
        } else {
            return null;
        }
    }

    static List<ReservedItem> parseResList(Document doc) {
        List<ReservedItem> reservations = new ArrayList<>();
        for (Element table : doc
                .select(".MessageBrowseItemDetailsCell table, " +
                        ".MessageBrowseItemDetailsCellStripe" +
                        " table")) {
            ReservedItem item = new ReservedItem();

            for (Element tr : table.select("tr")) {
                String desc = tr.select(".MessageBrowseFieldNameCell").text()
                                .trim();
                String value = tr.select(".MessageBrowseFieldDataCell").text()
                                 .trim();
                if (desc.equals("Titel")) item.setTitle(value);
                if (desc.equals("Publikationsform")) item.setFormat(value);
                if (desc.equals("Liefern an")) item.setBranch(value);
                if (desc.equals("Status")) item.setStatus(value);
            }
            if ("Gelöscht".equals(item.getStatus())) continue;
            reservations.add(item);
        }
        return reservations;
    }

    static List<LentItem> parseMediaList(Document doc) {
        List<LentItem> lent = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(Locale.GERMAN);
        Pattern id_pat = Pattern.compile("javascript:renewItem\\('[0-9]+','(.*)'\\)");
        Pattern cannotrenew_pat =
                Pattern.compile("javascript:CannotRenewLoan\\('[0-9]+','(.*)','[0-9]+'\\)");

        for (Element table : doc
                .select(".LoansBrowseItemDetailsCellStripe table, " +
                        ".LoansBrowseItemDetailsCell " +
                        "table")) {
            LentItem item = new LentItem();

            for (Element tr : table.select("tr")) {
                String desc = tr.select(".LoanBrowseFieldNameCell").text()
                                .trim();
                String value = tr.select(".LoanBrowseFieldDataCell").text()
                                 .trim();
                if (desc.equals("Titel")) {
                    item.setTitle(value);
                    if (tr.select(".LoanBrowseFieldDataCell a[href]").size() > 0) {
                        String href = tr.select(".LoanBrowseFieldDataCell a[href]").attr("href");
                        Map<String, String> params = getQueryParamsFirst(href);
                        if (params.containsKey("BACNO")) {
                            item.setId(params.get("BACNO"));
                        }
                    }
                }
                if (desc.equals("Verfasser")) item.setAuthor(value);
                if (desc.equals("Mediennummer")) item.setBarcode(value);
                if (desc.equals("ausgeliehen in")) item.setHomeBranch(value);
                if (desc.matches("F.+lligkeits.*datum")) {
                    value = value.split(" ")[0];
                    try {
                        item.setDeadline(fmt.parseLocalDate(value));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (table.select(".button[Title~=Zum]").size() == 1) {
                Matcher matcher1 = id_pat.matcher(table.select(".button[Title~=Zum]").attr("href"));
                if (matcher1.matches()) item.setProlongData(matcher1.group(1));
            } else if (table.select(".CannotRenewLink").size() == 1) {
                Matcher matcher = cannotrenew_pat
                        .matcher(table.select(".CannotRenewLink").attr("href").trim());
                if (matcher.matches()) {
                    item.setProlongData("cannotrenew|" + matcher.group(1));
                }
                item.setRenewable(false);
            }
            lent.add(item);
        }
        return lent;
    }

    @Override
    public String getShareUrl(String id, String title) {
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("Style", "Portal3"));
        params.add(new BasicNameValuePair("SubStyle", ""));
        params.add(new BasicNameValuePair("Lang", "GER"));
        params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
        params.add(new BasicNameValuePair("no", id));

        return opac_url + "/APS_PRESENT_BIB?"
                + URLEncodedUtils.format(params, "UTF-8");
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
    protected String getDefaultEncoding() {
        return "UTF-8";
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        Document login = login(account);
        if (login == null) {
            throw new NotReachableException("Login unsuccessful");
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

    protected static class AccountLinks {
        private String lentLink;
        private String resLink;

        public AccountLinks(Document doc, AccountData res) {
            lentLink = null;
            resLink = null;
            for (Element td : doc
                    .select(".AccountSummaryCounterNameCell, " +
                            ".AccountSummaryCounterNameCellStripe, " +
                            ".CAccountDetailFieldNameCellStripe, .CAccountDetailFieldNameCell")) {
                String section = td.text().trim();
                if (section.contains("Entliehene Medien")) {
                    lentLink = td.select("a").attr("href");
                } else if (section.contains("Vormerkungen")) {
                    resLink = td.select("a").attr("href");
                } else if (section.contains("Kontostand")) {
                    res.setPendingFees(td.nextElementSibling().text().trim());
                } else if (section.matches("Ausweis g.ltig bis")
                        || section.matches("Ausweis\u00a0g.ltig\u00a0bis")) {
                    res.setValidUntil(td.nextElementSibling().text().trim());
                }
            }
            for (Element a : doc.select("a.AccountMenuLink")) {
                if (a.text().contains("Ausleihen")) {
                    lentLink = a.attr("href");
                } else if (a.text().contains("Vormerkungen")) {
                    resLink = a.attr("href");
                }
            }
        }

        public String getLentLink() {
            return lentLink;
        }

        public String getResLink() {
            return resLink;
        }
    }
}
