package de.geeksfactory.opacclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class FrontpageActivity extends OpacActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.frontpage);
  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
  	  	if(sp.getString("opac_url", "").equals("") || sp.getString("opac_bib", "").equals("")){
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            return;
  	  	}
        
  	  	if(!((OpacClient) getApplication()).isOnline()){
  	  		app.web_error(null, "offline");
  	  		return;
  	  	}
  	  	
  	  	TextView tvBn = (TextView) findViewById(R.id.tvBibname);
  	  	tvBn.setText(sp.getString("opac_bib", "Mannheim"));

  	  	ImageView ivSearch = (ImageView) findViewById(R.id.ivGoSearch);
  	  	ImageView ivScan = (ImageView) findViewById(R.id.ivGoScan);
  	  	ImageView ivAccount = (ImageView) findViewById(R.id.ivGoAccount);
  	  	ImageView ivStarred = (ImageView) findViewById(R.id.ivGoStarred);

  	  	ivSearch.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
	            Intent intent = new Intent(FrontpageActivity.this, SearchActivity.class);
	            startActivity(intent);
			}
        });
  	  	ivAccount.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
	            Intent intent = new Intent(FrontpageActivity.this, AccountActivity.class);
	            startActivity(intent);
			}
        });
  	  	ivScan.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
	            Intent intent = new Intent(FrontpageActivity.this, SearchActivity.class);
	            intent.putExtra("barcode", true);
	            startActivity(intent);
			}
        });
  	  	ivStarred.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
	            Intent intent = new Intent(FrontpageActivity.this, StarredActivity.class);
	            startActivity(intent);
			}
        });
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.frontpage_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		Intent intent;
	    switch (item.getItemId()) {
	        case R.id.menu_prefs:
	            intent = new Intent(FrontpageActivity.this, MainPreferenceActivity.class);
	            startActivity(intent);
	            return true;
	        case R.id.menu_about:
	            intent = new Intent(FrontpageActivity.this, AboutActivity.class);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

}
