/**
 * Copyright (C) 2014 by Johan von Forstner under the MIT license:
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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.utils.Base64;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

/**
 * @author Johan von Forstner, 06.04.2014
 *
 * WebOPAC.net, Version 2.2.70 gestartet mit Gemeindebibliothek Nürensdorf (erstes
 * Google-Suchergebnis)
 *
 * Unterstützt bisher nur Katalogsuche, Accountunterstüzung könnte (wenn keine Kontodaten
 * verfügbar sind) über den Javascript-Code reverse-engineered werden:
 * http://www.winmedio.net/nuerensdorf/de/mobile/GetScript.ashx?id=mobile.de.min.js&amp;v=20140122
 */

/*
weitere kompatible Bibliotheken:
 https://www.google.de/search?q=webOpac.net%202.1.30%20powered%20by%20winMedio.net&qscrl=1#q=%22webOpac.net+2.2.70+powered+by+winMedio.net%22+inurl%3Awinmedio&qscrl=1&start=0
  */

public class WebOpacNet extends OkHttpBaseApi implements OpacApi {

    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();

    static {
        defaulttypes.put("1", MediaType.BOOK);
        defaulttypes.put("2", MediaType.CD_MUSIC);
        defaulttypes.put("3", MediaType.AUDIOBOOK);
        defaulttypes.put("4", MediaType.DVD);
        defaulttypes.put("5", MediaType.CD_SOFTWARE);
        defaulttypes.put("8", MediaType.MAGAZINE);
    }

    protected String opac_url = "";
    protected JSONObject data;
    protected List<SearchQuery> query;

    protected String sessionId;

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
        this.query = query;
        start();

        String json = httpGet(opac_url + "/de/mobile/GetMedien.ashx?"
                + buildParams(query, 1), getDefaultEncoding());

