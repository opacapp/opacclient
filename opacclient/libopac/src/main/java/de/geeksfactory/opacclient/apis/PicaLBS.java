package de.geeksfactory.opacclient.apis;

import org.json.JSONException;

import java.io.IOException;

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
    @Override
    public ReservationResult reservation(DetailledItem item, Account account,
            int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
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

    @Override
    public boolean isAccountSupported(Library library) {
        return false;
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
