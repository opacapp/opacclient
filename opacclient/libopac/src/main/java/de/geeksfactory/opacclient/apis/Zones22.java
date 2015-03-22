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
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.i18n.StringProvider;
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
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * API für Web-Opacs von Zones mit dem Hinweis "Zones.2.2.45.04" im Footer.
 * Einziger bekannter Einsatzort ist Hamburg.
 * <p/>
 * TODO: Suche nach Medientypen, alles mit Konten + Vorbestellen
 */
public class Zones22 extends BaseApi {

    private static HashMap<String, MediaType> defaulttypes = new HashMap<>();
    static {
        defaulttypes.put("Buch", MediaType.BOOK);
        defaulttypes.put("Buch/Druckschrift", MediaType.BOOK);
        defaulttypes.put("Buch Erwachsene", MediaType.BOOK);
        defaulttypes.put("Buch Kinder/Jugendliche", MediaType.BOOK);
        defaulttypes.put("Kinder-Buch", MediaType.BOOK);
        defaulttypes.put("DVD", MediaType.DVD);
        defaulttypes.put("Kinder-DVD", MediaType.DVD);
        defaulttypes.put("Konsolenspiele", MediaType.GAME_CONSOLE);
        defaulttypes.put("Blu-ray Disc", MediaType.BLURAY);
        defaulttypes.put("Compact Disc", MediaType.CD);
        defaulttypes.put("CD-ROM", MediaType.CD_SOFTWARE);
        defaulttypes.put("Kinder-CD", MediaType.CD_SOFTWARE);
        defaulttypes.put("Noten", MediaType.SCORE_MUSIC);
        defaulttypes.put("Zeitschrift, Heft", MediaType.MAGAZINE);
        defaulttypes.put("E-Book", MediaType.EBOOK);
        defaulttypes.put("CDROM", MediaType.CD_SOFTWARE);
        defaulttypes.put("E-Audio", MediaType.MP3);
        defaulttypes.put("CD", MediaType.CD);
    }
    private String opac_url = "";
    private JSONObject data;
    private int page;
    private String searchobj;
    private String accountobj;

    public static String getStringFromBundle(Map<String, String> bundle,
                                             String key) {
        // Workaround for Bundle.getString(key, default) being available not
        // before API 12
        String res = bundle.get(key);
        if (res == null)
            res = "";
        return res;
    }

    @Override
    public List<SearchField> getSearchFields() throws
            IOException {
        List<SearchField> fields = new ArrayList<>();
        String html = httpGet(
                opac_url
                        + "/APS_ZONES?fn=AdvancedSearch&Style=Portal3&SubStyle=&Lang=GER&ResponseEncoding=utf-8",
                getDefaultEncoding());

        Document doc = Jsoup.parse(html);

        // Textfelder auslesen
        Elements txt_opts = doc.select("#formSelectTerm_1 option");
        for (Element opt : txt_opts) {
            TextSearchField field = new TextSearchField();
            field.setId(opt.attr("value"));
            field.setHint("");
            field.setDisplayName(opt.text());
            fields.add(field);
        }

        // Zweigstellen auslesen
        Elements zst_opts = doc.select(".TabRechAv .limitChoice label");
        if (zst_opts.size() > 0) {
            DropdownSearchField brDropdown = new DropdownSearchField();
            brDropdown.setId(zst_opts.get(0).parent().select("input")
                    .attr("name"));
            brDropdown.setDisplayName("Zweigstelle");

            List<Map<String, String>> brOptions = new ArrayList<>();
            Map<String, String> all = new HashMap<>();
            all.put("key", "");
            all.put("value", "Alle");
            brOptions.add(all);
            for (Element opt : zst_opts) {
                Map<String, String> value = new HashMap<>();
                value.put("key", opt.attr("for"));
                value.put("value", opt.text().trim());
                brOptions.add(value);
            }
            brDropdown.setDropdownValues(brOptions);
            fields.add(brDropdown);
        }

        return fields;
    }

