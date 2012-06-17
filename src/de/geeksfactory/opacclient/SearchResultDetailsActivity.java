package de.geeksfactory.opacclient;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

public class SearchResultDetailsActivity extends OpacActivity {

	protected ProgressDialog dialog;
	protected DetailledItem item;
	
	public static int STATUS_SUCCESS = 0;
	public static int STATUS_NOUSER = 1;
	
	protected String[] items;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_details);
        
		dialog = ProgressDialog.show(this, "", 
				getString(R.string.loading_details), true, true, new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						finish();
					}
		});
		dialog.show();
		
        new FetchTask().execute(app, getIntent().getIntExtra("item", 0));
    }
	
	protected void reservation(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		items = sp.getString("opac_zst", "00:").split(",");
		if(items[0].startsWith(":")){
	        List<String> tmp = new ArrayList<String>(Arrays.asList(items));
	        tmp.remove(0);
	        String[] tmp2 = new String[tmp.size()];
	        items = tmp.toArray(tmp2);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.res_zst));
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface d, int item) {
				dialog = ProgressDialog.show(SearchResultDetailsActivity.this, "", 
						getString(R.string.doing_res), true);
				dialog.show();
		        
				new ResTask().execute(app, items[item].split(":",2)[0]);
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	public void reservation_done(int result){
		dialog.dismiss();
		
		if(result == STATUS_NOUSER){
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage(R.string.status_nouser)
	    	       .setCancelable(true)
	    	       .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	           }
	    	       });
	    	AlertDialog alert = builder.create();
	    	alert.show();
		}else if(result == STATUS_SUCCESS){
            Intent intent = new Intent(SearchResultDetailsActivity.this, AccountActivity.class);
            startActivity(intent);
		}
	}
	
    public class FetchTask extends OpacTask<DetailledItem> {

    	@Override
    	protected DetailledItem doInBackground(Object... arg0) {
            app = (OpacClient) arg0[0];
            Integer nr = (Integer) arg0[1];
            
            try {
    			DetailledItem res = app.ohc.getResult(nr);
				URL newurl;
				newurl = new URL(res.getCover());
        		Bitmap mIcon_val = BitmapFactory.decodeStream(newurl.openConnection().getInputStream()); 
        		res.setCoverBitmap(mIcon_val);
    			return res;
    		} catch (Exception e) {
    			publishProgress(e, "ioerror");
    		}
            
    		return null;
    	}
    	
        protected void onPostExecute(DetailledItem result) {
        	item = result;
        	Log.i("result", item.toString());
        	
        	ImageView iv = (ImageView) findViewById(R.id.ivCover);
        	
        	if(item.getCoverBitmap() != null){
        		iv.setVisibility(View.VISIBLE);
	        	iv.setImageBitmap(item.getCoverBitmap());
        	}else{
        		iv.setVisibility(View.GONE);
        	}
        	
        	TextView tvTitel = (TextView) findViewById(R.id.tvTitle);
        	tvTitel.setText(item.getTitle());
            
        	TableLayout td = (TableLayout) findViewById(R.id.tlDetails);
        	
        	for(int i = 0; i < result.getDetails().size(); i++){
                TableRow row = new TableRow(SearchResultDetailsActivity.this);

                TextView t1 = new TextView(SearchResultDetailsActivity.this);
                t1.setText(Html.fromHtml(result.getDetails().get(i)[0]));
                t1.setPadding(0, 0, 10, 0);
                row.addView(t1); 
                
                TextView t2 = new TextView(SearchResultDetailsActivity.this);
                t2.setText(Html.fromHtml(result.getDetails().get(i)[1]));
                row.addView(t2); 
                td.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        	}
        	
        	TableLayout tc = (TableLayout) findViewById(R.id.tlExemplare);
        	
        	for(int i = 0; i < result.getCopies().size(); i++){
                TableRow row = new TableRow(SearchResultDetailsActivity.this);

                TextView t1 = new TextView(SearchResultDetailsActivity.this);
                t1.setText(Html.fromHtml(result.getCopies().get(i)[0]+"<br />"+result.getCopies().get(i)[1]));
                row.addView(t1); 
                
                TextView t2 = new TextView(SearchResultDetailsActivity.this);
                String status = result.getCopies().get(i)[4]+"<br />";
                if(!result.getCopies().get(i)[5].equals("")){
                	status = status + getString(R.string.ret) + ": "+result.getCopies().get(i)[5]+"<br />";
                }
                t2.setPadding(10, 0, 0, 0);
            	status = status + getString(R.string.res) + ": "+result.getCopies().get(i)[6];
                t2.setText(Html.fromHtml(status));
                row.addView(t2); 
                
                tc.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        	}
        	
        	if(item.isReservable()){
        		Button btres = (Button) findViewById(R.id.btReserve);
        		btres.setVisibility(View.VISIBLE);
                btres.setOnClickListener(new OnClickListener() {
        			@Override
        			public void onClick(View arg0) {
        				reservation();
        			}
                });
        	}
        	
        	dialog.dismiss();

        }
    }
	public class ResTask extends OpacTask<Integer> {			
        	protected Integer doInBackground(Object... arg0) {
                app = (OpacClient) arg0[0];
                String zst = (String) arg0[1];
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(app);
                try {
                	if(sp.getString("opac_usernr", "").equals("") || sp.getString("opac_password", "").equals("")){
                		return STATUS_NOUSER;
                	}else{
    					app.ohc.reservation(zst, sp.getString("opac_usernr", ""), sp.getString("opac_password", ""));
                	}
				} catch (Exception e) {
	    			publishProgress(e, "ioerror");
				}
        		return STATUS_SUCCESS;
        	}
        	
            protected void onPostExecute(Integer result) {
            	reservation_done(result);
            }
    }
}
