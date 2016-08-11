package de.geeksfactory.opacclient.utils;

import android.content.Context;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    /**
     * Reads an {@link InputStream} to a String using UTF-8 encoding and closes it.
     *
     * @param is the InputStream to read
     * @return the content read from the InputStream
     * @throws IOException
     */
    public static String readStreamToString(InputStream is) throws IOException {
        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        is.close();
        return builder.toString();
    }
}
