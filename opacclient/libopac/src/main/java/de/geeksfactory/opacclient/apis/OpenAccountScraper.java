package de.geeksfactory.opacclient.apis;

import org.apache.http.client.utils.URIBuilder;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;
import java8.util.concurrent.CompletableFuture;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class OpenAccountScraper extends OpenSearch {

    @Override
    protected DetailedItem parse_result(Document doc) {
        DetailedItem item = super.parse_result(doc);

        if (doc.select("a[id*=BtnReserve]:not(.aspNetDisabled)").size() > 0) {
            item.setReservable(true);
            item.setReservation_info(item.getId());
        }
        return item;
    }

    private Document reservationDoc;
    private static final int BASE_ACTION = 10;
    private static final int ACTION_COPY = BASE_ACTION;
    private static final int ACTION_PICKUP_BRANCH = BASE_ACTION + 1;

    @Override
    public ReservationResult reservation(DetailedItem item, Account account, int useraction,
            String selection) throws IOException {
        switch (useraction) {
            case 0:
                try {
                    login(account);
                } catch (OpacErrorException e) {
                    return new ReservationResult(MultiStepResult.Status.ERROR, e.getMessage());
                }

                reservationDoc = null;

                Document doc;
                try {
                    String html =
                            httpGet(opac_url + "/" + data.getJSONObject("urls").getString(
                                    "simple_search") +
                                    NO_MOBILE + "&id=" + item.getId(), getDefaultEncoding());
                    doc = reservationDoc = Jsoup.parse(html);
                    doc.setBaseUri(opac_url);
                } catch (JSONException e) {
                    throw new IOException(e.getMessage());
                }

                // Check if there is a button to select the pickup branch, e.g. in Wolfsburg
                if (doc.select("a[id*=SelectPickupBranch]").size() > 0) {
                    return resPickupBranchSelection(doc);
                } else {
                    // otherwise, continue with copy selection
                    return resCopySelection(item, account, doc);
                }
            case ACTION_PICKUP_BRANCH:
                // pickup branch was chosen by the user, select it and continue with copy selection
                resSelectPickupBranch(selection, item);
                return resCopySelection(item, account, reservationDoc);
            case ACTION_COPY:
                // copy was chosen by the user, select it and check confirmation
                return resCheckConfirmation(item, account, selection);
            case MultiStepResult.ACTION_CONFIRMATION:
                return resConfirm();
            default:
                return null;
        }
    }

    private ReservationResult resConfirm() throws IOException {
        FormElement form = (FormElement) reservationDoc.select("form").first();
        MultipartBody data = formData(
                form,
                reservationDoc.select("input[name$=BtnConfirm]").first().attr("name")
        ).build();

        String postUrl = form.attr("abs:action");
        reservationDoc.setBaseUri(opac_url);
        String html = httpPost(postUrl, data, "UTF-8");
        reservationDoc = Jsoup.parse(html);
        reservationDoc.setBaseUri(postUrl);

        ReservationResult res;
        if (reservationDoc.select("span[id$=LblReservationCheckResultValue]").size() > 0) {
            res = new ReservationResult(MultiStepResult.Status.OK,
                    reservationDoc.select(
                            "span[id$=LblReservationCheckResultValue]").text().trim());
        } else {
            res = new ReservationResult(MultiStepResult.Status.OK);
        }
        reservationDoc = null;
        return res;
    }

    private ReservationResult resCheckConfirmation(DetailedItem item, Account account,
            String selection) throws IOException {
        Pattern pattern = Pattern.compile("javascript:__doPostBack\\('([^,]*)','([^\\)]*)'\\)");
        Matcher matcher = pattern.matcher(selection);
        if (!matcher.find()) {
            return new ReservationResult(
                    MultiStepResult.Status.ERROR,
                    stringProvider.getString(StringProvider.NO_COPY_RESERVABLE));
        }

        FormElement form = (FormElement) reservationDoc.select("form").first();
        MultipartBody data = formData(form, null).addFormDataPart("__EVENTTARGET", matcher.group(1))
                                                 .addFormDataPart("__EVENTARGUMENT",
                                                         matcher.group(2))
                                                 .build();
        String postUrl = form.attr("abs:action");
        reservationDoc.setBaseUri(opac_url);
        String html = httpPost(postUrl, data, "UTF-8");
        reservationDoc = Jsoup.parse(html);
        reservationDoc.setBaseUri(postUrl);

        if (reservationDoc.select("[id$=LblReservationFeeValue]").size() > 0) {
            ReservationResult res = new ReservationResult(
                    MultiStepResult.Status.CONFIRMATION_NEEDED);
            res.setActionIdentifier(MultiStepResult.ACTION_CONFIRMATION);
            String[] detail = new String[]{
                    reservationDoc.select(
                            "[id$=LblReservationFeeValue]").first().parent().text().trim()
            };
            List<String[]> details = new ArrayList<>();
            details.add(detail);
            res.setDetails(details);
            return res;
        } else if (reservationDoc.select("input[name$=BtnConfirm]").size() == 0) {
            ReservationResult res = new ReservationResult(MultiStepResult.Status.ERROR,
                    "Dieses Medium kann im Moment nicht vorbestellt werden. " +
                            "Dies kann entweder ein Fehler bei der Bibliothek sein oder " +
                            "z.B. daran liegen, dass es verfügbare Exemplare dieses Mediums " +
                            "gibt.");
            return res;
        } else {
            // TODO: Bibliotheken ohne Gebührenwarnung?
            return reservation(item, account, MultiStepResult.ACTION_CONFIRMATION, null);
        }
    }

    private ReservationResult resCopySelection(DetailedItem item, Account account,
            Document doc) throws IOException {
        // Now check for a selection of copies, copy-based reservation in e.g. Bern
        List<Map<String, String>> options = new ArrayList<>();
        Element table = doc.select("table[id$=grdViewMediumCopies]").first();
        Elements trs = table.select("tr");

        List<String> columnmap = new ArrayList<>();
        for (Element th : trs.first().select("th")) {
            columnmap.add(getCopyColumnKey(th.text()));
        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(
                Locale.GERMAN);
        for (int i = 1; i < trs.size(); i++) {
            Element tr = trs.get(i);
            if (tr.select("a[id*=BtnReserve]").size() > 0) {
                Element link = tr.select("a[id*=BtnReserve]").first();
                if (link.attr("href").trim().length() == 0) {
                    continue;
                }
                Map<String, String> map = new HashMap<>();
                map.put("key", link.attr("href"));

                Copy copy = new Copy();
                for (int j = 0; j < tr.children().size(); j++) {
                    if (columnmap.get(j) == null) continue;
                    tr.select(".oclc-module-label").remove();
                    String text = tr.children().get(j).text().replace("\u00a0", "");
                    if (text.equals("")) continue;
                    try {
                        copy.set(columnmap.get(j), text, fmt);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (copy.getLocation().equals("Bestsellerregal") && library.getIdent().equals("Wien")) {
                    continue;
                }
                map.put("value",
                        copy.getBranch() + " - " + (copy.getReturnDate() != null ? fmt.print(
                                copy.getReturnDate()) : "unbekannt"));
                options.add(map);
            }
        }

        if (options.size() == 0 && doc.select("a[id*=BtnReserve]").size() > 0) {
            // Title-based reservation e.g. in Erlangen and Wolfsburg
            Map<String, String> map = new HashMap<>();
            map.put("key", doc.select("a[id*=BtnReserve]").attr("href"));
            options.add(map);
        }

        if (options.size() == 0) {
            return new ReservationResult(MultiStepResult.Status.ERROR,
                    "Es ist kein vormerkbares Exemplar vorhanden.");
        } else if (options.size() == 1) {
            return reservation(item, account, ACTION_COPY, options.get(0).get("key"));
        } else {
            ReservationResult res = new ReservationResult(
                    MultiStepResult.Status.SELECTION_NEEDED);
            res.setActionIdentifier(ACTION_COPY);
            res.setMessage("Bitte Exemplar auswählen");
            res.setSelection(options);
            return res;
        }
    }

    private void resSelectPickupBranch(String selection, DetailedItem item) throws IOException {
        Element select = reservationDoc.select(
                "select[id$=DdlPickupBranches], select[id$=DdlPickupBranchesMediumBased]").first();
        for (Element opt : select.select("option")) {
            if (selection.equals(opt.val())) {
                opt.attr("selected", "selected");
            } else {
                opt.removeAttr("selected");
            }
        }

        FormElement form = (FormElement) reservationDoc.select("form").first();
        MultipartBody data = formData(form, "popupSelectPickupBranch$btnDefault").build();
        String postUrl = form.attr("abs:action");
        reservationDoc.setBaseUri(opac_url);
        String html = httpPost(postUrl, data, "UTF-8");
        reservationDoc = Jsoup.parse(html);
        reservationDoc.setBaseUri(postUrl);

        if (reservationDoc.select("table[id$=grdViewMediumCopies]").size() == 0) {
            // strange bug in Mannheim: we are catapulted back to the search page.
            // pickup branch has still been selected. So we just go back to the detail page.
            try {
                html = httpGet(opac_url + "/" +
                        this.data.getJSONObject("urls").getString("simple_search") + NO_MOBILE +
                        "&id=" + item.getId(), getDefaultEncoding());
                reservationDoc = Jsoup.parse(html);
                reservationDoc.setBaseUri(opac_url);
            } catch (JSONException ignored) {
            }
        }
    }

    private ReservationResult resPickupBranchSelection(Document doc) throws IOException {
        Pattern pattern = Pattern.compile(
                "javascript:__doPostBack\\('([^,]*)','([^\\)]*)'\\)");
        Matcher matcher = pattern.matcher(
                doc.select("a[id*=SelectPickupBranch]").attr("href"));
        if (!matcher.find()) {
            return new ReservationResult(MultiStepResult.Status.ERROR,
                    stringProvider.getString(StringProvider.INTERNAL_ERROR));
        }

        FormElement form = (FormElement) reservationDoc.select("form").first();
        MultipartBody data = formData(form, null).addFormDataPart("__EVENTTARGET",
                matcher.group(1))
                                                 .addFormDataPart("__EVENTARGUMENT",
                                                         matcher.group(2))
                                                 .build();
        String postUrl = form.attr("abs:action");
        reservationDoc.setBaseUri(opac_url);
        String html = httpPost(postUrl, data, "UTF-8");
        reservationDoc = Jsoup.parse(html);
        reservationDoc.setBaseUri(postUrl);

        List<Map<String, String>> options = new ArrayList<>();
        Element select = reservationDoc.select(
                "select[id$=DdlPickupBranchesMediumBased], select[id$=DdlPickupBranches]").first();
        for (Element option : select.children()) {
            Map<String, String> map = new HashMap<>();
            map.put("key", option.attr("value"));
            map.put("value", option.text());
            options.add(map);
        }

        ReservationResult res = new ReservationResult(
                MultiStepResult.Status.SELECTION_NEEDED);
        res.setActionIdentifier(ACTION_PICKUP_BRANCH);
        res.setMessage("Bitte Abholzweigstelle auswählen");
        res.setSelection(options);
        return res;
    }

    private Document prolongDoc;

    @Override
    public ProlongResult prolong(String media, Account account, int useraction, String selection)
            throws IOException {
        if (media.startsWith("fail:")) {
            return new ProlongResult(MultiStepResult.Status.ERROR, media.substring(5));
        }

        if (prolongDoc != null && useraction == MultiStepResult.ACTION_CONFIRMATION) {
            FormElement form = (FormElement) prolongDoc.select("form").first();
            MultipartBody data = handleProlongConfirmation(prolongDoc, form);
            if (data == null) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        stringProvider.getString(StringProvider.INTERNAL_ERROR));
            }

            String postUrl = form.attr("abs:action");
            String html = httpPost(postUrl, data, "UTF-8");
            Document doc = Jsoup.parse(html);
            prolongDoc = null;
            String message = doc.select(
                    "[id$=ucExtensionFailedMessagePopupView_LblPopupMessage], " +
                            "[id$=messagePopup_lblMessage]")
                                .text().trim();
            if (message.length() > 1 &&
                    !message.equals("Ihre Verlängerung wurde erfolgreich durchgeführt.") &&
                    !message.equals("Ihre Verlängerung wurde durchgeführt.")) {
                return new ProlongResult(MultiStepResult.Status.ERROR, message);
            } else {
                return new ProlongResult(MultiStepResult.Status.OK);
            }
        } else {
            Document doc = null;
            try {
                doc = login(account);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR, e.getMessage());
            }

            boolean found = false;
            FormElement form = (FormElement) doc.select("form").first();
            MultipartBody.Builder databuilder =
                    formData(form, doc.select("[name$=BtnExtendMediumsBottom]").attr("name"));

            // Find correct checkbox
            Element pendingTable = doc.select("[id$=tpnlLoans_ucLoansView_grdViewLoans]").first();
            if (pendingTable.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]")
                            .size() == 0) {
                for (Element row : pendingTable.select("tr")) {
                    if (row.select("th").size() > 0) {
                        continue;
                    }

                    if (row.select("input[name$=CopyIdChx]").size() > 0) {
                        if (row.select("input[name$=CopyIdChx]").val().equals(media)) {
                            found = true;
                            databuilder.addFormDataPart(
                                    row.select("input[name*=chkSelect]").attr("name"), "on");
                            break;
                        }
                    }
                }
            }
            if (!found) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        "Das Medium wurde nicht gefunden.");
            }
            MultipartBody data = databuilder.build();
            String postUrl = form.attr("abs:action");
            String html = httpPost(postUrl, data, "UTF-8");
            doc = Jsoup.parse(html);
            doc.setBaseUri(postUrl);

            ProlongResult res = new ProlongResult(MultiStepResult.Status.CONFIRMATION_NEEDED);
            List<String[]> details = new ArrayList<>();
            if (doc.select(
                    "#extendCatalogueCopiesPopupRegion, span[id$=loansExtensionPopup_lblHeader]")
                   .size() > 0) {
                Element headtr = null;
                for (Element tr : doc
                        .select("[id$=ucExtendCatalogueCopiesPopupView_grdViewLoans] tr, " +
                                "[id$=loansExtensionPopup_ucLoansExtension_grdViewLoans] tr")) {
                    if (tr.select("th").size() > 0) {
                        headtr = tr;
                        continue;
                    }
                    tr.select(".oclc-module-view-small").remove();
                    // title
                    details.add(new String[]{
                            headtr.child(1).text().trim(),
                            tr.child(1).text().trim(),
                    });
                    // current deadline
                    details.add(new String[]{
                            headtr.child(4).text().trim(),
                            tr.child(4).text().trim(),
                    });
                    // new deadline
                    details.add(new String[]{
                            headtr.child(5).text().trim(),
                            tr.child(5).text().trim()
                    });
                    if (headtr.children().size() > 6) {
                        // fees
                        details.add(new String[]{
                                headtr.child(6).text().trim(),
                                tr.child(6).text().trim()
                        });
                    }
                }
            }
            res.setDetails(details);
            prolongDoc = doc;
            return res;
        }
    }

    private static MultipartBody handleProlongConfirmation(Document doc, FormElement form) {
        MultipartBody data = null;
        for (Element scripttag : doc.select("script")) {
            String scr = scripttag.html();
            if (scr.contains("DivExtendCatalogueCopiesPopupControl")) {
                Pattern pattern = Pattern.compile(".*__doPostBack\\('([^,]*)','([^\\)]*)'\\).*",
                        Pattern.DOTALL);
                Matcher matcher = pattern.matcher(scr);
                if (!matcher.find()) {
                    return null;
                }

                data = formData(form, null).addFormDataPart("__EVENTTARGET", matcher.group(1))
                                           .addFormDataPart("__EVENTARGUMENT", matcher.group(2))
                                           .build();
            }
        }
        if (data == null) {
            // Bern
            if (form.select("input[id$=loansExtensionPopup_btnDefault]").size() > 0) {
                data = formData(form, null)
                        .addFormDataPart(form.select("input[id$=loansExtensionPopup_btnDefault]")
                                             .attr("name"),
                                form.select("input[id$=loansExtensionPopup_btnDefault]")
                                    .attr("value"))
                        .build();
            }
        }
        return data;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        if (prolongDoc != null && useraction == MultiStepResult.ACTION_CONFIRMATION) {
            FormElement form = (FormElement) prolongDoc.select("form").first();
            MultipartBody data = handleProlongConfirmation(prolongDoc, form);

            String postUrl = form.attr("abs:action");
            String html = httpPost(postUrl, data, "UTF-8");
            Document doc = Jsoup.parse(html);
            prolongDoc = null;
            if (doc.select(
                    "[id$=ucExtensionFailedMessagePopupView_LblPopupMessage], " +
                            "[id$=messagePopup_lblMessage]")
                   .size() > 0) {
                List<Map<String, String>> details = new ArrayList<>();
                Map<String, String> det = new HashMap<>();
                det.put(ProlongAllResult.KEY_LINE_MESSAGE, doc.select(
                        "[id$=ucExtensionFailedMessagePopupView_LblPopupMessage], " +
                                "[id$=messagePopup_lblMessage]")
                                                              .text().trim());
                details.add(det);
                return new ProlongAllResult(MultiStepResult.Status.OK, details);
            } else {
                return new ProlongAllResult(MultiStepResult.Status.ERROR);
            }
        } else {
            Document doc = null;
            try {
                doc = login(account);
            } catch (OpacErrorException e) {
                return new ProlongAllResult(MultiStepResult.Status.ERROR, e.getMessage());
            }

            FormElement form = (FormElement) doc.select("form").first();
            MultipartBody.Builder databuilder =
                    formData(form, doc.select("[name$=BtnExtendMediumsBottom]").attr("name"));

            // Find correct checkbox
            Element pendingTable = doc.select("[id$=tpnlLoans_ucLoansView_grdViewLoans]").first();
            if (pendingTable.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]")
                            .size() == 0) {
                for (Element row : pendingTable.select("tr")) {
                    if (row.select("th").size() > 0) {
                        continue;
                    }

                    if (row.select("input[name$=CopyIdChx]").size() > 0) {
                        databuilder
                                .addFormDataPart(row.select("input[name*=chkSelect]").attr("name"),
                                        "on");
                    }
                }
            }
            MultipartBody data = databuilder.build();
            String postUrl = form.attr("abs:action");
            String html = httpPost(postUrl, data, "UTF-8");
            doc = Jsoup.parse(html);
            doc.setBaseUri(postUrl);

            ProlongAllResult res = new ProlongAllResult(MultiStepResult.Status.CONFIRMATION_NEEDED);
            List<String[]> details = new ArrayList<>();
            if (doc.select("#extendCatalogueCopiesPopupRegion").size() > 0) {
                details.add(new String[]{
                        doc.select("[id$=ucExtendCatalogueCopiesPopupView_DivFeeDisplay]")
                           .text().trim()
                });
            } else if (doc.select("[id$=loansExtensionPopup_ucLoansExtension_LblFeeTotalData]")
                          .size() > 0) {
                details.add(new String[]{
                        doc.select("[id$=loansExtensionPopup_ucLoansExtension_LblFeeTotalTitle], " +
                                "[id$=loansExtensionPopup_ucLoansExtension_LblFeeTotalData]")
                           .text().trim()
                });
            }
            res.setDetails(details);
            prolongDoc = doc;
            return res;
        }
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction, String selection)
            throws IOException, OpacErrorException {
        Document doc = login(account);

        // Find correct checkbox
        Element pendingTable =
                doc.select("[id$=tpnlReservations_ucReservationsView_grdViewReservations]").first();
        if (pendingTable.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]").size() ==
                0) {
            for (Element row : pendingTable.select("tr")) {
                Elements cols = row.children();

                if (row.select("th").size() > 0) {
                    continue;
                }

                if (cols.get(2).select("a").size() > 0) {
                    Map<String, String> params =
                            getQueryParamsFirst(cols.get(2).select("a").first().absUrl("href"));
                    if (params.get("id").trim().equals(media)) {
                        if (row.select("input[name*=chkSelect]").size() > 0) {
                            FormElement form = (FormElement) doc.select("form").first();
                            MultipartBody data = formData(form,
                                    doc.select("[name$=BtnCancelReservationsBottom]").attr("name"))
                                    .addFormDataPart(
                                            row.select("input[name*=chkSelect]").attr("name"), "on")
                                    .build();
                            String postUrl = form.attr("abs:action");
                            httpPost(postUrl, data, "UTF-8");
                            return new CancelResult(MultiStepResult.Status.OK);
                        }
                    }
                }
            }
        }
        throw new OpacErrorException("Die Vormerkung wurde nicht gefunden.");
    }

    public String httpPost(String url, RequestBody data,
            String encoding, boolean ignore_errors)
            throws IOException {
        // Strict transport security, e.g. for Erlangen
        if (opac_url.startsWith("https://") && url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        return super.httpPost(url, data, encoding, ignore_errors);
    }

    public String httpGet(String url, String encoding, boolean ignore_errors)
            throws IOException {
        // Strict transport security, e.g. for Erlangen
        if (opac_url.startsWith("https://") && url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        return super.httpGet(url, encoding, ignore_errors);
    }

    protected Document login(Account account) throws IOException, OpacErrorException {
        String accountUrl;
        try {
            accountUrl = "/" + data.getJSONObject("urls").getString("account");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Document doc = Jsoup.parse(httpGet(opac_url + accountUrl, getDefaultEncoding()));
        doc.setBaseUri(opac_url + accountUrl);
        FormElement form = (FormElement) doc.select("form").first();

        form.select("input[name$=txtUsername]").val(account.getName());
        form.select("input[name$=txtPassword]").val(account.getPassword());
        String loginBtnName = doc.select("input[name$=cmdLogin]").attr("name");

        if (!loginBtnName.equals("")) {
            MultipartBody data = formData(form, loginBtnName).build();
            String postUrl = form.attr("abs:action");

            String html = httpPost(postUrl, data, "UTF-8");
            doc = Jsoup.parse(html);
            doc.setBaseUri(postUrl);
        }

        if (doc.select(".dnnFormValidationSummary").size() > 0) {
            throw new OpacErrorException(doc.select(".dnnFormValidationSummary").text().trim());
        }

        if (doc.select("[id$=tpnlReservations_ucReservationsView_grdViewReservations]").first()
                != null) {
            return doc;
        } else {
            // sometimes (-> Verden), we are redirected to the home page, not to the account page.
            doc = Jsoup.parse(httpGet(opac_url + accountUrl, getDefaultEncoding()));
            doc.setBaseUri(opac_url + accountUrl);
            if (doc.select("[id$=tpnlReservations_ucReservationsView_grdViewReservations]").first()
                    != null) {
                return doc;
            } else {
                // bug in Wien, sometimes login does not work and we just get the login page back,
                // without an error message
                throw new OpacErrorException(stringProvider.getString(StringProvider.COULD_NOT_LOAD_ACCOUNT));
            }
        }
    }

    @Override
    public AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        Document doc = login(account);
        AccountData data = new AccountData(account.getId());

        parse_lent(data, doc, account);
        parse_reservations(data, doc);

        String fees = doc.select("[id$=tpnlFees_ucFeesView_lblTotalSaldoData]").text().trim();
        // Flip sign
        if (fees.startsWith("-")) {
            fees = fees.substring(1);
        } else if (!fees.contains("-") && fees.matches(".*[1-9].*")) {
            fees = "-" + fees;
        }
        data.setPendingFees(fees);
        data.setValidUntil(
                doc.select("[id$=ucPatronAccountView_LblMembershipValidUntilData]").text().trim());

        Elements warnings =
                doc.select(".dnnFormWarning:not([style*=display: none] .dnnFormWarning)");
        if (warnings.size() > 0) {
            data.setWarning(warnings.text().trim());
        } else if (doc.select("[id$=patronAccountExtensionMessage]").size() > 0) {
            data.setWarning(
                    doc.select("[id$=patronAccountExtensionMessage]").first().text().trim());
        }

        return data;
    }

    void parse_reservations(AccountData data, Document doc) {
        List<ReservedItem> res = new ArrayList<>();
        data.setReservations(res);

        Element pendingTable =
                doc.select("[id$=tpnlReservations_ucReservationsView_grdViewReservations]").first();
        if (pendingTable.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]").size() ==
                0) {
            parseReservationsTable(res, pendingTable);
        }

        // Ready

        Element readyTable =
                doc.select("[id$=tpnlReservations_ucReservationsView_grdViewReadyForPickups]")
                   .first();
        if (readyTable.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]")
                      .size() == 0) {
            parseReadyTable(res, readyTable);
        }

        // ready interlibrary loans (seen in Bielefeld, untested)
        Element interlibraryTable =
                doc.select("[id$=tpnlRemoteLoans_ucRemoteLoansView_grdViewRemoteReadyForPickups]")
                   .first();
        if (interlibraryTable != null &&
                interlibraryTable.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]")
                                 .size() == 0) {
            parseReadyTable(res, interlibraryTable);
        }
    }

    private void parseReservationsTable(List<ReservedItem> res, Element pendingTable) {
        List<CompletableFuture> futures = new ArrayList<>();
        for (Element row : pendingTable.select("tr")) {
            if (row.select("th").size() > 0) {
                continue;
            }
            int offset = 0;

            Elements cols = row.children();
            if (row.select("input[name*=chkSelect]").size() == 0 && cols.size() == 4) {
                // e.g. Verden, cancelling not possible
                offset = -1;
            }
            ReservedItem item = new ReservedItem();

            if (cols.get(1 + offset).select("img").size() > 0) {
                futures.add(assignBestCover(item, getCoverUrlList(
                        cols.get(1 + offset).select("img[id*=coverView]").first())));
            }

            item.setTitle(cols.get(2 + offset).text().trim());
            if (cols.get(2 + offset).select("a").size() > 0) {
                Map<String, String> params = getQueryParamsFirst(
                        cols.get(2 + offset).select("a").first().absUrl("href"));
                item.setId(params.get("id"));
            }
            item.setAuthor(cols.get(3 + offset).text().trim());
            item.setFormat(cols.get(4 + offset).text().trim());

            if (row.select("input[name*=chkSelect]").size() > 0) {
                item.setCancelData(item.getId());
            }

            res.add(item);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void parseReadyTable(List<ReservedItem> res, Element readyTable) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy");
        List<CompletableFuture> futures = new ArrayList<>();

        Map<String, Integer> colmap = new HashMap<>();
        for (Element row : readyTable.select("tr")) {
            if (row.select("th").size() > 0) {
                int i = 0;
                for (Element th : row.select("th")) {
                    String th_html = th.html();
                    if (th_html.contains("Sort$Title")) {
                        colmap.put("title", i);
                    } else if (th_html.contains("Sort$Author")) {
                        colmap.put("author", i);
                    } else if (th_html.contains("Sort$MediaGroup")) {
                        colmap.put("format", i);
                    } else if (th_html.contains("Sort$DueDate")) {
                        colmap.put("expiration", i);
                    }
                    i++;
                }
                continue;
            }

            if (colmap.isEmpty()) {
                colmap.put("cover", 0);
                colmap.put("title", 1);
                colmap.put("author", 2);
                colmap.put("format", 3);
                colmap.put("branch", 4);
                colmap.put("expiration", 5);
            } else {
                colmap.put("cover", 0);
            }

            Elements cols = row.children();
            ReservedItem item = new ReservedItem();

            if (colmap.containsKey("cover")) {
                Element coverColumn = cols.get(colmap.get("cover"));
                futures.add(assignBestCover(item,
                        getCoverUrlList(coverColumn.select("img[id*=coverView]").first())));
            }
            if (colmap.containsKey("title")) {
                Element col = cols.get(colmap.get("title"));
                item.setTitle(col.text().trim());
                if (col.select("a").size() > 0) {
                    Map<String, String> params =
                            getQueryParamsFirst(col.select("a").first().absUrl("href"));
                    item.setId(params.get("id"));
                }
            }
            if (colmap.containsKey("author")) {
                item.setAuthor(cols.get(colmap.get("author")).text().trim());
            }
            if (colmap.containsKey("format")) {
                item.setFormat(cols.get(colmap.get("format")).text().trim());
            }
            if (colmap.containsKey("branch")) {
                item.setBranch(cols.get(colmap.get("branch")).text().trim());
            }
            if (colmap.containsKey("expiration")) {
                try {
                    Element col = cols.get(colmap.get("expiration"));
                    String value = col.text().replace("Aktuelle Frist: ", "").trim();
                    item.setExpirationDate(fmt.parseLocalDate(value));
                } catch (IllegalArgumentException e) {
                    // Ignore
                }
            }
            item.setStatus("Bereitgestellt");
            res.add(item);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    void parse_lent(AccountData data, Document doc, Account account) {
        List<LentItem> lent = new ArrayList<>();
        data.setLent(lent);
        Element table = doc.select("[id$=tpnlLoans_ucLoansView_grdViewLoans]").first();
        if (table.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]").size() == 0) {
            parseLentTable(doc, account, lent, table);
        }

        // interlibrary loans (seen in Bielefeld)
        Element interlibraryTable =
                doc.select("[id$=tpnlRemoteLoans_ucRemoteLoansView_grdViewRemoteLoans]").first();
        if (interlibraryTable != null &&
                interlibraryTable.select(".GridViewInnerBorderNoData, span[id$=LblNoDataReturned]")
                                 .size() == 0) {
            parseLentTable(doc, account, lent, interlibraryTable);
        }
    }

    private void parseLentTable(Document doc, Account account, List<LentItem> lent, Element table) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy");
        List<CompletableFuture> futures = new ArrayList<>();

        Map<String, Integer> colmap = new HashMap<>();
        Map<String, LentItem> copyIds = new HashMap<>();
        for (Element row : table.select("tr")) {
            if (row.select("th").size() > 0) {
                int i = 0;
                for (Element th : row.select("th")) {
                    String th_html = th.html();
                    if (th_html.contains("Sort$Author")) {
                        colmap.put("author", i);
                    } else if (th_html.contains("Sort$MediaGroup")) {
                        colmap.put("format", i);
                    } else if (th_html.contains("Sort$DueDate")) {
                        colmap.put("deadline", i);
                    } else if (th_html.contains("Sort$Branch")) {
                        colmap.put("branch", i);
                    }
                    i++;
                }

                if (row.select("th").get(0).select("input[type=checkbox]").size() > 0) {
                    colmap.put("cover", 1);
                    colmap.put("title", 2);
                } else {
                    colmap.put("cover", 0);
                    colmap.put("title", 1);
                }

                continue;
            }

            if (colmap.isEmpty()) {
                colmap.put("cover", 1);
                colmap.put("title", 2);
                colmap.put("author", 3);
                colmap.put("format", 4);
                colmap.put("branch", 5);
                colmap.put("deadline", 6);
            }

            Elements cols = row.children();
            final LentItem item = new LentItem();

            if (colmap.containsKey("cover")) {
                Element coverColumn = cols.get(colmap.get("cover"));
                futures.add(assignBestCover(item,
                        getCoverUrlList(coverColumn.select("img[id*=coverView]").first())));
            }

            row.select(".oclc-module-label").remove();

            if (colmap.containsKey("title")) {
                item.setTitle(cols.get(colmap.get("title")).text().trim());
                if (cols.get(colmap.get("title")).select("a").size() > 0) {
                    Map<String, String> params = getQueryParamsFirst(
                            cols.get(colmap.get("title")).select("a").first().absUrl("href"));
                    item.setId(params.get("id"));
                }
            }
            if (item.getId() == null) {
                if (row.select("a").size() > 0) {
                    Map<String, String> params =
                            getQueryParamsFirst(row.select("a").first().absUrl("href"));
                    item.setId(params.get("id"));
                }
            }

            if (colmap.containsKey("author")) {
                item.setAuthor(cols.get(colmap.get("author")).text().trim());
            }
            if (colmap.containsKey("format")) {
                item.setFormat(cols.get(colmap.get("format")).text().trim());
            }
            if (colmap.containsKey("branch")) {
                item.setHomeBranch(cols.get(colmap.get("branch")).text().trim());
            }
            if (colmap.containsKey("deadline")) {
                try {
                    item.setDeadline(
                            fmt.parseLocalDate(cols.get(colmap.get("deadline")).text().trim()));
                } catch (IllegalArgumentException e) {
                    // Ignore
                }
            }


            // Get extendable status
            if (row.select("[name$=CopyIdChx]") != null) {
                String copyid = row.select("[name$=CopyIdChx]").val();
                copyIds.put(copyid, item);
            }

            lent.add(item);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        fetchProlongability(copyIds, doc, account);
    }

    public void fetchProlongability(Map<String, LentItem> copyIds,
            Document doc, Account account) {
        if (copyIds.size() == 0) return;

        String url = opac_url +
                "/DesktopModules/OCLC.OPEN.PL.DNN.PatronAccountModule/PatronAccountService" +
                ".asmx/IsCatalogueCopyExtendable";
        String culture = doc.select("input[name$=Culture]").val();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Determine portalID value
        int portalId = 1;
        for (Element scripttag : doc.select("script")) {
            String scr = scripttag.html();
            if (scr.contains("LoadExtensionsAsync")) {
                Pattern portalIdPattern = Pattern.compile(
                        ".*LoadExtensionsAsync\\(\"([^\"]*)\",[^0-9,]*([0-9]+)[^0-9,]*,.*\\).*");
                Matcher portalIdMatcher = portalIdPattern.matcher(scr);
                if (portalIdMatcher.find()) {
                    portalId = Integer.parseInt(portalIdMatcher.group(2));
                    url = portalIdMatcher.group(1);

                    if (!url.contains("://")) {
                        try {
                            URIBuilder uriBuilder = new URIBuilder(opac_url);
                            url = uriBuilder.setPath(url)
                                            .build()
                                            .normalize().toString();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        int[] version = getOpenVersion(doc);

        if (version[0] == 7 && version[1] >= 1 || version[0] > 7) {
            // from OPEN 7.1, we can use one HTTP request to fetch prolongability of all items
            try {
                JSONObject postdata = new JSONObject();
                postdata.put("portalId", portalId)
                        .put("copyIds", join(",", copyIds.keySet()))
                        .put("userName", account.getName())
                        .put("localResourceFile",
                                "~/DesktopModules/OCLC.OPEN.PL.DNN" +
                                        ".PatronAccountModule/App_LocalResources" +
                                        "/PatronAccountModule.resx")
                        .put("culture", culture);
                RequestBody entity = RequestBody.create(MEDIA_TYPE_JSON, postdata.toString());
                JSONObject extensibilityData = new JSONObject(
                        httpPost(url, entity, getDefaultEncoding()));
                JSONArray array = extensibilityData.getJSONArray("d");

                for (int i = 0; i < array.length(); i++) {
                    // iterate through extensibility data for each item
                    JSONObject data = array.getJSONObject(i);
                    String copyid = data.getString("CopyId");
                    LentItem item = copyIds.get(copyid);
                    if (item == null) continue;

                    item.setRenewable(data.optBoolean("IsExtendable", false));
                    if (!item.isRenewable()) {
                        String msg = data.optString("StatusMessages");
                        if (!"".equals("msg") && msg != null && !"null".equals(msg)) {
                            item.setProlongData("fail:" + msg);
                        }
                    } else {
                        item.setProlongData(copyid);
                    }
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

        } else {
            for (Map.Entry<String, LentItem> entry : copyIds.entrySet()) {
                String copyid = entry.getKey();
                LentItem item = entry.getValue();

                JSONObject postdata = new JSONObject();
                try {
                    postdata.put("portalId", portalId)
                            .put("copyId", copyid)
                            .put("userName", account.getName())
                            .put("localResourceFile",
                                    "~/DesktopModules/OCLC.OPEN.PL.DNN" +
                                            ".PatronAccountModule/App_LocalResources" +
                                            "/PatronAccountModule.resx")
                            .put("culture", culture);
                    RequestBody entity = RequestBody.create(MEDIA_TYPE_JSON, postdata.toString());

                    futures.add(asyncPost(url, entity, false).handle((response, throwable) -> {
                        if (throwable != null) return null;
                        try {
                            JSONObject extensibilityData = new JSONObject(response.body().string());
                            item.setRenewable(extensibilityData.getJSONObject("d")
                                                               .optBoolean("IsExtendable", false));
                            if (!item.isRenewable()) {
                                String msg = extensibilityData.getJSONObject("d")
                                                              .optString("StatusMessages");
                                if (!"".equals("msg") && msg != null && !"null".equals(msg)) {
                                    item.setProlongData("fail:" + msg);
                                }
                            } else {
                                item.setProlongData(copyid);
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            CompletableFuture
                    .allOf(futures
                            .toArray(new java8.util.concurrent.CompletableFuture[futures.size()]))
                    .join();
        }
    }

    private String join(String s, Set<String> strings) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String part : strings) {
            if (first) {
                first = false;
            } else {
                builder.append(s);
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private int[] getOpenVersion(Document doc) {
        String url = doc.select("script[src*=open.js]").attr("src");
        String[] split = url.split("\\?");
        String[] version = split[split.length - 1].split("\\.");
        int[] numbers = new int[version.length];
        for (int i = 0; i < version.length; i++) {
            numbers[i] = Integer.parseInt(version[i]);
        }
        return numbers;
    }

    @Override
    public void checkAccountData(Account account)
            throws IOException, JSONException, OpacErrorException {
        login(account);
    }

    @Override
    public int getSupportFlags() {
        return super.getSupportFlags() | OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL |
                OpacApi.SUPPORT_FLAG_WARN_PROLONG_FEES | SUPPORT_FLAG_WARN_RESERVATION_FEES;
    }
}
