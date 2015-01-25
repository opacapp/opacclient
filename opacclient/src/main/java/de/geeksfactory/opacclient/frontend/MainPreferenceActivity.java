package de.geeksfactory.opacclient.frontend;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.geeksfactory.opacclient.R;

public class MainPreferenceActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new MainPreferenceFragment())
                .commit();
    }
}
