package de.geeksfactory.opacclient;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public abstract class OpacActivity extends SherlockActivity {
	protected OpacClient app;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		this.getSupportActionBar().setHomeButtonEnabled(true);
        app = (OpacClient) getApplication();
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(getString(R.string.prefs))
				.setIcon(R.drawable.ic_menu_preferences)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						Intent intent = new Intent(OpacActivity.this,
								MainPreferenceActivity.class);
						startActivity(intent);
						return true;
					}
				}).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(getString(R.string.about))
				.setIcon(R.drawable.ic_menu_info_details)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						Intent intent = new Intent(OpacActivity.this,
								AboutActivity.class);
						startActivity(intent);
						return true;
					}
				}).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		return true;
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

    protected void unbindDrawables(View view) {
        if (view.getBackground() != null) {
        view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            	unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            if(!(view instanceof AdapterView)){
            	((ViewGroup) view).removeAllViews();
            }
        }
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, FrontpageActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
