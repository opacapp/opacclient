package de.geeksfactory.opacclient.apis;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.*;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Patron extends BaseApi {

    private String base_url;
    protected CookieStore cookieStore;
    protected static String QUERY_URL = "faces/Szukaj.jsp";

    protected List<NameValuePair> lastFormState;

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);
        cookieStore = new BasicCookieStore();
        try {
            base_url = lib.getData().getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getDefaultEncoding() {
        return "UTF-8";
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {

        List<NameValuePair> sp = buildSearchParams(query);
        sp.add(new BasicNameValuePair("form1:btnSzukajIndeks", "Szukaj"));
        HttpEntity ent = buildHttpEntity(sp);
        String html = httpPost(base_url + QUERY_URL, ent, getDefaultEncoding(), false, cookieStore);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        Document doc = Jsoup.parse(html);

        return parseResults(doc, 1);
    }

    private SearchRequestResult parseResults(Document doc, int i) {
        List<SearchResult> results = new ArrayList<>();
        int total_result_count = 0;
        int page_count = 0;
        int page_index = i;

        SearchRequestResult result =
                new SearchRequestResult(results, total_result_count, page_count, page_index);

        return result;
    }

    private HttpEntity buildHttpEntity(List<NameValuePair> nameValuePairs) throws IOException {
        return new UrlEncodedFormEntity(nameValuePairs);
    }

    protected List<NameValuePair> buildSearchParams(List<SearchQuery> query)
            throws OpacErrorException {
        List<NameValuePair> params = lastFormState;

        int n = 0;
        for (SearchQuery term : query) {
            if (term.getValue().isEmpty()) continue;
            if (term.getKey().startsWith("@")) {
                n++;

                if (n > 3) {
                    throw new OpacErrorException(stringProvider.getQuantityString(
                            StringProvider.LIMITED_NUM_OF_CRITERIA, 3, 3));
                }

                params.add(new BasicNameValuePair("form1:textField" + n, term.getValue()));
                if (n > 1) {
                    params.add(new BasicNameValuePair("rbOper" + (n - 1), "a"));
                }
                continue;
            }
            if (term.getSearchField().getId().startsWith("#")) {

                params.add(new BasicNameValuePair(term.getKey().substring(1), term.getValue()));
            }
        }
        params.add(new BasicNameValuePair("rbOperStem", "a"));

        return params;
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        return null;
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

    @Override
    public ReservationResult reservation(DetailledItem item, Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction, String selection)
            throws IOException, OpacErrorException {
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

    protected FormParseResult parseForm(String html) {
        return parseForm(Jsoup.parse(html));
    }

    protected FormParseResult parseForm(Document doc) {
        List<NameValuePair> params = new ArrayList<>();
        Element form = doc.select("#form1").first();
        for (Element e : form.select("input[type=\"text\"],input[type=\"hidden\"]," +
                "input[type=\"radio\"][checked=\"checked\"],input[type=\"submit\"]"
        )) {
            params.add(new BasicNameValuePair(e.attr("name"), e.attr("value")));
        }

        for (Element select : form.select("select")) {
            Elements selectedOptions = select.select("option[selected=\"selected\"]");
            if (!selectedOptions.isEmpty()) {
                params.add(new BasicNameValuePair(select.attr("name"),
                        selectedOptions.first().attr("value")));
            } else {
                Element firstOptions = select.select("option").first();
                params.add(new BasicNameValuePair(select.attr("name"), firstOptions.attr("value")));
            }

        }

        return new FormParseResult(form, params);
    }

    @Override
    public List<SearchField> getSearchFields()
            throws IOException, OpacErrorException, JSONException {

        String url = base_url + "faces/Szukaj.jsp";
        String html = httpGet(url, getDefaultEncoding(), false, cookieStore);
        FormParseResult res = parseForm(html);
        res.params.add(new BasicNameValuePair("form1:lnkZaaw", "form1:lnkZaaw"));

        url = res.form.attr("action");
        try {
            URIBuilder ub = new URIBuilder(base_url);
            ub.setPath(url);
            url = ub.build().toString();
        } catch (URISyntaxException e) {
            url = base_url + "faces/Szukaj.jsp";
        }
        html = httpPost(url, new UrlEncodedFormEntity(res.params),
                getDefaultEncoding(), false, cookieStore);

        res = parseForm(html);
        lastFormState = res.params;
        Element doc = res.form;
        List<SearchField> fields = new LinkedList<>();

        try {
            Elements options = doc.select("[id=\"form1:dropdown1\"]").first().select("option");

            for (Element option : options) {

                TextSearchField field = new TextSearchField();
                field.setDisplayName(option.text());
                field.setId("@term" + option.val());

                field.setData(new JSONObject());
                field.getData().put("meaning", field.getId());
                field.setHint("");
                fields.add(field);
            }


            Elements txtFields = doc.select("[id=\"form1:textField4\"],[id=\"form1:textField5\"]");
            for (Element txtField : txtFields) {
                TextSearchField field = new TextSearchField();
                String displayName = doc
                        .select(String.format("label[for=\"%s\"", txtField.id()))
                        .first()
                        .text()
                        .replaceAll(":$", "");
                field.setDisplayName(displayName);
                field.setId("#" + txtField.id());

                field.setData(new JSONObject());
                field.getData().put("meaning", field.getId());
                field.setHint("");
                fields.add(field);
            }

            Elements selects = doc.select("[id=\"form1:dropdown4\"]," +
                    "[id=\"form1:dropdown5\"]," +
                    "[id=\"form1:dropdown6\"]," +
                    "[id=\"form1:dropdown7\"]"
            );

            for (Element select : selects) {
                options = select.select("option");
                DropdownSearchField field = new DropdownSearchField();
                for (Element option : options) {
                    field.addDropdownValue(option.val(), option.text());
                }
                field.setId("#" + select.id());
                field.setData(new JSONObject());
                field.getData().put("meaning", field.getId());

                String displayName = doc
                        .select(String.format("label[for=\"%s\"", select.id()))
                        .first()
                        .text()
                        .replaceAll(":$", "");

                field.setDisplayName(displayName);
                fields.add(field);
            }
            throw new Exception("thesebas some err");

        } catch (Exception e) {
            System.err.print(e.getMessage());
        }

        return fields;
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

    private class FormParseResult {
        public Element form;
        public List<NameValuePair> params;

        public FormParseResult(Element form, List<NameValuePair> params) {
            this.form = form;
            this.params = params;
        }
    }
}