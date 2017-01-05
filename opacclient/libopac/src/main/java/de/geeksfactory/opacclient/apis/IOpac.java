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
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import de.geeksfactory.opacclient.objects.SearchResult.Status;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * Implementation of Fleischmann iOpac, including account support Seems to work in all the libraries
 * currently supported without any modifications.
 *
 * @author Johan von Forstner, 17.09.2013
 */

public class IOpac extends ApacheBaseApi implements OpacApi {

    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();

    static {
        defaulttypes.put("b", MediaType.BOOK);
        defaulttypes.put("o", MediaType.BOOK);
        defaulttypes.put("e", MediaType.BOOK);
        defaulttypes.put("p", MediaType.BOOK);
        defaulttypes.put("j", MediaType.BOOK);
        defaulttypes.put("g", MediaType.BOOK);
        defaulttypes.put("k", MediaType.BOOK);
        defaulttypes.put("a", MediaType.BOOK);
        defaulttypes.put("c", MediaType.AUDIOBOOK);
        defaulttypes.put("u", MediaType.AUDIOBOOK);
        defaulttypes.put("l", MediaType.AUDIOBOOK);
        defaulttypes.put("q", MediaType.CD_SOFTWARE);
        defaulttypes.put("r", MediaType.CD_SOFTWARE);
        defaulttypes.put("v", MediaType.MOVIE);
        defaulttypes.put("d", MediaType.CD_MUSIC);
        defaulttypes.put("n", MediaType.SCORE_MUSIC);
        defaulttypes.put("s", MediaType.BOARDGAME);
        defaulttypes.put("z", MediaType.MAGAZINE);
        defaulttypes.put("x", MediaType.MAGAZINE);
    }

    protected String opac_url = "";
    protected String dir = "/iopac";
    protected JSONObject data;
    protected String reusehtml;
    protected String rechnr;
    protected int results_total;

