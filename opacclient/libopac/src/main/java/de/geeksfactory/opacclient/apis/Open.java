/*
 * Copyright (C) 2015 by Johan von Forstner under the MIT license:
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

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * API for Bibliotheca+/OPEN OPAC software
 *
 * @author Johan von Forstner, 29.03.2015
 */

public class Open extends BaseApi implements OpacApi {
    protected JSONObject data;
    protected String opac_url;
    protected Document searchResultDoc;

    protected static HashMap<String, SearchResult.MediaType> defaulttypes = new HashMap<>();

    static {
        defaulttypes.put("archv", SearchResult.MediaType.BOOK);
        defaulttypes.put("archv-digital", SearchResult.MediaType.EDOC);
        defaulttypes.put("artchap", SearchResult.MediaType.ART);
        defaulttypes.put("artchap-artcl", SearchResult.MediaType.ART);
        defaulttypes.put("artchap-chptr", SearchResult.MediaType.ART);
        defaulttypes.put("artchap-digital", SearchResult.MediaType.ART);
        defaulttypes.put("audiobook", SearchResult.MediaType.AUDIOBOOK);
        defaulttypes.put("audiobook-cd", SearchResult.MediaType.AUDIOBOOK);
        defaulttypes.put("audiobook-lp", SearchResult.MediaType.AUDIOBOOK);
        defaulttypes.put("audiobook-digital", SearchResult.MediaType.MP3);
        defaulttypes.put("book", SearchResult.MediaType.BOOK);
        defaulttypes.put("book-braille", SearchResult.MediaType.BOOK);
        defaulttypes.put("book-continuing", SearchResult.MediaType.BOOK);
        defaulttypes.put("book-digital", SearchResult.MediaType.EBOOK);
        defaulttypes.put("book-largeprint", SearchResult.MediaType.BOOK);
        defaulttypes.put("book-mic", SearchResult.MediaType.BOOK);
        defaulttypes.put("book-thsis", SearchResult.MediaType.BOOK);
        defaulttypes.put("compfile", SearchResult.MediaType.PACKAGE);
        defaulttypes.put("compfile-digital", SearchResult.MediaType.PACKAGE);
        defaulttypes.put("corpprof", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("encyc", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("game", SearchResult.MediaType.BOARDGAME);
        defaulttypes.put("game-digital", SearchResult.MediaType.GAME_CONSOLE);
        defaulttypes.put("image", SearchResult.MediaType.ART);
        defaulttypes.put("image-2d", SearchResult.MediaType.ART);
        defaulttypes.put("intmm", SearchResult.MediaType.EVIDEO);
        defaulttypes.put("intmm-digital", SearchResult.MediaType.EVIDEO);
        defaulttypes.put("jrnl", SearchResult.MediaType.MAGAZINE);
        defaulttypes.put("jrnl-issue", SearchResult.MediaType.MAGAZINE);
        defaulttypes.put("jrnl-digital", SearchResult.MediaType.EBOOK);
        defaulttypes.put("kit", SearchResult.MediaType.PACKAGE);
        defaulttypes.put("map", SearchResult.MediaType.MAP);
        defaulttypes.put("map-digital", SearchResult.MediaType.EBOOK);
        defaulttypes.put("msscr", SearchResult.MediaType.MP3);
        defaulttypes.put("msscr-digital", SearchResult.MediaType.MP3);
        defaulttypes.put("music", SearchResult.MediaType.MP3);
        defaulttypes.put("music-cassette", SearchResult.MediaType.AUDIO_CASSETTE);
        defaulttypes.put("music-cd", SearchResult.MediaType.CD_MUSIC);
        defaulttypes.put("music-digital", SearchResult.MediaType.MP3);
        defaulttypes.put("music-lp", SearchResult.MediaType.LP_RECORD);
        defaulttypes.put("news", SearchResult.MediaType.NEWSPAPER);
        defaulttypes.put("news-digital", SearchResult.MediaType.EBOOK);
        defaulttypes.put("object", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("object-digital", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("paper", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("pub", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("rev", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("snd", SearchResult.MediaType.MP3);
        defaulttypes.put("snd-cassette", SearchResult.MediaType.AUDIO_CASSETTE);
        defaulttypes.put("snd-cd", SearchResult.MediaType.CD_MUSIC);
        defaulttypes.put("snd-lp", SearchResult.MediaType.LP_RECORD);
        defaulttypes.put("snd-digital", SearchResult.MediaType.EAUDIO);
        defaulttypes.put("toy", SearchResult.MediaType.BOARDGAME);
        defaulttypes.put("und", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("video-bluray", SearchResult.MediaType.BLURAY);
        defaulttypes.put("video-digital", SearchResult.MediaType.EVIDEO);
        defaulttypes.put("video-dvd", SearchResult.MediaType.DVD);
        defaulttypes.put("video-film", SearchResult.MediaType.MOVIE);
        defaulttypes.put("video-vhs", SearchResult.MediaType.MOVIE);
        defaulttypes.put("vis", SearchResult.MediaType.ART);
        defaulttypes.put("vis-digital", SearchResult.MediaType.ART);
        defaulttypes.put("web", SearchResult.MediaType.URL);
        defaulttypes.put("web-digital", SearchResult.MediaType.URL);
        defaulttypes.put("art", SearchResult.MediaType.ART);
        defaulttypes.put("arturl", SearchResult.MediaType.URL);
        defaulttypes.put("bks", SearchResult.MediaType.BOOK);
        defaulttypes.put("bksbrl", SearchResult.MediaType.BOOK);
        defaulttypes.put("bksdeg", SearchResult.MediaType.BOOK);
        defaulttypes.put("bkslpt", SearchResult.MediaType.BOOK);
        defaulttypes.put("bksurl", SearchResult.MediaType.EBOOK);
        defaulttypes.put("braille", SearchResult.MediaType.BOOK);
        defaulttypes.put("com", SearchResult.MediaType.CD_SOFTWARE);
        defaulttypes.put("comcgm", SearchResult.MediaType.GAME_CONSOLE);
        defaulttypes.put("comcgmurl", SearchResult.MediaType.GAME_CONSOLE);
        defaulttypes.put("comimm", SearchResult.MediaType.EVIDEO);
        defaulttypes.put("comimmurl", SearchResult.MediaType.EVIDEO);
        defaulttypes.put("comurl", SearchResult.MediaType.URL);
        defaulttypes.put("int", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("inturl", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("map", SearchResult.MediaType.MAP);
        defaulttypes.put("mapurl", SearchResult.MediaType.MAP);
        defaulttypes.put("mic", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("micro", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("mix", SearchResult.MediaType.PACKAGE);
        defaulttypes.put("mixurl", SearchResult.MediaType.PACKAGE);
        defaulttypes.put("rec", SearchResult.MediaType.MP3);
        defaulttypes.put("recmsr", SearchResult.MediaType.MP3);
        defaulttypes.put("recmsrcas", SearchResult.MediaType.AUDIO_CASSETTE);
        defaulttypes.put("recmsrcda", SearchResult.MediaType.CD_MUSIC);
        defaulttypes.put("recmsrlps", SearchResult.MediaType.LP_RECORD);
        defaulttypes.put("recmsrurl", SearchResult.MediaType.EAUDIO);
        defaulttypes.put("recnsr", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("recnsrcas", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("recnsrcda", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("recnsrlps", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("recnsrurl", SearchResult.MediaType.UNKNOWN);
        defaulttypes.put("recurl", SearchResult.MediaType.EAUDIO);
        defaulttypes.put("sco", SearchResult.MediaType.SCORE_MUSIC);
        defaulttypes.put("scourl", SearchResult.MediaType.SCORE_MUSIC);
        defaulttypes.put("ser", SearchResult.MediaType.PACKAGE_BOOKS);
        defaulttypes.put("sernew", SearchResult.MediaType.PACKAGE_BOOKS);
        defaulttypes.put("sernewurl", SearchResult.MediaType.PACKAGE_BOOKS);
        defaulttypes.put("serurl", SearchResult.MediaType.PACKAGE_BOOKS);
        defaulttypes.put("url", SearchResult.MediaType.URL);
        defaulttypes.put("vis", SearchResult.MediaType.ART);
        defaulttypes.put("visart", SearchResult.MediaType.ART);
        defaulttypes.put("visdvv", SearchResult.MediaType.DVD);
        defaulttypes.put("vismot", SearchResult.MediaType.MOVIE);
        defaulttypes.put("visngr", SearchResult.MediaType.ART);
        defaulttypes.put("visngrurl", SearchResult.MediaType.ART);
        defaulttypes.put("visphg", SearchResult.MediaType.BOARDGAME);
        defaulttypes.put("vistoy", SearchResult.MediaType.BOARDGAME);
        defaulttypes.put("visurl", SearchResult.MediaType.URL);
        defaulttypes.put("visvhs", SearchResult.MediaType.MOVIE);
        defaulttypes.put("visvid", SearchResult.MediaType.MOVIE);
        defaulttypes.put("visvidurl", SearchResult.MediaType.EVIDEO);
        defaulttypes.put("web", SearchResult.MediaType.URL);
    }

    /**
     * This parameter needs to be passed to a URL to make sure we are not redirected to the mobile
     * site
     */
    protected static final String NO_MOBILE = "?nomo=1";

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
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, OpacErrorException, JSONException {
        String url =
                opac_url + "/" + data.getJSONObject("urls").getString("advanced_search") +
                        NO_MOBILE;
        Document doc = Jsoup.parse(httpGet(url, getDefaultEncoding()));
        doc.setBaseUri(url);

        int selectableCount = 0;
        for (SearchQuery query : queries) {
            if (query.getValue().equals("") || query.getValue().equals("false")) continue;

            if (query.getSearchField() instanceof TextSearchField) {
                TextSearchField field = (TextSearchField) query.getSearchField();
                if (field.getData().getBoolean("selectable")) {
                    selectableCount++;
                    if (selectableCount > 3) {
                        throw new OpacErrorException(stringProvider.getQuantityString(
                                StringProvider.LIMITED_NUM_OF_CRITERIA, 3, 3));
                    }
                    String number = numberToText(selectableCount);
                    Element searchField =
                            doc.select("select[name$=" + number + "SearchField]").first();
                    Element searchValue =
                            doc.select("input[name$=" + number + "SearchValue]").first();
                    setSelectValue(searchField, field.getId());
                    searchValue.val(query.getValue());
                } else {
                    Element input = doc.select("input[name=" + field.getId() + "]").first();
                    input.val(query.getValue());
                }
            } else if (query.getSearchField() instanceof DropdownSearchField) {
                DropdownSearchField field = (DropdownSearchField) query.getSearchField();
                Element select = doc.select("select[name=" + field.getId() + "]").first();
                setSelectValue(select, query.getValue());
            } else if (query.getSearchField() instanceof CheckboxSearchField) {
                CheckboxSearchField field = (CheckboxSearchField) query.getSearchField();
                Element input = doc.select("input[name=" + field.getId() + "]").first();
                input.attr("checked", query.getValue());
            }
        }

        // Submit form
        FormElement form = (FormElement) doc.select("form").first();
        HttpEntity data = formData(form, "BtnSearch").build();
        String postUrl = form.attr("abs:action");

        String html = httpPost(postUrl, data, "UTF-8");
        Document doc2 = Jsoup.parse(html);
        doc2.setBaseUri(postUrl);
        return parse_search(doc2, 0);
    }

    protected void setSelectValue(Element select, String value) {
        for (Element opt : select.select("option")) {
            if (value.equals(opt.val())) {
                opt.attr("selected", "selected");
            } else {
                opt.removeAttr("selected");
            }
        }
    }

    protected SearchRequestResult parse_search(Document doc, int page) throws OpacErrorException {
        searchResultDoc = doc;

        if (doc.select("#Label1, span[id$=LblInfoMessage]").size() > 0) {
            String message = doc.select("#Label1, span[id$=LblInfoMessage]").text();
            if (message.contains("keine Treffer")) {
                return new SearchRequestResult(new ArrayList<SearchResult>(), 0, 1, page);
            } else {
                throw new OpacErrorException(message);
            }
        }

        int totalCount = Integer.parseInt(doc.select("span[id$=TotalItemsLabel]").first().text());

        Elements elements = doc.select("div[id$=divMedium], div[id$=divComprehensiveItem]");
        List<SearchResult> results = new ArrayList<>();
        int i = 0;
        for (Element element : elements) {
            SearchResult result = new SearchResult();
            // Cover
            if (element.select("input[id$=mediumImage]").size() > 0) {
                result.setCover(element.select("input[id$=mediumImage]").first().attr("src"));
            } else if (element.select("img[id$=CoverView_Image]").size() > 0) {
                result.setCover(getCoverUrl(element.select("img[id$=CoverView_Image]").first()));
            }

            Element catalogueContent = element.select(".catalogueContent").first();
            // Media Type
            if (catalogueContent.select("#spanMediaGrpIcon").size() > 0) {
                String mediatype = catalogueContent.select("#spanMediaGrpIcon").attr("class");
                if (mediatype.startsWith("itemtype ")) {
                    mediatype = mediatype.substring("itemtype ".length());
                }

                SearchResult.MediaType defaulttype = defaulttypes.get(mediatype);
                if (defaulttype == null) defaulttype = SearchResult.MediaType.UNKNOWN;

                if (data.has("mediatypes")) {
                    try {
                        result.setType(SearchResult.MediaType
                                .valueOf(data.getJSONObject("mediatypes").getString(mediatype)));
                    } catch (JSONException e) {
                        result.setType(defaulttype);
                    }
                } else {
                    result.setType(defaulttype);
                }
            } else {
                result.setType(SearchResult.MediaType.UNKNOWN);
            }

            // Text
            String title = catalogueContent
                    .select("a[id$=LbtnShortDescriptionValue], a[id$=LbtnTitleValue]").text();
            String subtitle = catalogueContent.select("span[id$=LblSubTitleValue]").text();
            String author = catalogueContent.select("span[id$=LblAuthorValue]").text();
            String year = catalogueContent.select("span[id$=LblProductionYearValue]").text();
            String series = catalogueContent.select("span[id$=LblSeriesValue]").text();

            // Some libraries, such as Bern, have labels but no <span id="..Value"> tags
            int j = 0;
            for (Element div : catalogueContent.children()) {
                if (subtitle.equals("") && div.select("span").size() == 0 && j > 0 && j < 3) {
                    subtitle = div.text().trim();
                }
                if (author.equals("") && div.select("span[id$=LblAuthor]").size() == 1) {
                    author = div.text().trim();
                    if (author.contains(":")) {
                        author = author.split(":")[1];
                    }
                }
                if (year.equals("") && div.select("span[id$=LblProductionYear]").size() == 1) {
                    year = div.text().trim();
                    if (year.contains(":")) {
                        year = year.split(":")[1];
                    }
                }
                j++;
            }

            StringBuilder text = new StringBuilder();
            text.append("<b>").append(title).append("</b>");
            if (!subtitle.equals("")) text.append("<br/>").append(subtitle);
            if (!author.equals("")) text.append("<br/>").append(author);
            if (!year.equals("")) text.append("<br/>").append(year);
            if (!series.equals("")) text.append("<br/>").append(series);

            result.setInnerhtml(text.toString());

            // ID
            Pattern idPattern = Pattern.compile("\\$mdv(\\d+)\\$");
            Matcher matcher = idPattern.matcher(element.html());
            if (matcher.find()) {
                result.setId(matcher.group(1));
            }

            // Availability
            if (result.getId() != null) {
                String url = opac_url +
                        "/DesktopModules/OCLC.OPEN.PL.DNN.SearchModule/SearchService" +
                        ".asmx/GetAvailability";
                String culture = element.select("input[name$=culture]").val();
                JSONObject data = new JSONObject();
                try {
                    // Determine portalID value
                    int portalId = 1;
                    for (Element scripttag : doc.select("script")) {
                        String scr = scripttag.html();
                        if (scr.contains("LoadSharedCatalogueViewAvailabilityAsync")) {
                            Pattern portalIdPattern = Pattern.compile(
                                    ".*LoadSharedCatalogueViewAvailabilityAsync\\([^,]*,[^,]*," +
                                            "[^0-9,]*([0-9]+)[^0-9,]*,.*\\).*");
                            Matcher portalIdMatcher = portalIdPattern.matcher(scr);
                            if (portalIdMatcher.find()) {
                                portalId = Integer.parseInt(portalIdMatcher.group(1));
                            }
                        }
                    }

                    data.put("portalId", portalId).put("mednr", result.getId())
                        .put("culture", culture).put("requestCopyData", false)
                        .put("branchFilter", "");
                    StringEntity entity = new StringEntity(data.toString());
                    entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
                    String json = httpPost(url, entity, getDefaultEncoding());
                    JSONObject availabilityData = new JSONObject(json);
                    String isAvail = availabilityData.getJSONObject("d").getString("IsAvail");
                    switch (isAvail) {
                        case "true":
                            result.setStatus(SearchResult.Status.GREEN);
                            break;
                        case "false":
                            result.setStatus(SearchResult.Status.RED);
                            break;
                        case "digital":
                            result.setStatus(SearchResult.Status.UNKNOWN);
                            break;
                    }

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            result.setNr(i);
            results.add(result);
        }
        return new SearchRequestResult(results, totalCount, page);
    }

    private String getCoverUrl(Element img) {
        String[] parts = img.attr("sources").split("\\|");
        // Example: SetSimpleCover|a|https://vlb.de/GetBlob.aspx?strIsbn=9783868511291&amp;
        // size=S|a|http://www.buchhandel.de/default.aspx?strframe=titelsuche&amp;
        // caller=vlbPublic&amp;func=DirectIsbnSearch&amp;isbn=9783868511291&amp;
        // nSiteId=11|c|SetNoCover|a|/DesktopModules/OCLC.OPEN.PL.DNN
        // .BaseLibrary/StyleSheets/Images/Fallbacks/emptyURL.gif?4.2.0.0|a|
        for (int i = 0; i + 2 < parts.length; i++) {
            if (parts[i].equals("SetSimpleCover")) {
                String url = parts[i + 2].replace("&amp;", "&");
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setRequestMethod("HEAD");
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        return url;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String numberToText(int number) {
        switch (number) {
            case 1:
                return "First";
            case 2:
                return "Second";
            case 3:
                return "Third";
            default:
                return null;
        }
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        /*
            When there are many pages of results, there will only be links to the next 4 and
            previous 4 pages, so we will click links until it gets to the correct page.
         */

        if (searchResultDoc == null) throw new NotReachableException();

        Document doc = searchResultDoc;

        Elements pageLinks =
                doc.select("span[id$=DataPager1]").first()
                   .select("a[id*=LinkButtonPageN], span[id*=LabelPageN]");
        int from = Integer.valueOf(pageLinks.first().text());
        int to = Integer.valueOf(pageLinks.last().text());
        Element linkToClick;
        boolean willBeCorrectPage;

        if (page < from) {
            linkToClick = pageLinks.first();
            willBeCorrectPage = false;
        } else if (page > to) {
            linkToClick = pageLinks.last();
            willBeCorrectPage = false;
        } else {
            linkToClick = pageLinks.get(page - from);
            willBeCorrectPage = true;
        }

        if (linkToClick.tagName().equals("span")) {
            // we are trying to get the page we are already on
            return parse_search(searchResultDoc, page);
        }

        Pattern pattern = Pattern.compile("javascript:__doPostBack\\('([^,]*)','([^\\)]*)'\\)");
        Matcher matcher = pattern.matcher(linkToClick.attr("href"));
        if (!matcher.find()) throw new OpacErrorException(StringProvider.INTERNAL_ERROR);

        FormElement form = (FormElement) doc.select("form").first();
        HttpEntity data = formData(form, null)
                .addTextBody("__EVENTTARGET", matcher.group(1))
                .addTextBody("__EVENTARGUMENT", matcher.group(2))
                .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        data.writeTo(stream);

        String postUrl = form.attr("abs:action");

        String html = httpPost(postUrl, data, "UTF-8");
        if (willBeCorrectPage) {
            // We clicked on the correct link
            Document doc2 = Jsoup.parse(html);
            doc2.setBaseUri(postUrl);
            return parse_search(doc2, page);
        } else {
            // There was no correct link, so try to find one again
            searchResultDoc = Jsoup.parse(html);
            searchResultDoc.setBaseUri(postUrl);
            return searchGetPage(page);
        }
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        try {
            String html =
                    httpGet(opac_url + "/" + data.getJSONObject("urls").getString("simple_search") +
                            NO_MOBILE + "&id=" + id, getDefaultEncoding());
            return parse_result(Jsoup.parse(html));
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected DetailledItem parse_result(Document doc) {
        DetailledItem item = new DetailledItem();

        // Title and Subtitle
        item.setTitle(doc.select("span[id$=LblShortDescriptionValue]").text());
        String subtitle = doc.select("span[id$=LblSubTitleValue]").text();
        if (subtitle.equals("")) {
            // Subtitle detection for Bern
            Element next = doc.select("span[id$=LblShortDescriptionValue]").first().parent().nextElementSibling();
            if (next.select("span").size() == 0) {
                subtitle = next.text().trim();
            }
        }
        if (!subtitle.equals("")) {
            item.addDetail(new Detail(stringProvider.getString(StringProvider.SUBTITLE), subtitle));
        }

        // Cover
        if (doc.select("input[id$=mediumImage]").size() > 0) {
            item.setCover(doc.select("input[id$=mediumImage]").attr("src"));
        } else if (doc.select("img[id$=CoverView_Image]").size() > 0) {
            item.setCover(getCoverUrl(doc.select("img[id$=CoverView_Image]").first()));
        }

        // ID
        item.setId(doc.select("input[id$=regionmednr]").val());

        // Description
        if (doc.select("span[id$=ucCatalogueContent_LblAnnotation]").size() > 0) {
            String name = doc.select("span[id$=lblCatalogueContent]").text();
            String value = doc.select("span[id$=ucCatalogueContent_LblAnnotation]").text();
            item.addDetail(new Detail(name, value));
        }
        // Details
        String DETAIL_SELECTOR = "div[id$=CatalogueDetailView] .spacingBottomSmall:has(span+span)," +
                "div[id$=CatalogueDetailView] .spacingBottomSmall:has(span+a)";
        for (Element detail : doc.select(DETAIL_SELECTOR)) {
            String name = detail.select("span").get(0).text().replace(": ", "");
            String value = "";
            if (detail.select("a").size() > 1) {
                int i = 0;
                for (Element a : detail.select("a")) {
                    if (i != 0) {
                        value += ", ";
                    }
                    value += a.text().trim();
                    i++;
                }
            } else {
                value = detail.select("span, a").get(1).text();
            }
            item.addDetail(new Detail(name, value));
        }

        // Copies
        Element table = doc.select("table[id$=grdViewMediumCopies]").first();
        Elements trs = table.select("tr");
        List<String> columnmap = new ArrayList<>();
        for (Element th : trs.first().select("th")) {
            columnmap.add(getCopyColumnKey(th.text()));
        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        for (int i = 1; i < trs.size(); i++) {
            Elements tds = trs.get(i).select("td");
            Copy copy = new Copy();
            for (int j = 0; j < tds.size(); j++) {
                if (columnmap.get(j) == null) continue;
                String text = tds.get(j).text().replace("\u00a0", "");
                if (text.equals("")) continue;
                copy.set(columnmap.get(j), text, fmt);
            }
            item.addCopy(copy);
        }

        return item;
    }

    private String getCopyColumnKey(String text) {
        switch (text) {
            case "Zweigstelle":
                return "branch";
            case "Standorte":
                return "location";
            case "Status":
                return "status";
            case "Vorbestellungen":
                return "reservations";
            case "Frist":
                return "returndate";
            default:
                return null;
        }
    }

    @Override
    public DetailledItem getResult(int position) throws IOException, OpacErrorException {
        return null;
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

    @Override
    public List<SearchField> parseSearchFields()
            throws IOException, OpacErrorException, JSONException {
        String url =
                opac_url + "/" + data.getJSONObject("urls").getString("advanced_search") +
                        NO_MOBILE;
        Document doc = Jsoup.parse(httpGet(url, getDefaultEncoding()));

        Element table = doc.select(".ModOPENExtendedSearchModuleC table").first();

        List<SearchField> fields = new ArrayList<>();

        JSONObject selectable = new JSONObject();
        selectable.put("selectable", true);

        JSONObject notSelectable = new JSONObject();
        notSelectable.put("selectable", false);

        // Selectable search criteria
        Elements options = table.select("select[id$=FirstSearchField] option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setId(option.val());
            field.setDisplayName(option.text());
            field.setData(selectable);
            fields.add(field);
        }

        // More criteria
        Element moreHeader =
                table.select("span[id$=LblMoreCriterias]").parents().select("tr").first();
        if (moreHeader != null) {
            Elements siblings = moreHeader.siblingElements();
            int startIndex = moreHeader.elementSiblingIndex();
            for (int i = startIndex; i < siblings.size(); i++) {
                Element tr = siblings.get(i);
                if (tr.select("input, select").size() == 0) continue;

                if (tr.select("input[type=text]").size() == 1) {
                    Element input = tr.select("input[type=text]").first();
                    TextSearchField field = new TextSearchField();
                    field.setId(input.attr("name"));
                    field.setDisplayName(tr.select("span[id*=Lbl]").first().text());
                    field.setData(notSelectable);
                    if (tr.text().contains("nur Ziffern")) field.setNumber(true);
                    fields.add(field);
                } else if (tr.select("input[type=text]").size() == 2) {
                    Element input1 = tr.select("input[type=text]").get(0);
                    Element input2 = tr.select("input[type=text]").get(1);

                    TextSearchField field1 = new TextSearchField();
                    field1.setId(input1.attr("name"));
                    field1.setDisplayName(tr.select("span[id*=Lbl]").first().text());
                    field1.setData(notSelectable);
                    if (tr.text().contains("nur Ziffern")) field1.setNumber(true);
                    fields.add(field1);

                    TextSearchField field2 = new TextSearchField();
                    field2.setId(input2.attr("name"));
                    field2.setDisplayName(tr.select("span[id*=Lbl]").first().text());
                    field2.setData(notSelectable);
                    field2.setHalfWidth(true);
                    if (tr.text().contains("nur Ziffern")) field2.setNumber(true);
                    fields.add(field2);
                } else if (tr.select("select").size() == 1) {
                    Element select = tr.select("select").first();
                    DropdownSearchField dropdown = new DropdownSearchField();
                    dropdown.setId(select.attr("name"));
                    dropdown.setDisplayName(tr.select("span[id*=Lbl]").first().text());
                    List<DropdownSearchField.Option> values = new ArrayList<>();
                    for (Element option : select.select("option")) {
                        DropdownSearchField.Option opt =
                                new DropdownSearchField.Option(option.val(), option.text());
                        values.add(opt);
                    }
                    dropdown.setDropdownValues(values);
                    fields.add(dropdown);
                } else if (tr.select("input[type=checkbox]").size() == 1) {
                    Element checkbox = tr.select("input[type=checkbox]").first();
                    CheckboxSearchField field = new CheckboxSearchField();
                    field.setId(checkbox.attr("name"));
                    field.setDisplayName(tr.select("span[id*=Lbl]").first().text());
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return opac_url + "/Permalink.aspx" + "?id" + "=" + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        return null;
    }

    @Override
    public void setLanguage(String language) {

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

    /**
     * Better version of JSoup's implementation of this function ({@link
     * org.jsoup.nodes.FormElement#formData()}).
     *
     * @param form       The form to submit
     * @param submitName The name attribute of the button which is clicked to submit the form, or
     *                   null
     * @return A MultipartEntityBuilder containing the data of the form
     */
    protected MultipartEntityBuilder formData(FormElement form, String submitName) {
        MultipartEntityBuilder data = MultipartEntityBuilder.create();
        data.setLaxMode();

        // iterate the form control elements and accumulate their values
        for (Element el : form.elements()) {
            if (!el.tag().isFormSubmittable()) {
                continue; // contents are form listable, superset of submitable
            }
            String name = el.attr("name");
            if (name.length() == 0) continue;
            String type = el.attr("type");

            if ("select".equals(el.tagName())) {
                Elements options = el.select("option[selected]");
                boolean set = false;
                for (Element option : options) {
                    data.addTextBody(name, option.val());
                    set = true;
                }
                if (!set) {
                    Element option = el.select("option").first();
                    if (option != null) {
                        data.addTextBody(name, option.val());
                    }
                }
            } else if ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type)) {
                // only add checkbox or radio if they have the checked attribute
                if (el.hasAttr("checked")) {
                    data.addTextBody(name, el.val().length() > 0 ? el.val() : "on");
                }
            } else if ("submit".equalsIgnoreCase(type) || "image".equalsIgnoreCase(type)) {
                if (submitName != null && el.attr("name").contains(submitName)) {
                    data.addTextBody(name, el.val());
                }
            } else {
                data.addTextBody(name, el.val());
            }
        }
        return data;
    }
}