    @Override
    public void start() throws
            IOException {
        String html = httpGet(
                opac_url
                        + "/APS_ZONES?fn=AdvancedSearch&Style=Portal3&SubStyle=&Lang=GER&ResponseEncoding=utf-8",
                getDefaultEncoding());

        Document doc = Jsoup.parse(html);

        searchobj = doc.select("#ExpertSearch").attr("action");
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

    private int addParameters(SearchQuery query, List<NameValuePair> params,
                              int index) {
        if (query.getValue().equals(""))
            return index;

        if (query.getSearchField() instanceof TextSearchField) {
            if (index != 1)
                params.add(new BasicNameValuePair(".form.t" + index + ".logic",
                        "and"));
            params.add(new BasicNameValuePair("q.form.t" + index + ".term",
                    query.getKey()));
            params.add(new BasicNameValuePair("q.form.t" + index + ".expr",
                    query.getValue()));
            return index + 1;
        } else if (query.getSearchField() instanceof DropdownSearchField) {
            params.add(new BasicNameValuePair(query.getKey(), query.getValue()));
        }
        return index;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, OpacErrorException {
        start();

        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("Style", "Portal3"));
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
            throw new OpacErrorException(stringProvider.getFormattedString(
                    StringProvider.LIMITED_NUM_OF_CRITERIA, 3));
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

        params.add(new BasicNameValuePair("Style", "Portal3"));
        params.add(new BasicNameValuePair("SubStyle", ""));
        params.add(new BasicNameValuePair("Lang", "GER"));
        params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
        if (page > this.page) {
            params.add(new BasicNameValuePair("Method", "PageDown"));
        } else {
            params.add(new BasicNameValuePair("Method", "PageUp"));
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

        if (doc.select(".searchHits").size() > 0) {
            results_total = Integer.parseInt(doc.select(".searchHits").first()
                    .text().trim().replaceAll(".*\\(([0-9]+)\\).*", "$1"));
        }

        if (doc.select(".pageNavLink").size() > 0) {
            searchobj = doc.select(".pageNavLink").first().attr("href")
                    .split("\\?")[0];
        }

        Elements table = doc.select("#BrowseList > tbody > tr");
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            Element tr = table.get(i);
            SearchResult sr = new SearchResult();

            String typetext = tr.select(".SummaryMaterialTypeField").text()
                    .replace("\n", " ").trim();
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

            if (tr.select(".SummaryImageCell img[id^=Bookcover]").size() > 0) {
                String imgUrl = tr
                        .select(".SummaryImageCell img[id^=Bookcover]").first()
                        .attr("src");
                sr.setCover(imgUrl);
            }

            String desc = "";
            Elements children = tr
                    .select(".SummaryDataCell tr, .SummaryDataCellStripe tr");
            int childrennum = children.size();
            boolean haslink = false;

            for (int ch = 0; ch < childrennum; ch++) {
                Element node = children.get(ch);
                if (node.select(".SummaryFieldLegend").text().equals("Titel")) {
                    desc += "<b>"
                            + node.select(".SummaryFieldData").text().trim()
                            + "</b><br />";

                } else if (node.select(".SummaryFieldLegend").text()
                        .equals("Verfasser")
                        || node.select(".SummaryFieldLegend").text()
                        .equals("Jahr")) {
                    desc += node.select(".SummaryFieldData").text().trim()
                            + "<br />";
                }

                if (node.select(".SummaryFieldData a.SummaryFieldLink").size() > 0
                        && !haslink) {
                    String href = node.select(
                            ".SummaryFieldData a.SummaryFieldLink").attr(
                            "abs:href");
                    Map<String, String> hrefq = getQueryParamsFirst(href);
                    sr.setId(hrefq.get("no"));
                    haslink = true;
                }
            }
            if (desc.endsWith("<br />"))
                desc = desc.substring(0, desc.length() - 6);
            sr.setInnerhtml(desc);
            sr.setNr(i);

            results.add(sr);
        }

        return new SearchRequestResult(results, results_total, page);
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException {

        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("Style", "Portal3"));
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
    public DetailledItem getResult(int nr) throws IOException {
        return null;
    }

    private DetailledItem parse_result(String id, String html) {
        Document doc = Jsoup.parse(html);

        DetailledItem result = new DetailledItem();
        result.setTitle("");
        boolean title_is_set = false;

        result.setId(id);

        Elements detaildiv = doc.select("div.record-item-new");

        Elements detailtrs1 = doc
                .select(".DetailDataCell table table:not(.inRecordHeader) tr");
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
                    if (val.length() > 0)
                        result.addDetail(new Detail(tr.child(0).text().trim(),
                                val));
                }
            }
        }

