package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;

/**
 * API for the PICA OPAC by OCLC with the old default PICA account functions
 *
 * @author Johan von Forstner, 30.08.2015
 */
public class PicaOld extends Pica {

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        try {
            if (selection == null || !selection.startsWith("{")) {
                JSONArray json = new JSONArray(item.getReservation_info());
                int selectedPos;
                if (json.length() == 1) {
                    selectedPos = 0;
                } else if (selection != null) {
                    selectedPos = Integer.parseInt(selection);
                } else {
                    // a location must be selected
                    ReservationResult res = new ReservationResult(
                            MultiStepResult.Status.SELECTION_NEEDED);
                    res.setActionIdentifier(ReservationResult.ACTION_BRANCH);
                    List<Map<String, String>> selections = new ArrayList<>();
                    for (int i = 0; i < json.length(); i++) {
                        Map<String, String> selopt = new HashMap<>();
                        selopt.put("key", String.valueOf(i));
                        selopt.put("value", json.getJSONObject(i).getString("desc"));
                        selections.add(selopt);
                    }
                    res.setSelection(selections);
                    return res;
                }

                try {
                    URL link = new URL(json.getJSONObject(selectedPos).getString("link"));
                    if (!opac_url.contains(link.getHost())) {
                        ReservationResult res = new ReservationResult(
                                MultiStepResult.Status.EXTERNAL);
                        res.setMessage(link.toString());
                        return res;
                    }
                } catch (MalformedURLException e) {
                    // empty on purpose
                }

                if (json.getJSONObject(selectedPos).getBoolean("multi")) {
                    // A copy must be selected
                    String html1 = httpGet(json.getJSONObject(selectedPos)
                                               .getString("link"), getDefaultEncoding());
                    Document doc1 = Jsoup.parse(html1);

                    Elements trs = doc1
                            .select("table[summary=list of volumes header] tr:has" +
                                    "(input[type=radio])");

                    if (trs.size() > 0) {
                        List<Map<String, String>> selections = new ArrayList<>();
                        for (Element tr : trs) {
                            JSONObject values = new JSONObject();
                            for (Element input : doc1
                                    .select("input[type=hidden]")) {
                                values.put(input.attr("name"),
                                        input.attr("value"));
                            }
                            values.put(tr.select("input").attr("name"), tr
                                    .select("input").attr("value"));
                            Map<String, String> selopt = new HashMap<>();
                            selopt.put("key", values.toString());
                            selopt.put("value", tr.text());
                            selections.add(selopt);
                        }

                        ReservationResult res = new ReservationResult(
                                MultiStepResult.Status.SELECTION_NEEDED);
                        res.setActionIdentifier(ReservationResult.ACTION_USER);
                        res.setMessage(stringProvider
                                .getString(StringProvider.PICA_WHICH_COPY));
                        res.setSelection(selections);
                        return res;
                    } else {
                        ReservationResult res = new ReservationResult(
                                MultiStepResult.Status.ERROR);
                        res.setMessage(stringProvider
                                .getString(StringProvider.NO_COPY_RESERVABLE));
                        return res;
                    }

                } else {
                    String url = json.getJSONObject(selectedPos).getString("link");
                    if (!url.contains("LNG=")) {
                        url = url.replace("DB=", "LNG=" + getLang() + "/DB=");
                    }
                    String html1 = httpGet(url, getDefaultEncoding());
                    Document doc1 = Jsoup.parse(html1);

                    Map<String, String> params = new HashMap<>();

                    if (doc1.select("input[type=radio][name=CTRID]").size() > 0 &&
                            selection == null) {
                        ReservationResult res = new ReservationResult(
                                MultiStepResult.Status.SELECTION_NEEDED);
                        res.setActionIdentifier(ReservationResult.ACTION_BRANCH);
                        List<Map<String, String>> selections = new ArrayList<>();
                        for (Element input : doc1.select("input[type=radio][name=CTRID]")) {
                            Map<String, String> selopt = new HashMap<>();
                            selopt.put("key", input.attr("value"));
                            selopt.put("value", input.parent().parent().text().trim());
                            selections.add(selopt);
                        }
                        res.setSelection(selections);
                        return res;
                    } else if (useraction == 0 && doc1.select("table[summary=title data]").size() > 0) {
                        ReservationResult res = new ReservationResult(MultiStepResult.Status.CONFIRMATION_NEEDED);
                        List<String[]> details = new ArrayList<>();
                        for (Element tr : doc1.select("table[summary=title data] tr")) {
                            details.add(new String[]{
                                    tr.select("td").first().text(),
                                    tr.select("td").last().text()
                            });
                        }
                        res.setDetails(details);
                        return res;
                    } else {
                        params.put("CTRID", selection);
                    }

                    for (Element input : doc1.select("input[type=hidden]")) {
                        if (!input.attr("name").equals("CTRID") || selection == null) {
                            params.put(input.attr("name"), input.attr("value"));
                        }
                    }

                    params.put("BOR_U", account.getName());
                    params.put("BOR_PW", account.getPassword());

                    List<NameValuePair> paramlist = new ArrayList<>();
                    for (Map.Entry<String, String> param : params.entrySet()) {
                        paramlist.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                    }

                    return reservation_result(paramlist,
                            doc1.select("form").attr("action").contains("REQCONT"));
                }
            } else {
                // A copy has been selected
                JSONObject values = new JSONObject(selection);
                List<NameValuePair> params = new ArrayList<>();

                //noinspection unchecked
                Iterator<String> keys = values.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = values.getString(key);
                    params.add(new BasicNameValuePair(key, value));
                }

                params.add(new BasicNameValuePair("BOR_U", account.getName()));
                params.add(new BasicNameValuePair("BOR_PW", account
                        .getPassword()));

                return reservation_result(params, true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            ReservationResult res = new ReservationResult(MultiStepResult.Status.ERROR);
            res.setMessage(stringProvider.getString(StringProvider.INTERNAL_ERROR));
            return res;
        }
    }

