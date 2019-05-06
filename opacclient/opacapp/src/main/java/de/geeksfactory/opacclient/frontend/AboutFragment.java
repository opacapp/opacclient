package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;

public class AboutFragment extends PreferenceFragmentCompat {

    protected Activity context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        this.context = getActivity();
        populate();
    }

    protected void populate() {
        addPreferencesFromResource(R.xml.about);

        String version = OpacClient.versionName;
        findPreference("version").setSummary(version);

        findPreference("website").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        if (getString(R.string.website_url).contains("de")) {
                            i.setData(Uri.parse("http://opac.app/de/"));
                        } else {
                            i.setData(Uri.parse("http://opac.app/en/"));
                        }
                        startActivity(i);
                        return false;
                    }
                });

        findPreference("support").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(getString(R.string.support_url)));
                        startActivity(i);
                        return false;
                    }
                });

        findPreference("source").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri
                                .parse("http://github.com/raphaelm/opacclient"));
                        startActivity(i);
                        return false;
                    }
                });

        findPreference("osl").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        res_dialog(R.raw.licenses, R.string.osl);

                        return false;
                    }
                });

        findPreference("privacy").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        res_dialog(R.raw.privacy, R.string.privacy);
                        return false;
                    }
                });

        findPreference("thanks").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        res_dialog(R.raw.thanks, R.string.thanks);
                        return false;
                    }
                });

        findPreference("changelog").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference arg0) {
                        res_dialog(R.raw.changelog, R.string.changelog);
                        return false;
                    }
                });
    }

    private void res_dialog(int file, int title) {

        final AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.setContentView(R.layout.dialog_about);
        dialog.setTitle(title);
        TextView tvText = (TextView) dialog.findViewById(R.id.tvText);

        String text = "";

        StringBuilder builder = new StringBuilder();
        InputStream fis;
        try {
            fis = getResources().openRawResource(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    fis, "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            text = builder.toString();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        tvText.setText(Html.fromHtml(text));

        Button closeButton = (Button) dialog.findViewById(R.id.btnClose);
        // if button is clicked, close the custom dialog
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
