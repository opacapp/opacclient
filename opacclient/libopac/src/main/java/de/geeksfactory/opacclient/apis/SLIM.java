package de.geeksfactory.opacclient.apis;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.geeksfactory.opacclient.i18n.StringProvider;
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

public class SLIM extends OkHttpBaseApi {

    protected String ENCODING = "UTF-8", opac_url = "", share_url = "";
    protected int totoal_result_count = 0;
    protected int total_page = 0;
    protected JSONObject data;
    protected CookieStore cookieStore = new BasicCookieStore();
    private List<SearchResult> list = new ArrayList<>();
    protected List<SearchQuery> searchQuery = new ArrayList<>();
    static String borrowerID = "";

    @Override
    public void start() throws IOException {

    }

    @Override
    public void init(Library library, HttpClientFactory httpClientFactory) {
        super.init(library, httpClientFactory);
        this.library = library;
        this.data = library.getData();
        this.opac_url = data.optString("baseurl", "");
        this.share_url = data.optString("itemdetail_url", "");
    }

    private void ParseSearchResults(JSONObject jo)
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
            res.setNr(Integer.parseInt(l.getString("seqNum")));
            res.setPage(Integer.parseInt(l.getString("pageNum")));
            list.add(res);
        }
        totoal_result_count = Integer.parseInt(jo.getString("total_result_count"));
        total_page = Integer.parseInt(jo.getString("page_count"));

    }

    private void fetchSearchResults(List<SearchQuery> query, int page) {
        try {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            BuildSearchParams(formData, query);
            formData.add("page", Integer.toString(page));
            formData.add("userID", this.borrowerID);
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "OPAC/SearchRequestResult", formData.build(), ENCODING,
                            true));
            ParseSearchResults(jo);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            fields.add(new TextSearchField(l.getString("id"), l.getString("displayName"),
                    Boolean.parseBoolean(l.getString("advanced")),
                    Boolean.parseBoolean(l.getString("halfWidth")), l.getString("hint"),
                    Boolean.parseBoolean(l.getString("freeSearch")),
                    Boolean.parseBoolean(l.getString("number"))));
        }
    }

    private void addSelectFields(List<SearchField> fields, JSONObject jo) throws JSONException {
        for (int i = 0; i < jo.getJSONArray("Select").length(); i++) {
            JSONObject l = jo.getJSONArray("Select").getJSONObject(i);
            DropdownSearchField catField =
                    new DropdownSearchField(l.getString("id"), l.getString("displayName"),
                            Boolean.parseBoolean(l.getString("advanced")), null);
            for (int j = 0; j < l.getJSONArray("dropdownValues").length(); j++) {
                JSONObject ddv = l.getJSONArray("dropdownValues").getJSONObject(j);
                catField.addDropdownValue(ddv.getString("code"), ddv.getString("value"));
            }
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
            throws IOException, OpacErrorException, JSONException {
        searchQuery = query;
        fetchSearchResults(query, 1);
        return new SearchRequestResult(list, totoal_result_count, total_page, 1);
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
        fetchSearchResults(searchQuery, page);
        return new SearchRequestResult(list, totoal_result_count, total_page, page);
    }

    private void ParseDetailedItem(DetailedItem item, JSONObject jo)
            throws JSONException {
        item.setId(jo.getString("id"));
        item.setTitle(jo.getString("title"));
        item.setCover(jo.getString("cover"));
        item.setMediaType(SearchResult.MediaType.valueOf(jo.getString("mediaType").toUpperCase()));
        item.setReservable(Boolean.parseBoolean(jo.getString("reservable")));

        for (int i = 0; i < jo.getJSONArray("details").length(); i++) {
            JSONObject l = jo.getJSONArray("details").getJSONObject(i);
            item.addDetail(new Detail(l.getString("desc"), l.getString("content")));
        }
        for (int i = 0; i < jo.getJSONArray("copies").length(); i++) {
            JSONObject l = jo.getJSONArray("copies").getJSONObject(i);
            Copy oCopy = new Copy();
            oCopy.set("barcode", l.getString("barcode"));
            oCopy.set("location", l.getString("location"));
            oCopy.set("department", l.getString("department"));
            oCopy.set("branch", l.getString("branch"));
            String sRetDt = l.getString("returndate");
            if (sRetDt != null && !sRetDt.isEmpty()) {
                oCopy.set("returndate", l.getString("returndate"));
            }
            oCopy.set("reservations", l.getString("reservations"));
            oCopy.setShelfmark(l.getString("shelfmark"));
            oCopy.set("url", l.getString("url"));
            oCopy.setStatus(l.getString("status"));
            item.addCopy(oCopy);
        }
        /*for (int i = 0; i < jo.getJSONArray("volumes").length(); i++) {
            JSONObject l = jo.getJSONArray("volumes").getJSONObject(i);
            item.addVolume(new Volume(l.getString("id"), l.getString("title")));
        }*/
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        DetailedItem item = new DetailedItem();
        try {
            JSONObject jo =
                    new JSONObject(httpGet(this.opac_url + "OPAC/DetailedItem?id=" + id, ENCODING));
            ParseDetailedItem(item, jo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return item;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        try {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("userid", account.getName());
            formData.add("pwd", account.getPassword());
            formData.add("itemID", item.getId());
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "Account/ReserveItem", formData.build(), ENCODING,
                            true));
            boolean isReissued = Boolean.parseBoolean(jo.getString("success"));
            if (!isReissued) {
                return new ReservationResult(MultiStepResult.Status.ERROR, jo.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ReservationResult(MultiStepResult.Status.OK);
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        try {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("userid", account.getName());
            formData.add("pwd", account.getPassword());
            formData.add("barcode", media);
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "Account/RenewItem", formData.build(), ENCODING,
                            true));
            boolean isReissued = Boolean.parseBoolean(jo.getString("success"));
            if (!isReissued) {
                return new ProlongResult(MultiStepResult.Status.ERROR, jo.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ProlongResult(MultiStepResult.Status.OK);
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
                            ENCODING,
                            true));
            boolean isCancelled = Boolean.parseBoolean(jo.getString("success"));
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
        try {

            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("userid", account.getName());
            formData.add("pwd", account.getPassword());
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "Account/AccountData", formData.build(), ENCODING,
                            true));

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
                lentItem.setRenewable(Boolean.parseBoolean(l.getString("renewable")));
                if (lentItem.isRenewable()) {
                    lentItem.setProlongData(l.getString("prolongData"));
                }
                lentItem.setEbook(Boolean.parseBoolean(l.getString("eBook")));
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
                reservedItem.setReadyDate(l.getString("readyDate"));
                reservedItem.setExpirationDate(l.getString("expirationDate"));
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
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

    private void login(Account account) throws OpacErrorException {
        try {
            FormBody.Builder formData = new FormBody.Builder(Charset.forName(getDefaultEncoding()));
            formData.add("userid", account.getName());
            formData.add("pwd", account.getPassword());
            JSONObject jo = new JSONObject(
                    httpPost(this.opac_url + "Account/Login", formData.build(), ENCODING,
                            true));
            boolean isUserVerified = Boolean.parseBoolean(jo.getString("isVerified"));
            if (isUserVerified) {
                this.borrowerID = account.getName();
            } else {
                throw new OpacApi.OpacErrorException(jo.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getShareUrl(String id, String title) {
        return share_url + id;
    }

    @Override
    public int getSupportFlags() {
        return 0;
    }

    @Override
    public boolean shouldUseMeaningDetector() {
        return false;
    }

    @Override
    public void setStringProvider(StringProvider stringProvider) {

    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        return null;
    }

    @Override
    public void setLanguage(String language) {

    }
}
