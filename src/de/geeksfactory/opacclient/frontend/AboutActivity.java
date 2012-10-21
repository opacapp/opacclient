package de.geeksfactory.opacclient.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;

public class AboutActivity extends OpacActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_activity);

		TextView tvAbout = (TextView) findViewById(R.id.tvAbout);
		TextView tvVersion = (TextView) findViewById(R.id.tvVersion);
		String abouttext = "";

		try {
			StringBuilder builder = new StringBuilder();
			InputStream fis;
			try {
				fis = getAssets().open("about.html");
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(fis, "utf-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}

				abouttext = builder.toString();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			tvVersion.setText(Html
					.fromHtml("OpacClient für Android "
							+ (getPackageManager().getPackageInfo(
									getPackageName(), 0).versionName)));
			tvAbout.setText(Html.fromHtml(abouttext));
			tvAbout.setMovementMethod(LinkMovementMethod.getInstance());
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
