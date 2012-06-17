package de.geeksfactory.opacclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class OpacActivity extends Activity {
	protected OpacClient app;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        app = (OpacClient) getApplication();
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		Intent intent;
	    switch (item.getItemId()) {
	        case R.id.menu_prefs:
	            intent = new Intent(OpacActivity.this, MainPreferenceActivity.class);
	            startActivity(intent);
	            return true;
	        case R.id.menu_account:
	            intent = new Intent(OpacActivity.this, AccountActivity.class);
	            startActivity(intent);
	            return true;
	        case R.id.menu_about:
	            intent = new Intent(OpacActivity.this, AboutActivity.class);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	protected void dialog_no_user(){
		dialog_no_user(false);
	}
	
	protected void dialog_no_user(final boolean finish) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.status_nouser)
    	       .setCancelable(false)
    	       .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	                if(finish) finish();
    	           }
    	       })
    	       .setPositiveButton(R.string.prefs, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    		            Intent intent = new Intent(OpacActivity.this, MainPreferenceActivity.class);
    		            startActivity(intent);
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
	}
	
	protected void dialog_wrong_credentials(String s, final boolean finish) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(getString(R.string.opac_error)+" "+s)
    	       .setCancelable(false)
    	       .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	                if(finish) finish();
    	           }
    	       })
    	       .setPositiveButton(R.string.prefs, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    		            Intent intent = new Intent(OpacActivity.this, MainPreferenceActivity.class);
    		            startActivity(intent);
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
	}
	
	protected void dialog_fatal_error(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(id)
    	       .setCancelable(false)
    	       .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	                finish();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
	}
	
	protected void dialog_fatal_error(String s) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(s)
    	       .setCancelable(false)
    	       .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	                finish();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
	}
}