        return parse_search(json, 1);
    }

    protected int addParameters(SearchQuery query, StringBuilder params,
            int index) {
        if (query.getValue().equals("")) {
            return index;
        }
        if (index > 0) {
            params.append("$0");
        }
        params.append("|").append(query.getKey()).append("|").append(query.getValue());
        return index + 1;
    }

    protected SearchRequestResult parse_search(String text, int page)
            throws OpacErrorException {
        if (!text.equals("")) {
            try {
                List<SearchResult> results = new ArrayList<>();
                JSONObject json = new JSONObject(text);
                int total_result_count = Integer.parseInt(json
                        .getString("totalcount"));

                JSONArray resultList = json.getJSONArray("mobmeds");
                for (int i = 0; i < resultList.length(); i++) {
                    JSONObject resultJson = resultList.getJSONObject(i);
                    SearchResult result = new SearchResult();
                    result.setId(resultJson.getString("medid"));

                    String title = resultJson.getString("titel");
                    String publisher = resultJson.getString("verlag");
                    String series = resultJson.getString("reihe");
                    String html = "<b>" + title + "</b><br />" + publisher
                            + ", " + series;

                    result.setType(getMediaType(resultJson.getString("iconurl")));

                    result.setInnerhtml(html);

                    if (resultJson.getString("imageurl").length() > 0) {
                        result.setCover(resultJson.getString("imageurl"));
                    }

                    results.add(result);
                }

                return new SearchRequestResult(results, total_result_count,
                        page);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new OpacErrorException(stringProvider.getFormattedString(
                        StringProvider.INTERNAL_ERROR_WITH_DESCRIPTION,
                        e.getMessage()));
            }
        } else {
            return new SearchRequestResult(new ArrayList<SearchResult>(), 0,
                    page);
        }

    }

    private static MediaType getMediaType(String iconurl) {
        String number = iconurl.substring(12, 13);
        return defaulttypes.get(number);
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException, OpacErrorException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException, JSONException {
        start();

        String json = httpGet(opac_url + "/de/mobile/GetMedien.ashx?"
                + buildParams(query, page), getDefaultEncoding());

        return parse_search(json, page);
    }

    protected String buildParams(List<SearchQuery> queryList, int page)
            throws JSONException, OpacErrorException {
        int index = 0;

        StringBuilder queries = new StringBuilder();
        queries.append("erw:0");
        for (SearchQuery query : queryList) {
            if (!query.getSearchField().getData().getBoolean("filter")) {
                index = addParameters(query, queries, index);
            }
        }

        for (SearchQuery query : queryList) {
            if (query.getSearchField().getData().getBoolean("filter")
                    && !query.getValue().equals("")) {
                queries.append("&").append(query.getKey()).append("=").append(query.getValue());
            }
        }

        StringBuilder params = new StringBuilder();
        try {
            params.append("q=").append(URLEncoder.encode(queries.toString(), "UTF-8"));
            params.append("&p=").append(String.valueOf(page - 1));
            params.append("&t=1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }
        return params.toString();
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        String json = httpGet(opac_url + "/de/mobile/GetDetail.ashx?id=" + id + "&orientation=1",
                getDefaultEncoding());
        return parse_detail(json);
    }

    private DetailedItem parse_detail(String text) throws OpacErrorException {
        try {
            DetailedItem result = new DetailedItem();
            JSONObject json = new JSONObject(text);

            String title = json.getString("titel");
            setTitleAndSubtitle(result, title, stringProvider);
            result.setCover(json.getString("imageurl"));
            result.setId(json.getString("medid"));

            // Details
            JSONArray info = json.getJSONArray("medium");
            for (int i = 0; i < info.length(); i++) {
                JSONObject detailJson = info.getJSONObject(i);
                String name = detailJson.getString("bez");
                String value = "";

                JSONArray values = detailJson.getJSONArray("values");
                for (int j = 0; j < values.length(); j++) {
                    JSONObject valJson = values.getJSONObject(j);
                    if (j != 0) {
                        value += ", ";
                    }
                    String content = valJson.getString("dval");
                    content = content.replaceAll("<span[^>]*>", "");
                    content = content.replaceAll("</span>", "");
                    value += content;
                }
                Detail detail = new Detail(name, value);
                result.addDetail(detail);
            }

            // Copies
            JSONArray copies = json.getJSONArray("exemplare");
            for (int i = 0; i < copies.length(); i++) {
                JSONObject copyJson = copies.getJSONObject(i);
                Copy copy = new Copy();

                JSONArray values = copyJson.getJSONArray("rows");
                for (int j = 0; j < values.length(); j++) {
                    JSONObject valJson = values.getJSONObject(j);
                    String name = valJson.getString("bez");
                    String value = valJson.getJSONArray("values")
                                          .getJSONObject(0).getString("dval");
                    if (!value.equals("")) {
                        switch (name) {
                            case "Exemplarstatus":
                                copy.setStatus(value);
                                break;
                            case "Signatur":
                                copy.setShelfmark(value);
                                break;
                            case "Standort":
                                copy.setLocation(value);
                                break;
                            case "Themenabteilung":
                                if (copy.getDepartment() != null) {
                                    value = copy.getDepartment() + value;
                                }
                                copy.setDepartment(value);
                                break;
                            case "Themenbereich":
                                if (copy.getDepartment() != null) {
                                    value = copy.getDepartment() + value;
                                }
                                copy.setDepartment(value);
                                break;
                        }
                    }
                }
                result.addCopy(copy);
            }

            result.setReservable(true); // we don't know if it's reservable until we try
            // there is some logic in the JavaScript code that determines whether the reservation
            // button is shown or not, but it is more complicated to implement as it depends on the
            // configuration of the OPAC

            return result;

        } catch (JSONException e) {
            e.printStackTrace();
            throw new OpacErrorException(stringProvider.getFormattedString(
                    StringProvider.INTERNAL_ERROR_WITH_DESCRIPTION,
                    e.getMessage()));
        }

    }

    static void setTitleAndSubtitle(DetailedItem result, String title, StringProvider sp) {
        Matcher titleMatcher = Pattern.compile("<\u00ac1>([^<]*)</\u00ac1>").matcher(title);
        Matcher subtitleMatcher = Pattern.compile("<\u00ac2>([^<]*)</\u00ac2>").matcher(title);
        if (titleMatcher.find()) {
            result.setTitle(Jsoup.parse(titleMatcher.group(1)).text());
            if (subtitleMatcher.find()) {
                result.addDetail(new Detail(sp.getString(
                        StringProvider.SUBTITLE), Jsoup.parse(subtitleMatcher.group(1)).text()));
            }
        } else {
            result.setTitle(title);
        }
    }

    @Override
    public DetailedItem getResult(int position) {
        return null;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        if (sessionId == null) {
            try {
                login(account);
            } catch (JSONException e) {
                throw new IOException(e);
            } catch (OpacErrorException e) {
                return new ReservationResult(MultiStepResult.Status.ERROR, e.getMessage());
            }
        }

        try {
            if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
                return reservationDo(item.getId(), account);
            } else {
                return reservationCheck(item.getId(), account);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        } catch (OpacErrorException e) {
            return new ReservationResult(MultiStepResult.Status.ERROR, e.getMessage());
        }
    }

    private ReservationResult reservationCheck(String media, Account acc)
            throws JSONException, IOException, OpacErrorException {
        JSONObject response = res(media, "3", acc);
        if (response.getString("possible").equals("True")) {
            if (response.getString("hasgebuehr").equals("1")) {
                ReservationResult res =
                        new ReservationResult(MultiStepResult.Status.CONFIRMATION_NEEDED);
                res.setDetails(
                        Collections.singletonList(new String[]{response.getString("gebuehr")}));
                return res;
            } else {
                return reservationDo(media, acc);
            }
        } else {
            return new ReservationResult(MultiStepResult.Status.ERROR,
                    response.getString("message"));
        }
    }

    private ReservationResult reservationDo(String media, Account acc)
            throws IOException, JSONException, OpacErrorException {
        JSONObject response = res(media, "1", acc);
        if (response.getString("success").equals("True")) {
            return new ReservationResult(MultiStepResult.Status.OK);
        } else {
            return new ReservationResult(MultiStepResult.Status.ERROR, response.getString("error"));
        }
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        if (sessionId == null) {
            try {
                login(account);
            } catch (JSONException e) {
                throw new IOException(e);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR, e.getMessage());
            }
        }

        try {
            if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
                return prolongDo(media, account);
            } else {
                return prolongCheck(media, account);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        } catch (OpacErrorException e) {
            return new ProlongResult(MultiStepResult.Status.ERROR, e.getMessage());
        }
    }

    private ProlongResult prolongCheck(String media, Account acc)
            throws JSONException, IOException, OpacErrorException {
        JSONObject response = ausleihe(media, "3", acc);

        if (response.getString("possible").equals("True")) {
            if (!response.getString("gebuehr").equals("0.00")) {
                ProlongResult res =
                        new ProlongResult(MultiStepResult.Status.CONFIRMATION_NEEDED);
                String fee = stringProvider.getFormattedString(StringProvider.FEE_CONFIRMATION,
                        response.getString("gebuehr"));
                res.setDetails(Collections.singletonList(new String[]{fee}));
                return res;
            } else {
                return prolongDo(media, acc);
            }
        } else {
            return new ProlongResult(MultiStepResult.Status.ERROR, response.getString("message"));
        }
    }

    private ProlongResult prolongDo(String media, Account acc)
            throws IOException, JSONException, OpacErrorException {
        JSONObject response = ausleihe(media, "4", acc);
        if (response.getString("success").equals("True")) {
            return new ProlongResult(MultiStepResult.Status.OK);
        } else {
            return new ProlongResult(MultiStepResult.Status.ERROR, response.getString("error"));
        }
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        if (sessionId == null) {
            try {
                login(account);
            } catch (JSONException e) {
                throw new IOException(e);
            } catch (OpacErrorException e) {
                return new ProlongAllResult(MultiStepResult.Status.ERROR, e.getMessage());
            }
        }

        try {
            if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
                return prolongAllDo(account);
            } else {
                return prolongAllCheck(account);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        } catch (OpacErrorException e) {
            return new ProlongAllResult(MultiStepResult.Status.ERROR, e.getMessage());
        }
    }

    private ProlongAllResult prolongAllCheck(Account acc)
            throws IOException, JSONException, OpacErrorException {
        JSONObject response = ausleihe(null, "7", acc);
        if (response.getString("possible").equals("True")) {
            if (!response.getString("gebuehr").equals("0.00")) {
                ProlongAllResult res =
                        new ProlongAllResult(MultiStepResult.Status.CONFIRMATION_NEEDED);
                String fee = stringProvider.getFormattedString(StringProvider.FEE_CONFIRMATION,
                        response.getString("gebuehr"));
                res.setDetails(Collections.singletonList(new String[]{fee}));
                return res;
            } else {
                return prolongAllDo(acc);
            }
        } else {
            return new ProlongAllResult(MultiStepResult.Status.ERROR,
                    response.getString("message"));
        }
    }

    private ProlongAllResult prolongAllDo(Account acc)
            throws JSONException, IOException, OpacErrorException {
        JSONObject response = ausleihe(null, "8", acc);
        if (response.getString("success").equals("True")) {
            return new ProlongAllResult(MultiStepResult.Status.OK);
        } else if (response.getString("success").equals("False") && response.getString("error")
                                                                            .matches(
                                                                                    "Es konnten \\d+ von \\d+ Medien verlängert werden.")) {
            return new ProlongAllResult(MultiStepResult.Status.OK, response.getString("error"));
        } else {
            return new ProlongAllResult(MultiStepResult.Status.ERROR, response.getString("error"));
        }
    }

    private JSONObject ausleihe(String media, String aktion, Account acc)
            throws JSONException, IOException, OpacErrorException {
        // aktion:
        // 3 = check prolong, 4 = prolong
        // 7 = check prolong all, 8 = prolong all
        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("aktion", aktion);
        formData.add("zugid", media != null ? media : "");
        formData.add("biblNr", "0");
        formData.add("aid", "");
        formData.add("sessionid", sessionId);

        return httpPostAccount(opac_url + "/de/mobile/Ausleihe.ashx", formData.build(), acc);
    }

    /**
     * Executes a HTTP POST. When the response is empty, the request is retried after login.
     */
    private JSONObject httpPostAccount(String url, FormBody body, Account acc)
            throws IOException, OpacErrorException, JSONException {
        String s = httpPost(url, body, getDefaultEncoding());
        if (s.equals("")) {
            login(acc);
            String newBody =
                    bodyToString(body).replaceAll("sessionid=[^&]*", "sessionid=" + sessionId);
            s = httpPost(url, new RequestBody() {

                @Override
                public okhttp3.MediaType contentType() {
                    return okhttp3.MediaType.parse("application/x-www-form-urlencoded");
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    sink.write(newBody.getBytes(getDefaultEncoding()));
                }
            }, getDefaultEncoding());
        }
        return new JSONObject(s);
    }

    private String bodyToString(FormBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException {
        if (sessionId == null) {
            try {
                login(account);
            } catch (JSONException e) {
                throw new IOException(e);
            } catch (OpacErrorException e) {
                return new CancelResult(MultiStepResult.Status.ERROR, e.getMessage());
            }
        }

        try {
            JSONObject response = res(media, "2", account);
            if (response.getString("success").equals("True")) {
                return new CancelResult(MultiStepResult.Status.OK);
            } else {
                return new CancelResult(MultiStepResult.Status.ERROR, response.getString("error"));
            }
        } catch (JSONException e) {
            throw new IOException(e);
        } catch (OpacErrorException e) {
            return new CancelResult(MultiStepResult.Status.ERROR, e.getMessage());
        }
    }

    private JSONObject res(String media, String aktion, Account acc)
            throws JSONException, IOException, OpacErrorException {
        // aktion:
        // 1 = make reservation
        // 2 = cancel reservation
        // 3 = check reservation

        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("aktion", aktion);

        if (aktion.equals("2")) {
            String medid = media.split("Z")[0].replace("dat", "");
            String resid = media.split("Z")[1];
            formData.add("medid", medid);
            formData.add("resid", resid);
        } else {
            formData.add("medid", media);
            formData.add("bemerkung", "");
            formData.add("zws", "");
        }
        formData.add("biblNr", "0");
        formData.add("sessionid", sessionId);

        return httpPostAccount(opac_url + "/de/mobile/Res.ashx", formData.build(), acc);
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        if (sessionId == null) login(account);

        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("art", "7");
        formData.add("rsa", "");
        formData.add("sessionId", sessionId);
        JSONObject response =
                httpPostAccount(opac_url + "/de/mobile/Konto.ashx", formData.build(), account);

        AccountData data = new AccountData(account.getId());
        parseAccount(response, data);

        return data;
    }

    static void parseAccount(JSONObject response, AccountData data) throws JSONException {
        data.setValidUntil(ifNotEmpty(response.getString("gueltigbis")));
        data.setPendingFees(ifNotEmpty(response.getString("gebuehren")));

        DateTimeFormatter format = DateTimeFormat.forPattern("dd.MM.yyyy");
        List<LentItem> lent = new ArrayList<>();
        JSONArray ausleihen = response.getJSONArray("ausleihen");
        for (int i = 0; i < ausleihen.length(); i++) {
            LentItem item = new LentItem();
            JSONObject json = ausleihen.getJSONObject(i);
            item.setAuthor(json.getString("urheber"));
            item.setTitle(json.getString("titelkurz").replace(item.getAuthor() + " : ", ""));

            item.setCover(json.getString("imageurl"));
            item.setMediaType(getMediaType(json.getString("iconurl")));
            item.setStatus(ifNotEmpty(json.getString("hinweis")));
            item.setProlongData(json.getString("exemplarid"));

            JSONArray felder = json.getJSONArray("felder");
            for (int j = 0; j < felder.length(); j++) {
                String value = felder.getJSONObject(j).getString("display");
                if (value.startsWith("Leihfrist: ")) {
                    String dateStr = value.replace("Leihfrist: ", "");
                    item.setDeadline(format.parseLocalDate(dateStr));
                    break;
                }
            }
            lent.add(item);
        }
        data.setLent(lent);

        List<ReservedItem> reservations = new ArrayList<>();
        JSONArray reservationen = response.getJSONArray("reservationen");
        for (int i = 0; i < reservationen.length(); i++) {
            ReservedItem item = new ReservedItem();
            JSONObject json = reservationen.getJSONObject(i);
            item.setAuthor(json.getString("urheber"));
            item.setTitle(json.getString("titelkurz").replace(item.getAuthor() + " : ", ""));

            item.setCover(json.getString("imageurl"));
            item.setMediaType(getMediaType(json.getString("iconurl")));
            item.setStatus(ifNotEmpty(json.getString("hinweis")));
            if (!json.getString("abholdat").equals("")) {
                item.setExpirationDate(format.parseLocalDate(json.getString("abholdat")));
            }
            if (json.getString("status").equals("1")) {
                item.setCancelData(json.getString("exemplarid"));
            }
            reservations.add(item);
        }
        data.setLent(lent);
        data.setReservations(reservations);
    }

    private static String ifNotEmpty(String value) {
        if (value == null || value.equals("")) {
            return null;
        } else {
            return value;
        }
    }

    private void login(Account account) throws IOException, JSONException, OpacErrorException {
        String toEncrypt =
                account.getName() + "|" + account.getPassword() + "|"; // + stammbibliothek + "|"
        toEncrypt += randomString();

        JSONObject response = new JSONObject(
                httpGet(opac_url + "/de/mobile/GetRsaPublic.ashx", getDefaultEncoding()));
        BigInteger key = new BigInteger(response.getString("key"), 16);
        BigInteger modulus = new BigInteger(response.getString("modulus"), 16);

        try {
            final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic
                    (new RSAPublicKeySpec(key, modulus)));
            byte[] result = cipher.doFinal(toEncrypt.getBytes());
            String str = Base64.encodeBytes(result);

            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("art", "1");
            formData.add("rsa", str);
            response = new JSONObject(httpPost(opac_url + "/de/mobile/Konto.ashx", formData.build(),
                    getDefaultEncoding()));

            if (!response.optString("success", "0").equals("1")) {
                throw new OpacErrorException(
                        stringProvider.getString(StringProvider.WRONG_LOGIN_DATA));
            }

            this.sessionId = response.getString("sessionid");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException |
                InvalidKeyException | IllegalBlockSizeException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    private static String randomString() {
        StringBuilder a = new StringBuilder();
        for (int b = 0; b < 12; b++) {
            int c = (int) Math.floor(Math.random() * 61);
            a.append("0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz"
                    .substring(c, c + 1));
        }
        return a.toString();
    }

    @Override
    public List<SearchField> parseSearchFields() throws IOException,
            JSONException {
        List<SearchField> fields = new ArrayList<>();

        // Text fields
        String html = httpGet(opac_url + "/de/mobile/default.aspx",
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        Elements options = doc.select("#drpOptSearchT option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.attr("value"));
            field.setData(new JSONObject("{\"filter\":false}"));
            field.setHint("");
            fields.add(field);
        }

        // Dropdowns
        String text = httpGet(opac_url + "/de/mobile/GetRestrictions.ashx",
                getDefaultEncoding());
        JSONArray filters = new JSONObject(text)
                .getJSONArray("restrcontainers");
        for (int i = 0; i < filters.length(); i++) {
            JSONObject filter = filters.getJSONObject(i);
            if (filter.getString("querytyp").equals("EJ")) {
                // Querying by year also works for other years than the ones
                // listed
                // -> Make it a text field instead of a dropdown
                TextSearchField field = new TextSearchField();
                field.setDisplayName(filter.getString("kopf"));
                field.setId(filter.getString("querytyp"));
                field.setData(new JSONObject("{\"filter\":true}"));
                field.setHint("");
                fields.add(field);
            } else {
                DropdownSearchField field = new DropdownSearchField();
                field.setId(filter.getString("querytyp"));
                field.setDisplayName(filter.getString("kopf"));

                JSONArray restrictions = filter.getJSONArray("restrictions");

                field.addDropdownValue("", "Alle");

                for (int j = 0; j < restrictions.length(); j++) {
                    JSONObject restriction = restrictions.getJSONObject(j);
                    field.addDropdownValue(restriction.getString("id"),
                            restriction.getString("bez"));
                }

                field.setData(new JSONObject("{\"filter\":true}"));
                fields.add(field);
            }
        }

        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return opac_url + "/default.aspx?id=" + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT |
                SUPPORT_FLAG_WARN_PROLONG_FEES | SUPPORT_FLAG_WARN_RESERVATION_FEES |
                SUPPORT_FLAG_ACCOUNT_PROLONG_ALL;
    }

    @Override
    protected String getDefaultEncoding() {
        return "UTF-8";
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
