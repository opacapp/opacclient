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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * An implementation of {@link *.web-opac.at} sites operated by the Austrian company
 * <a href="https://littera.eu/">Littera</a>.
 */
public class WebOpacAt extends ReadOnlyApi {
    protected static final Map<String, String> LANGUAGE_CODES = new HashMap<String, String>() {{
        put("en", "eng");
        put("de", "deu");
        //put("tr", "tur");
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
    protected String opac_url = "";
    protected Map<String, String> lastQueryParam;

    @Override
    public void init(Library library) {
        super.init(library);
        try {
            this.opac_url = library.getData().getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        lastQueryParam = buildSearchParams(query);
        return executeSearch(lastQueryParam);
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        lastQueryParam.put("page", String.valueOf(page));
        return executeSearch(lastQueryParam);
    }

    protected SearchRequestResult executeSearch(Map<String, String> lastQuery)
            throws IOException, OpacErrorException, JSONException {
        final String html = httpGet(
                opac_url + "/search" + buildHttpGetParams(lastQuery, getDefaultEncoding()),
                getDefaultEncoding());
        final Document doc = Jsoup.parse(html);

        final String navigation = doc.select(".result_view .navigation").first().text();
        final int totalResults = parseTotalResults(navigation);
        final int pageIndex = Integer.parseInt(lastQuery.get("page"));

        final Element ul = doc.select(".result_view ul.list").first();
        final List<SearchResult> results = new ArrayList<>();
        for (final Element li : ul.children()) {
            final SearchResult result = new SearchResult();
            final Element title = li.select(".titelinfo a").first();
            result.setId(getQueryParamsFirst(title.attr("href")).get("id"));
            result.setInnerhtml(title.text() + "<br>" + title.parent().nextElementSibling().text());
            result.setNr(results.size());
            result.setPage(pageIndex);
            result.setType(MEDIA_TYPES.get(li.select(".statusinfo .ma").first().text()));
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
        final Matcher matcher = Pattern.compile("(von|of) (?<num1>\\d+)|(?<num2>\\d+) sonuçtan")
                .matcher(navigation);
        if (matcher.find()) {
            final String num1 = matcher.group("num1");
            return Integer.parseInt(num1 != null ? num1 : matcher.group("num2"));
        } else {
            return 0;
        }
    }

    protected Map<String, String> buildSearchParams(List<SearchQuery> query)
            throws IOException, JSONException {
        final Map<String, String> params = new TreeMap<>();
        if (query.size() == 1 && "q".equals(query.get(0).getSearchField().getId())) {
            params.put("mode", "s");
            params.put(query.get(0).getKey(), query.get(0).getValue());
        } else {
            int i = 0;
            for (SearchQuery q : query) {
                // crit_, value_, op_ are 0-indexed
                params.put("crit_" + i, q.getKey());
                params.put("value_" + i, q.getValue());
                if (i > 0) {
                    params.put("op_" + i, "AND");
                }
                i++;
            }
            params.put("mode", "a");
            params.put("critCount", String.valueOf(params.size())); // 1-index
        }
        params.put("page", "1"); // 1-indexed
        params.put("page_size", "30");
        return params;
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        final String html = httpGet(opac_url + "/search?view=detail&id=" + id, getDefaultEncoding());
        final Document doc = Jsoup.parse(html);
        final Element detailData = doc.select(".detailData").first();
        final Element detailTable = detailData.select("table.titel").first();
        final Element availabilityTable = doc.select(".bibliothek table").first();

        final DetailledItem result = new DetailledItem();
        final HashMap<String, String> copy = new HashMap<>();
        result.addCopy(copy);
        result.setId(id);
        result.setCover(getCover(doc));
        result.setTitle(detailData.select("h3").first().text());
        result.setMediaType(MEDIA_TYPES.get(getCellContent(detailTable, "Medienart|Type of media")));
        copy.put(DetailledItem.KEY_COPY_STATUS, getCellContent(availabilityTable, "Verfügbar|Available"));
        copy.put(DetailledItem.KEY_COPY_RESERVATIONS, getCellContent(availabilityTable, "Reservierungen|Reservations"));
        for (final Element tr : detailTable.select("tr")) {
            final String desc = tr.child(0).text();
            final String content = tr.child(1).text();
            if (desc != null && !desc.trim().isEmpty()) {
                result.addDetail(new Detail(desc, content));
            } else if (!result.getDetails().isEmpty()) {
                final Detail lastDetail = result.getDetails().get(result.getDetails().size() - 1);
                lastDetail.setHtml(true);
                lastDetail.setContent(lastDetail.getContent() + "<br>" + content);
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

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public DetailledItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public List<SearchField> getSearchFields()
            throws IOException, OpacErrorException, JSONException {
        start();
        final List<SearchField> fields = new ArrayList<>();
        getSimpleSearchField(fields);
        getAdvancedSearchFields(fields);
        return fields;
    }

    protected void getSimpleSearchField(List<SearchField> fields)
            throws IOException, JSONException {
        final String html = httpGet(opac_url + "/search?mode=s", getDefaultEncoding());
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

    protected void getAdvancedSearchFields(List<SearchField> fields)
            throws IOException, JSONException {
        final String html = httpGet(opac_url + "/search?mode=a", getDefaultEncoding());
        final Document doc = Jsoup.parse(html);
        final Elements options = doc.select("select#adv_search_crit_0").first().select("option");
        for (final Element option : options) {
            final TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.val());
            field.setHint("");
            field.setData(new JSONObject());
            field.getData().put("meaning", field.getId());
            fields.add(field);
        }
    }

    @Override
    protected String getDefaultEncoding() {
        return "utf-8";
    }

    @Override
    public String getShareUrl(String id, String title) {
        return null;
    }

    @Override
    public int getSupportFlags() {
        return 0;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        return null;
    }

    @Override
    public void setLanguage(String language) {
    }
}
