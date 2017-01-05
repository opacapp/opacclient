/**
 * Copyright (C) 2015 by Simon Legner under the MIT license:
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

import org.apache.http.client.utils.URIBuilder;
import org.joda.time.LocalDate;
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * An implementation of *.web-opac.at sites operated by the Austrian company
 * <a href="https://littera.eu/">Littera</a>.
 */
public class Littera extends ApacheSearchOnlyApi {
    protected static final Map<String, String> LANGUAGE_CODES = new HashMap<String, String>() {{
        put("en", "eng");
        put("de", "deu");
        put("tr", "tur");
    }};
    protected static final Map<String, SearchResult.MediaType> MEDIA_TYPES =
            new HashMap<String, SearchResult.MediaType>() {{
        // de
        put("Book", SearchResult.MediaType.BOOK);
        put("Zeitschrift", SearchResult.MediaType.MAGAZINE);
        put("CD ROM", SearchResult.MediaType.CD_SOFTWARE);
        put("DVD", SearchResult.MediaType.DVD);
        put("Hörbuch", SearchResult.MediaType.AUDIOBOOK);
        put("eMedium", SearchResult.MediaType.EBOOK);
        // en
        put("Book", SearchResult.MediaType.BOOK);
        put("Periodical", SearchResult.MediaType.MAGAZINE);
        put("CD ROM", SearchResult.MediaType.CD_SOFTWARE);
        put("DVD", SearchResult.MediaType.DVD);
        put("Audiobook", SearchResult.MediaType.AUDIOBOOK);
        put("eMedium", SearchResult.MediaType.EBOOK);
    }};
    protected static final List<String> SEARCH_FIELDS_FOR_DROPDOWN =
            Arrays.asList("ma", "sy", "og");
    protected String opac_url = "";
    protected String languageCode;
    protected List<SearchQuery> lastQuery;

