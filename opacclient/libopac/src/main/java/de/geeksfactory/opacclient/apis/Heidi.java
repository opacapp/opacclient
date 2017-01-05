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
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
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
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class Heidi extends ApacheBaseApi implements OpacApi {

    protected String opac_url = "";
    protected Library library;
    protected JSONObject data;
    protected String sessid;
    protected String ENCODING = "UTF-8";
    protected String last_error;
    protected int pagesize = 20;
    protected List<SearchQuery> last_query;
    protected CookieStore cookieStore = new BasicCookieStore();

    @Override
    public void start() throws IOException {
        String html = httpGet(opac_url + "/search.cgi?art=f", ENCODING, false,
                cookieStore);
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);
        sessid = null;
        for (Element link : doc.select("a")) {
            String sid = getQueryParamsFirst(link.absUrl("href")).get("sess");
            if (sid != null) {
                sessid = sid;
                break;
            }
        }
        super.start();
    }

    @Override
    protected String getDefaultEncoding() {
        return ENCODING;
    }

    @Override
    public void init(Library library, HttpClientFactory httpClientFactory) {
        super.init(library, httpClientFactory);
        this.library = library;
        this.data = library.getData();
        this.opac_url = data.optString("baseurl", "");
    }

    protected int addParameters(String key, String value,
            List<NameValuePair> params, int index) {
        index++;

        if (index != 3) {
            params.add(new BasicNameValuePair("op" + index, "AND"));
        }
        params.add(new BasicNameValuePair("kat" + index, key));
        params.add(new BasicNameValuePair("var" + index, value));
        return index;

    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, OpacErrorException {

        last_query = queries;

        List<NameValuePair> params = new ArrayList<>();

        if (sessid == null) {
            start();
        }
        int index = 0;
        int page = 1;
        String homebranch = "";

        params.add(new BasicNameValuePair("fsubmit", "1"));
        params.add(new BasicNameValuePair("sess", sessid));
        params.add(new BasicNameValuePair("art", "f"));
        params.add(new BasicNameValuePair("pagesize", String.valueOf(pagesize)));

        for (SearchQuery query : queries) {
            if (query.getKey().equals("_heidi_page")) {
                page = Integer.parseInt(query.getValue());
                params.add(new BasicNameValuePair("start", String
                        .valueOf((page - 1) * pagesize + 1)));
            } else if (query.getKey().equals("_heidi_branch")) {
                homebranch = query.getValue();
            } else if (query.getKey().equals("f[teil2]")) {
                params.add(new BasicNameValuePair(query.getKey(), query
                        .getValue()));
            } else {
                if (!query.getValue().equals("")) {
                    index = addParameters(query.getKey(), query.getValue(),
                            params, index);
                }
            }
        }

        params.add(new BasicNameValuePair("vr", "1"));

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }
        if (index > 3) {
            throw new OpacErrorException(stringProvider.getQuantityString(
                    StringProvider.LIMITED_NUM_OF_CRITERIA, 3, 3));
        }

        while (index < 3) {
            index++;
            if (index != 3) {
                params.add(new BasicNameValuePair("op" + index, "AND"));
            }
            params.add(new BasicNameValuePair("kat" + index, "freitext"));
            params.add(new BasicNameValuePair("var" + index, ""));
        }

        // Homebranch selection
        httpGet(opac_url + "/zweigstelle.cgi?sess=" + sessid + "&zweig="
                + homebranch, ENCODING, false, cookieStore);
        // The actual search
        String html = httpGet(
                opac_url + "/search.cgi?"
                        + URLEncodedUtils.format(params, "UTF-8"), ENCODING,
                false, cookieStore);
        return parse_search(html, page);
    }

    private SearchRequestResult parse_search(String html, int page) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);

        int results_total = 0;
        if (doc.select("#heiditreffer").size() > 0) {
            String resstr = doc.select("#heiditreffer").text();
            String resnum = resstr.replaceAll("\\(([0-9.]+)([^0-9]*)\\)", "$1")
                                  .replace(".", "");
            results_total = Integer.parseInt(resnum);
        }

        Elements table = doc.select("table.treffer tr");
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            Element tr = table.get(i);
            SearchResult sr = new SearchResult();

            StringBuilder description = null;
            String author = "";

            for (Element link : tr.select("a")) {
                String kk = getQueryParamsFirst(link.absUrl("href")).get(
                        "katkey");
                if (kk != null) {
                    sr.setId(kk);
                    break;
                }
            }

            if (tr.select("span.Z3988").size() == 1) {
                // Luckily there is a <span class="Z3988"> item which provides
                // data in a standardized format.
                String zdata = tr.select("span.Z3988").attr("title").replace(";", "%3B").replace(":", "%3A").replace("/", "%2F");
                boolean hastitle = false;
                description = new StringBuilder();
                List<NameValuePair> z3988data = parse_z3988data(zdata);
                for (NameValuePair nv : z3988data) {
                    if (nv.getValue() != null) {
                        if (!nv.getValue().trim().equals("")) {
                            if (nv.getName().equals("rft.btitle")
                                    && !hastitle) {
                                description.append("<b>").append(nv.getValue()).append("</b>");
                                hastitle = true;
                            } else if (nv.getName().equals("rft.atitle")
                                    && !hastitle) {
                                description.append("<b>").append(nv.getValue()).append("</b>");
                                hastitle = true;
                            } else if (nv.getName().equals("rft.au")) {
                                author = nv.getValue();
                            } else if (nv.getName().equals("rft.aufirst")) {
                                author = author + ", " + nv.getValue();
                            } else if (nv.getName().equals("rft.aulast")) {
                                author = nv.getValue();
                            } else if (nv.getName().equals("rft.date")) {
                                description.append("<br />").append(nv.getValue());
                            }
                        }
                    }
                }
            }
            if (!"".equals(author)) {
                author = author + "<br />";
            }
            sr.setInnerhtml(author + description.toString());

            if (tr.select(".kurzstat").size() > 0) {
                String stattext = tr.select(".kurzstat").first().text();
                if (stattext.contains("ausleihbar")) {
                    sr.setStatus(Status.GREEN);
                } else if (stattext.contains("online")) {
                    sr.setStatus(Status.GREEN);
                } else if (stattext.contains("entliehen")) {
                    sr.setStatus(Status.RED);
                } else if (stattext.contains("Präsenznutzung")) {
                    sr.setStatus(Status.YELLOW);
                } else if (stattext.contains("bestellen")) {
                    sr.setStatus(Status.YELLOW);
                }
            }
            if (tr.select(".typbild").size() > 0) {
                String typtext = tr.select(".typbild").first().text();
                if (typtext.contains("Buch")) {
                    sr.setType(MediaType.BOOK);
                } else if (typtext.contains("DVD-ROM")) {
                    sr.setType(MediaType.CD_SOFTWARE);
                } else if (typtext.contains("Online-Ressource")) {
                    sr.setType(MediaType.EDOC);
                } else if (typtext.contains("DVD")) {
                    sr.setType(MediaType.DVD);
                } else if (typtext.contains("Film")) {
                    sr.setType(MediaType.MOVIE);
                } else if (typtext.contains("Zeitschrift")) {
                    sr.setType(MediaType.MAGAZINE);
                } else if (typtext.contains("Musiknoten")) {
                    sr.setType(MediaType.SCORE_MUSIC);
                } else if (typtext.contains("Bildliche Darstellung")) {
                    sr.setType(MediaType.ART);
                } else if (typtext.contains("Zeitung")) {
                    sr.setType(MediaType.NEWSPAPER);
                } else if (typtext.contains("Karte")) {
                    sr.setType(MediaType.MAP);
                } else if (typtext.contains("Mehrteilig")) {
                    sr.setType(MediaType.PACKAGE_BOOKS);
                }
            }

            results.add(sr);
        }
        // TODO
        return new SearchRequestResult(results, results_total, page);
    }

    private List<NameValuePair> parse_z3988data(String zdata) {
        List<NameValuePair> nvps = new ArrayList<>();
        for (String tuple : zdata.split("&")) {
            if (tuple.contains("=")) {
                String[] parts = tuple.split("=");
                if (parts.length < 2) {
                    continue;
                }
                try {
                    nvps.add(new BasicNameValuePair(parts[0], URLDecoder.decode(parts[1],
                                "UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    nvps.add(new BasicNameValuePair(parts[0], "?"));
                }
            } else {
                nvps.add(new BasicNameValuePair(tuple, ""));
            }
        }
        return nvps;
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException {
        // Not implemented
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        TextSearchField pagefield = new TextSearchField();
        pagefield.setId("_heidi_page");
        pagefield.setVisible(false);
        pagefield.setDisplayName("Seite");
        pagefield.setHint("");
        for (SearchQuery q : last_query) {
            if (q.getKey().equals("_heidi_page")) {
                last_query.remove(q);
            }
        }
        last_query.add(new SearchQuery(pagefield, String.valueOf(page)));
        return search(last_query);
    }

    @Override
    public SearchRequestResult volumeSearch(Map<String, String> query)
            throws IOException, OpacErrorException {
        String html = httpGet(opac_url + "/search.cgi?sess=" + sessid
                        + "&format=html&query=" + query.get("query"), ENCODING, false,
                cookieStore);
        return parse_search(html, 1);
    }

    @Override
    public DetailedItem getResultById(String id, final String homebranch)
            throws IOException {

        if (sessid == null) {
            start();
        }

        // Homebranch
        if (homebranch != null && !"".equals(homebranch)) {
            cookieStore.addCookie(new BasicClientCookie("zweig", homebranch));
        }

        String html = httpGet(opac_url + "/titel.cgi?katkey=" + id + "&sess="
                + sessid, ENCODING, false, cookieStore);
        Document doc = Jsoup.parse(html);

        DetailedItem item = new DetailedItem();
        item.setId(id);

        Elements table = doc.select(".titelsatz tr");
        for (Element tr : table) {
            if (tr.select("th").size() == 0 || tr.select("td").size() == 0) {
                continue;
            }
            String d = tr.select("th").first().text();
            String c = tr.select("td").first().text();
            if (d.equals("Titel:")) {
                item.setTitle(c);
            } else if ((d.contains("URL") || d.contains("Link")) && tr.select("td a").size() > 0) {
                item.addDetail(new Detail(d, tr.select("td a").first().attr("href")));
            } else {
                item.addDetail(new Detail(d, c));
            }
        }

        if (doc.select(".ex table tr").size() > 0) {
            table = doc.select(".ex table tr");
            DateTimeFormatter
                    fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
            for (Element tr : table) {
                if (tr.hasClass("exueber") || tr.select(".exsig").size() == 0
                        || tr.select(".exso").size() == 0
                        || tr.select(".exstatus").size() == 0) {
                    continue;
                }
                Copy copy = new Copy();
                copy.setShelfmark(tr.select(".exsig").first().text());
                copy.setBranch(tr.select(".exso").first().text());
                String status = tr.select(".exstatus").first().text();
                if (status.contains("entliehen bis")) {
                    copy.setReturnDate(fmt.parseLocalDate(
                            status.replaceAll("entliehen bis ([0-9.]+) .*", "$1")));
                    copy.setReservations(
                            status.replaceAll(".*\\(.*Vormerkungen: ([0-9]+)\\)", "$1"));
                    copy.setStatus("entliehen");
                } else {
                    copy.setStatus(status);
                }
                item.addCopy(copy);
            }
        }

        for (Element a : doc.select(".status1 a")) {
            if (a.attr("href").contains("bestellung.cgi")) {
                item.setReservable(true);
                item.setReservation_info(id);
                break;
            }
        }
        for (Element a : doc.select(".titelsatz a")) {
            if (a.text().trim().matches("B.+nde")) {
                Map<String, String> volumesearch = new HashMap<>();
                volumesearch.put("query", getQueryParamsFirst(a.attr("href"))
                        .get("query"));
                item.setVolumesearch(volumesearch);
            }
        }

        return item;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public String getShareUrl(String id, String title) {
        return opac_url + "/titel.cgi?katkey=" + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING
                | SUPPORT_FLAG_ACCOUNT_PROLONG_ALL;
    }

    @Override
    public List<SearchField> parseSearchFields() throws IOException,
            OpacErrorException, JSONException {
        String html = httpGet(opac_url + "/search.cgi?art=f", ENCODING, false,
                cookieStore);
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);
        List<SearchField> fields = new ArrayList<>();

        Elements options = doc.select("select[name=kat1] option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.attr("value"));
            field.setHint("");
            fields.add(field);
        }

        DropdownSearchField field = new DropdownSearchField();

        Elements zst_opts = doc.select("#teilk2 option");
        for (int i = 0; i < zst_opts.size(); i++) {
            Element opt = zst_opts.get(i);
            field.addDropdownValue(opt.val(), opt.text());
        }
        field.setDisplayName("Einrichtung");
        field.setId("f[teil2]");
        field.setVisible(true);
        field.setMeaning(SearchField.Meaning.BRANCH);
        fields.add(field);

        try {
            field = new DropdownSearchField();
            Document doc2 = Jsoup.parse(httpGet(opac_url
                            + "/zweigstelle.cgi?sess=" + sessid, ENCODING, false,
                    cookieStore));
            Elements home_opts = doc2.select("#zweig option");
            for (int i = 0; i < home_opts.size(); i++) {
                Element opt = home_opts.get(i);
                if (!opt.val().equals("")) {
                    Map<String, String> option = new HashMap<>();
                    option.put("key", opt.val());
                    option.put("value", opt.text());
                    field.addDropdownValue(opt.val(), opt.text());
                }
            }
            field.setDisplayName("Leihstelle");
            field.setId("_heidi_branch");
            field.setVisible(true);
            field.setMeaning(SearchField.Meaning.HOME_BRANCH);
            fields.add(field);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextSearchField pagefield = new TextSearchField();
        pagefield.setId("_heidi_page");
        pagefield.setVisible(false);
        pagefield.setDisplayName("Seite");
        pagefield.setHint("");
        fields.add(pagefield);

        return fields;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        String html = httpGet(opac_url + "/bestellung.cgi?ks=" + item.getId()
                + "&sess=" + sessid, ENCODING, false, cookieStore);
        Document doc = Jsoup.parse(html);
        if (doc.select("input[name=pw]").size() > 0) {
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("id", account.getName()));
            nameValuePairs.add(new BasicNameValuePair("pw", account
                    .getPassword()));
            nameValuePairs.add(new BasicNameValuePair("sess", sessid));
            nameValuePairs.add(new BasicNameValuePair("log", "login"));
            nameValuePairs.add(new BasicNameValuePair("weiter",
                    "bestellung.cgi?ks=" + item.getId()));
            html = httpPost(opac_url + "/login.cgi", new UrlEncodedFormEntity(
                    nameValuePairs), ENCODING);
            doc = Jsoup.parse(html);
            if (doc.select(".loginbox .meld").size() > 0) {
                return new ReservationResult(MultiStepResult.Status.ERROR, doc
                        .select(".loginbox .meld").text());
            }
        }
        if (doc.select("input[name=ort]").size() > 0) {
            if (selection != null) {
                List<NameValuePair> nameValuePairs = new ArrayList<>(
                        2);
                nameValuePairs.add(new BasicNameValuePair("ks", item.getId()));
                nameValuePairs.add(new BasicNameValuePair("ort", selection));
                nameValuePairs.add(new BasicNameValuePair("sess", sessid));
                nameValuePairs.add(new BasicNameValuePair("funktion",
                        "Vormerkung"));
                html = httpPost(opac_url + "/bestellung.cgi",
                        new UrlEncodedFormEntity(nameValuePairs), ENCODING);
                doc = Jsoup.parse(html);
            } else {
                List<Map<String, String>> options = new ArrayList<>();
                for (Element input : doc.select("input[name=ort]")) {
                    Element label = doc.select("label[for=" + input.id() + "]")
                                       .first();
                    Map<String, String> selopt = new HashMap<>();
                    selopt.put("key", input.attr("value"));
                    selopt.put("value", label.text());
                    options.add(selopt);
                }
                ReservationResult res = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED);
                res.setSelection(options);
                return res;
            }
        }
        if (doc.select(".fehler").size() > 0) {
            String text = doc.select(".fehler").text();
            return new ReservationResult(MultiStepResult.Status.ERROR, text);
        }
        String text = doc.select(".meld2").text();
        if (text.contains("Das Medium wurde")) {
            return new ReservationResult(MultiStepResult.Status.OK, text);
        } else {
            return new ReservationResult(MultiStepResult.Status.ERROR, text);
        }
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        // Internal convention: a is either a § followed by an error message or
        // the URI of the page this item was found on and the query string the
        // prolonging link links to, seperated by a $.
        if (media.startsWith("§")) {
            String error = stringProvider.getString(StringProvider.PROLONGING_IMPOSSIBLE);
            if (media.substring(1).equals("rot")) {
                error = stringProvider.getString(StringProvider.PROLONGING_EXPIRED);
            } else if (media.substring(1).equals("gruen")) {
                error = stringProvider.getString(StringProvider.PROLONGING_WAITING);
            }
            return new ProlongResult(MultiStepResult.Status.ERROR, error);
        }

        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs
                .add(new BasicNameValuePair("everl", "Einzelverlängerung"));
        nameValuePairs.add(new BasicNameValuePair("mailversand", "Ok"));
        nameValuePairs.add(new BasicNameValuePair("mark", media));
        nameValuePairs.add(new BasicNameValuePair("sess", sessid));
        String html = httpPost(opac_url + "/konto.cgi",
                new UrlEncodedFormEntity(nameValuePairs), ENCODING);
        Document doc = Jsoup.parse(html);
        if (doc.select("input[name=pw]").size() > 0) {
            try {
                login(account);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
            return prolong(media, account, useraction, selection);
        }
        if (doc.select(".meld2").size() > 0) {
            String text = doc.select(".meld2").text();
            if (text.matches(".*Neues Leihfristende.*")) {
                return new ProlongResult(MultiStepResult.Status.OK, text);
            } else {
                return new ProlongResult(MultiStepResult.Status.ERROR, text);
            }
        }
        return new ProlongResult(MultiStepResult.Status.OK);
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        String html = httpGet(opac_url + "/konto.cgi?sess=" + sessid
                + "&email=&verl=Gesamtkontoverlängerung", ENCODING);
        Document doc = Jsoup.parse(html);

        if (doc.select("input[name=pw]").size() > 0) {
            try {
                login(account);
            } catch (OpacErrorException e) {
                return new ProlongAllResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
            return prolongAll(account, useraction, selection);
        }

        List<Map<String, String>> result = new ArrayList<>();

        Map<String, String> line = new HashMap<>();
        for (Element tr : doc.select(".kontobox table tbody tr")) {
            if (tr.children().size() < 2) {
                if (line.size() > 0) {
                    line.put(ProlongAllResult.KEY_LINE_MESSAGE, tr.child(0)
                                                                  .text().trim());
                    result.add(line);
                    line = new HashMap<>();
                }
                continue;
            }
            String label = tr.child(0).text();
            String text = tr.child(1).text().trim();
            if (label.contains("Verfasser")) {
                line.put(ProlongAllResult.KEY_LINE_AUTHOR, text);
            } else if (label.contains("Titel")) {
                line.put(ProlongAllResult.KEY_LINE_TITLE, text);
            } else if (label.contains("Altes Leihfristende")) {
                line.put(ProlongAllResult.KEY_LINE_OLD_RETURNDATE, text);
            } else if (label.contains("Neues")) {
                line.put(ProlongAllResult.KEY_LINE_NEW_RETURNDATE, text);
            }
        }

        return new ProlongAllResult(MultiStepResult.Status.OK, result);
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("storno",
                "Vormerkung stornieren"));
        nameValuePairs.add(new BasicNameValuePair("mark", media));
        nameValuePairs.add(new BasicNameValuePair("sess", sessid));
        String html = httpPost(opac_url + "/konto.cgi",
                new UrlEncodedFormEntity(nameValuePairs), ENCODING);
        Document doc = Jsoup.parse(html);
        if (doc.select("input[name=pw]").size() > 0) {
            login(account);
            return cancel(media, account, useraction, selection);
        }
        if (doc.select(".meld2").size() > 0) {
            String text = doc.select(".meld2").text();
            if (text.matches(".*durchgef.+hrt.*")) {
                return new CancelResult(MultiStepResult.Status.OK, text);
            } else {
                return new CancelResult(MultiStepResult.Status.ERROR, text);
            }
        }
        return new CancelResult(MultiStepResult.Status.OK);
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        login(account);
        String html;
        Document doc;
        AccountData adata = new AccountData(account.getId());
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        html = httpGet(opac_url + "/konto.cgi?sess=" + sessid,
                getDefaultEncoding());
        doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url + "/");

        for (Element td : doc.select("table.konto td")) {
            if (td.text().contains("Offene")) {
                String text = td
                        .text()
                        .trim()
                        .replaceAll(
                                "Offene[^0-9]+Geb.+hren:[^0-9]+([0-9.," +
                                        "]+)[^0-9€A-Z]*(€|EUR|CHF|Fr.)",
                                "$1 $2");
                adata.setPendingFees(text);
            }
        }

        List<LentItem> lent = new ArrayList<>();
        for (Element tr : doc.select("table.kontopos tr")) {
            LentItem item = new LentItem();
            Element desc = tr.child(1).select("label").first();
            String dates = tr.child(2).text().trim();
            if (tr.child(1).select("a").size() > 0) {
                String kk = getQueryParamsFirst(
                        tr.child(1).select("a").first().absUrl("href")).get(
                        "katkey");
                item.setId(kk);
            }

            int i = 0;
            for (Node node : desc.childNodes()) {
                if (node instanceof TextNode) {
                    String text = ((TextNode) node).text().trim();
                    if (i == 0) {
                        item.setAuthor(text);
                    } else if (i == 1) {
                        item.setTitle(text);
                    } else if (text.contains("Mediennummer")) {
                        item.setBarcode(text.replace("Mediennummer: ", ""));
                    }
                    i++;
                }
            }

            if (tr.child(0).select("input").size() == 1) {
                item.setProlongData(tr.child(0).select("input").first().val());
                item.setRenewable(true);
            } else {
                item.setProlongData("§" + tr.child(0).select("span").first().attr("class"));
                item.setRenewable(false);
            }

            String todate = dates;
            if (todate.contains("-")) {
                String[] datesplit = todate.split("-");
                todate = datesplit[1].trim();
            }
            try {
                item.setDeadline(fmt.parseLocalDate(todate.substring(0, 10)));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            lent.add(item);
        }
        adata.setLent(lent);

        List<ReservedItem> reservations = new ArrayList<>();
        html = httpGet(opac_url + "/konto.cgi?konto=v&sess=" + sessid,
                getDefaultEncoding());
        reservations.addAll(parse_reservations(html));
        html = httpGet(opac_url + "/konto.cgi?konto=b&sess=" + sessid,
                getDefaultEncoding());
        reservations.addAll(parse_reservations(html));

        adata.setReservations(reservations);

        return adata;
    }

    protected List<ReservedItem> parse_reservations(String html) {
        Document doc = Jsoup.parse(html);
        List<ReservedItem> reservations = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        for (Element tr : doc.select("table.kontopos tr")) {
            ReservedItem item = new ReservedItem();
            Element desc = tr.child(1).select("label").first();
            Element pos = tr.child(3);
            if (tr.child(1).select("a").size() > 0) {
                String kk = getQueryParamsFirst(
                        tr.child(1).select("a").first().absUrl("href")).get("katkey");
                item.setId(kk);
            }
            if (tr.child(0).select("input").size() > 0) {
                item.setCancelData(tr.child(0).select("input").first().val());
            }

            int i = 0;
            for (Node node : desc.childNodes()) {
                if (node instanceof TextNode) {
                    String text = ((TextNode) node).text().trim();
                    if (i == 0) {
                        item.setAuthor(text);
                    } else if (i == 1) {
                        item.setTitle(text);
                    }
                    i++;
                }
            }
            i = 0;
            for (Node node : pos.childNodes()) {
                if (node instanceof TextNode) {
                    String text = ((TextNode) node).text().trim();
                    if (i == 0 && text.contains("")) {
                        try {
                            item.setReadyDate(fmt.parseLocalDate(text));
                        } catch (IllegalArgumentException e) {
                            item.setStatus(text);
                        }
                    } else if (i == 1) {
                        item.setBranch(text);
                    }
                    i++;
                }
            }
            reservations.add(item);
        }
        return reservations;
    }

    protected void login(Account account) throws IOException,
            OpacErrorException {
        start();
        List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("id", account.getName()));
        nameValuePairs.add(new BasicNameValuePair("pw", account.getPassword()));
        nameValuePairs.add(new BasicNameValuePair("sess", sessid));
        nameValuePairs.add(new BasicNameValuePair("log", "login"));
        nameValuePairs.add(new BasicNameValuePair("weiter", "konto.cgi"));
        String html = httpPost(opac_url + "/login.cgi",
                new UrlEncodedFormEntity(nameValuePairs), ENCODING);
        Document doc = Jsoup.parse(html);
        if (doc.select(".loginbox .meld").size() > 0) {
            throw new OpacErrorException(doc.select(".loginbox .meld").text());
        }
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        login(account);
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
