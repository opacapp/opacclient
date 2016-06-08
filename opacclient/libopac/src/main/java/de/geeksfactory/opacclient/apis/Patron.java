package de.geeksfactory.opacclient.apis;

import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.*;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Patron extends BaseApi {

    private String base_url;

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);
        try {
            base_url = lib.getData().getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        System.err.print("starting Patron");

    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        return null;
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

    protected HttpEntity preGetSearchFields(String url) throws IOException {
        String html = httpGet(url, getDefaultEncoding(), true);
        Document doc = Jsoup.parse(html);

        List<NameValuePair> params = new ArrayList<>();
        for (Element e : doc.select("#form1").select("input")) {
            params.add(new BasicNameValuePair(e.attr("name"), e.attr("value")));
        }
        return new UrlEncodedFormEntity(params);
    }

    @Override
    public List<SearchField> getSearchFields()
            throws IOException, OpacErrorException, JSONException {

        String url = base_url + "faces/Szukaj.jsp";
        String html = httpPost(url, preGetSearchFields(base_url), getDefaultEncoding());
        Document doc = Jsoup.parse(html);
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
}