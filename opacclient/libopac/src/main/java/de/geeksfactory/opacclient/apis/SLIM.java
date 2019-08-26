/**
 * Copyright (C) 2019 by Mayur Patil, Algorhythms Consultants Pvt. Ltd under the MIT license:
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

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import okhttp3.FormBody;

/**
 * Implementation of the SLIM21 OPAC developed by Algorhythms Consultants Pvt. Ltd
 * https://www.slimkm.com
 */
public class SLIM extends OkHttpBaseApi {

    protected String ENCODING = "UTF-8", opac_url = "", share_url = "";
    protected int totoal_result_count = 0;
    protected int total_page = 0;
    protected JSONObject data;
    private List<SearchResult> list = new ArrayList<>();
    protected List<SearchQuery> searchQuery = new ArrayList<>();

    @Override
    public void start() {

    }

    @Override
    public void init(Library library, HttpClientFactory httpClientFactory) {
        super.init(library, httpClientFactory);
        this.library = library;
        this.data = library.getData();
        this.opac_url = data.optString("baseurl", "");
        this.share_url = data.optString("itemdetail_url", "");
    }

    private SearchRequestResult ParseSearchResults(JSONObject jo)
            throws JSONException {
        list.clear();
        for (int i = 0; i < jo.getJSONArray("results").length(); i++) {
            JSONObject l = jo.getJSONArray("results").getJSONObject(i);
            SearchResult res = new SearchResult();
            res.setId(l.getString("id"));
            res.setInnerhtml(l.getString("innerhtml"));
            res.setCover(l.getString("cover"));
            res.setStatus(SearchResult.Status.valueOf(l.getString("status").toUpperCase()));
            res.setType(SearchResult.MediaType.valueOf(l.getString("type").toUpperCase()));
            res.setNr(l.getInt("seqNum"));
            res.setPage(Integer.parseInt(l.getString("pageNum")));
            list.add(res);
        }
        totoal_result_count = jo.getInt("total_result_count");
        total_page = jo.getInt("page_count");
        return new SearchRequestResult(list, totoal_result_count, total_page, 1);
    }

    private JSONObject fetchSearchResults(List<SearchQuery> query, int page)
            throws JSONException, IOException {
        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        BuildSearchParams(formData, query);
        formData.add("page", Integer.toString(page));
        JSONObject jo = new JSONObject(
                httpPost(this.opac_url + "OPAC/SearchRequestResult", formData.build(), ENCODING,
                        true));
        return jo;
    }

    private void BuildSearchParams(FormBody.Builder formData, List<SearchQuery> query) {
        String qStr = "[", sep = "";
        for (SearchQuery q : query) {
            if (!q.getValue().isEmpty()) {
                qStr += sep + "{ key: \"" + q.getKey() + "\"" + " , value: " + "\"" + q.getValue() +
                        "\" }";
                sep = ",";
            }
        }
        qStr += "]";
        formData.add("SearchParams", qStr);
    }

    private void addTextField(List<SearchField> fields, JSONObject jo)
            throws IOException, OpacErrorException, JSONException {
        for (int i = 0; i < jo.getJSONArray("Text").length(); i++) {
            JSONObject l = jo.getJSONArray("Text").getJSONObject(i);
            TextSearchField tsf = new TextSearchField(l.getString("id"), l.getString("displayName"),
                    l.getBoolean("advanced"), l.getBoolean("halfWidth"), l.getString("hint"),
                    l.getBoolean("freeSearch"),
                    l.getBoolean("number"));
            tsf.setMeaning(SearchField.Meaning.valueOf(l.getString("meaning")));
            fields.add(tsf);
        }
    }

    private void addSelectFields(List<SearchField> fields, JSONObject jo) throws JSONException {
        for (int i = 0; i < jo.getJSONArray("Select").length(); i++) {
            JSONObject l = jo.getJSONArray("Select").getJSONObject(i);
            DropdownSearchField catField =
                    new DropdownSearchField(l.getString("id"), l.getString("displayName"),
                            l.getBoolean("advanced"), null);
            for (int j = 0; j < l.getJSONArray("dropdownValues").length(); j++) {
                JSONObject ddv = l.getJSONArray("dropdownValues").getJSONObject(j);
                catField.addDropdownValue(ddv.getString("code"), ddv.getString("value"));
            }
            catField.setMeaning(SearchField.Meaning.valueOf(l.getString("meaning")));
            fields.add(catField);
        }
    }