    protected boolean newShareLinks;

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);

        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
            if (data.has("dir")) {
                this.dir = data.getString("dir");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected int addParameters(SearchQuery query, List<NameValuePair> params,
            int index) {
        if (query.getValue().equals("")) {
            return index;
        }

        params.add(new BasicNameValuePair(query.getKey(), query.getValue()));
        return index + 1;

    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, OpacErrorException {
        if (!initialised) {
            start();
        }

        List<NameValuePair> params = new ArrayList<>();

        int index = 0;
        start();

        for (SearchQuery query : queries) {
            index = addParameters(query, params, index);
        }

        params.add(new BasicNameValuePair("Anzahl", "10"));
        params.add(new BasicNameValuePair("pshStart", "Suchen"));

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }

        String html = httpPost(opac_url + "/cgi-bin/di.exe",
                new UrlEncodedFormEntity(params, "iso-8859-1"),
                getDefaultEncoding());

        return parse_search(html, 1);
    }

    protected SearchRequestResult parse_search(String html, int page)
            throws OpacErrorException, NotReachableException {
        Document doc = Jsoup.parse(html);

        if (doc.select("h4").size() > 0) {
            if (doc.select("h4").text().trim().startsWith("0 gefundene Medien")) {
                // nothing found
                return new SearchRequestResult(new ArrayList<SearchResult>(),
                        0, 1, 1);
            } else if (!doc.select("h4").text().trim()
                           .contains("gefundene Medien")
                    && !doc.select("h4").text().trim()
                           .contains("Es wurden mehr als")) {
                // error
                throw new OpacErrorException(doc.select("h4").text().trim());
            }
        } else if (doc.select("h1").size() > 0) {
            if (doc.select("h1").text().trim().contains("RUNTIME ERROR")) {
                // Server Error
                throw new NotReachableException("IOPAC RUNTIME ERROR");
            } else {
                throw new OpacErrorException(stringProvider.getFormattedString(
                        StringProvider.UNKNOWN_ERROR_WITH_DESCRIPTION, doc
                                .select("h1").text().trim()));
            }
        } else {
            return null;
        }

        updateRechnr(doc);

        reusehtml = html;

        results_total = -1;

        if (doc.select("h4").text().trim().contains("Es wurden mehr als")) {
            results_total = 200;
        } else {
            String resultnumstr = doc.select("h4").first().text();
            resultnumstr = resultnumstr.substring(0, resultnumstr.indexOf(" "))
                                       .trim();
            results_total = Integer.parseInt(resultnumstr);
        }

        List<SearchResult> results = new ArrayList<>();

        Elements tables = doc.select("table").first().select("tr:has(td)");

        Map<String, Integer> colmap = new HashMap<>();
        Element thead = doc.select("table").first().select("tr:has(th)")
                           .first();
        int j = 0;
        for (Element th : thead.select("th")) {
            String text = th.text().trim().toLowerCase(Locale.GERMAN);
            if (text.contains("cover")) {
                colmap.put("cover", j);
            } else if (text.contains("titel")) {
                colmap.put("title", j);
            } else if (text.contains("verfasser")) {
                colmap.put("author", j);
            } else if (text.contains("mtyp")) {
                colmap.put("category", j);
            } else if (text.contains("jahr")) {
                colmap.put("year", j);
            } else if (text.contains("signatur")) {
                colmap.put("shelfmark", j);
            } else if (text.contains("info")) {
                colmap.put("info", j);
            } else if (text.contains("abteilung")) {
                colmap.put("department", j);
            } else if (text.contains("verliehen") || text.contains("verl.")) {
                colmap.put("returndate", j);
            } else if (text.contains("anz.res")) {
                colmap.put("reservations", j);
            }
            j++;
        }
        if (colmap.size() == 0) {
            colmap.put("cover", 0);
            colmap.put("title", 1);
            colmap.put("author", 2);
            colmap.put("publisher", 3);
            colmap.put("year", 4);
            colmap.put("department", 5);
            colmap.put("shelfmark", 6);
            colmap.put("returndate", 7);
            colmap.put("category", 8);
        }

        for (int i = 0; i < tables.size(); i++) {
            Element tr = tables.get(i);
            SearchResult sr = new SearchResult();

            if (tr.select("td").get(colmap.get("cover")).select("img").size() > 0) {
                String imgUrl = tr.select("td").get(colmap.get("cover"))
                                  .select("img").first().attr("src");
                sr.setCover(imgUrl);
            }

            // Media Type
            if (colmap.get("category") != null) {
                String mType = tr.select("td").get(colmap.get("category"))
                                 .text().trim().replace("\u00a0", "");
                if (data.has("mediatypes")) {
                    try {
                        sr.setType(MediaType.valueOf(data.getJSONObject(
                                "mediatypes").getString(
                                mType.toLowerCase(Locale.GERMAN))));
                    } catch (JSONException | IllegalArgumentException e) {
                        sr.setType(defaulttypes.get(mType
                                .toLowerCase(Locale.GERMAN)));
                    }
                } else {
                    sr.setType(defaulttypes.get(mType
                            .toLowerCase(Locale.GERMAN)));
                }
            }

            // Title and additional info
            String title;
            String additionalInfo = "";
            if (colmap.get("info") != null) {
                Element info = tr.select("td").get(colmap.get("info"));
                title = info.select("a[title=Details-Info]").text().trim();
                String authorIn = info.text().substring(0,
                        info.text().indexOf(title));
                if (authorIn.contains(":")) {
                    authorIn = authorIn.replaceFirst("^([^:]*):(.*)$", "$1");
                    additionalInfo += " - " + authorIn;
                }
            } else {
                title = tr.select("td").get(colmap.get("title")).text().trim()
                          .replace("\u00a0", "");
                if (title.contains("(") && title.indexOf("(") > 0) {
                    additionalInfo += title.substring(title.indexOf("("));
                    title = title.substring(0, title.indexOf("(") - 1).trim();
                }

                // Author
                if (colmap.containsKey("author")) {
                    String author = tr.select("td").get(colmap.get("author"))
                                      .text().trim().replace("\u00a0", "");
                    additionalInfo += " - " + author;
                }
            }

            // Publisher
            if (colmap.containsKey("publisher")) {
                String publisher = tr.select("td").get(colmap.get("publisher"))
                                     .text().trim().replace("\u00a0", "");
                additionalInfo += " (" + publisher;
            }

            // Year
            if (colmap.containsKey("year")) {
                String year = tr.select("td").get(colmap.get("year")).text()
                                .trim().replace("\u00a0", "");
                additionalInfo += ", " + year + ")";
            }

            sr.setInnerhtml("<b>" + title + "</b><br>" + additionalInfo);

            // Status
            String status = tr.select("td").get(colmap.get("returndate"))
                              .text().trim().replace("\u00a0", "");
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
            try {
                df.parse(status);
                // this is a return date
                sr.setStatus(Status.RED);
                sr.setInnerhtml(sr.getInnerhtml() + "<br><i>"
                        + stringProvider.getString(StringProvider.LENT_UNTIL)
                        + " " + status + "</i>");
            } catch (ParseException e) {
                // this is a different status text
                String lc = status.toLowerCase(Locale.GERMAN);
                if ((lc.equals("")
                        || lc.toLowerCase(Locale.GERMAN).contains("onleihe")
                        || lc.contains("verleihbar") || lc.contains("entleihbar")
                        || lc.contains("ausleihbar")) && !lc.contains("nicht")) {
                    sr.setStatus(Status.GREEN);
                } else {
                    sr.setStatus(Status.YELLOW);
                    sr.setInnerhtml(sr.getInnerhtml() + "<br><i>" + status + "</i>");
                }
            }

            // In some libraries (for example search for "atelier" in Preetz)
            // the results are sorted differently than their numbers suggest, so
            // we need to detect the number ("recno") from the link
            String link = tr.select("a[href^=/cgi-bin/di.exe?page=]").attr(
                    "href");
            Map<String, String> params = getQueryParamsFirst(link);
            if (params.containsKey("recno")) {
                int recno = Integer.valueOf(params.get("recno"));
                sr.setNr(recno - 1);
            } else {
                // the above should work, but fall back to this if it doesn't
                sr.setNr(10 * (page - 1) + i);
            }

            // In some libraries (for example Preetz) we can detect the media ID
            // here using another link present in the search results
            Elements idLinks = tr.select("a[href^=/cgi-bin/di.exe?cMedNr]");
            if (idLinks.size() > 0) {
                Map<String, String> idParams = getQueryParamsFirst(idLinks
                        .first().attr("href"));
                String id = idParams.get("cMedNr");
                sr.setId(id);
            } else {
                sr.setId(null);
            }

            results.add(sr);
        }
        return new SearchRequestResult(results, results_total, page);
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        String html = httpGet(opac_url + "/cgi-bin/di.exe?page=" + page
                        + "&rechnr=" + rechnr + "&Anzahl=10&FilNr=",
                getDefaultEncoding());
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

        if (!initialised) {
            start();
        }

        if (id == null && reusehtml != null) {
            return parse_result(reusehtml);
        }

        String html = httpGet(opac_url + "/cgi-bin/di.exe?cMedNr=" + id
                + "&mode=23", getDefaultEncoding());

        return parse_result(html);
    }

    @Override
    public DetailedItem getResult(int position) throws IOException {
        if (!initialised) {
            start();
        }

        int page = Double.valueOf(Math.floor(position / 10)).intValue() + 1;
        String html = httpGet(opac_url + "/cgi-bin/di.exe?page=" + page
                + "&rechnr=" + rechnr + "&Anzahl=10&recno=" + (position + 1)
                + "&FilNr=", getDefaultEncoding());

        return parse_result(html);
    }

    protected DetailedItem parse_result(String html) throws IOException {
        Document doc = Jsoup.parse(html);

        DetailedItem result = new DetailedItem();

        String id = null;
        if (doc.select("input[name=mednr]").size() > 0) {
            id = doc.select("input[name=mednr]").first().val().trim();
        } else if(doc.select("a[href*=mednr]").size() > 0) {
            String href = doc.select("a[href*=mednr]").first().attr("href");
            id = getQueryParamsFirst(href).get("mednr").trim();
        }

        result.setId(id);

        // check if new share button is available (allows to share a link to the standard
        // frameset of the OPAC instead of only the detail frame)
        newShareLinks = doc.select("#sharebutton").size() > 0;

        Elements table = doc.select("table").get(1).select("tr");

        // GET COVER IMAGE
        String imgUrl = table.get(0)
                             .select("img[src~=^https?://(:?images(?:-[^\\.]*)?\\.|[^\\.]*\\" +
                                     ".images-)amazon\\.com]")
                             .attr("src");
        result.setCover(imgUrl);

        // GET INFORMATION
        Copy copy = new Copy();

        for (Element element : table) {
            String detail = element.select("td").text().trim()
                                   .replace("\u00a0", "");
            String title = element.select("th").text().trim()
                                  .replace("\u00a0", "");

            if (!title.equals("")) {

                if (title.contains("verliehen bis")) {
                    if (detail.equals("")) {
                        copy.setStatus("verfügbar");
                    } else {
                        copy.setStatus("verliehen bis " + detail);
                    }
                } else if (title.contains("Abteilung")) {
                    copy.setDepartment(detail);
                } else if (title.contains("Signatur")) {
                    copy.setShelfmark(detail);
                } else if (title.contains("Titel")) {
                    result.setTitle(detail);
                } else if (!title.contains("Cover")) {
                    result.addDetail(new Detail(title, detail));
                }
            }
        }

        // GET RESERVATION INFO
        if ("verfügbar".equals(copy.getStatus())
                || doc.select("a[href^=/cgi-bin/di.exe?mode=10], input.resbutton").size() == 0) {
            result.setReservable(false);
        } else {
            result.setReservable(true);
            if (doc.select("a[href^=/cgi-bin/di.exe?mode=10]").size() > 0) {
                // Reservation via link
                result.setReservation_info(doc
                        .select("a[href^=/cgi-bin/di.exe?mode=10]").first()
                        .attr("href").substring(1).replace(" ", ""));
            } else {
                // Reservation via form (method="get")
                Element form = doc.select("input.resbutton").first().parent();
                result.setReservation_info(generateQuery(form));
            }
        }

        if (copy.notEmpty()) result.addCopy(copy);

        return result;
    }

    private String generateQuery(Element form)
            throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        builder.append(form.attr("action").substring(1));
        int i = 0;
        for (Element input : form.select("input")) {
            builder.append(i == 0 ? "?" : "&");
            builder.append(input.attr("name")).append("=")
                   .append(URLEncoder.encode(input.attr("value"), "UTF-8"));
            i++;
        }
        return builder.toString();
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        String reservation_info = item.getReservation_info();
        // STEP 1: Login page
        String html = httpGet(opac_url + "/" + reservation_info,
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        if (doc.select("table").first().text().contains("kann nicht")) {
            return new ReservationResult(MultiStepResult.Status.ERROR, doc
                    .select("table").first().text().trim());
        }

        if (doc.select("form[name=form1]").size() == 0) {
            return new ReservationResult(MultiStepResult.Status.ERROR);
        }

        Element form = doc.select("form[name=form1]").first();
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("sleKndNr", account.getName()));
        params.add(new BasicNameValuePair("slePw", account.getPassword()));
        params.add(new BasicNameValuePair("pshLogin", "Reservieren"));
        for (Element input : form.select("input[type=hidden]")) {
            params.add(new BasicNameValuePair(input.attr("name"), input
                    .attr("value")));
        }

        // STEP 2: Confirmation page
        html = httpPost(opac_url + "/cgi-bin/di.exe", new UrlEncodedFormEntity(
                params), getDefaultEncoding());
        doc = Jsoup.parse(html);

        if (doc.select("form[name=form1]").size() > 0) {
            // STEP 3: There is another confirmation needed
            form = doc.select("form[name=form1]").first();
            html = httpGet(opac_url + "/" + generateQuery(form),
                    getDefaultEncoding());
            doc = Jsoup.parse(html);
        }

        if (doc.text().contains("fehlgeschlagen")
                || doc.text().contains("Achtung") || doc.text().contains("nicht m")) {
            return new ReservationResult(MultiStepResult.Status.ERROR, doc
                    .select("table").first().text().trim());
        } else {
            return new ReservationResult(MultiStepResult.Status.OK);
        }

    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String Selection) throws IOException {
        // internal convention: We add "NEW" to the media ID to show that we have the new iOPAC
        // version
        if (media.startsWith("NEW")) {
            String mediaNr = media.substring(3);
            String html = httpGet(opac_url + "/cgi-bin/di.exe?mode=42&MedNrVerlAll=" +
                    URLEncoder.encode(mediaNr, "UTF-8"), getDefaultEncoding());

            Document doc = Jsoup.parse(html);
            if (doc.text().contains("1 Medium wurde verl")) {
                return new ProlongResult(MultiStepResult.Status.OK);
            } else {
                return new ProlongResult(MultiStepResult.Status.ERROR, doc.text());
            }
        } else {
            String html = httpGet(opac_url + "/" + media, getDefaultEncoding());
            Document doc = Jsoup.parse(html);
            if (doc.select("table th").size() > 0) {
                if (doc.select("h1").size() > 0) {
                    if (doc.select("h1").first().text().contains("Hinweis")) {
                        return new ProlongResult(MultiStepResult.Status.ERROR, doc
                                .select("table th").first().text());
                    }
                }
                try {
                    Element form = doc.select("form[name=form1]").first();
                    String sessionid = form.select("input[name=sessionid]").attr(
                            "value");
                    String mednr = form.select("input[name=mednr]").attr("value");
                    httpGet(opac_url + "/cgi-bin/di.exe?mode=8&kndnr="
                                    + account.getName() + "&mednr=" + mednr + "&sessionid="
                                    + sessionid + "&psh100=Verl%C3%A4ngern",
                            getDefaultEncoding());
                    return new ProlongResult(MultiStepResult.Status.OK);
                } catch (Throwable e) {
                    e.printStackTrace();
                    return new ProlongResult(MultiStepResult.Status.ERROR);
                }
            }
            return new ProlongResult(MultiStepResult.Status.ERROR);
        }
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        Document doc = getAccountPage(account);
        // Check if the iOPAC verion supports this feature
        if (doc.select("button.verlallbutton").size() > 0) {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("mode", "42"));
            for (Element checkbox : doc.select("input.VerlAllCheckboxOK")) {
                params.add(new BasicNameValuePair("MedNrVerlAll", checkbox.val()));
            }
            String html = httpGet(
                    opac_url + "/cgi-bin/di.exe?" + URLEncodedUtils.format(params, "UTF-8"),
                    getDefaultEncoding());
            Document doc2 = Jsoup.parse(html);
            Pattern pattern = Pattern.compile("(\\d+ Medi(?:en|um) wurden? verl.ngert)\\s*(\\d+ " +
                    "Medi(?:en|um) wurden? nicht verl.ngert)?");
            Matcher matcher = pattern.matcher(doc2.text());
            if (matcher.find()) {
                String text1 = matcher.group(1);
                String text2 = matcher.group(2);
                List<Map<String, String>> list = new ArrayList<>();
                Map<String, String> map1 = new HashMap<>();
                // TODO: We are abusing the ProlongAllResult.KEY_LINE_ ... keys here because we
                // do not get information about all the media
                map1.put(ProlongAllResult.KEY_LINE_TITLE, text1);
                list.add(map1);
                if (text2 != null && !text2.equals("")) {
                    Map<String, String> map2 = new HashMap<>();
                    map2.put(ProlongAllResult.KEY_LINE_TITLE, text2);
                    list.add(map2);
                }
                return new ProlongAllResult(MultiStepResult.Status.OK, list);
            } else {
                return new ProlongAllResult(MultiStepResult.Status.ERROR, doc2.text());
            }
        } else {
            return new ProlongAllResult(MultiStepResult.Status.ERROR,
                    stringProvider.getString(StringProvider.UNSUPPORTED_IN_LIBRARY));
        }
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        String html = httpGet(opac_url + "/" + media, getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        try {
            Element form = doc.select("form[name=form1]").first();
            String sessionid = form.select("input[name=sessionid]").attr(
                    "value");
            String kndnr = form.select("input[name=kndnr]").attr("value");
            String mednr = form.select("input[name=mednr]").attr("value");
            httpGet(opac_url + "/cgi-bin/di.exe?mode=9&kndnr="
                    + kndnr + "&mednr=" + mednr + "&sessionid="
                    + sessionid + "&psh100=Stornieren", getDefaultEncoding());
            return new CancelResult(MultiStepResult.Status.OK);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new NotReachableException(e.getMessage());
        }
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        if (!initialised) {
            start();
        }

        Document doc = getAccountPage(account);

        AccountData res = new AccountData(account.getId());

        List<LentItem> media = new ArrayList<>();
        List<ReservedItem> reserved = new ArrayList<>();
        parseMediaList(media, doc, data);
        parseResList(reserved, doc, data);

        res.setLent(media);
        res.setReservations(reserved);
        if (doc.select("h4:contains(Kontostand)").size() > 0) {
            Element h4 = doc.select("h4:contains(Kontostand)").first();
            Pattern regex = Pattern.compile("Kontostand (-?\\d+\\.\\d\\d EUR)");
            Matcher matcher = regex.matcher(h4.text());
            if (matcher.find()) res.setPendingFees(matcher.group(1));
        }
        if (doc.select("h4:contains(Ausweis g)").size() > 0) {
            Element h4 = doc.select("h4:contains(Ausweis g)").first();
            Pattern regex =
                    Pattern.compile("Ausweis g.+ltig bis\\s*.\\s*(\\d\\d.\\d\\d.\\d\\d\\d\\d)");
            Matcher matcher = regex.matcher(h4.text());
            if (matcher.find()) res.setValidUntil(matcher.group(1));
        }
        if (doc.select(".ReaderAccount_expiredID").size() > 0) {
            res.setWarning(doc.select(".ReaderAccount_expiredID").text());
        }

        if (media.isEmpty() && reserved.isEmpty()) {
            if (doc.select("h1").size() > 0) {
                //noinspection StatementWithEmptyBody
                if (doc.select("h4").text().trim().contains("keine ausgeliehenen Medien")) {
                    // There is no lent media, but the server is working
                    // correctly
                } else if (doc.select("h1").text().trim()
                              .contains("RUNTIME ERROR")) {
                    // Server Error
                    throw new NotReachableException("IOPAC RUNTIME ERROR");
                } else {
                    throw new OpacErrorException(
                            stringProvider
                                    .getFormattedString(
                                            StringProvider.UNKNOWN_ERROR_ACCOUNT_WITH_DESCRIPTION,
                                            doc.select("h1").text().trim()));
                }
            } else {
                throw new OpacErrorException(
                        stringProvider
                                .getString(StringProvider.UNKNOWN_ERROR_ACCOUNT));
            }
        }
        return res;

    }

    private Document getAccountPage(Account account) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("sleKndNr", account.getName()));
        params.add(new BasicNameValuePair("slePw", account.getPassword()));
        params.add(new BasicNameValuePair("pshLogin", "Login"));

        String html = httpPost(opac_url + "/cgi-bin/di.exe",
                new UrlEncodedFormEntity(params, "iso-8859-1"),
                getDefaultEncoding());
        return Jsoup.parse(html);
    }

    public void checkAccountData(Account account) throws IOException,
            OpacErrorException {
        Document doc = getAccountPage(account);
        if (doc.select("h1, .HTMLInfo_Head").text().contains("fehlgeschlagen")) {
            throw new OpacErrorException(doc.select("h1, th, .HTMLInfo_Text").text());
        }
    }

    static void parseMediaList(List<LentItem> media, Document doc, JSONObject data) {
        if (doc.select("a[name=AUS]").size() == 0) return;

        Elements copytrs = doc.select("a[name=AUS] ~ table, a[name=AUS] ~ form table").first()
                              .select("tr");
        doc.setBaseUri(data.optString("baseurl"));

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        int trs = copytrs.size();
        if (trs < 2) {
            return;
        }
        assert (trs > 0);

        JSONObject copymap = new JSONObject();
        try {
            if (data.has("accounttable")) {
                copymap = data.getJSONObject("accounttable");
            }
        } catch (JSONException e) {
        }

        Pattern datePattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
        Pattern reservedPattern = Pattern.compile("\\d+ x reserv.");
        for (int i = 1; i < trs; i++) {
            Element tr = copytrs.get(i);
            LentItem item = new LentItem();

            if (copymap.optInt("title", 0) >= 0) {
                item.setTitle(
                        tr.child(copymap.optInt("title", 0)).text().trim().replace("\u00a0", ""));
            }
            if (copymap.optInt("author", 1) >= 0) {
                item.setAuthor(
                        tr.child(copymap.optInt("author", 1)).text().trim().replace("\u00a0", ""));
            }
            if (copymap.optInt("format", 2) >= 0) {
                item.setFormat(
                        tr.child(copymap.optInt("format", 2)).text().trim().replace("\u00a0", ""));
            }
            int prolongCount = 0;
            if (copymap.optInt("prolongcount", 3) >= 0) {
                prolongCount = Integer.parseInt(tr
                        .child(copymap.optInt("prolongcount", 3)).text().trim()
                        .replace("\u00a0", ""));
                item.setStatus(String.valueOf(prolongCount) + "x verl.");
            }
            if (data.optInt("maxprolongcount", -1) != -1) {
                item.setRenewable(prolongCount < data.optInt("maxprolongcount", -1));
            }
            if (copymap.optInt("returndate", 4) >= 0) {
                String value = tr.child(copymap.optInt("returndate", 4)).text().trim()
                                 .replace("\u00a0", "");
                Matcher matcher = datePattern.matcher(value);
                if (matcher.find()) {
                    try {
                        item.setDeadline(fmt.parseLocalDate(matcher.group()));
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    }
                }
                matcher = reservedPattern.matcher(value);
                if (matcher.find()) {
                    if (item.getStatus() != null) {
                        item.setStatus(item.getStatus() + ", " + matcher.group());
                    } else {
                        item.setStatus(matcher.group());
                    }
                }
            }
            if (copymap.optInt("prolongurl", 5) >= 0) {
                if (tr.children().size() > copymap.optInt("prolongurl", 5)) {
                    Element cell = tr.child(copymap.optInt("prolongurl", 5));
                    if (cell.select("input[name=MedNrVerlAll]").size() > 0) {
                        // new iOPAC Version 1.45 - checkboxes to prolong multiple items
                        // internal convention: We add "NEW" to the media ID to show that we have
                        // the new iOPAC version
                        Element input = cell.select("input[name=MedNrVerlAll]").first();
                        String value = input.val();
                        item.setProlongData("NEW" + value);
                        item.setId(value.split(";")[0]);
                        if (input.hasAttr("disabled")) item.setRenewable(false);
                    } else {
                        // previous versions - link for prolonging on every medium
                        String link = cell.select("a").attr("href");
                        item.setProlongData(link);
                        // find media number with regex
                        Pattern pattern = Pattern.compile("mednr=([^&]*)&");
                        Matcher matcher = pattern.matcher(link);
                        if (matcher.find() && matcher.group() != null) item.setId(matcher.group(1));
                    }
                }
            }

            media.add(item);
        }
        assert (media.size() == trs - 1);

    }

    static void parseResList(List<ReservedItem> media, Document doc, JSONObject data) {
        if (doc.select("a[name=RES]").size() == 0) return;
        Elements copytrs = doc.select("a[name=RES] ~ table:contains(Titel)").first().select("tr");
        doc.setBaseUri(data.optString("baseurl"));
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        int trs = copytrs.size();
        if (trs < 2) {
            return;
        }
        assert (trs > 0);
        for (int i = 1; i < trs; i++) {
            Element tr = copytrs.get(i);
            ReservedItem item = new ReservedItem();

            item.setTitle(tr.child(0).text().trim().replace("\u00a0", ""));
            item.setAuthor(tr.child(1).text().trim().replace("\u00a0", ""));
            String readyDate = tr.child(4).text().trim().replace("\u00a0", "");
            if (readyDate.equals("")) {
                item.setStatus("bereit");
            } else {
                try {
                    item.setReadyDate(
                            fmt.parseLocalDate(readyDate));
                } catch (IllegalArgumentException e) {
                    item.setStatus(readyDate);
                }
            }
            if (tr.select("a").size() > 0) {
                item.setCancelData(tr.select("a").last().attr("href"));
            }

            media.add(item);
        }
        assert (media.size() == trs - 1);

    }

    private SearchField createSearchField(Element descTd, Element inputTd) {
        String name = descTd.select("span, blockquote").text().replace(":", "")
                            .trim().replace("\u00a0", "");
        if (inputTd.select("select").size() > 0
                && !name.equals("Treffer/Seite") && !name.equals("Medientypen")
                && !name.equals("Medientyp")
                && !name.equals("Treffer pro Seite")) {
            Element select = inputTd.select("select").first();
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName(name);
            field.setId(select.attr("name"));
            for (Element option : select.select("option")) {
                field.addDropdownValue(option.attr("value"), option.text());
            }
            return field;
        } else if (inputTd.select("input").size() > 0) {
            TextSearchField field = new TextSearchField();
            Element input = inputTd.select("input").first();
            field.setDisplayName(name);
            field.setId(input.attr("name"));
            field.setHint("");
            return field;
        } else {
            return null;
        }
    }

    @Override
    public List<SearchField> parseSearchFields() throws IOException {
        List<SearchField> fields = new ArrayList<>();

        // Extract all search fields, except media types
        String html;
        try {
            html = httpGet(opac_url + dir + "/search_expert.htm",
                    getDefaultEncoding());
        } catch (NotReachableException e) {
            html = httpGet(opac_url + dir + "/iopacie.htm",
                    getDefaultEncoding());
        }
        Document doc = Jsoup.parse(html);
        Elements trs = doc
                .select("form tr:has(input:not([type=submit], [type=reset])), form tr:has(select)");
        for (Element tr : trs) {
            Elements tds = tr.children();
            if (tds.size() == 4) {
                // Two search fields next to each other in one row
                SearchField field1 = createSearchField(tds.get(0), tds.get(1));
                SearchField field2 = createSearchField(tds.get(2), tds.get(3));
                if (field1 != null) {
                    fields.add(field1);
                }
                if (field2 != null) {
                    fields.add(field2);
                }
            } else if (tds.size() == 2
                    || (tds.size() == 3 && tds.get(2).children().size() == 0)) {
                SearchField field = createSearchField(tds.get(0), tds.get(1));
                if (field != null) {
                    fields.add(field);
                }
            }
        }

        if (fields.size() == 0 && doc.select("[name=sleStichwort]").size() > 0) {
            TextSearchField field = new TextSearchField();
            Element input = doc.select("input[name=sleStichwort]").first();
            field.setDisplayName(stringProvider
                    .getString(StringProvider.FREE_SEARCH));
            field.setId(input.attr("name"));
            field.setHint("");
            fields.add(field);
        }

        // Extract available media types.
        // We have to parse JavaScript. Doing this with RegEx is evil.
        // But not as evil as including a JavaScript VM into the app.
        // And I honestly do not see another way.
        Pattern pattern_key = Pattern
                .compile("mtyp\\[[0-9]+\\]\\[\"typ\"\\] = \"([^\"]+)\";");
        Pattern pattern_value = Pattern
                .compile("mtyp\\[[0-9]+\\]\\[\"bez\"\\] = \"([^\"]+)\";");

        DropdownSearchField mtyp = new DropdownSearchField();
        try {
            try {
                html = httpGet(opac_url + dir + "/mtyp.js", getDefaultEncoding());
            } catch (NotReachableException e) {
                html = httpGet(opac_url + "/mtyp.js", getDefaultEncoding());
            }

            String[] parts = html.split("new Array\\(\\);");
            for (String part : parts) {
                Matcher matcher1 = pattern_key.matcher(part);
                String key = "";
                String value = "";
                if (matcher1.find()) {
                    key = matcher1.group(1);
                }
                Matcher matcher2 = pattern_value.matcher(part);
                if (matcher2.find()) {
                    value = matcher2.group(1);
                }
                if (!value.equals("")) {
                    mtyp.addDropdownValue(key, value);
                }
            }
        } catch (IOException e) {
            try {
                html = httpGet(opac_url + dir
                                + "/frames/search_form.php?bReset=1?bReset=1",
                        getDefaultEncoding());
                doc = Jsoup.parse(html);

                for (Element opt : doc.select("#imtyp option")) {
                    mtyp.addDropdownValue(opt.attr("value"), opt.text());
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
        if (mtyp.getDropdownValues() != null && !mtyp.getDropdownValues().isEmpty()) {
            mtyp.setDisplayName("Medientypen");
            mtyp.setId("Medientyp");
            fields.add(mtyp);
        }
        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        if (newShareLinks) {
            return opac_url + dir + "/?mednr=" + id;
        } else {
            return opac_url + "/cgi-bin/di.exe?cMedNr=" + id + "&mode=23";
        }
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT |
                SUPPORT_FLAG_ACCOUNT_PROLONG_ALL;
    }

    public void updateRechnr(Document doc) {
        String url = null;
        for (Element a : doc.select("table a")) {
            if (a.attr("href").contains("rechnr=")) {
                url = a.attr("href");
                break;
            }
        }
        if (url == null) {
            return;
        }

        Integer rechnrPosition = url.indexOf("rechnr=") + 7;
        rechnr = url
                .substring(rechnrPosition, url.indexOf("&", rechnrPosition));
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
