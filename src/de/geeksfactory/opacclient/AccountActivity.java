package de.geeksfactory.opacclient;

import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

public class AccountActivity extends OpacActivity {

	protected ProgressDialog dialog;
	
	public static int STATUS_SUCCESS = 0;
	public static int STATUS_NOUSER = 1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onResume() {
    	super.onResume();
        setContentView(R.layout.account);
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(app);
    	if(sp.getString("opac_usernr", "").equals("") || sp.getString("opac_password", "").equals("")){
    		dialog_no_user(true);
    	}else{
			dialog = ProgressDialog.show(AccountActivity.this, "", 
					getString(R.string.loading_account), true, true, new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							finish();
						}
			});
			dialog.show();
			
	        new LoadTask().execute(app, getIntent().getIntExtra("item", 0));
    	}
    }
    
	protected void cancel(String a){
		dialog = ProgressDialog.show(this, "", 
				getString(R.string.doing_cancel), true);
		dialog.show();
        
		new CancelTask().execute(app, a);
	}
	
	public void cancel_done(int result){
		dialog.dismiss();
		
		if(result == STATUS_SUCCESS){
			dialog = ProgressDialog.show(this, "", 
				getString(R.string.loading_account), true);
			dialog.show();
			
	        new LoadTask().execute(app, getIntent().getIntExtra("item", 0));
		}
	}
	
    public class LoadTask extends OpacTask<List<List<String[]>>> {

    	@Override
    	protected List<List<String[]>> doInBackground(Object... arg0) {
            app = (OpacClient) arg0[0];
            
    		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(app);
            try {
        		List<List<String[]>> res = app.ohc.account(sp.getString("opac_usernr", ""), sp.getString("opac_password", ""));
    			return res;
    		} catch (Exception e) {
    			publishProgress(e, "ioerror");
    		}
            
    		return null;
    	}
    	
        protected void onPostExecute(List<List<String[]>> result) {
        	loaded(result);
        }
    }
    
    public void loaded(final List<List<String[]>> result) {
		if(result == null){
			dialog.dismiss();
			dialog_wrong_credentials(app.ohc.getLast_error(), true);
			return;
		}
		
    	TableLayout td = (TableLayout) findViewById(R.id.tlMedien);        	
    	td.removeAllViews();
    	for(int i = 0; i < result.get(0).size(); i++){
            TableRow row = new TableRow(AccountActivity.this);

            TextView t1 = new TextView(AccountActivity.this);
            t1.setText(Html.fromHtml(result.get(0).get(i)[0]+"<br />"+result.get(0).get(i)[1]+"<br />"+result.get(0).get(i)[2]));
            t1.setPadding(0, 0, 10, 10);
            row.addView(t1); 
            
            TextView t2 = new TextView(AccountActivity.this);
            t2.setText(Html.fromHtml(result.get(0).get(i)[3]+" ("+result.get(0).get(i)[4]+")<br />"+result.get(0).get(i)[6]));
            row.addView(t2); 
            td.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    	}
        
    	TableLayout tr = (TableLayout) findViewById(R.id.tlReservations);      	
    	tr.removeAllViews();
    	for(int i = 0; i < result.get(1).size(); i++){
            TableRow row = new TableRow(AccountActivity.this);

            TextView t1 = new TextView(AccountActivity.this);
            t1.setText(Html.fromHtml(result.get(1).get(i)[0]+"<br />"+result.get(1).get(i)[1]));
            t1.setPadding(0, 0, 10, 10);
            row.addView(t1); 
            
            TextView t2 = new TextView(AccountActivity.this);
            t2.setText(Html.fromHtml(result.get(1).get(i)[2]+"<br />"+result.get(1).get(i)[3]));
            row.addView(t2); 
            
	    	if(result.get(1).get(i)[4] != null){
	            final int j = i;
	            Button b1 = new Button(AccountActivity.this);
	            b1.setText("X");
	            b1.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						AccountActivity.this.cancel(result.get(1).get(j)[4]);
					}
	            });
	            row.addView(b1);
	    	}
	    	
            tr.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    	}
    	
    	dialog.dismiss();
    }
    
	public class CancelTask extends OpacTask<Integer> {
		
    	@Override
    	protected Integer doInBackground(Object... arg0) {
            app = (OpacClient) arg0[0];
            String a = (String) arg0[1];
            try {
				app.ohc.cancel(a);
			} catch (Exception e) {
    			publishProgress(e, "ioerror");
			}
    		return STATUS_SUCCESS;
    	}
    	
        protected void onPostExecute(Integer result) {
        	cancel_done(result);
        }
	}
}
