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
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Name;

import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class VuFind extends BaseApi {
    protected static HashMap<String, String> languageCodes = new HashMap<>();

    static {
        languageCodes.put("de", "de");
        languageCodes.put("en", "en");
        languageCodes.put("el", "el");
        languageCodes.put("es", "es");
        languageCodes.put("it", "it");
        languageCodes.put("fr", "fr");
        languageCodes.put("da", "da");
    }

    protected String languageCode = "en";
    protected String opac_url = "";
    protected JSONObject data;
    protected List<SearchQuery> last_query;

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

    protected List<NameValuePair> buildSearchParams(List<SearchQuery> query) {
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("sort", "relevancee"));
        params.add(new BasicNameValuePair("join", "AND"));

        for (SearchQuery singleQuery : query) {
            if (singleQuery.getValue().equals("")) continue;
            if (singleQuery.getKey().contains("filter[]")) {
                params.add(new BasicNameValuePair("filter[]", singleQuery.getValue()));
            } else {
                params.add(new BasicNameValuePair("type0[]", singleQuery.getKey()));
                params.add(new BasicNameValuePair("lookfor0[]", singleQuery.getValue()));
            }
        }
        return params;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        if (!initialised) start();
        last_query = query;
        String html = httpGet(opac_url + "/Search/Results" +
                buildHttpGetParams(buildSearchParams(query), getDefaultEncoding()),
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        return parse_search(doc, 1);
    }

    protected SearchRequestResult parse_search(Document doc, int page) throws OpacErrorException {
        doc.setBaseUri(opac_url + "/Search/Results");

        if (doc.select("p.error").size() > 0) {
            throw new OpacErrorException(doc.select("p.error").text());
        } else if (doc.select("div.result").size() == 0 && doc.select(".main p").size() > 0) {
            throw new OpacErrorException(doc.select(".main p").first().text());
        }

        int rescount = -1;
        if (doc.select(".resulthead").size() == 1)
            rescount = Integer.parseInt(
                    doc.select(".resulthead strong").get(2).text());
        List<SearchResult> reslist = new ArrayList<>();

        for (Element row : doc.select("div.result")) {
            SearchResult res = new SearchResult();
            Element z3988el = null;
            if (row.select("span.Z3988").size() == 1) {
                z3988el = row.select("span.3988").first();
            } else if (row.parent().tagName().equals("li") && row.parent().select("span.Z3988").size() > 0) {
                z3988el = row.parent().select("span.3988").first();
            }
            if (z3988el != null) {
                List<NameValuePair> z3988data;
                boolean hastitle = false;
                try {
                    StringBuilder description = new StringBuilder();
                    z3988data = URLEncodedUtils.parse(new URI("http://dummy/?"
                            + z3988el.select("span.Z3988").attr("title")), "UTF-8");
                    for (NameValuePair nv : z3988data) {
                        if (nv.getValue() != null) {
                            if (!nv.getValue().trim().equals("")) {
                                if (nv.getName().equals("rft.btitle")) {
                                    description.append("<b>").append(nv.getValue()).append("</b>");
                                    hastitle = true;
                                } else if (nv.getName().equals("rft.atitle")) {
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
                    res.setInnerhtml(description.toString());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                res.setInnerhtml(row.select("a.title").text());
            }

            res.setPage(page);
            res.setId(row.select("a.title").first().attr("href").replace("/Record/", ""));
            reslist.add(res);
        }

        return new SearchRequestResult(reslist, rescount, page);
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        List<NameValuePair> params = buildSearchParams(last_query);
        params.add(new BasicNameValuePair("page", String.valueOf(page)));
        String html = httpGet(opac_url + "/Search/Results" +
                        buildHttpGetParams(params, getDefaultEncoding()),
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        return parse_search(doc, page);
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public DetailledItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    public void start() throws IOException {
        super.start();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("mylang", languageCode));
        httpPost(opac_url + "/Search/Advanced", new UrlEncodedFormEntity(params),
                getDefaultEncoding());
    }

    @Override
    public List<SearchField> getSearchFields()
            throws IOException, OpacErrorException, JSONException {
        start();
        String html = httpGet(opac_url + "/Search/Advanced?mylang = " + languageCode,
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        List<SearchField> fields = new ArrayList<>();

        Elements options = doc.select("select#search_type0_0 option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.val());
            field.setHint("");
            field.setData(new JSONObject());
            field.getData().put("meaning", option.val());
            fields.add(field);
        }
        if (fields.size() == 0) {
            // Weird JavaScript, e.g. view-source:http://vopac.nlg.gr/Search/Advanced
            Pattern pattern_key = Pattern
                    .compile("searchFields\\[\"([^\"]+)\"\\] = \"([^\"]+)\";");
            for (Element script : doc.select("script")) {
                if (!script.html().contains("searchFields")) continue;
                for (String line : script.html().split("\n")) {
                    Matcher matcher = pattern_key.matcher(line);
                    if (matcher.find()) {
                        TextSearchField field = new TextSearchField();
                        field.setDisplayName(matcher.group(2));
                        field.setId(matcher.group(1));
                        field.setHint("");
                        field.setData(new JSONObject());
                        field.getData().put("meaning", field.getId());
                        fields.add(field);
                    }
                }
            }
        }

        Elements selects = doc.select("select");
        for (Element select : selects) {
            if (!select.attr("name").equals("filter[]")) continue;
            DropdownSearchField field = new DropdownSearchField();
            if (select.parent().select("label").size() > 0) {
                field.setDisplayName(select.parent().select("label").first()
                                           .text());
            }
            field.setId(select.attr("name") + select.attr("id"));
            List<Map<String, String>> dropdownOptions = new ArrayList<>();
            String meaning = select.attr("id");
            Map<String, String> emptyDropdownOption = new HashMap<>();
            emptyDropdownOption.put("key", "");
            emptyDropdownOption.put("value", "");
            dropdownOptions.add(emptyDropdownOption);
            for (Element option : select.select("option")) {
                if (option.val().contains(":")) {
                    meaning = option.val().split(":")[0];
                }
                Map<String, String> dropdownOption = new HashMap<>();
                dropdownOption.put("key", option.val());
                dropdownOption.put("value", option.text());
                dropdownOptions.add(dropdownOption);
            }
            field.setDropdownValues(dropdownOptions);
            field.setData(new JSONObject());
            field.getData().put("meaning", meaning);
            fields.add(field);
        }

        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return null;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        Set<String> langs = new HashSet<>();
        String html = httpGet(opac_url + "/Search/Advanced",
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        if (doc.select("select[name=mylang]").size() > 0) {
            for (Element opt : doc.select("select[name=mylang] option")) {
                if (languageCodes.containsValue(opt.val())) {
                    for (Map.Entry<String, String> lc : languageCodes.entrySet()) {
                        if (lc.getValue().equals(opt.val())) {
                            langs.add(lc.getKey());
                            break;
                        }
                    }
                } else {
                    langs.add(opt.val());
                }
            }
        }
        return langs;
    }

    protected String getDefaultEncoding() {
        return "UTF-8";
    }

    @Override
    public void setLanguage(String language) {
        languageCode = languageCodes.containsKey(language) ? languageCodes.get(language) : language;
    }

    @Override
    public boolean isAccountSupported(Library library) {
        return false;
    }

    @Override
    public boolean isAccountExtendable() {
        return false;
    }

    @Override
    public String getAccountExtendableInfo(Account account) throws IOException {
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
}