    @Override
    public void init(Library library, HttpClientFactory httpClientFactory) {
        super.init(library, httpClientFactory);
        try {
            this.opac_url = library.getData().getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getApiUrl() {
        return opac_url + "/search?lang=" + getLanguage();
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        lastQuery = query;
        return executeSearch(query, 1);
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        return executeSearch(lastQuery, page);
    }

    protected SearchRequestResult executeSearch(List<SearchQuery> query, int pageIndex)
            throws IOException, OpacErrorException, JSONException {
        final String searchUrl;
        if (!initialised) {
            start();
        }
        try {
            searchUrl = buildSearchUrl(query, pageIndex);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final String html = httpGet(searchUrl, getDefaultEncoding());
        final Document doc = Jsoup.parse(html);

        final Element navigation = doc.select(".result_view .navigation").first();
        final int totalResults = navigation != null ? parseTotalResults(navigation.text()) : 0;

        final Element ul = doc.select(".result_view ul.list").first();
        final List<SearchResult> results = new ArrayList<>();
        for (final Element li : ul.children()) {
            if (li.hasClass("zugangsmonat")) {
                continue;
            }
            final SearchResult result = new SearchResult();
            final Element title = li.select(".titelinfo a").first();
            result.setId(getQueryParamsFirst(title.attr("href")).get("id"));
            result.setInnerhtml(title.text() + "<br>" + title.parent().nextElementSibling().text());
            result.setNr(results.size());
            result.setPage(pageIndex);
            result.setType(MEDIA_TYPES.get(li.select(".statusinfo .ma").text()));
            result.setCover(getCover(li));
            final String statusImg = li.select(".status img").attr("src");
            result.setStatus(statusImg.contains("-yes")
                    ? SearchResult.Status.GREEN
                    : statusImg.contains("-no")
                    ? SearchResult.Status.RED
                    : null);
            results.add(result);
        }
        return new SearchRequestResult(results, totalResults, pageIndex);
    }

    static int parseTotalResults(final String navigation) {
        final Matcher matcher = Pattern.compile("(von|of) (\\d+)|(\\d+) sonu\\u00e7tan")
                .matcher(navigation);
        if (matcher.find()) {
            final String num1 = matcher.group(2);
            return Integer.parseInt(num1 != null ? num1 : matcher.group(3));
        } else {
            return 0;
        }
    }

    protected String buildSearchUrl(final List<SearchQuery> query, final int page)
            throws IOException, JSONException, URISyntaxException {
        final URIBuilder builder = new URIBuilder(getApiUrl());
        final List<SearchQuery> nonEmptyQuery = new ArrayList<>();
        for (SearchQuery q : query) {
            if (q.getValue().equals("")) continue;
            if (q.getKey().startsWith("sort")) {
                builder.addParameter(q.getKey(), q.getValue());
            } else {
                nonEmptyQuery.add(q);
            }
        }
        if (nonEmptyQuery.isEmpty()) {
            builder.setParameter("mode", "n");
        } else if (nonEmptyQuery.size() == 1 && "q".equals(nonEmptyQuery.get(0).getSearchField().getId())) {
            builder.setParameter("mode", "s");
            builder.setParameter(nonEmptyQuery.get(0).getKey(), nonEmptyQuery.get(0).getValue());
        } else {
            int i = 0;
            for (SearchQuery q : nonEmptyQuery) {
                // crit_, value_, op_ are 0-indexed
                String key = q.getKey();
                String value = q.getValue();
                if ("q".equals(key)) {
                    // fall back to title since free search cannot be combined with other criteria
                    key = "ht";
                    value = "*" + value + "*";
                }
                builder.setParameter("crit_" + i, key);
                builder.setParameter("value_" + i, value);
                if (i > 0) {
                    builder.setParameter("op_" + i, "AND");
                }
                i++;
            }
            builder.setParameter("mode", "a");
            builder.setParameter("critCount", String.valueOf(i)); // 1-index
        }
        builder.setParameter("page", String.valueOf(page)); // 1-indexed
        builder.setParameter("page_size", "30");
        return builder.build().toString();
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        if (!initialised) {
            start();
        }
        final String html = httpGet(getApiUrl() + "&view=detail&id=" + id, getDefaultEncoding());
        final Document doc = Jsoup.parse(html);
        final Element detailData = doc.select(".detailData").first();
        final Element detailTable = detailData.select("table.titel").first();
        final Element availabilityTable = doc.select(".bibliothek table").first();

        final DetailedItem result = new DetailedItem();
        final Copy copy = new Copy();
        result.addCopy(copy);
        result.setId(id);
        result.setCover(getCover(doc));
        result.setTitle(detailData.select("h3").first().text());
        result.setMediaType(MEDIA_TYPES.get(getCellContent(detailTable, "Medienart|Type of media")));
        copy.setStatus(getCellContent(availabilityTable, "Verfügbar|Available"));
        copy.setReturnDate(parseCopyReturn(
                getCellContent(availabilityTable, "Exemplare verliehen|Copies lent")));
        copy.setReservations(getCellContent(availabilityTable, "Reservierungen|Reservations"));
        for (final Element tr : detailTable.select("tr")) {
            final String desc = tr.child(0).text();
            final String content = tr.child(1).text();
            if (desc != null && !desc.trim().equals("")) {
                result.addDetail(new Detail(desc, content));
            } else if (!result.getDetails().isEmpty()) {
                final Detail lastDetail = result.getDetails().get(result.getDetails().size() - 1);
                lastDetail.setHtml(true);
                lastDetail.setContent(lastDetail.getContent() + "\n" + content);
            }
        }
        return result;
    }

    private String getCellContent(Element detailTable, String pattern) {
        final Element first = detailTable.select("td.label:matchesOwn(" + pattern + ")").first();
        return first == null ? null : first.nextElementSibling().text();
    }

    private static String getCover(Element doc) {
        return doc.select(".coverimage img").first().attr("src").replaceFirst("&width=\\d+", "");
    }

    static LocalDate parseCopyReturn(String str) {
        if (str == null)
            return null;
        DateTimeFormatter fmt =
                DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
        final Matcher matcher = Pattern.compile("[0-9.-]{4,}").matcher(str);
        if (matcher.find()) {
            return fmt.parseLocalDate(matcher.group());
        } else {
            return null;
        }
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException, OpacErrorException {
        // Not necessary since getResultById returns an ID with every result
        return null;
    }

    @Override
    public List<SearchField> parseSearchFields()
            throws IOException, OpacErrorException, JSONException {
        start();
        final List<SearchField> fields = new ArrayList<>();
        addSimpleSearchField(fields);
        addAdvancedSearchFields(fields);
        addSortingSearchFields(fields);
        return fields;
    }

    protected void addSimpleSearchField(List<SearchField> fields)
            throws IOException, JSONException {
        final String html = httpGet(getApiUrl() + "&mode=s", getDefaultEncoding());
        final Document doc = Jsoup.parse(html);
        final Element simple = doc.select(".simple_search").first();
        final TextSearchField field = new TextSearchField();
        field.setFreeSearch(true);
        field.setDisplayName(simple.select("h4").first().text());
        field.setId(simple.select("#keyboard").first().attr("name"));
        field.setHint("");
        field.setData(new JSONObject());
        field.getData().put("meaning", field.getId());
        fields.add(field);
    }

    protected void addAdvancedSearchFields(List<SearchField> fields)
            throws IOException, JSONException {
        final String html = httpGet(getApiUrl() + "&mode=a", getDefaultEncoding());
        final Document doc = Jsoup.parse(html);
        final Elements options = doc.select("select#adv_search_crit_0").first().select("option");
        for (final Element option : options) {
            final SearchField field;
            if (SEARCH_FIELDS_FOR_DROPDOWN.contains(option.val())) {
                field = new DropdownSearchField();
                addDropdownValuesForField(((DropdownSearchField) field), option.val());
            } else {
                field = new TextSearchField();
                ((TextSearchField) field).setHint("");
            }
            field.setDisplayName(option.text());
            field.setId(option.val());
            field.setData(new JSONObject());
            field.getData().put("meaning", field.getId());
            fields.add(field);
        }
    }

    protected void addDropdownValuesForField(DropdownSearchField field, String id)
            throws IOException, JSONException {
        field.addDropdownValue("", "");
        final String url = opac_url + "/search/adv_ac?crit=" + id;
        final String json = httpGet(url, getDefaultEncoding());
        try {
            final JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                field.addDropdownValue(obj.getString("value"), obj.getString("label"));
            }
        } catch (JSONException e) {
            if (json.startsWith("[")) {
                throw e;
            } else {
                // This is probably a different format
                final String[] lines = json.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (!line.equals("")) {
                        if (line.contains("|")) {
                            String[] parts = line.split("\\|");
                            field.addDropdownValue(parts[0], parts[1]);
                        } else {
                            field.addDropdownValue(line, line);
                        }
                    }
                }
            }
        }
    }

    protected void addSortingSearchFields(List<SearchField> fields)
            throws IOException, JSONException {
        final String html = httpGet(getApiUrl() + "&mode=a", getDefaultEncoding());
        final Document doc = Jsoup.parse(html);
        for (int i = 0; i < 3; i++) {
            final Element tr = doc.select("#sort_editor tr.sort_" + i).first();
            final DropdownSearchField field = new DropdownSearchField();
            field.setMeaning(SearchField.Meaning.ORDER);
            field.setId("sort_" + i);
            field.setDisplayName(tr.select("td").first().text());
            field.addDropdownValue("", "");
            for (final Element option : tr.select(".crit option")) {
                if (option.hasAttr("selected")) {
                    field.addDropdownValue(0, option.attr("value"), option.text());
                } else {
                    field.addDropdownValue(option.attr("value"), option.text());
                }
            }
            fields.add(field);
        }
    }

    @Override
    protected String getDefaultEncoding() {
        return "utf-8";
    }

    @Override
    public String getShareUrl(String id, String title) {
        return getApiUrl() + "&view=detail&id=" + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        final String html = httpGet(getApiUrl() + "&mode=a", getDefaultEncoding());
        final Document doc = Jsoup.parse(html);
        final String menuHtml = doc.select(".mainmenu").first().html();
        final Set<String> languages = new HashSet<>();
        for (final Map.Entry<String, String> i : LANGUAGE_CODES.entrySet()) {
            if (menuHtml.contains("lang=" + i.getValue()) /* language switch link */
                    || menuHtml.contains("/" + i.getValue() + "/") /* help link */) {
                languages.add(i.getKey());
            }
        }
        return languages;
    }

    @Override
    public void setLanguage(String language) {
        if (initialised && supportedLanguages.contains(language)) {
            this.languageCode = LANGUAGE_CODES.get(language);
        }
    }

    protected String getLanguage() {
        return languageCode != null ? languageCode : "eng";
    }
}
