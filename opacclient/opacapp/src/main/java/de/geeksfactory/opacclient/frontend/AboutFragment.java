package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.machinarius.preferencefragment.PreferenceFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;

/**
 * Created by johan_000 on 25.01.2015.
 */
public class AboutFragment extends PreferenceFragment {

    protected Activity context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getActivity();
        populate();
    }

    protected void populate() {
        addPreferencesFromResource(R.xml.about);

        String version = OpacClient.versionName;

        try {
            String text = "";

            StringBuilder builder = new StringBuilder();
            InputStream fis;
            fis = context.getAssets().open("buildnum.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    fis, "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            text = builder.toString();
            fis.close();
            if (!text.equals(version))
                version += " (Build: " + text + ")";
        } catch (IOException e) {
            e.printStackTrace();
        }

        findPreference("version").setSummary(version);

        findPreference("website").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        if (getString(R.string.website_url).contains("de"))
                            i.setData(Uri.parse("http://de.opacapp.net"));
                        else
                            i.setData(Uri.parse("http://en.opacapp.net"));
                        startActivity(i);
                        return false;
                    }
                });

        findPreference("developer").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("http://www.raphaelmichel.de"));
                        startActivity(i);
                        return false;
                    }
                });

        findPreference("feedback").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent emailIntent = new Intent(
                                android.content.Intent.ACTION_SEND);
                        emailIntent.putExtra(
                                android.content.Intent.EXTRA_EMAIL,
                                new String[] { "info@opacapp.de" });
                        emailIntent.setType("text/plain");
                        startActivity(Intent.createChooser(emailIntent,
                                getString(R.string.write_mail)));
                        return false;
                    }
                });

        findPreference("rate_play").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            Intent i = new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=de.geeksfactory.opacclient"));
                            startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            Log.i("rate_play", "no market installed");
                        }
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

        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_about);
        dialog.setTitle(title);
        TextView textview1 = (TextView) dialog.findViewById(R.id.textView1);

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

        textview1.setText(Html.fromHtml(text));

        Button dialogButton = (Button) dialog.findViewById(R.id.button1);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
