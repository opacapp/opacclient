/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
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
package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.reminder.ReminderHelper;
import de.geeksfactory.opacclient.reminder.SyncAccountService;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;

public class MainPreferenceFragment extends PreferenceFragmentCompat {

    protected Activity context;

    @SuppressWarnings("SameReturnValue") // Plus Edition compatibility
    protected boolean ebooksSupported() {
        return false;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        this.context = getActivity();

        addPreferencesFromResource(R.xml.settings);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH
                || !context.getPackageManager()
                           .hasSystemFeature("android.hardware.nfc")) {
            if (findPreference("nfc_search") != null) {
                findPreference("nfc_search").setEnabled(false);
            }
        }

        Preference assistant = findPreference("accounts");
        assistant.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                ((OpacClient) context.getApplication())
                        .openAccountList(context);
                return false;
            }
        });

        if (!ebooksSupported()) {
            ((PreferenceCategory) findPreference("cat_web_opac"))
                    .removePreference(findPreference("email"));
        }

        CheckBoxPreference notification =
                (CheckBoxPreference) findPreference("notification_service");
        notification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (Boolean) newValue;
                new ReminderHelper((OpacClient) getActivity().getApplication())
                        .updateAlarms(enabled);
                return true;
            }
        });

        ListPreference warning = (ListPreference) findPreference("notification_warning");
        warning.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                //int oldWarning = Integer.parseInt(prefs.getString("notification_warning", "3"));
                int newWarning = Integer.parseInt((String) newValue);
                new ReminderHelper((OpacClient) getActivity().getApplication())
                        .updateAlarms(newWarning);
                return true;
            }
        });

        Preference meta = findPreference("meta_clear");
        meta.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                AccountDataSource adata = new AccountDataSource(context);
                adata.open();
                adata.invalidateCachedData();
                adata.close();

                SearchFieldDataSource sfdata = new JsonSearchFieldDataSource(context);
                sfdata.clearAll();

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sp.edit().remove("reservation_fee_warning_ignore").apply();

                Intent i = new Intent(context, SyncAccountService.class);
                context.startService(i);
                return false;
            }
        });
    }
}
