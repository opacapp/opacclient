package de.geeksfactory.opacclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class FrontpageActivity extends OpacActivity {

	public void urlintent(){
		Uri d = getIntent().getData();
		String medianr = d.getQueryParameter("id");
		
		if(medianr != null){
	        Intent intent = new Intent(FrontpageActivity.this, SearchResultDetailsActivity.class);
	        intent.putExtra("item_id", medianr);
	        startActivity(intent);
	        return;
		}
		
		String titel = d.getQueryParameter("titel"); 
		String verfasser = d.getQueryParameter("verfasser"); 
		String schlag_a = d.getQueryParameter("schlag_a"); 
		String schlag_b = d.getQueryParameter("schlag_b"); 
		String isbn = d.getQueryParameter("isbn"); 
		String jahr_von = d.getQueryParameter("jahr_von"); 
		String jahr_bis = d.getQueryParameter("jahr_bis"); 
		String verlag = d.getQueryParameter("verlag"); 
    	Intent myIntent = new Intent(FrontpageActivity.this, SearchResultsActivity.class);
		myIntent.putExtra("titel", (titel != null ? titel : ""));
		myIntent.putExtra("verfasser", (verfasser != null ? verfasser : ""));
		myIntent.putExtra("schlag_a", (schlag_a != null ? schlag_a : ""));
		myIntent.putExtra("schlag_b", (schlag_b != null ? schlag_b : ""));
		myIntent.putExtra("isbn", (isbn != null ? isbn : ""));
		myIntent.putExtra("jahr_von", (jahr_von != null ? jahr_von : ""));
		myIntent.putExtra("jahr_bis", (jahr_bis != null ? jahr_bis : ""));
		myIntent.putExtra("verlag", (verlag != null ? verlag : ""));
    	startActivity(myIntent);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
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
        setContentView(R.layout.frontpage);
        
        if(getIntent().getAction().equals("android.intent.action.VIEW")){        	
        	urlintent();
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
