package de.geeksfactory.opacclient.apis;

import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.*;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Patron extends BaseApi{
    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {


    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query) throws IOException, OpacErrorException, JSONException {
        return null;
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException, OpacErrorException, JSONException {
        return null;
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public DetailledItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public ReservationResult reservation(DetailledItem item, Account account, int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction, String selection) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public AccountData account(Account account) throws IOException, JSONException, OpacErrorException {
        return null;
    }

    @Override
    public void checkAccountData(Account account) throws IOException, JSONException, OpacErrorException {

    }

    @Override
    public List<SearchField> getSearchFields() throws IOException, OpacErrorException, JSONException {
        return null;
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