package de.geeksfactory.opacclient;

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
	ProgressDialog dialog;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        
        ListView lv = (ListView) findViewById(R.id.lvBibs);
        lv.setAdapter(new ArrayAdapter<String>(this,
        		android.R.layout.simple_list_item_1, OpacClient.BIBNAMES));
        lv.setTextFilterEnabled(true);
        
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
		        int position, long id) {
		  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this);
		  	  	Editor e = sp.edit();
		  	  	e.remove("opac_mg");
		  	  	e.remove("opac_zst");
		        e.putString("opac_url", OpacClient.BIBURLS[position]);
		        e.commit();
		        
		        app.ohc.opac_url = OpacClient.BIBURLS[position];
		        
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
	        Intent intent = new Intent(WelcomeActivity.this, SearchActivity.class);
	        startActivity(intent);
        }
}
    
}