        for (Element a : doc.select("a.SummaryActionLink")) {
            if (a.text().contains("Vormerken")) {
                result.setReservable(true);
                result.setReservation_info(a.attr("href"));
            }
        }

        if (!detaildiv.isEmpty()) {
            for (int i = 0; i < detaildiv.size(); i++) {
                Element dd = detaildiv.get(i);
                String text = "";
                for (Node node : dd.childNodes()) {
                    if (node instanceof TextNode) {
                        String snip = ((TextNode) node).text();
                        if (snip.length() > 0)
                            text += snip;
                    } else if (node instanceof Element) {
                        if (((Element) node).tagName().equals("br"))
                            text += "\n";
                        else {
                            String snip = ((Element) node).text().trim();
                            if (snip.length() > 0)
                                text += snip;
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
            for (String pair : z3988data.split("\\&")) {
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

        //Cover
        if (doc.select(".BookCover, .LargeBookCover").size() > 0) {
            result.setCover(doc.select(".BookCover, .LargeBookCover").first().attr("src"));
        }

        Elements copydivs = doc.select(".DetailDataCell div[id^=stock_]");
        String pop = "";
        for (int i = 0; i < copydivs.size(); i++) {
            Element div = copydivs.get(i);

            if (div.attr("id").startsWith("stock_head")) {
                pop = div.text().trim();
                continue;
            }

            Map<String, String> copy = new HashMap<>();

            // This is getting very ugly - check if it is valid for libraries
            // which are not
            // Hamburg.
            int j = 0;
            for (Node node : div.childNodes()) {
                try {
                    if (node instanceof Element) {
                        if (((Element) node).tag().getName().equals("br")) {
                            copy.put(DetailledItem.KEY_COPY_BRANCH, pop);
                            result.addCopy(copy);
                            j = -1;
                        } else if (((Element) node).tag().getName().equals("b")
                                && j == 1) {
                            copy.put(DetailledItem.KEY_COPY_LOCATION,
                                    ((Element) node).text());
                        } else if (((Element) node).tag().getName().equals("b")
                                && j > 1) {
                            copy.put(DetailledItem.KEY_COPY_STATUS,
                                    ((Element) node).text());
                        }
                        j++;
                    } else if (node instanceof TextNode) {
                        if (j == 0)
                            copy.put(DetailledItem.KEY_COPY_DEPARTMENT,
                                    ((TextNode) node).text());
                        if (j == 2)
                            copy.put(DetailledItem.KEY_COPY_BARCODE,
                                    ((TextNode) node).getWholeText().trim()
                                            .split("\n")[0].trim());
                        if (j == 6) {
                            String text = ((TextNode) node).text().trim();
                            copy.put(DetailledItem.KEY_COPY_RETURN,
                                    text.substring(text.length() - 10));
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
    public ReservationResult reservation(DetailledItem item, Account acc,
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
            if (res != null)
                return res;
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
        if (dialog.contains("erfolgreich"))
            return new ProlongResult(MultiStepResult.Status.OK, dialog);
        else
            return new ProlongResult(MultiStepResult.Status.ERROR, dialog);
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
                               String selection) throws IOException, OpacErrorException {
        throw new UnsupportedOperationException();
    }

    private Document login(Account acc) throws IOException, OpacErrorException {
        String html = httpGet(
                opac_url
                        + "/APS_ZONES?fn=MyZone&Style=Portal3&SubStyle=&Lang=GER&ResponseEncoding=utf-8",
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url + "/APS_ZONES");
        if (doc.select(".AccountSummaryCounterLink").size() > 0) {
            return doc;
        }
        if (doc.select("#LoginForm").size() == 0) {
            throw new NotReachableException();
        }
        List<NameValuePair> params = new ArrayList<>();

        for (Element input : doc.select("#LoginForm input")) {
            if (!input.attr("name").equals("BRWR")
                    && !input.attr("name").equals("PIN"))
                params.add(new BasicNameValuePair(input.attr("name"), input
                        .attr("value")));
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
        if (login == null)
            return null;

        AccountData res = new AccountData(acc.getId());

        String lent_link = null;
        String res_link = null;
        int lent_cnt = -1;
        int res_cnt = -1;
        for (Element td : login
                .select(".AccountSummaryCounterNameCell, .AccountSummaryCounterNameCellStripe, .CAccountDetailFieldNameCellStripe, .CAccountDetailFieldNameCell")) {
            String section = td.text().trim();
            if (section.contains("Entliehene Medien")) {
                lent_link = td.select("a").attr("href");
                lent_cnt = Integer.parseInt(td.nextElementSibling().text()
                        .trim());
            } else if (section.contains("Vormerkungen")) {
                res_link = td.select("a").attr("href");
                res_cnt = Integer.parseInt(td.nextElementSibling().text()
                        .trim());
            } else if (section.contains("Kontostand")) {
                res.setPendingFees(td.nextElementSibling().text().trim());
            } else if (section.matches("Ausweis g.ltig bis")) {
                res.setValidUntil(td.nextElementSibling().text().trim());
            }
        }
        assert (lent_cnt >= 0);
        assert (res_cnt >= 0);
        if (lent_link == null)
            return null;

        String lent_html = httpGet(
                opac_url + "/"
                        + lent_link.replace("utf-8?Method", "utf-8&Method"),
                getDefaultEncoding());
        Document lent_doc = Jsoup.parse(lent_html);
        List<Map<String, String>> lent = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.GERMAN);
        Pattern id_pat = Pattern
                .compile("javascript:renewItem\\('[0-9]+','(.*)'\\)");

        for (Element table : lent_doc
                .select(".LoansBrowseItemDetailsCellStripe table, .LoansBrowseItemDetailsCell table")) {
            Map<String, String> item = new HashMap<>();

            for (Element tr : table.select("tr")) {
                String desc = tr.select(".LoanBrowseFieldNameCell").text()
                        .trim();
                String value = tr.select(".LoanBrowseFieldDataCell").text()
                        .trim();
                if (desc.equals("Titel"))
                    item.put(AccountData.KEY_LENT_TITLE, value);
                if (desc.equals("Verfasser"))
                    item.put(AccountData.KEY_LENT_AUTHOR, value);
                if (desc.equals("Mediennummer"))
                    item.put(AccountData.KEY_LENT_BARCODE, value);
                if (desc.equals("ausgeliehen in"))
                    item.put(AccountData.KEY_LENT_BRANCH, value);
                if (desc.matches("F.+lligkeits.*datum")) {
                    value = value.split(" ")[0];
                    item.put(AccountData.KEY_LENT_DEADLINE, value);
                    try {
                        item.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP,
                                String.valueOf(sdf.parse(value).getTime()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (table.select(".button[Title~=Zum]").size() == 1) {
                Matcher matcher1 = id_pat.matcher(table.select(
                        ".button[Title~=Zum]").attr("href"));
                if (matcher1.matches()) {
                    item.put(AccountData.KEY_LENT_LINK, matcher1.group(1));
                }
            }
            lent.add(item);
        }
        res.setLent(lent);
        assert (lent_cnt <= lent.size());

        List<Map<String, String>> reservations = new ArrayList<>();
        String res_html = httpGet(opac_url + "/" + res_link,
                getDefaultEncoding());
        Document res_doc = Jsoup.parse(res_html);

        for (Element table : res_doc
                .select(".MessageBrowseItemDetailsCell table, .MessageBrowseItemDetailsCellStripe table")) {
            Map<String, String> item = new HashMap<>();

            for (Element tr : table.select("tr")) {
                String desc = tr.select(".MessageBrowseFieldNameCell").text()
                        .trim();
                String value = tr.select(".MessageBrowseFieldDataCell").text()
                        .trim();
                if (desc.equals("Titel"))
                    item.put(AccountData.KEY_RESERVATION_TITLE, value);
                if (desc.equals("Publikationsform"))
                    item.put(AccountData.KEY_RESERVATION_FORMAT, value);
                if (desc.equals("Liefern an"))
                    item.put(AccountData.KEY_RESERVATION_BRANCH, value);
                if (desc.equals("Status"))
                    item.put(AccountData.KEY_RESERVATION_READY, value);
            }
            if ("Gelöscht".equals(item.get(AccountData.KEY_RESERVATION_READY))) {
                continue;
            }
            reservations.add(item);
        }
        res.setReservations(reservations);
        assert (reservations.size() >= res_cnt);

        return res;
    }

    @Override
    public boolean isAccountSupported(Library library) {
        return true;
    }

    @Override
    public boolean isAccountExtendable() {
        return false;
    }

    @Override
    public String getAccountExtendableInfo(Account acc)
            throws IOException {
        return null;
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
        if (login == null)
            throw new NotReachableException();
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
