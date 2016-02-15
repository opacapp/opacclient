package de.geeksfactory.opacclient.utils;

import android.content.Context;

import org.json.JSONException;

import java.io.IOException;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;

public class Utils {
    public static String getAccountTitle(Account account, Context context) {
        if (context.getString(R.string.default_account_name).equals(
                account.getLabel())) {
            try {
                OpacClient app = (OpacClient) context.getApplicationContext();
                return app.getLibrary(account.getLibrary()).getCity();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return account.getLabel();
        }
    }

    public static String getAccountSubtitle(Account account, Context context) {
        try {
            OpacClient app = (OpacClient) context.getApplicationContext();
            Library library = app.getLibrary(account.getLibrary());
            if (context.getString(R.string.default_account_name).equals(
                    account.getLabel())) {
                return library.getTitle();
            } else {
                return library.getCity() + " · " + library.getTitle();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
