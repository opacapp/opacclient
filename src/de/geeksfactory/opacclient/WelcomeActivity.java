package de.geeksfactory.opacclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class WelcomeActivity extends OpacActivity {
	protected ProgressDialog dialog;
	protected String[] bibnamesA;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        ListView lv = (ListView) findViewById(R.id.lvBibs);
    	try {
	        List<String> bibnames = new ArrayList<String>();
	        for(int i = 0; i < app.bibs.names().length(); i++){
				bibnames.add(app.bibs.names().getString(i));
	        }
	        bibnamesA = new String[bibnames.size()];
	        bibnames.toArray(bibnamesA);
	        Arrays.sort(bibnamesA);
	        lv.setAdapter(new ArrayAdapter<String>(this,
	        		android.R.layout.simple_list_item_1, bibnamesA));
		} catch (JSONException e) {
			app.web_error(e, "jsonerror");
		}
        lv.setTextFilterEnabled(true);
        
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
		        int position, long id) {

		        try {
			        app.ohc.opac_url = app.bibs.getJSONArray(bibnamesA[position]).getString(0);
				} catch (JSONException e) {
					app.web_error(e, "jsonerror");
				}
		  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this);
		  	  	Editor e = sp.edit();
		  	  	e.remove("opac_mg");
		  	  	e.remove("opac_zst");
				e.putString("opac_url", app.ohc.opac_url);
				e.putString("opac_bib", bibnamesA[position]);
		        e.commit();
		        
				dialog = ProgressDialog.show(WelcomeActivity.this, "", 
						getString(R.string.loading), true);
				dialog.show();
				
				new InitTask().execute(app);
		    }
		});
    }
	public class InitTask extends OpacTask<Integer> {		
    	@Override
    	protected Integer doInBackground(Object... arg0) {
            app = (OpacClient) arg0[0];
            try {
            	app.ohc.init();
            } catch (Exception e){
    			publishProgress(e, "ioerror");
            }
    		return 0;
    	}
    	
        protected void onPostExecute(Integer result) {
        	dialog.dismiss();
	        Intent intent = new Intent(WelcomeActivity.this, FrontpageActivity.class);
	        startActivity(intent);
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(dialog != null){
			if(dialog.isShowing()){
				dialog.cancel();
			}
		}
	}
    
}
