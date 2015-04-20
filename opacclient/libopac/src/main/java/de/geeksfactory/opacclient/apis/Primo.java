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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class Primo extends BaseApi {
    protected static HashMap<String, String> languageCodes = new HashMap<>();

    static {
        languageCodes.put("en", "en_US");
        languageCodes.put("de", "de_DE");
    }

    protected String languageCode = "en_US";
    protected String opac_url = "";
    protected String vid = "";
    protected JSONObject data;
    protected List<SearchQuery> last_query;

    @Override
    public void init(Library lib) {
        super.init(lib);

        this.library = lib;
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
            this.vid = data.getString("db");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<NameValuePair> buildSearchParams(List<SearchQuery> query) {
        return null;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        if (!initialised) start();
        last_query = query;
        Document doc = null;
        return parse_search(doc, 1);
    }

    protected SearchRequestResult parse_search(Document doc, int page) throws OpacErrorException {
        List<SearchResult> reslist = new ArrayList<>();

        return new SearchRequestResult(reslist, 0, page);
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
        Document doc = null;

        return parse_search(doc, page);
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        if (!initialised) start();
        Document doc = null;
        return parse_detail(id, doc);
    }

    protected DetailledItem parse_detail(String id, Document doc) throws OpacErrorException {
        DetailledItem res = new DetailledItem();
        res.setId(id);

        return res;
    }

    @Override
    public DetailledItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    public void start() throws IOException {
        super.start();
        httpGet(opac_url + "/action/preferences.do?fn=change_lang&vid="+ vid + "&prefLang=" + languageCode,
                getDefaultEncoding());
    }

    @Override
    public List<SearchField> getSearchFields()
            throws IOException, OpacErrorException, JSONException {
        start();
        String html = httpGet(opac_url + "/action/search.do?mode=Advanced&ct=AdvancedSearch&vid=" + vid,
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        List<SearchField> fields = new ArrayList<>();

        Elements options = doc.select("select#exlidInput_scope_1").first().select("option");
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

        Elements selects = doc.select("#exlidInput_mediaType_, #exlidInput_publicationDate_, " +
                "#exlidInput_language_");
        for (Element select : selects) {
            DropdownSearchField field = new DropdownSearchField();
            if (select.parent().select("label").size() > 0) {
                field.setDisplayName(select.parent().select("label").first()
                                           .text());
            }
            field.setId("#" + select.attr("id"));
            List<Map<String, String>> dropdownOptions = new ArrayList<>();
            for (Element option : select.select("option")) {
                Map<String, String> dropdownOption = new HashMap<>();
                dropdownOption.put("key", option.val());
                dropdownOption.put("value", option.text());
                dropdownOptions.add(dropdownOption);
            }
            field.setDropdownValues(dropdownOptions);
            field.setData(new JSONObject());
            field.getData().put("meaning", field.getId());
            fields.add(field);
        }

        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return opac_url + "/Record/" + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        Set<String> langs = new HashSet<>();
        // TODO: Implement. Not that easy, as some libaries do and some don't include the
        // default language in their language chooser
        // With full chooser: http://primo.kobv.de/primo_library/libweb/action/search.do?vid=FUB
        // With toggle only: http://primo.kobv.de/primo_library/libweb/action/search.do?vid=hub_ub
        // Without any chooser: http://explore.bl.uk/primo_library/libweb/action/search.do?vid=BLVU1
        langs.add("de");
        langs.add("en");
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