    @Override
    public List<SearchField> parseSearchFields()
            throws IOException, OpacErrorException, JSONException {
        JSONObject jo = new JSONObject(httpGet(this.opac_url + "OPAC/SearchFields", ENCODING));

        List<SearchField> fields = new ArrayList<>();
        addTextField(fields, jo);
        addSelectFields(fields, jo);
        return fields;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, JSONException {
        searchQuery = query;
        JSONObject sr = fetchSearchResults(query, 1);
        return ParseSearchResults(sr);
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        if (searchQuery == null) {
            throw new OpacApi.OpacErrorException("Internal Error");
        }
        JSONObject sr = fetchSearchResults(searchQuery, page);
        return ParseSearchResults(sr);
    }

    private DetailedItem ParseDetailedItem(JSONObject jo)
            throws JSONException {
        DetailedItem item = new DetailedItem();
        item.setId(jo.getString("id"));
        item.setTitle(jo.getString("title"));
        item.setCover(jo.getString("cover"));
        item.setMediaType(SearchResult.MediaType.valueOf(jo.getString("mediaType").toUpperCase()));
        item.setReservable(jo.getBoolean("reservable"));

        for (int i = 0; i < jo.getJSONArray("details").length(); i++) {
            JSONObject l = jo.getJSONArray("details").getJSONObject(i);
            item.addDetail(new Detail(l.getString("desc"), l.getString("content")));
        }
        for (int i = 0; i < jo.getJSONArray("copies").length(); i++) {
            JSONObject l = jo.getJSONArray("copies").getJSONObject(i);
            Copy oCopy = new Copy();
            oCopy.setBarcode(l.getString("barcode"));
            oCopy.setLocation(l.getString("location"));
            oCopy.setDepartment(l.getString("department"));
            oCopy.setBranch(l.getString("branch"));
            String sRetDt = l.getString("returndate");
            if (sRetDt != null && !sRetDt.isEmpty()) {
                oCopy.setReturnDate(LocalDate.parse(l.getString("returndate")));
            }
            oCopy.setReservations(l.getString("reservations"));
            oCopy.setShelfmark(l.getString("shelfmark"));
            oCopy.setUrl(l.getString("url"));
            oCopy.setStatus(l.getString("status"));
            item.addCopy(oCopy);
        }
        return item;
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        try {
            JSONObject jo =
                    new JSONObject(httpGet(this.opac_url + "OPAC/DetailedItem?id=" + id, ENCODING));
            return ParseDetailedItem(jo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        String strMsg = "";
        try {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("userid", account.getName());
            formData.add("pwd", account.getPassword());
            formData.add("itemID", item.getId());
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "Account/ReserveItem", formData.build(), ENCODING));
            boolean isReissued = jo.getBoolean("success");
            strMsg = jo.getString("message");
            if (!isReissued) {
                return new ReservationResult(MultiStepResult.Status.ERROR, jo.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ReservationResult(MultiStepResult.Status.OK, strMsg);
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        String strMsg = "";
        try {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("userid", account.getName());
            formData.add("pwd", account.getPassword());
            formData.add("barcode", media);
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "Account/RenewItem", formData.build(), ENCODING));
            boolean isReissued = jo.getBoolean("success");
            strMsg = jo.getString("message");
            if (!isReissued) {
                return new ProlongResult(MultiStepResult.Status.ERROR, jo.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ProlongResult(MultiStepResult.Status.OK, strMsg);
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        try {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("userid", account.getName());
            formData.add("pwd", account.getPassword());
            formData.add("itemID", media);
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "Account/CancelReservation", formData.build(),
                            ENCODING));
            boolean isCancelled = jo.getBoolean("success");
            if (!isCancelled) {
                return new CancelResult(MultiStepResult.Status.ERROR, jo.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CancelResult(MultiStepResult.Status.OK);
    }

    @Override
    public AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        AccountData data = new AccountData(account.getId());
        List<LentItem> lent = new ArrayList<>();
        List<ReservedItem> reservations = new ArrayList<>();

        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("userid", account.getName());
        formData.add("pwd", account.getPassword());
        JSONObject jo = new JSONObject(
                httpPost(this.opac_url + "Account/AccountData", formData.build(), ENCODING));

        data.setPendingFees(jo.getString("pendingFees"));
        data.setValidUntil(jo.getString("validUntil"));
        data.setWarning(jo.getString("warningMessage"));

        for (int i = 0; i < jo.getJSONArray("lentItems").length(); i++) {
            JSONObject l = jo.getJSONArray("lentItems").getJSONObject(i);
            LentItem lentItem = new LentItem();
            lentItem.setTitle(l.getString("title"));
            lentItem.setAuthor(l.getString("author"));
            lentItem.setStatus(l.getString("status"));
            lentItem.setDeadline(l.getString("returnDate"));
            lentItem.setRenewable(l.getBoolean("renewable"));
            if (lentItem.isRenewable()) {
                lentItem.setProlongData(l.getString("prolongData"));
            }
            lentItem.setEbook(l.getBoolean("eBook"));
            if (!l.getString("coverImage").isEmpty()) {
                lentItem.setCover(l.getString("coverImage"));
            }
            lentItem.setDownloadData(l.getString("downloadData"));
            lentItem.setHomeBranch(l.getString("homeBranch"));
            lentItem.setLendingBranch(l.getString("lendingBranch"));
            lentItem.setBarcode(l.getString("barcode"));
            lentItem.setId(l.getString("catrefnum"));
            lentItem.setMediaType(
                    SearchResult.MediaType.valueOf(l.getString("mediaType").toUpperCase()));
            lent.add(lentItem);
        }

        for (int i = 0; i < jo.getJSONArray("reservations").length(); i++) {
            JSONObject l = jo.getJSONArray("reservations").getJSONObject(i);
            ReservedItem reservedItem = new ReservedItem();
            reservedItem.setAuthor(l.getString("author"));
            reservedItem.setTitle(l.getString("title"));
            reservedItem.setStatus(l.getString("status"));
            if (!l.getString("readyDate").isEmpty()) {
                reservedItem.setReadyDate(l.getString("readyDate"));
            }
            if (!l.getString("expirationDate").isEmpty()) {
                reservedItem.setExpirationDate(l.getString("expirationDate"));
            }
            reservedItem.setBranch(l.getString("branch"));
            if (!l.getString("cancelData").isEmpty()) {
                reservedItem.setCancelData(l.getString("cancelData"));
            }
            reservedItem.setId(l.getString("catrefnum"));
            if (!l.getString("coverImage").isEmpty()) {
                reservedItem.setCover(l.getString("coverImage"));
            }
            reservedItem.setMediaType(
                    SearchResult.MediaType.valueOf(l.getString("mediaType").toUpperCase()));
            reservations.add(reservedItem);
        }
        data.setLent(lent);
        data.setReservations(reservations);

        return data;
    }

    @Override
    public void checkAccountData(Account account)
            throws IOException, JSONException, OpacErrorException {
        login(account);
    }

    private void login(Account account) throws IOException, JSONException, OpacErrorException {
        FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
        formData.add("userid", account.getName());
        formData.add("pwd", account.getPassword());
        JSONObject jo = new JSONObject(
                httpPost(this.opac_url + "Account/Login", formData.build(), ENCODING));
        boolean isUserVerified = jo.getBoolean("isVerified");
        if (!isUserVerified) {
            throw new OpacApi.OpacErrorException(jo.getString("message"));
        }
    }

    @Override
    public String getShareUrl(String id, String title) {
        return share_url + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING;
    }

    @Override
    public boolean shouldUseMeaningDetector() {
        return false;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        return null;
    }

    @Override
    public void setLanguage(String language) {

    }
}
