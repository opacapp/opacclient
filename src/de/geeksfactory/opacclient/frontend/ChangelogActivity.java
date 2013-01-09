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

public class ChangelogActivity extends OpacActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.changelog_activity);

		TextView tvAbout = (TextView) findViewById(R.id.tvAbout);
		String abouttext = "";

		StringBuilder builder = new StringBuilder();
		InputStream fis;
		try {
			fis = getAssets().open("changelog.html");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, "utf-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			abouttext = builder.toString();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		tvAbout.setText(Html.fromHtml(abouttext));
		tvAbout.setMovementMethod(LinkMovementMethod.getInstance());
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
