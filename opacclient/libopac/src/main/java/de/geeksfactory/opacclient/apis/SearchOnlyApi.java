package de.geeksfactory.opacclient.apis;

import org.json.JSONException;

import java.io.IOException;

import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;

/**
 * For search-only API implementations, i.e. without account support, some abstract method
 * implementations are provided.
 */
public abstract class SearchOnlyApi extends BaseApi {

    @Override
    public final ReservationResult reservation(DetailledItem item, Account account,
                                         int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public final ProlongResult prolong(String media, Account account, int useraction,
                                 String selection) throws IOException {
        return null;
    }

    @Override
    public final ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public final CancelResult cancel(String media, Account account, int useraction,
                               String selection) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public final AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        return null;
    }

    @Override
    public final void checkAccountData(Account account)
            throws IOException, JSONException, OpacErrorException {
    }
}
