package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;

/**
 * API for the PICA OPAC by OCLC combined with LBS account functions Tested with LBS 4 in TU
 * Hamburg-Harburg
 *
 * @author Johan von Forstner, 30.08.2015
 */
public class PicaLBS extends Pica {
    private String lbsUrl;

    public void init(Library lib) {
        super.init(lib);
        this.lbsUrl = data.optString("lbs_url", this.opac_url);
    }

    @Override
    public ReservationResult reservation(DetailledItem item, Account account,
            int useraction, String selection) throws IOException {
        try {
            JSONArray json = new JSONArray(item.getReservation_info());
            if (json.length() != 1) {
                // TODO: This case is not implemented, don't know if it is possible with LBS
                ReservationResult res = new ReservationResult(MultiStepResult.Status.ERROR);
                res.setMessage(stringProvider.getString(StringProvider.INTERNAL_ERROR));
                return res;
            } else {
                String url = json.getJSONObject(0).getString("link");
                Document doc = Jsoup.parse(httpGet(url, getDefaultLBSEncoding()));

                if (doc.select("#opacVolumesForm").size() == 0) {
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("j_username", account.getName()));
                    params.add(new BasicNameValuePair("j_password", account.getPassword()));
                    params.add(new BasicNameValuePair("login", "Login"));
                    doc = Jsoup.parse(httpPost(url,
                            new UrlEncodedFormEntity(params), getDefaultLBSEncoding()));
                }

                if (doc.select(".error, font[color=red]").size() > 0) {
                    ReservationResult res = new ReservationResult(MultiStepResult.Status.ERROR);
                    res.setMessage(doc.select(".error, font[color=red]").text());
                    return res;
                }
                System.out.println(doc.text());
                List<Connection.KeyVal> keyVals =
                        ((FormElement) doc.select("#opacVolumesForm").first()).formData();
                List<NameValuePair> params = new ArrayList<>();
                for (Connection.KeyVal kv : keyVals) {
                    params.add(new BasicNameValuePair(kv.key(), kv.value()));
                }
                doc = Jsoup.parse(
                        httpPost(url, new UrlEncodedFormEntity(params), getDefaultEncoding()));
                if (doc.select(".error").size() > 0) {
                    ReservationResult res = new ReservationResult(MultiStepResult.Status.ERROR);
                    res.setMessage(doc.select(".error").text());
                    return res;
                } else if (doc.select(".info").text().contains("Reservation saved")
                        || doc.select(".info").text().contains("vorgemerkt")) {
                    return new ReservationResult(MultiStepResult.Status.OK);
                } else {
                    ReservationResult res = new ReservationResult(MultiStepResult.Status.ERROR);
                    res.setMessage(stringProvider.getString(StringProvider.UNKNOWN_ERROR));
                    return res;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            ReservationResult res = new ReservationResult(MultiStepResult.Status.ERROR);
            res.setMessage(stringProvider.getString(StringProvider.INTERNAL_ERROR));
            return res;
        }
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("renew", "Renew"));
        params.add(new BasicNameValuePair("_volumeNumbersToRenew", ""));
        params.add(new BasicNameValuePair("volumeNumbersToRenew", media));

        String html = httpPost(lbsUrl + "/LBS_WEB/borrower/loans.htm",
                new UrlEncodedFormEntity(params), getDefaultLBSEncoding());
        Document doc = Jsoup.parse(html);
        String message = doc.select(".alertmessage").text();
        if (message.contains("wurde verlängert") || message.contains("has been renewed")) {
            return new ProlongResult(MultiStepResult.Status.OK);
        } else {
            return new ProlongResult(MultiStepResult.Status.ERROR, message);
        }
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("cancel", "Cancel reservation"));
        params.add(new BasicNameValuePair("_volumeReservationsToCancel", ""));
        params.add(new BasicNameValuePair("volumeReservationsToCancel", media));

