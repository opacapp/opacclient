package de.geeksfactory.opacclient.frontend;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.github.machinarius.preferencefragment.PreferenceFragment;

import de.geeksfactory.opacclient.R;

public class MainPreferenceActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, getNewMainPreferenceFragment())
                .commit();
    }

    protected PreferenceFragment getNewMainPreferenceFragment() {
        return new MainPreferenceFragment();
    }
}