    public ReservationResult reservation_result(List<NameValuePair> params,
            boolean multi) throws IOException {
        String html2 = httpPost(https_url + "/loan/DB=" + db + "/LNG="
                + getLang() + "/SET=" + searchSet + "/TTL=1/"
                + (multi ? "REQCONT" : "RESCONT"), new UrlEncodedFormEntity(
                params, getDefaultEncoding()), getDefaultEncoding());
        Document doc2 = Jsoup.parse(html2);

        String alert = doc2.select(".alert").text().trim();
        if (alert.contains("ist fuer Sie vorgemerkt")
                || alert.contains("has been reserved")) {
            return new ReservationResult(MultiStepResult.Status.OK);
        } else {
            ReservationResult res = new ReservationResult(
                    MultiStepResult.Status.ERROR);
            res.setMessage(doc2.select(".cnt .alert").text());
            return res;
        }
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String Selection) throws IOException {
        if (pwEncoded == null) {
            try {
                account(account);
            } catch (JSONException e1) {
                return new ProlongResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e1) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e1.getMessage());
            }
        }

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("ACT", "UI_RENEWLOAN"));

        params.add(new BasicNameValuePair("BOR_U", account.getName()));
        params.add(new BasicNameValuePair("BOR_PW_ENC", URLDecoder.decode(
                pwEncoded, "UTF-8")));

        params.add(new BasicNameValuePair("VB", media));

        String html = httpPost(https_url + "/loan/DB=" + db + "/LNG="
                + getLang() + "/USERINFO", new UrlEncodedFormEntity(params,
                getDefaultEncoding()), getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        if (doc.select("td.regular-text")
               .text()
               .contains(
                       "Die Leihfrist Ihrer ausgeliehenen Publikationen ist ")
                || doc.select("td.regular-text").text()
                      .contains("Ihre ausgeliehenen Publikationen sind verl")) {
            return new ProlongResult(MultiStepResult.Status.OK);
        } else if (doc.select(".cnt").text().contains("identify")) {
            try {
                account(account);
                return prolong(media, account, useraction, Selection);
            } catch (JSONException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        } else {
            ProlongResult res = new ProlongResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.select(".cnt").text());
            return res;
        }
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("ACT", "UI_CANCELRES"));

        params.add(new BasicNameValuePair("BOR_U", account.getName()));
        params.add(new BasicNameValuePair("BOR_PW_ENC", URLDecoder.decode(
                pwEncoded, "UTF-8")));
        if (lor_reservations != null) {
            params.add(new BasicNameValuePair("LOR_RESERVATIONS", lor_reservations));
        }

        params.add(new BasicNameValuePair("VB", media));

        String html = httpPost(https_url + "/loan/DB=" + db + "/LNG="
                        + getLang() + "/SET=" + searchSet + "/TTL=1/USERINFO",
                new UrlEncodedFormEntity(params, getDefaultEncoding()),
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        if (doc.select("td.regular-text").text()
               .contains("Ihre Vormerkungen sind ")) {
            return new CancelResult(MultiStepResult.Status.OK);
        } else if (doc.select(".cnt .alert").text()
                      .contains("identify yourself")) {
            try {
                account(account);
                return cancel(media, account, useraction, selection);
            } catch (JSONException e) {
                throw new OpacErrorException(
                        stringProvider.getString(StringProvider.INTERNAL_ERROR));
            }
        } else {
            CancelResult res = new CancelResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.select(".cnt").text());
            return res;
        }
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        if (!initialised) {
            start();
        }

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("ACT", "UI_DATA"));
        params.add(new BasicNameValuePair("HOST_NAME", ""));
        params.add(new BasicNameValuePair("HOST_PORT", ""));
        params.add(new BasicNameValuePair("HOST_SCRIPT", ""));
        params.add(new BasicNameValuePair("LOGIN", "KNOWNUSER"));
        params.add(new BasicNameValuePair("STATUS", "HML_OK"));

        params.add(new BasicNameValuePair("BOR_U", account.getName()));
        params.add(new BasicNameValuePair("BOR_PW", account.getPassword()));

        String html = httpPost(https_url + "/loan/DB=" + db + "/LNG="
                + getLang() + "/USERINFO", new UrlEncodedFormEntity(params,
                getDefaultEncoding()), getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        AccountData res = new AccountData(account.getId());

        if (doc.select(".cnt .alert, .cnt .error").size() > 0) {
            String text = doc.select(".cnt .alert, .cnt .error").text();
            if (doc.select("table[summary^=User data]").size() > 0) {
                res.setWarning(text);
            } else {
                throw new OpacErrorException(text);
            }
        }
        //noinspection StatementWithEmptyBody
        if (doc.select("input[name=BOR_PW_ENC]").size() > 0) {
            pwEncoded = URLEncoder.encode(doc.select("input[name=BOR_PW_ENC]")
                                             .attr("value"), "UTF-8");
        } else {
            // TODO: do something here to help fix bug #229
        }

        html = httpGet(https_url + "/loan/DB=" + db + "/LNG=" + getLang()
                + "/USERINFO?ACT=UI_LOL&BOR_U=" + account.getName()
                + "&BOR_PW_ENC=" + pwEncoded, getDefaultEncoding());
        doc = Jsoup.parse(html);

        html = httpGet(https_url + "/loan/DB=" + db + "/LNG=" + getLang()
                + "/USERINFO?ACT=UI_LOR&BOR_U=" + account.getName()
                + "&BOR_PW_ENC=" + pwEncoded, getDefaultEncoding());
        Document doc2 = Jsoup.parse(html);

        List<LentItem> media = new ArrayList<>();
        List<ReservedItem> reserved = new ArrayList<>();
        if (doc.select("table[summary^=list]").size() > 0
                && !doc.select(".alert").text().contains("Keine Entleihungen")
                && !doc.select(".alert").text().contains("No outstanding loans")
                && !doc.select(".alert").text().contains("Geen uitlening")
                && !doc.select(".alert").text().contains("Aucun emprunt")) {
            List<String> renewalCounts = loadRenewalCounts(doc);
            parseMediaList(media, doc, stringProvider, renewalCounts);
        }
        if (doc2.select("table[summary^=list]").size() > 0
                && !doc2.select(".alert").text().contains("Keine Vormerkungen")
                && !doc2.select(".alert").text().contains("No outstanding reservations")
                && !doc2.select(".alert").text().contains("Geen reservering")
                && !doc2.select(".alert").text().contains("Aucune réservation")) {
            updateLorReservations(doc);
            parseResList(reserved, doc2, stringProvider);
        }

        res.setLent(media);
        res.setReservations(reserved);

        return res;

    }

    private List<String> loadRenewalCounts(Document doc) {
        List<String> renewalCounts = new ArrayList<>();
        for (Element iframe : doc.select("iframe[name=nr_renewals_in_a_box]")) {
            try {
                String html = httpGet(iframe.attr("src"), getDefaultEncoding());
                renewalCounts.add(Jsoup.parse(html).text());
            } catch (IOException e) {
                renewalCounts.add(null);
            }
        }
        return renewalCounts;
    }

    static void parseMediaList(List<LentItem> media, Document doc,
            StringProvider stringProvider, List<String> renewalCounts) throws OpacErrorException {

        Elements copytrs = doc.select("table[summary^=list] > tbody > tr[valign=top]");

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-yyyy").withLocale(Locale.GERMAN);

        int trs = copytrs.size();
        if (trs < 1) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
        }
        assert (trs > 0);
        for (int i = 0; i < trs; i++) {
            Element tr = copytrs.get(i);
            if (tr.select("table[summary=title data]").size() > 0) {
                // According to HTML code from Bug reports (Server TU Darmstadt,
                // Berlin Ibero-Amerikanisches Institut)
                LentItem item = new LentItem();
                // Check if there is a checkbox to prolong this item
                if (tr.select("input").size() > 0) {
                    item.setProlongData(tr.select("input").attr("value"));
                } else {
                    item.setRenewable(false);
                }

                Elements datatrs = tr.select("table[summary=title data] tr");
                item.setTitle(datatrs.get(0).text());

                String reservations = null;

                for (Element td : datatrs.get(1).select("td")) {
                    List<TextNode> textNodes = td.textNodes();
                    Elements titles = td.select("span.label-small");

                    List<String> values = new ArrayList<>();
                    if (td.select("span[name=xxxxx]").size() > 0) {
                        for (Element span : td.select("span[name=xxxxx]")) {
                            values.add(span.text());
                        }
                    } else {
                        for (TextNode node : textNodes) {
                            if (!node.text().equals(" ")) {
                                values.add(node.text());
                            }
                        }
                    }

                    assert (values.size() == titles.size());
                    for (int j = 0; j < values.size(); j++) {
                        String title = titles.get(j).text();
                        String value = values.get(j).trim().replace(";", "");
                        //noinspection StatementWithEmptyBody
                        if (title.contains("Signatur")
                                || title.contains("shelf mark")
                                || title.contains("signatuur")) {
                            // not supported
                        } else if (title.contains("Status")
                                || title.contains("status")
                                || title.contains("statut")) {
                            item.setStatus(value);
                        } else if (title.contains("Leihfristende")
                                || title.contains("expiry date")
                                || title.contains("vervaldatum")
                                || title.contains("date d'expiration")) {
                            try {
                                item.setDeadline(fmt.parseLocalDate(value));
                            } catch (IllegalArgumentException e1) {
                                e1.printStackTrace();
                            }
                        } else //noinspection StatementWithEmptyBody
                            if (title.contains("Vormerkungen")
                                    || title.contains("reservations")
                                    || title.contains("reserveringen")
                                    || title.contains("réservations")) {
                                reservations = value;
                            }
                    }
                }
            media.add(item);
            } else { // like in Kiel
                String prolongCount = "";
                if (renewalCounts.size() == trs && renewalCounts.get(i) != null) {
                    prolongCount = renewalCounts.get(i);
                }
                String reminderCount = tr.child(13).text().trim();
                if (reminderCount.contains(" Mahn")
                        && reminderCount.contains("(")
                        && reminderCount.indexOf("(") < reminderCount
                        .indexOf(" Mahn")) {
                    reminderCount = reminderCount.substring(
                            reminderCount.indexOf("(") + 1,
                            reminderCount.indexOf(" Mahn"));
                } else {
                    reminderCount = "";
                }
                LentItem item = new LentItem();

                if (tr.child(4).text().trim().length() < 5
                        && tr.child(5).text().trim().length() > 4) {
                    item.setTitle(tr.child(5).text().trim());
                } else {
                    item.setTitle(tr.child(4).text().trim());
                }
                String status = tr.child(13).text().trim();
                if (!reminderCount.equals("0") && !reminderCount.equals("")) {
                    if (!status.equals("")) status += ", ";
                    status += reminderCount + " " + stringProvider
                            .getString(StringProvider.REMINDERS) + ", ";
                }
                if (!"".equals(prolongCount)) {
                    if (!status.equals("")) status += ", ";
                    status += prolongCount + "x " + stringProvider
                            .getString(StringProvider.PROLONGED_ABBR);
                }
                if (tr.children().size() >= 26 && !"".equals(tr.child(25).text().trim())) {
                    if (!status.equals("")) status += ", ";
                    try {
                        status +=
                                stringProvider.getQuantityString(StringProvider.RESERVATIONS_NUMBER,
                                        Integer.parseInt(tr.child(25).text().trim()),
                                        Integer.parseInt(tr.child(25).text().trim()));
                    } catch (NumberFormatException e) {
                        // pass
                    }
                }
                // + tr.child(25).text().trim() + " Vormerkungen");
                item.setStatus(status);
                try {
                    item.setDeadline(fmt.parseLocalDate(tr.child(21).text().trim()));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                if (tr.child(1).select("input").size() > 0) {
                    // If there is no checkbox, the medium is not renewable
                    item.setProlongData(tr.child(1).select("input").attr("value"));
                }

                media.add(item);
            }
        }
        assert (media.size() == trs);
    }

    static void parseResList(List<ReservedItem> media, Document doc,
            StringProvider stringProvider) throws OpacErrorException {

        Elements copytrs = doc.select("table[summary^=list] > tbody >  tr[valign=top]");

        int trs = copytrs.size();
        if (trs < 1) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
        }
        assert (trs > 0);
        for (Element tr : copytrs) {
            ReservedItem item = new ReservedItem();
            if (tr.select("table[summary=title data]").size() > 0) {
                // According to HTML code from Bug report (UB Frankfurt)

                // Check if there is a checkbox to cancel this item
                if (tr.select("input").size() > 0) {
                    item.setCancelData(tr.select("input").attr("value"));
                }

                Elements datatrs = tr.select("table[summary=title data] tr");
                item.setTitle(datatrs.get(0).text());

                List<TextNode> textNodes = datatrs.get(1).select("td").first()
                                                  .textNodes();
                List<TextNode> nodes = new ArrayList<>();
                Elements titles = datatrs.get(1).select("span.label-small");

                for (TextNode node : textNodes) {
                    if (!node.text().equals(" ")) {
                        nodes.add(node);
                    }
                }

                assert (nodes.size() == titles.size());
                for (int j = 0; j < nodes.size(); j++) {
                    String title = titles.get(j).text();
                    String value = nodes.get(j).text().trim().replace(";", "");
                    //noinspection StatementWithEmptyBody
                    if (title.contains("Signatur")
                            || title.contains("shelf mark")
                            || title.contains("signatuur")) {
                        // not supported
                    } else //noinspection StatementWithEmptyBody
                        if (title.contains("Vormerkdatum")) {
                            // not supported
                        }
                }
            } else {
                // like in Kiel
                item.setTitle(tr.child(5).text().trim());
                item.setStatus(tr.child(17).text().trim());
                item.setCancelData(tr.child(1).select("input").attr("value"));
            }

            media.add(item);
        }
        assert (media.size() == trs);
    }

    private void updateLorReservations(Document doc) {
        if (doc.select("input[name=LOR_RESERVATIONS]").size() > 0) {
            lor_reservations = doc.select("input[name=LOR_RESERVATIONS]").attr("value");
        }
    }


    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("ACT", "UI_DATA"));
        params.add(new BasicNameValuePair("HOST_NAME", ""));
        params.add(new BasicNameValuePair("HOST_PORT", ""));
        params.add(new BasicNameValuePair("HOST_SCRIPT", ""));
        params.add(new BasicNameValuePair("LOGIN", "KNOWNUSER"));
        params.add(new BasicNameValuePair("STATUS", "HML_OK"));

        params.add(new BasicNameValuePair("BOR_U", account.getName()));
        params.add(new BasicNameValuePair("BOR_PW", account.getPassword()));

        String html = httpPost(https_url + "/loan/DB=" + db + "/LNG="
                + getLang() + "/USERINFO", new UrlEncodedFormEntity(params,
                getDefaultEncoding()), getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        if (doc.select(".cnt .alert, .cnt .error").size() > 0) {
            String text = doc.select(".cnt .alert, .cnt .error").text();
            if (doc.select("table[summary^=User data]").size() == 0) {
                throw new OpacErrorException(text);
            }
        }
    }

}