        String html = httpPost(lbsUrl + "/LBS_WEB/borrower/reservations.htm",
                new UrlEncodedFormEntity(params), getDefaultLBSEncoding());
        Document doc = Jsoup.parse(html);
        String message = doc.select(".alertmessage").text();
        if (message.contains("ist storniert") || message.contains("has been cancelled")) {
            return new CancelResult(MultiStepResult.Status.OK);
        } else {
            return new CancelResult(MultiStepResult.Status.ERROR, message);
        }
    }

    @Override
    public AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        if (!initialised) {
            start();
        }

        login(account);
        AccountData adata = new AccountData(account.getId());

        Document dataDoc = Jsoup.parse(
                httpGet(lbsUrl + "/LBS_WEB/borrower/borrower.htm", getDefaultLBSEncoding()));
        adata.setPendingFees(extractAccountInfo(dataDoc, "Total Costs", "Gesamtbetrag Kosten"));
        adata.setValidUntil(extractAccountInfo(dataDoc, "Expires at", "endet am"));

        Document lentDoc = Jsoup.parse(
                httpGet(lbsUrl + "/LBS_WEB/borrower/loans.htm", getDefaultLBSEncoding()));
        adata.setLent(parse_medialist(lentDoc));

        Document reservationsDoc = Jsoup.parse(
                httpGet(lbsUrl + "/LBS_WEB/borrower/reservations.htm", getDefaultLBSEncoding()));
        adata.setReservations(parse_reslist(reservationsDoc));

        return adata;
    }

    private List<Map<String, String>> parse_medialist(Document doc) {
        List<Map<String, String>> lent = new ArrayList<>();

        for (Element tr : doc.select(".resultset > tbody > tr:has(.rec_title)")) {
            Map<String, String> item = new HashMap<>();
            if (tr.select("input[name=volumeNumbersToRenew]").size() > 0) {
                item.put(AccountData.KEY_LENT_LINK,
                        tr.select("input[name=volumeNumbersToRenew]").val());
            } else {
                item.put(AccountData.KEY_LENT_RENEWABLE, "N");
            }
            String titleAuthor = extractAccountInfo(tr, "Title / Author", "Titel");
            if (titleAuthor != null) {
                String[] parts = titleAuthor.split(" / ");
                item.put(AccountData.KEY_LENT_TITLE, parts[0]);
                if (parts.length == 2) item.put(AccountData.KEY_LENT_AUTHOR, parts[1]);
            }
            String returndate = extractAccountInfo(tr, "Returndate", "ausgeliehen bis");
            Date date = parseDate(returndate);
            item.put(AccountData.KEY_LENT_DEADLINE, returndate);
            if (date != null) {
                item.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String.valueOf(date.getTime()));
            }

            StringBuilder status = new StringBuilder();

            String statusData = extractAccountInfo(tr, "Status");
            if (statusData != null) status.append(statusData);

            String prolong = extractAccountInfo(tr, "No of Renewals", "Anzahl Verlängerungen");
            if (prolong != null && !prolong.equals("0")) {
                if (status.length() > 0) status.append(", ");
                status.append(prolong).append("x ").append(stringProvider
                        .getString(StringProvider.PROLONGED_ABBR));
            }

            String reminder = extractAccountInfo(tr, "Remind.", "Mahnungen");
            if (reminder != null && !reminder.equals("0")) {
                if (status.length() > 0) status.append(", ");
                status.append(reminder).append(" ").append(stringProvider
                        .getString(StringProvider.REMINDERS));
            }

            String error = tr.select(".error").text();
            if (!error.equals("")) {
                if (status.length() > 0) status.append(", ");
                status.append(error);
            }

            item.put(AccountData.KEY_LENT_STATUS, status.toString());
            extractAccountInfo(item, AccountData.KEY_LENT_BRANCH, tr, "Counter", "Theke");
            extractAccountInfo(item, AccountData.KEY_LENT_BARCODE, tr, "Shelf mark", "Signatur");
            lent.add(item);
        }

        return lent;
    }

    private Date parseDate(String date) {
        try {
            if (date.matches("\\d\\d.\\d\\d.\\d\\d\\d\\d")) {
                return new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(date);
            } else if (date.matches("\\d\\d/\\d\\d/\\d\\d\\d\\d")) {
                return new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(date);
            } else {
                return null;
            }
        } catch (ParseException e) {
            return null;
        }
    }

    private List<Map<String, String>> parse_reslist(Document doc) {
        List<Map<String, String>> reservations = new ArrayList<>();

        for (Element tr : doc.select(".resultset > tbody > tr:has(.rec_title)")) {
            Map<String, String> item = new HashMap<>();
            if (tr.select("input[name=volumeReservationsToCancel]").size() > 0) {
                item.put(AccountData.KEY_RESERVATION_CANCEL,
                        tr.select("input[name=volumeReservationsToCancel]").val());
            }
            String titleAuthor = extractAccountInfo(tr, "Title / Author", "Titel");
            if (titleAuthor != null) {
                String[] parts = titleAuthor.split(" / ");
                item.put(AccountData.KEY_RESERVATION_TITLE, parts[0]);
                if (parts.length == 2) item.put(AccountData.KEY_RESERVATION_AUTHOR, parts[1]);
            }

            extractAccountInfo(item, AccountData.KEY_RESERVATION_BRANCH, tr, "Destination",
                    "Theke");
            // not supported: extractAccountInfo(tr, "Shelf mark", "Signatur")

            StringBuilder status = new StringBuilder();
            String numberOfReservations =
                    extractAccountInfo(tr, "Vormerkung", "Number of reservations");
            if (numberOfReservations != null) {
                status.append(stringProvider.getFormattedString(
                        StringProvider.RESERVATIONS_NUMBER, numberOfReservations));
            }

            String reservationDate = extractAccountInfo(tr, "Reservationdate", "Vormerkungsdatum");
            if (reservationDate != null) {
                if (status.length() > 0) {
                    status.append(", ");
                }
                status.append(stringProvider.getFormattedString(
                        StringProvider.RESERVED_AT_DATE, reservationDate));
            }

            if (status.length() > 0) item.put(AccountData.KEY_RESERVATION_READY, status.toString());

            // TODO: I don't know how reservations are marked that are already available
            reservations.add(item);
        }

        return reservations;
    }

    private void extractAccountInfo(Map<String, String> map, String key, Element doc,
            String... dataNames) {
        String data = extractAccountInfo(doc, dataNames);
        if (data != null && !data.equals("")) map.put(key, data);
    }

    private String extractAccountInfo(Element doc, String... dataNames) {
        StringBuilder labelSelector = new StringBuilder();
        boolean first = true;
        for (String dataName : dataNames) {
            if (first) {
                first = false;
            } else {
                labelSelector.append(", ");
            }
            labelSelector.append(".rec_data > .label:contains(").append(dataName).append(")");
        }
        if (doc.select(labelSelector.toString()).size() > 0) {
            return doc.select(labelSelector.toString()).first()
                    .parent() // td
                    .parent() // tr
                    .select("td").get(1) // second column
                    .text();
        } else {
            return null;
        }
    }

    @Override
    public void checkAccountData(Account account)
            throws IOException, JSONException, OpacErrorException {
        login(account);
    }

    private void login(Account account) throws IOException, OpacErrorException {
        // check if already logged in
        String html = httpGet(lbsUrl + "/LBS_WEB/borrower/borrower.htm",
                getDefaultLBSEncoding(), true);
        if (!html.contains("Login") && !html.equals("")) return;

        // Get JSESSIONID cookie
        httpGet(lbsUrl + "/LBS_WEB/borrower/borrower.htm?USR=1000&BES=1&LAN=" + getLang(),
                getDefaultLBSEncoding());
        List<NameValuePair> data = new ArrayList<>();
        data.add(new BasicNameValuePair("j_username", account.getName()));
        data.add(new BasicNameValuePair("j_password", account.getPassword()));
        Document doc = Jsoup.parse(httpPost(lbsUrl + "/LBS_WEB/j_spring_security_check",
                new UrlEncodedFormEntity(data), getDefaultLBSEncoding()));

        if (doc.select("font[color=red]").size() > 0) {
            throw new OpacErrorException(doc.select("font[color=red]").text());
        }
    }

    private String getDefaultLBSEncoding() {
        return "ISO-8859-1";
    }

    @Override
    public boolean isAccountSupported(Library library) {
        return library.isAccountSupported();
    }

    @Override
    public boolean isAccountExtendable() {
        return false;
    }

    @Override
    public String getAccountExtendableInfo(Account account) throws IOException {
        return null;
    }
}
