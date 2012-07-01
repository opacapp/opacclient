package de.geeksfactory.opacclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ErrorActivity extends Activity {
	String st = "";
	ProgressDialog dialog;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error);
        Button btRestart = (Button) findViewById(R.id.btRestart);
        btRestart.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				PendingIntent RESTART_INTENT = PendingIntent.getActivity(getBaseContext(), 0, 
						new Intent(ErrorActivity.this, SearchActivity.class), getIntent().getFlags());
				AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, RESTART_INTENT);
				System.exit(2);
			}
        });
        
        TextView tvDetails = (TextView) findViewById(R.id.tvErrorDetails);
        st = getIntent().getExtras().getString("e");
        tvDetails.setText(st);

        Button btSend = (Button) findViewById(R.id.btSend);
        
        TextView tvMsg = (TextView) findViewById(R.id.tvError);
        if(st.startsWith("java.net.UnknownHostException") || getIntent().getExtras().getString("t").equals("offline")){
        	tvMsg.setText(R.string.no_connection);
        }else if(st.startsWith("org.apache.http.NoHttpResponseException")){
            tvMsg.setText(R.string.no_response);
        }else if(st.startsWith("de.geeksfactory.opacclient.NotReachableException")){
            tvMsg.setText(R.string.not_reachable);
        }else{
        	tvMsg.setText(R.string.ioerror);
        	btSend.setVisibility(View.VISIBLE);
        }
        btSend.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog = ProgressDialog.show(ErrorActivity.this, "", 
						getString(R.string.report_sending), true, true, new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface arg0) {
								finish();
							}
				});
				dialog.show();
				new SendTask().execute(this);
			}
        });
    }
    
    @Override
    public void onBackPressed() {
    }
    
	public class SendTask extends AsyncTask<Object, Object, Integer> {
		
    	@Override
    	protected Integer doInBackground(Object... arg0) {
			DefaultHttpClient dc = new DefaultHttpClient();
		    HttpPost httppost = new HttpPost("http://www.raphaelmichel.de/opacclient/crashreport.php"); 
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("traceback", st));
	        try {
	        	nameValuePairs.add(new BasicNameValuePair("version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
	        } catch (Exception e) {
	        	
	        }
	  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ErrorActivity.this);
	        nameValuePairs.add(new BasicNameValuePair("android", android.os.Build.VERSION.RELEASE));
	        nameValuePairs.add(new BasicNameValuePair("sdk", ""+android.os.Build.VERSION.SDK_INT));
	        nameValuePairs.add(new BasicNameValuePair("device", android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL));
	        nameValuePairs.add(new BasicNameValuePair("bib", sp.getString("opac_bib", "Unknown")));

	        try {
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
	        HttpResponse response;
			try {
				response = dc.execute(httppost);
		    	response.getEntity().consumeContent();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return 0;
    	}
    	
        protected void onPostExecute(Integer result) {
        	dialog.dismiss();
            Button btSend = (Button) findViewById(R.id.btSend);
            btSend.setEnabled(false); 
            Toast toast = Toast.makeText(ErrorActivity.this, getString(R.string.report_sent), Toast.LENGTH_SHORT);
            toast.show();
        }
	}
}
