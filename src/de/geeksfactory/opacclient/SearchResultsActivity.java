package de.geeksfactory.opacclient;

import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

public class SearchResultsActivity extends OpacActivity {
	
	protected ProgressDialog dialog;
	protected List<SearchResult> items;
	private int page = 1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_results);
        
		dialog = ProgressDialog.show(this, "", 
				getString(R.string.loading_results), true, true, new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						finish();
					}
		});
		dialog.show();

		ListView lv = (ListView) findViewById(R.id.lvResults);
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
		        int position, long id) {
		        Intent intent = new Intent(SearchResultsActivity.this, SearchResultDetailsActivity.class);
		        intent.putExtra("item", (int) items.get(position).getNr());
		        startActivity(intent);
		    }
		});
		
        new SearchStartTask().execute(app, 
        			getIntent().getStringExtra("titel"),
        			getIntent().getStringExtra("verfasser"),
        			getIntent().getStringExtra("schlag_a"),
        			getIntent().getStringExtra("schlag_b"),
        			getIntent().getStringExtra("zst"),
        			getIntent().getStringExtra("mg"),
        			getIntent().getStringExtra("isbn"),
        			getIntent().getStringExtra("jahr_von"),
        			getIntent().getStringExtra("jahr_bis"),
        			getIntent().getStringExtra("systematik"),
        			getIntent().getStringExtra("ikr"),
        			getIntent().getStringExtra("verlag"),
        			getIntent().getStringExtra("order")
        		);
        
        final ImageView btPrev = (ImageView) findViewById(R.id.btPrev);
        if(page == 1){
        	btPrev.setVisibility(View.INVISIBLE);
        }
        btPrev.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dialog = ProgressDialog.show(SearchResultsActivity.this, "", 
						getString(R.string.loading_results), true, true, new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface arg0) {
								finish();
							}
				});
				dialog.show();
				page--;
		        new SearchPageTask().execute(app, page);
		        if(page == 1){
		        	btPrev.setVisibility(View.INVISIBLE);
		        }
			}
        });
        
        final ImageView btNext = (ImageView) findViewById(R.id.btNext);
        btNext.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dialog = ProgressDialog.show(SearchResultsActivity.this, "", 
						getString(R.string.loading_results), true, true, new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface arg0) {
								finish();
							}
				});
				dialog.show();
				page++;
		        new SearchPageTask().execute(app, page);
		        if(page > 1){
		        	btPrev.setVisibility(View.VISIBLE);
		        }
			}
        });
    }
    public class SearchStartTask extends OpacTask<List<SearchResult>> {    	
    	@Override
    	protected List<SearchResult> doInBackground(Object... arg0) {
            app = (OpacClient) arg0[0];
            String stichwort = (String) arg0[1];
            String verfasser = (String) arg0[2];
            String schlag_a = (String) arg0[3];
            String schlag_b = (String) arg0[4];
            String zweigstelle = (String) arg0[5];
            String mediengruppe = (String) arg0[6];
            String isbn = (String) arg0[7];
            String jahr_von = (String) arg0[8];
            String jahr_bis = (String) arg0[9];
            String notation = (String) arg0[10];
            String interessenkreis = (String) arg0[11];
            String verlag = (String) arg0[12];
            String order = (String) arg0[13];
            
            try {
    			return app.ohc.search(stichwort, verfasser, schlag_a, schlag_b, zweigstelle, 
    					mediengruppe, isbn, jahr_von, jahr_bis, notation, interessenkreis, verlag, order);
    		} catch (Exception e) {
    			publishProgress(e, "ioerror");
    		}
            
    		return null;
    	} 
        protected void onPostExecute(List<SearchResult> result) {
        	items = result;
        	
        	dialog.dismiss();
        	TextView rn = (TextView) findViewById(R.id.tvResultNum);
        	rn.setText(app.ohc.getResults());
        	
    		ListView lv = (ListView) findViewById(R.id.lvResults);
    		lv.setAdapter(new ResultsAdapter(SearchResultsActivity.this, (result)));
    		lv.setTextFilterEnabled(true);

        }
    }
    public class SearchPageTask extends OpacTask<List<SearchResult>> {

    	@Override
    	protected List<SearchResult> doInBackground(Object... arg0) {
            app = (OpacClient) arg0[0];
            Integer page = (Integer) arg0[1];
            
            try {
    			return app.ohc.search_page(page);
    		} catch (Exception e) {
    			publishProgress(e, "ioerror");
    		}
            
    		return null;
    	}
        protected void onPostExecute(List<SearchResult> result) {
        	items = result;
        	
        	dialog.dismiss();
        	TextView rn = (TextView) findViewById(R.id.tvResultNum);
        	rn.setText(app.ohc.getResults());
        	
    		ListView lv = (ListView) findViewById(R.id.lvResults);
    		lv.setAdapter(new ResultsAdapter(SearchResultsActivity.this, (result)));
    		lv.setTextFilterEnabled(true);

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
