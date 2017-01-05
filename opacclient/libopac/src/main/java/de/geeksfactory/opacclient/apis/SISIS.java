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
import org.apache.http.client.ClientProtocolException;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult.Status;
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
 * OpacApi implementation for Web Opacs of the SISIS SunRise product, developed by OCLC.
 *
 * Restrictions: Bookmarks are only constantly supported if the library uses the BibTip extension.
 */
public class SISIS extends ApacheBaseApi implements OpacApi {
    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();

    static {
        defaulttypes.put("g", MediaType.EBOOK);
        defaulttypes.put("d", MediaType.CD);
        defaulttypes.put("Buch", MediaType.BOOK);
        defaulttypes.put("Bücher", MediaType.BOOK);
        defaulttypes.put("Printmedien", MediaType.BOOK);
        defaulttypes.put("Zeitschrift", MediaType.MAGAZINE);
        defaulttypes.put("Zeitschriften", MediaType.MAGAZINE);
        defaulttypes.put("zeitung", MediaType.NEWSPAPER);
        defaulttypes.put(
                "Einzelband einer Serie, siehe auch übergeordnete Titel",
                MediaType.BOOK);
        defaulttypes.put("0", MediaType.BOOK);
        defaulttypes.put("1", MediaType.BOOK);
        defaulttypes.put("2", MediaType.BOOK);
        defaulttypes.put("3", MediaType.BOOK);
        defaulttypes.put("4", MediaType.BOOK);
        defaulttypes.put("5", MediaType.BOOK);
        defaulttypes.put("Buch-Kinderbuch", MediaType.BOOK);
        defaulttypes.put("6", MediaType.SCORE_MUSIC);
        defaulttypes.put("7", MediaType.CD_MUSIC);
        defaulttypes.put("8", MediaType.CD_MUSIC);
        defaulttypes.put("Tonträger", MediaType.CD_MUSIC);
        defaulttypes.put("12", MediaType.CD);
        defaulttypes.put("13", MediaType.CD);
        defaulttypes.put("CD", MediaType.CD);
        defaulttypes.put("DVD", MediaType.DVD);
        defaulttypes.put("14", MediaType.CD);
        defaulttypes.put("15", MediaType.DVD);
        defaulttypes.put("16", MediaType.CD);
        defaulttypes.put("audiocd", MediaType.CD);
        defaulttypes.put("Film", MediaType.MOVIE);
        defaulttypes.put("Filme", MediaType.MOVIE);
        defaulttypes.put("17", MediaType.MOVIE);
        defaulttypes.put("18", MediaType.MOVIE);
        defaulttypes.put("19", MediaType.MOVIE);
        defaulttypes.put("20", MediaType.DVD);
        defaulttypes.put("dvd", MediaType.DVD);
        defaulttypes.put("21", MediaType.SCORE_MUSIC);
        defaulttypes.put("Noten", MediaType.SCORE_MUSIC);
        defaulttypes.put("22", MediaType.BOARDGAME);
        defaulttypes.put("26", MediaType.CD);
        defaulttypes.put("27", MediaType.CD);
        defaulttypes.put("28", MediaType.EBOOK);
        defaulttypes.put("31", MediaType.BOARDGAME);
        defaulttypes.put("35", MediaType.MOVIE);
        defaulttypes.put("36", MediaType.DVD);
        defaulttypes.put("37", MediaType.CD);
        defaulttypes.put("29", MediaType.AUDIOBOOK);
        defaulttypes.put("41", MediaType.GAME_CONSOLE);
        defaulttypes.put("42", MediaType.GAME_CONSOLE);
        defaulttypes.put("46", MediaType.GAME_CONSOLE_NINTENDO);
        defaulttypes.put("52", MediaType.EBOOK);
        defaulttypes.put("56", MediaType.EBOOK);
        defaulttypes.put("96", MediaType.EBOOK);
        defaulttypes.put("97", MediaType.EBOOK);
        defaulttypes.put("99", MediaType.EBOOK);
        defaulttypes.put("EB", MediaType.EBOOK);
        defaulttypes.put("ebook", MediaType.EBOOK);
        defaulttypes.put("buch01", MediaType.BOOK);
        defaulttypes.put("buch02", MediaType.PACKAGE_BOOKS);
        defaulttypes.put("Medienpaket", MediaType.PACKAGE);
        defaulttypes.put("datenbank", MediaType.PACKAGE);
        defaulttypes
                .put("Medienpaket, Lernkiste, Lesekiste", MediaType.PACKAGE);
        defaulttypes.put("buch03", MediaType.BOOK);
        defaulttypes.put("buch04", MediaType.PACKAGE_BOOKS);
        defaulttypes.put("buch05", MediaType.PACKAGE_BOOKS);
        defaulttypes.put("Web-Link", MediaType.URL);
        defaulttypes.put("ejournal", MediaType.EDOC);
        defaulttypes.put("karte", MediaType.MAP);
    }

    protected final long SESSION_LIFETIME = 1000 * 60 * 3;
    protected String opac_url = "";
    protected JSONObject data;
    protected String CSId;
    protected String identifier;
    protected int resultcount = 10;
    protected long logged_in;
    protected Account logged_in_as;
    protected static final String ENCODING = "UTF-8";

    protected String getDefaultEncoding() {
        return ENCODING;
    }

    public List<SearchField> parseSearchFields() throws IOException,
            JSONException {
        if (!initialised) {
            start();
        }

        String html = httpGet(opac_url
                        + "/search.do?methodToCall=switchSearchPage&SearchType=2",
                ENCODING);
        Document doc = Jsoup.parse(html);
        List<SearchField> fields = new ArrayList<>();

        Elements options = doc
                .select("select[name=searchCategories[0]] option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.attr("value"));
            field.setHint("");
            fields.add(field);
        }

        for (Element dropdown : doc.select("#tab-content select")) {
            parseDropdown(dropdown, fields);
        }

        return fields;
    }

    private void parseDropdown(Element dropdownElement,
            List<SearchField> fields) throws JSONException {
        Elements options = dropdownElement.select("option");
        DropdownSearchField dropdown = new DropdownSearchField();
        if (dropdownElement.parent().select("input[type=hidden]").size() > 0) {
            dropdown.setId(dropdownElement.parent()
                                          .select("input[type=hidden]").attr("value"));
            dropdown.setData(new JSONObject("{\"restriction\": true}"));
        } else {
            dropdown.setId(dropdownElement.attr("name"));
            dropdown.setData(new JSONObject("{\"restriction\": false}"));
        }
        for (Element option : options) {
            dropdown.addDropdownValue(option.attr("value"), option.text());
        }
        dropdown.setDisplayName(dropdownElement.parent().select("label").text());
        fields.add(dropdown);
    }

    @Override
    public void start() throws
            IOException {

        // Some libraries require start parameters for start.do, like Login=foo
        String startparams = "";
        if (data.has("startparams")) {
            try {
                startparams = "?" + data.getString("startparams");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String html = httpGet(opac_url + "/start.do" + startparams, ENCODING);

        initialised = true;

        Document doc = Jsoup.parse(html);
        CSId = doc.select("input[name=CSId]").val();

        super.start();
    }

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException,
            JSONException {
        List<NameValuePair> params = new ArrayList<>();

        int index = 0;
        int restrictionIndex = 0;
        start();

        params.add(new BasicNameValuePair("methodToCall", "submit"));
        params.add(new BasicNameValuePair("CSId", CSId));
        params.add(new BasicNameValuePair("methodToCallParameter",
                "submitSearch"));

        for (SearchQuery entry : query) {
            if (entry.getValue().equals("")) {
                continue;
            }
            if (entry.getSearchField() instanceof DropdownSearchField) {
                JSONObject data = entry.getSearchField().getData();
                if (data.optBoolean("restriction", false)) {
                    params.add(new BasicNameValuePair("searchRestrictionID["
                            + restrictionIndex + "]", entry.getSearchField()
                                                           .getId()));
                    params.add(new BasicNameValuePair(
                            "searchRestrictionValue1[" + restrictionIndex + "]",
                            entry.getValue()));
                    restrictionIndex++;
                } else {
                    params.add(new BasicNameValuePair(entry.getKey(), entry
                            .getValue()));
                }
            } else {
                if (index != 0) {
                    params.add(new BasicNameValuePair("combinationOperator["
                            + index + "]", "AND"));
                }
                params.add(new BasicNameValuePair("searchCategories[" + index
                        + "]", entry.getKey()));
                params.add(new BasicNameValuePair(
                        "searchString[" + index + "]", entry.getValue()));
                index++;
            }
        }

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }
        if (index > 4) {
            throw new OpacErrorException(stringProvider.getQuantityString(
                    StringProvider.LIMITED_NUM_OF_CRITERIA, 4, 4));
        }

        params.add(new BasicNameValuePair("submitSearch", "Suchen"));
        params.add(new BasicNameValuePair("callingPage", "searchParameters"));
        params.add(new BasicNameValuePair("numberOfHits", "10"));

        String html = httpGet(
                opac_url + "/search.do?"
                        + URLEncodedUtils.format(params, "UTF-8"), ENCODING);
        return parse_search_wrapped(html, 1);
    }

    public SearchRequestResult volumeSearch(Map<String, String> query)
            throws IOException, OpacErrorException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("methodToCall", "volumeSearch"));
        params.add(new BasicNameValuePair("dbIdentifier", query
                .get("dbIdentifier")));
        params.add(new BasicNameValuePair("catKey", query.get("catKey")));
        params.add(new BasicNameValuePair("periodical", "N"));
        String html = httpGet(
                opac_url + "/search.do?"
                        + URLEncodedUtils.format(params, "UTF-8"), ENCODING);
        return parse_search_wrapped(html, 1);
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        String html = httpGet(opac_url
                + "/hitList.do?methodToCall=pos&identifier=" + identifier
                + "&curPos=" + (((page - 1) * resultcount) + 1), ENCODING);
        return parse_search_wrapped(html, page);
    }

    public class SingleResultFound extends Exception {
    }

    protected SearchRequestResult parse_search_wrapped(String html, int page)
            throws IOException, OpacErrorException {
        try {
            return parse_search(html, page);
        } catch (SingleResultFound e) {
            html = httpGet(opac_url + "/hitList.do?methodToCall=backToPrimaryHitList", ENCODING);
            try {
                return parse_search(html, page);
            } catch (SingleResultFound e1) {
                throw new NotReachableException();
            }
        }
    }

    public SearchRequestResult parse_search(String html, int page)
            throws OpacErrorException, SingleResultFound {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url + "/searchfoo");

        if (doc.select(".error").size() > 0) {
            throw new OpacErrorException(doc.select(".error").text().trim());
        } else if (doc.select(".nohits").size() > 0) {
            throw new OpacErrorException(doc.select(".nohits").text().trim());
        } else if (doc.select(".box-header h2, #nohits").text()
                      .contains("keine Treffer")) {
            return new SearchRequestResult(new ArrayList<SearchResult>(), 0, 1,
                    1);
        }

        int results_total = -1;

        String resultnumstr = doc.select(".box-header h2").first().text();
        if (resultnumstr.contains("(1/1)") || resultnumstr.contains(" 1/1")) {
            throw new SingleResultFound();
        } else if (resultnumstr.contains("(")) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(
                    ".*\\(([0-9]+)\\).*", "$1"));
        } else if (resultnumstr.contains(": ")) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(
                    ".*: ([0-9]+)$", "$1"));
        }

        Elements table = doc.select("table.data tbody tr");
        identifier = null;

        Elements links = doc.select("table.data a");
        boolean haslink = false;
        for (int i = 0; i < links.size(); i++) {
            Element node = links.get(i);
            if (node.hasAttr("href")
                    & node.attr("href").contains("singleHit.do") && !haslink) {
                haslink = true;
                try {
                    List<NameValuePair> anyurl = URLEncodedUtils.parse(
                            new URI(node.attr("href").replace(" ", "%20")
                                        .replace("&amp;", "&")), ENCODING);
                    for (NameValuePair nv : anyurl) {
                        if (nv.getName().equals("identifier")) {
                            identifier = nv.getValue();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            Element tr = table.get(i);
            SearchResult sr = new SearchResult();
            if (tr.select("td img[title]").size() > 0) {
                String title = tr.select("td img").get(0).attr("title");
                String[] fparts = tr.select("td img").get(0).attr("src")
                                    .split("/");
                String fname = fparts[fparts.length - 1];
                MediaType default_by_fname = defaulttypes.get(fname
                        .toLowerCase(Locale.GERMAN).replace(".jpg", "")
                        .replace(".gif", "").replace(".png", ""));
                MediaType default_by_title = defaulttypes.get(title);
                MediaType default_name = default_by_title != null ? default_by_title
                        : default_by_fname;
                if (data.has("mediatypes")) {
                    try {
                        sr.setType(MediaType.valueOf(data.getJSONObject(
                                "mediatypes").getString(fname)));
                    } catch (JSONException | IllegalArgumentException e) {
                        sr.setType(default_name);
                    }
                } else {
                    sr.setType(default_name);
                }
            }
            String alltext = tr.text();
            if (alltext.contains("eAudio") || alltext.contains("eMusic")) {
                sr.setType(MediaType.MP3);
            } else if (alltext.contains("eVideo")) {
                sr.setType(MediaType.EVIDEO);
            } else if (alltext.contains("eBook")) {
                sr.setType(MediaType.EBOOK);
            } else if (alltext.contains("Munzinger")) {
                sr.setType(MediaType.EDOC);
            }

            if (tr.children().size() > 3
                    && tr.child(3).select("img[title*=cover]").size() == 1) {
                sr.setCover(tr.child(3).select("img[title*=cover]")
                              .attr("abs:src"));
                if (sr.getCover().contains("showCover.do")) {
                    downloadCover(sr);
                }
            }

            Element middlething;
            if (tr.children().size() > 2 && tr.child(2).select("a").size() > 0) {
                middlething = tr.child(2);
            } else {
                middlething = tr.child(1);
            }

            List<Node> children = middlething.childNodes();
            if (middlething.select("div")
                           .not("#hlrightblock,.bestellfunktionen").size() == 1) {
                Element indiv = middlething.select("div")
                                           .not("#hlrightblock,.bestellfunktionen").first();
                if (indiv.select("a").size() > 0 && indiv.children().size() > 1) {
                    children = indiv.childNodes();
                }
            } else if (middlething.select("span.titleData").size() == 1) {
                children = middlething.select("span.titleData").first()
                                      .childNodes();
            }
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

            StringBuilder description = null;
            if (tr.select("span.Z3988").size() == 1) {
                // Sometimes there is a <span class="Z3988"> item which provides
                // data in a standardized format.
                List<NameValuePair> z3988data;
                boolean hastitle = false;
                try {
                    description = new StringBuilder();
                    z3988data = URLEncodedUtils.parse(new URI("http://dummy/?"
                            + tr.select("span.Z3988").attr("title")), "UTF-8");
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
                                    description.append("<br />").append(nv.getValue());
                                } else if (nv.getName().equals("rft.date")) {
                                    description.append("<br />").append(nv.getValue());
                                }
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    description = null;
                }
            }
            boolean described = false;
            if (description != null && description.length() > 0) {
                sr.setInnerhtml(description.toString());
                described = true;
            } else {
                description = new StringBuilder();
            }
            int k = 0;
            boolean yearfound = false;
            boolean titlefound = false;
            boolean sigfound = false;
            for (String[] part : strings) {
                if (!described) {
                    if (part[0].equals("a") && (k == 0 || !titlefound)) {
                        if (k != 0) {
                            description.append("<br />");
                        }
                        description.append("<b>").append(part[2]).append("</b>");
                        titlefound = true;
                    } else if (part[2].matches("\\D*[0-9]{4}\\D*")
                            && part[2].length() <= 10) {
                        yearfound = true;
                        if (k != 0) {
                            description.append("<br />");
                        }
                        description.append(part[2]);
                    } else if (k == 1 && !yearfound
                            && part[2].matches("^\\s*\\([0-9]{4}\\)$")) {
                        if (k != 0) {
                            description.append("<br />");
                        }
                        description.append(part[2]);
                    } else if (k == 1 && !yearfound
                            && part[2].matches("^\\s*\\([0-9]{4}\\)$")) {
                        if (k != 0) {
                            description.append("<br />");
                        }
                        description.append(part[2]);
                    } else if (k == 1 && !yearfound) {
                        description.append("<br />");
                        description.append(part[2]);
                    } else if (k > 1 && k < 4 && !sigfound
                            && part[0].equals("text")
                            && part[2].matches("^[A-Za-z0-9,\\- ]+$")) {
                        description.append("<br />");
                        description.append(part[2]);
                    }
                }
                if (part.length == 4) {
                    if (part[0].equals("span") && part[3].equals("textgruen")) {
                        sr.setStatus(SearchResult.Status.GREEN);
                    } else if (part[0].equals("span")
                            && part[3].equals("textrot")) {
                        sr.setStatus(SearchResult.Status.RED);
                    }
                } else if (part.length == 5) {
                    if (part[4].contains("purple")) {
                        sr.setStatus(SearchResult.Status.YELLOW);
                    }
                }
                if (sr.getStatus() == null) {
                    if ((part[2].contains("entliehen") && part[2]
                            .startsWith("Vormerkung ist leider nicht möglich"))
                            || part[2].contains("Alle Exemplare des gewählten Titels sind entliehen")
                            || part[2]
                            .contains(
                                    "nur in anderer Zweigstelle ausleihbar und nicht bestellbar")) {
                        sr.setStatus(SearchResult.Status.RED);
                    } else if (part[2].startsWith("entliehen")
                            || part[2]
                            .contains("Ein Exemplar finden Sie in einer anderen Zweigstelle")) {
                        sr.setStatus(SearchResult.Status.YELLOW);
                    } else if ((part[2].startsWith("bestellbar") && !part[2]
                            .contains("nicht bestellbar"))
                            || (part[2].startsWith("vorbestellbar") && !part[2]
                            .contains("nicht vorbestellbar"))
                            || (part[2].startsWith("vorbestellbar") && !part[2]
                            .contains("nicht vorbestellbar"))
                            || (part[2].startsWith("vormerkbar") && !part[2]
                            .contains("nicht vormerkbar"))
                            || (part[2].contains("heute zurückgebucht"))
                            || (part[2].contains("ausleihbar") && !part[2]
                            .contains("nicht ausleihbar"))) {
                        sr.setStatus(SearchResult.Status.GREEN);
                    }
                    if (sr.getType() != null) {
                        if (sr.getType().equals(MediaType.EBOOK)
                                || sr.getType().equals(MediaType.EVIDEO)
                                || sr.getType().equals(MediaType.MP3))
                        // Especially Onleihe.de ebooks are often marked
                        // green though they are not available.
                        {
                            sr.setStatus(SearchResult.Status.UNKNOWN);
                        }
                    }
                }
                k++;
            }
            if (!described) {
                sr.setInnerhtml(description.toString());
            }

            sr.setNr(10 * (page - 1) + i);
            sr.setId(null);
            results.add(sr);
        }
        resultcount = results.size();
        return new SearchRequestResult(results, results_total, page);
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException {

        if (id.startsWith("http://") || id.startsWith("https://")) {
            return loadDetail(id);
        }

        // Some libraries require start parameters for start.do, like Login=foo
        String startparams = "";
        if (data.has("startparams")) {
            try {
                startparams = data.getString("startparams") + "&";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String hbp = "";
        if (homebranch != null) {
            hbp = "&selectedViewBranchlib=" + homebranch;
        }

        String html = httpGet(opac_url + "/start.do?" + startparams
                + "searchType=1&Query=0%3D%22" + id + "%22" + hbp, ENCODING);

        return loadDetail(html);
    }

    @Override
    public DetailedItem getResult(int nr) throws IOException {

        String html = httpGet(
                opac_url
                        + "/singleHit.do?tab=showExemplarActive&methodToCall=showHit&curPos="
                        + (nr + 1) + "&identifier=" + identifier, ENCODING);

        return loadDetail(html);
    }

    protected DetailedItem loadDetail(String html) throws IOException {
        String html2 = httpGet(opac_url
                        + "/singleHit.do?methodToCall=activateTab&tab=showTitleActive",
                ENCODING);
        String html3 = httpGet(
                opac_url
                        + "/singleHit.do?methodToCall=activateTab&tab=showAvailabilityActive",
                ENCODING);

        String coverJs = null;
        Pattern coverPattern = Pattern.compile("\\$\\.ajax\\(\\{[\\n\\s]*url: '(jsp/result/cover" +
                ".jsp\\?[^']+')");
        Matcher coverMatcher = coverPattern.matcher(html);
        if (coverMatcher.find()) {
            coverJs = httpGet(opac_url + "/" + coverMatcher.group(1), ENCODING);
        }

        DetailedItem result = parseDetail(html, html2, html3, coverJs, data, stringProvider);
        try {
            if (!result.getCover().contains("amazon")) downloadCover(result);
        } catch (Exception e) {

        }
        return result;
    }

    static DetailedItem parseDetail(String html, String html2, String html3, String coverJs,
            JSONObject data,
            StringProvider stringProvider)
            throws IOException {
        Document doc = Jsoup.parse(html);
        String opac_url = data.optString("baseurl", "");
        doc.setBaseUri(opac_url);

        Document doc2 = Jsoup.parse(html2);
        doc2.setBaseUri(opac_url);

        Document doc3 = Jsoup.parse(html3);
        doc3.setBaseUri(opac_url);

        DetailedItem result = new DetailedItem();

        try {
            result.setId(doc.select("#bibtip_id").text().trim());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        List<String> reservationlinks = new ArrayList<>();
        for (Element link : doc3.select("#vormerkung a, #tab-content a")) {
            String href = link.absUrl("href");
            Map<String, String> hrefq = getQueryParamsFirst(href);
            if (result.getId() == null) {
                // ID retrieval
                String key = hrefq.get("katkey");
                if (key != null) {
                    result.setId(key);
                    break;
                }
            }

            // Vormerken
            if (hrefq.get("methodToCall") != null) {
                if (hrefq.get("methodToCall").equals("doVormerkung")
                        || hrefq.get("methodToCall").equals("doBestellung")) {
                    reservationlinks.add(href.split("\\?")[1]);
                }
            }
        }
        if (reservationlinks.size() == 1) {
            result.setReservable(true);
            result.setReservation_info(reservationlinks.get(0));
        } else if (reservationlinks.size() == 0) {
            result.setReservable(false);
        } else {
            // TODO: Multiple options - handle this case!
        }

        if (result.getId() == null && doc.select("#permalink_link").size() > 0) {
            result.setId(doc.select("#permalink_link").text());
        }

        if (coverJs != null) {
            Pattern srcPattern = Pattern.compile("<img .* src=\"([^\"]+)\">");
            Matcher matcher = srcPattern.matcher(coverJs);
            if (matcher.find()) {
                result.setCover(matcher.group(1));
            }
        } else if (doc.select(".data td img").size() == 1) {
            result.setCover(doc.select(".data td img").first().attr("abs:src"));
        }

        if (doc.select(".aw_teaser_title").size() == 1) {
            result.setTitle(doc.select(".aw_teaser_title").first().text()
                               .trim());
        } else if (doc.select(".data td strong").size() > 0) {
            result.setTitle(doc.select(".data td strong").first().text().trim());
        } else {
            result.setTitle("");
        }
        if (doc.select(".aw_teaser_title_zusatz").size() > 0) {
            result.addDetail(new Detail("Titelzusatz", doc
                    .select(".aw_teaser_title_zusatz").text().trim()));
        }

        String title = "";
        String text = "";
        boolean takeover = false;
        Element detailtrs = doc2.select(".box-container .data td").first();
        for (Node node : detailtrs.childNodes()) {
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.tagName().equals("strong")) {
                    if (element.hasClass("c2")) {
                        if (!title.equals("")) {
                            result.addDetail(new Detail(title, text.trim()));
                        }
                        title = element.text().trim();
                        text = "";
                    } else {
                        text = text + element.text();
                    }
                } else {
                    if (element.tagName().equals("a")) {
                        if (element.text().trim().contains("hier klicken") ||
                                title.contains("Link")) {
                            text = text + node.attr("href");
                            takeover = true;
                            break;
                        } else {
                            text = text + element.text();
                        }
                    }
                }
            } else if (node instanceof TextNode) {
                text = text + ((TextNode) node).text();
            }
        }
        if (!takeover) {
            text = "";
            title = "";
        }

        detailtrs = doc2.select("#tab-content .data td").first();
        if (detailtrs != null) {
            for (Node node : detailtrs.childNodes()) {
                if (node instanceof Element) {
                    if (((Element) node).tagName().equals("strong")) {
                        if (!text.equals("") && !title.equals("")) {
                            result.addDetail(new Detail(title.trim(), text.trim()));
                            if (title.equals("Titel:")) {
                                result.setTitle(text.trim());
                            }
                            text = "";
                        }

                        title = ((Element) node).text().trim();
                    } else {
                        if (((Element) node).tagName().equals("a")
                                && (((Element) node).text().trim()
                                                    .contains("hier klicken") || title
                                .equals("Link:"))) {
                            text = text + node.attr("href");
                        } else {
                            text = text + ((Element) node).text();
                        }
                    }
                } else if (node instanceof TextNode) {
                    text = text + ((TextNode) node).text();
                }
            }
        } else {
            if (doc2.select("#tab-content .fulltitle tr").size() > 0) {
                Elements rows = doc2.select("#tab-content .fulltitle tr");
                for (Element tr : rows) {
                    if (tr.children().size() == 2) {
                        Element valcell = tr.child(1);
                        String value = valcell.text().trim();
                        if (valcell.select("a").size() == 1) {
                            value = valcell.select("a").first().absUrl("href");
                        }
                        result.addDetail(new Detail(tr.child(0).text().trim(),
                                value));
                    }
                }
            } else {
                result.addDetail(new Detail(stringProvider
                        .getString(StringProvider.ERROR), stringProvider
                        .getString(StringProvider.COULD_NOT_LOAD_DETAIL)));
            }
        }
        if (!text.equals("") && !title.equals("")) {
            result.addDetail(new Detail(title.trim(), text.trim()));
            if (title.equals("Titel:")) {
                result.setTitle(text.trim());
            }
        }
        for (Element link : doc3.select("#tab-content a")) {
            Map<String, String> hrefq = getQueryParamsFirst(link.absUrl("href"));
            if (result.getId() == null) {
                // ID retrieval
                String key = hrefq.get("katkey");
                if (key != null) {
                    result.setId(key);
                    break;
                }
            }
        }
        for (Element link : doc3.select(".box-container a")) {
            if (link.text().trim().equals("Download")) {
                result.addDetail(new Detail(stringProvider
                        .getString(StringProvider.DOWNLOAD), link
                        .absUrl("href")));
            }
        }
        if (doc3.select("#tab-content .textrot").size() > 0) {
            result.addDetail(new Detail(
                    stringProvider.getString(StringProvider.STATUS),
                    doc3.select("#tab-content .textrot").text()
            ));
        }

        Map<String, Integer> copy_columnmap = new HashMap<>();
        // Default values
        copy_columnmap.put("barcode", 1);
        copy_columnmap.put("branch", 3);
        copy_columnmap.put("status", 4);
        Elements copy_columns = doc.select("#tab-content .data tr#bg2 th");
        for (int i = 0; i < copy_columns.size(); i++) {
            Element th = copy_columns.get(i);
            String head = th.text().trim();
            if (head.contains("Status")) {
                copy_columnmap.put("status", i);
            }
            if (head.contains("Zweigstelle")) {
                copy_columnmap.put("branch", i);
            }
            if (head.contains("Mediennummer")) {
                copy_columnmap.put("barcode", i);
            }
            if (head.contains("Standort")) {
                copy_columnmap.put("location", i);
            }
            if (head.contains("Signatur")) {
                copy_columnmap.put("signature", i);
            }
        }

        Pattern status_lent = Pattern
                .compile(
                        "^(entliehen) bis ([0-9]{1,2}.[0-9]{1,2}.[0-9]{2," +
                                "4}) \\(gesamte Vormerkungen: ([0-9]+)\\)$");
        Pattern status_and_barcode = Pattern.compile("^(.*) ([0-9A-Za-z]+)$");

        Elements exemplartrs = doc.select("#tab-content .data tr").not("#bg2");
        DateTimeFormatter fmt =
                DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        for (Element tr : exemplartrs) {
            try {
                Copy copy = new Copy();
                Element status = tr.child(copy_columnmap.get("status"));
                Element barcode = tr.child(copy_columnmap.get("barcode"));
                String barcodetext = barcode.text().trim()
                                            .replace(" Wegweiser", "");

                // STATUS
                String statustext;
                if (status.getElementsByTag("b").size() > 0) {
                    statustext = status.getElementsByTag("b").text().trim();
                } else {
                    statustext = status.text().trim();
                }
                if (copy_columnmap.get("status").equals(copy_columnmap
                        .get("barcode"))) {
                    Matcher matcher1 = status_and_barcode.matcher(statustext);
                    if (matcher1.matches()) {
                        statustext = matcher1.group(1);
                        barcodetext = matcher1.group(2);
                    }
                }

                Matcher matcher = status_lent.matcher(statustext);
                if (matcher.matches()) {
                    copy.setStatus(matcher.group(1));
                    copy.setReservations(matcher.group(3));
                    copy.setReturnDate(fmt.parseLocalDate(matcher.group(2)));
                } else {
                    copy.setStatus(statustext.trim().replace(" Wegweiser", ""));
                }
                copy.setBarcode(barcodetext);
                if (status.select("a[href*=doVormerkung]").size() == 1) {
                    copy.setResInfo(status.select("a[href*=doVormerkung]").attr("href")
                                          .split("\\?")[1]);
                }

                String branchtext = tr
                        .child(copy_columnmap.get("branch")).text()
                        .trim().replace(" Wegweiser", "");
                copy.setBranch(branchtext);

                if (copy_columnmap.containsKey("location")) {
                    copy.setLocation(tr.child(copy_columnmap.get("location"))
                                       .text().trim().replace(" Wegweiser", ""));
                }

                if (copy_columnmap
                        .containsKey("signature")) {
                    copy.setShelfmark(
                            tr.child(copy_columnmap.get("signature"))
                              .text().trim().replace(" Wegweiser", ""));
                }

                result.addCopy(copy);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            Element isvolume = null;
            Map<String, String> volume = new HashMap<>();
            Elements links = doc.select(".data td a");
            int elcount = links.size();
            for (int eli = 0; eli < elcount; eli++) {
                List<NameValuePair> anyurl = URLEncodedUtils.parse(new URI(
                        links.get(eli).attr("href")), "UTF-8");
                for (NameValuePair nv : anyurl) {
                    if (nv.getName().equals("methodToCall")
                            && nv.getValue().equals("volumeSearch")) {
                        isvolume = links.get(eli);
                    } else if (nv.getName().equals("catKey")) {
                        volume.put("catKey", nv.getValue());
                    } else if (nv.getName().equals("dbIdentifier")) {
                        volume.put("dbIdentifier", nv.getValue());
                    }
                }
                if (isvolume != null) {
                    volume.put("volume", "true");
                    result.setVolumesearch(volume);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account acc,
            int useraction, String selection) throws IOException {
        String reservation_info = item.getReservation_info();
        final String branch_inputfield = "issuepoint";

        Document doc = null;

        String action = "reservation";
        if (reservation_info.contains("doBestellung")) {
            action = "order";
        }

        if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("methodToCall", action));
            nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
            String html = httpPost(opac_url + "/" + action + ".do",
                    new UrlEncodedFormEntity(nameValuePairs), ENCODING);
            doc = Jsoup.parse(html);
        } else if (selection == null || useraction == 0) {
            String html = httpGet(opac_url + "/availability.do?"
                    + reservation_info, ENCODING);
            doc = Jsoup.parse(html);

            if (doc.select("input[name=username]").size() > 0) {
                // Login vonnöten
                CSId = doc.select("input[name=CSId]").val();
                List<NameValuePair> nameValuePairs = new ArrayList<>(
                        2);
                nameValuePairs.add(new BasicNameValuePair("username", acc
                        .getName()));
                nameValuePairs.add(new BasicNameValuePair("password", acc
                        .getPassword()));
                nameValuePairs.add(new BasicNameValuePair("methodToCall",
                        "submit"));
                nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
                nameValuePairs.add(new BasicNameValuePair("login_action",
                        "Login"));

                html = handleLoginMessage(httpPost(opac_url + "/login.do",
                        new UrlEncodedFormEntity(nameValuePairs), ENCODING));
                doc = Jsoup.parse(html);

                if (doc.getElementsByClass("error").size() == 0) {
                    logged_in = System.currentTimeMillis();
                    logged_in_as = acc;
                }
            }
            if (doc.select("input[name=expressorder]").size() > 0) {
                List<NameValuePair> nameValuePairs = new ArrayList<>(
                        2);
                nameValuePairs.add(new BasicNameValuePair(branch_inputfield,
                        selection));
                nameValuePairs.add(new BasicNameValuePair("methodToCall",
                        action));
                nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
                nameValuePairs.add(new BasicNameValuePair("expressorder", " "));
                html = httpPost(opac_url + "/" + action + ".do",
                        new UrlEncodedFormEntity(nameValuePairs), ENCODING);
                doc = Jsoup.parse(html);
            }
            if (doc.select("input[name=" + branch_inputfield + "]").size() > 0) {
                List<Map<String, String>> branches = new ArrayList<>();
                for (Element option : doc
                        .select("input[name=" + branch_inputfield + "]")
                        .first().parent().parent().parent().select("td")) {
                    if (option.select("input").size() != 1) {
                        continue;
                    }
                    String value = option.text().trim();
                    String key = option.select("input").val();
                    Map<String, String> selopt = new HashMap<>();
                    selopt.put("key", key);
                    selopt.put("value", value);
                    branches.add(selopt);
                }
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
            nameValuePairs.add(new BasicNameValuePair("methodToCall", action));
            nameValuePairs.add(new BasicNameValuePair("CSId", CSId));

            String html = httpPost(opac_url + "/" + action + ".do",
                    new UrlEncodedFormEntity(nameValuePairs), ENCODING);
            doc = Jsoup.parse(html);
        }

        if (doc == null) {
            return new ReservationResult(MultiStepResult.Status.ERROR);
        }

        if (doc.getElementsByClass("error").size() >= 1) {
            return new ReservationResult(MultiStepResult.Status.ERROR, doc
                    .getElementsByClass("error").get(0).text());
        }

        if (doc.html().contains("jsp/error.jsp")) {
            return new ReservationResult(MultiStepResult.Status.ERROR, doc
                    .getElementsByTag("h2").get(0).text());
        }

        if (doc.select("#CirculationForm p").size() > 0
                && doc.select("input[type=button]").size() >= 2) {
            List<String[]> details = new ArrayList<>();
            for (String row : doc.select("#CirculationForm p").first().html()
                                 .split("<br>")) {
                Document frag = Jsoup.parseBodyFragment(row);
                if (frag.text().contains(":")) {
                    String[] split = frag.text().split(":");
                    if (split.length >= 2) {
                        details.add(new String[]{split[0].trim() + ":",
                                split[1].trim()});
                    }
                } else {
                    details.add(new String[]{"", frag.text().trim()});
                }
            }
            ReservationResult result = new ReservationResult(
                    Status.CONFIRMATION_NEEDED);
            result.setDetails(details);
            return result;
        }

        if (doc.select("#CirculationForm .textrot").size() >= 1) {
            String errmsg = doc.select("#CirculationForm .textrot").get(0).text();
            if (errmsg
                    .contains("Dieses oder andere Exemplare in anderer Zweigstelle ausleihbar")) {
                Copy best = null;
                for (Copy copy : item.getCopies()) {
                    if (copy.getResInfo() == null) {
                        continue;
                    }
                    if (best == null) {
                        best = copy;
                        continue;
                    }
                    try {
                        if (Integer.parseInt(copy.getReservations()) <
                                Long.parseLong(best.getReservations())) {
                            best = copy;
                        } else if (Integer.parseInt(copy
                                .getReservations()) == Long
                                .parseLong(best.getReservations())) {
                            if (copy.getReturnDate().isBefore(best.getReturnDate())) {
                                best = copy;
                            }
                        }
                    } catch (NumberFormatException e) {

                    }
                }
                if (best != null) {
                    item.setReservation_info(best
                            .getResInfo());
                    return reservation(item, acc, 0, null);
                }
            }
            return new ReservationResult(MultiStepResult.Status.ERROR, errmsg);
        }

        if (doc.select("#CirculationForm td[colspan=2] strong").size() >= 1) {
            return new ReservationResult(MultiStepResult.Status.OK, doc
                    .select("#CirculationForm td[colspan=2] strong").get(0)
                    .text());
        }
        return new ReservationResult(Status.OK);
    }

    @Override
    public ProlongResult prolong(String a, Account account, int useraction,
            String Selection) throws IOException {
        // Internal convention: a is either a § followed by an error message or
        // the URI of the page this item was found on and the query string the
        // prolonging link links to, seperated by a $.
        if (a.startsWith("§")) {
            return new ProlongResult(MultiStepResult.Status.ERROR,
                    a.substring(1));
        }
        String[] parts = a.split("\\$");
        String offset = parts[0];
        String query = parts[1];

        if (!initialised) {
            start();
        }
        if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
                || logged_in_as == null) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ProlongResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        } else if (logged_in_as.getId() != account.getId()) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ProlongResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        }

        // We have to call the page we originally found the link on first...
        httpGet(opac_url + "/userAccount.do?methodToCall=showAccount&typ=1",
                ENCODING);
        if (!offset.equals("1")) {
            httpGet(opac_url
                    + "/userAccount.do?methodToCall=pos&accountTyp=AUSLEIHEN&anzPos="
                    + offset, ENCODING);
        }
        String html = httpGet(opac_url + "/userAccount.do?" + query, ENCODING);
        Document doc = Jsoup.parse(html);
        if (doc.select("#middle .textrot").size() > 0) {
            return new ProlongResult(MultiStepResult.Status.ERROR, doc
                    .select("#middle .textrot").first().text());
        }

        return new ProlongResult(MultiStepResult.Status.OK);
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        if (!initialised) {
            start();
        }

        String[] parts = media.split("\\$");
        String type = parts[0];
        String offset = parts[1];
        String query = parts[2];

        if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
                || logged_in_as == null) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new OpacErrorException(
                        stringProvider.getString(StringProvider.INTERNAL_ERROR));
            }
        } else if (logged_in_as.getId() != account.getId()) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new OpacErrorException(
                        stringProvider.getString(StringProvider.INTERNAL_ERROR));
            }
        }

        // We have to call the page we originally found the link on first...
        httpGet(opac_url + "/userAccount.do?methodToCall=showAccount&typ="
                + type, ENCODING);
        if (!offset.equals("1")) {
            httpGet(opac_url + "/userAccount.do?methodToCall=pos&anzPos="
                    + offset, ENCODING);
        }
        httpGet(opac_url + "/userAccount.do?" + query, ENCODING);
        return new CancelResult(MultiStepResult.Status.OK);
    }

    protected String handleLoginMessage(String html)
            throws IOException {
        if (html.contains("methodToCall=done")) {
            return httpGet(opac_url + "/login.do?methodToCall=done", ENCODING);
        } else {
            return html;
        }
    }

    protected boolean login(Account acc) throws OpacErrorException {
        String html;

        List<NameValuePair> nameValuePairs = new ArrayList<>(2);

        try {
            String loginPage;
            loginPage = httpGet(opac_url
                    + "/userAccount.do?methodToCall=show&type=1", ENCODING);
            Document loginPageDoc = Jsoup.parse(loginPage);
            if (loginPageDoc.select("input[name=as_fid]").size() > 0) {
                nameValuePairs.add(new BasicNameValuePair("as_fid",
                        loginPageDoc.select("input[name=as_fid]").first()
                                    .attr("value")));
            }
            CSId = loginPageDoc.select("input[name=CSId]").val();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        nameValuePairs.add(new BasicNameValuePair("username", acc.getName()));
        nameValuePairs
                .add(new BasicNameValuePair("password", acc.getPassword()));
        nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
        nameValuePairs.add(new BasicNameValuePair("methodToCall", "submit"));
        try {
            html = handleLoginMessage(httpPost(opac_url + "/login.do",
                    new UrlEncodedFormEntity(nameValuePairs, ENCODING), ENCODING));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Document doc = Jsoup.parse(html);

        if (doc.getElementsByClass("error").size() > 0) {
            throw new OpacErrorException(doc.getElementsByClass("error").get(0)
                                            .text());
        }

        logged_in = System.currentTimeMillis();
        logged_in_as = acc;

        return true;
    }

    public static void parse_medialist(List<LentItem> media, Document doc, int offset,
            JSONObject data) {
        Elements copytrs = doc.select(".data tr");
        doc.setBaseUri(data.optString("baseurl"));

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        int trs = copytrs.size();
        if (trs == 1) {
            return;
        }
        assert (trs > 0);
        for (int i = 1; i < trs; i++) {
            Element tr = copytrs.get(i);
            LentItem item = new LentItem();

            if (tr.text().contains("keine Daten") || (trs == 2 && tr.children().size() == 1)) {
                // Dresden: Konto entält keine &lt;Ausleihen&gt;. [sic!]
                return;
            }

            item.setTitle(tr.child(1).select("strong").text().trim());
            try {
                item.setAuthor(tr.child(1).html().split("<br[ /]*>")[1].trim());

                String[] col2split = tr.child(2).html().split("<br[ /]*>");
                String deadline = col2split[0].trim();
                if (deadline.contains("-")) {
                    deadline = deadline.split("-")[1].trim();
                }
                try {
                    item.setDeadline(fmt.parseLocalDate(deadline).toString());
                } catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                }

                if (col2split.length > 1) {
                    item.setHomeBranch(col2split[1].trim());
                }

                if (tr.select("a").size() > 0) {
                    for (Element link : tr.select("a")) {
                        String href = link.attr("abs:href");
                        Map<String, String> hrefq = getQueryParamsFirst(href);
                        if (hrefq.get("methodToCall").equals("renewalPossible")) {
                            item.setProlongData(offset + "$" + href.split("\\?")[1]);
                            item.setRenewable(true);
                            break;
                        }
                    }
                } else if (tr.select(".textrot, .textgruen, .textdunkelblau")
                             .size() > 0) {
                    item.setProlongData(
                            "§" + tr.select(".textrot, .textgruen, .textdunkelblau").text());
                    item.setRenewable(false);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            media.add(item);
        }
        assert (media.size() == trs - 1);

    }

    protected void parse_reslist(String type,
            List<ReservedItem> reservations, Document doc, int offset) {
        Elements copytrs = doc.select(".data tr");
        doc.setBaseUri(opac_url);
        int trs = copytrs.size();
        if (trs == 1) {
            return;
        }
        assert (trs > 0);
        for (int i = 1; i < trs; i++) {
            Element tr = copytrs.get(i);
            ReservedItem item = new ReservedItem();

            if (tr.text().contains("keine Daten") || tr.children().size() == 1) {
                return;
            }

            item.setTitle(tr.child(1).select("strong").text().trim());
            try {
                String[] rowsplit1 = tr.child(1).html().split("<br[ /]*>");
                String[] rowsplit2 = tr.child(2).html().split("<br[ /]*>");
                if (rowsplit1.length > 1) item.setAuthor(rowsplit1[1].trim());
                if (rowsplit2.length > 2) item.setBranch(rowsplit2[2].trim());
                if (rowsplit2.length > 2) item.setStatus(rowsplit2[0].trim());

                if (tr.select("a").size() == 1) {
                    item.setCancelData(type + "$" + offset + "$" +
                            tr.select("a").attr("abs:href").split("\\?")[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            reservations.add(item);
        }
        assert (reservations.size() == trs - 1);
    }

    @Override
    public AccountData account(Account acc) throws IOException,
            JSONException,
            OpacErrorException {
        start(); // TODO: Is this necessary?

        int resultNum;

        if (!login(acc)) {
            return null;
        }

        // Geliehene Medien
        String html = httpGet(opac_url
                + "/userAccount.do?methodToCall=showAccount&typ=1", ENCODING);
        List<LentItem> medien = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);
        parse_medialist(medien, doc, 1, data);
        if (doc.select(".box-right").size() > 0) {
            for (Element link : doc.select(".box-right").first().select("a")) {
                String href = link.attr("abs:href");
                Map<String, String> hrefq = getQueryParamsFirst(href);
                if (hrefq == null || hrefq.get("methodToCall") == null) {
                    continue;
                }
                if (hrefq.get("methodToCall").equals("pos")
                        && !"1".equals(hrefq.get("anzPos"))) {
                    html = httpGet(href, ENCODING);
                    parse_medialist(medien, Jsoup.parse(html),
                            Integer.parseInt(hrefq.get("anzPos")), data);
                }
            }
        }
        if (doc.select("#label1").size() > 0) {
            resultNum = 0;
            String rNum = doc.select("#label1").first().text().trim()
                             .replaceAll(".*\\(([0-9]*)\\).*", "$1");
            if (rNum.length() > 0) {
                resultNum = Integer.parseInt(rNum);
            }

            assert (resultNum == medien.size());
        }

        // Ordered media ("Bestellungen")
        html = httpGet(opac_url + "/userAccount.do?methodToCall=showAccount&typ=6", ENCODING);
        List<ReservedItem> reserved = new ArrayList<>();
        doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);
        parse_reslist("6", reserved, doc, 1);
        Elements label6 = doc.select("#label6");
        if (doc.select(".box-right").size() > 0) {
            for (Element link : doc.select(".box-right").first().select("a")) {
                String href = link.attr("abs:href");
                Map<String, String> hrefq = getQueryParamsFirst(href);
                if (hrefq == null || hrefq.get("methodToCall") == null) {
                    break;
                }
                if (hrefq.get("methodToCall").equals("pos")
                        && !"1".equals(hrefq.get("anzPos"))) {
                    html = httpGet(href, ENCODING);
                    parse_reslist("6", reserved, Jsoup.parse(html),
                            Integer.parseInt(hrefq.get("anzPos")));
                }
            }
        }

        // Prebooked media ("Vormerkungen")
        html = httpGet(opac_url
                + "/userAccount.do?methodToCall=showAccount&typ=7", ENCODING);
        doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);
        parse_reslist("7", reserved, doc, 1);
        if (doc.select(".box-right").size() > 0) {
            for (Element link : doc.select(".box-right").first().select("a")) {
                String href = link.attr("abs:href");
                Map<String, String> hrefq = getQueryParamsFirst(href);
                if (hrefq == null || hrefq.get("methodToCall") == null) {
                    break;
                }
                if (hrefq.get("methodToCall").equals("pos")
                        && !"1".equals(hrefq.get("anzPos"))) {
                    html = httpGet(href, ENCODING);
                    parse_reslist("7", reserved, Jsoup.parse(html),
                            Integer.parseInt(hrefq.get("anzPos")));
                }
            }
        }
        if (label6.size() > 0 && doc.select("#label7").size() > 0) {
            resultNum = 0;
            String rNum = label6.text().trim()
                                .replaceAll(".*\\(([0-9]*)\\).*", "$1");
            if (rNum.length() > 0) {
                resultNum = Integer.parseInt(rNum);
            }
            rNum = doc.select("#label7").text().trim()
                      .replaceAll(".*\\(([0-9]*)\\).*", "$1");
            if (rNum.length() > 0) {
                resultNum += Integer.parseInt(rNum);
            }
            assert (resultNum == reserved.size());
        }

        AccountData res = new AccountData(acc.getId());

        if (doc.select("#label8").size() > 0) {
            String text = doc.select("#label8").first().text().trim();
            if (text.matches("Geb.+hren[^\\(]+\\(([0-9.,]+)[^0-9€A-Z]*(€|EUR|CHF|Fr)\\)")) {
                text = text
                        .replaceAll(
                                "Geb.+hren[^\\(]+\\(([0-9.,]+)[^0-9€A-Z]*(€|EUR|CHF|Fr)\\)",
                                "$1 $2");
                res.setPendingFees(text);
            }
        }
        Pattern p = Pattern.compile("[^0-9.]*", Pattern.MULTILINE);
        if (doc.select(".box3").size() > 0) {
            for (Element box : doc.select(".box3")) {
                if (box.select("strong").size() == 1) {
                    String text = box.select("strong").text();
                    if (text.equals("Jahresgebühren")) {
                        text = box.text();
                        text = p.matcher(text).replaceAll("");
                        res.setValidUntil(text);
                    }
                }

            }
        }

        res.setLent(medien);
        res.setReservations(reserved);
        return res;
    }

    @Override
    public String getShareUrl(String id, String title) {
        String startparams = "";
        if (data.has("startparams")) {
            try {
                startparams = data.getString("startparams") + "&";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (id != null && !id.equals("")) {
            return opac_url + "/start.do?" + startparams
                    + "searchType=1&Query=0%3D%22" + id + "%22";
        } else {
            try {
                title = URLEncoder.encode(title, getDefaultEncoding());
            } catch (UnsupportedEncodingException e) {
                //noinspection deprecation
                title = URLEncoder.encode(title);
            }
            return opac_url + "/start.do?" + startparams
                    + "searchType=1&Query=-1%3D%22" + title + "%22";
        }
    }

    @Override
    public int getSupportFlags() {
        int flags = SUPPORT_FLAG_ACCOUNT_PROLONG_ALL
                | SUPPORT_FLAG_CHANGE_ACCOUNT;
        flags |= SUPPORT_FLAG_ENDLESS_SCROLLING;
        return flags;
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
                return new ProlongAllResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e) {
                return new ProlongAllResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        } else if (logged_in_as.getId() != account.getId()) {
            try {
                account(account);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ProlongAllResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e) {
                return new ProlongAllResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        }

        // We have to call the page we originally found the link on first...
        String html = httpGet(
                opac_url
                        + "/userAccount.do?methodToCall=renewalPossible&renewal=account",
                ENCODING);
        Document doc = Jsoup.parse(html);

        if (doc.select("table.data").size() > 0) {
            List<Map<String, String>> result = new ArrayList<>();
            for (Element td : doc.select("table.data tr td")) {
                Map<String, String> line = new HashMap<>();
                if (!td.text().contains("Titel")
                        || !td.text().contains("Status")) {
                    continue;
                }
                String nextNodeIs = "";
                for (Node n : td.childNodes()) {
                    String text;
                    if (n instanceof Element) {
                        text = ((Element) n).text();
                    } else if (n instanceof TextNode) {
                        text = ((TextNode) n).text();
                    } else {
                        continue;
                    }
                    if (text.trim().length() == 0) {
                        continue;
                    }
                    if (text.contains("Titel:")) {
                        nextNodeIs = ProlongAllResult.KEY_LINE_TITLE;
                    } else if (text.contains("Verfasser:")) {
                        nextNodeIs = ProlongAllResult.KEY_LINE_AUTHOR;
                    } else if (text.contains("Leihfristende:")) {
                        nextNodeIs = ProlongAllResult.KEY_LINE_NEW_RETURNDATE;
                    } else if (text.contains("Status:")) {
                        nextNodeIs = ProlongAllResult.KEY_LINE_MESSAGE;
                    } else if (text.contains("Mediennummer:")
                            || text.contains("Signatur:")) {
                        nextNodeIs = "";
                    } else if (nextNodeIs.length() > 0) {
                        line.put(nextNodeIs, text.trim());
                        nextNodeIs = "";
                    }
                }
                result.add(line);
            }
            return new ProlongAllResult(MultiStepResult.Status.OK, result);
        }

        return new ProlongAllResult(MultiStepResult.Status.ERROR,
                stringProvider.getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException, OpacErrorException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        start(); // TODO: Is this necessary?
        boolean success = login(account);
        if (!success) {
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
}
