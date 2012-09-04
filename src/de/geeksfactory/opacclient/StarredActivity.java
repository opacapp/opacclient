package de.geeksfactory.opacclient;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class StarredActivity extends OpacActivity {

	List<Starred> items;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		setContentView(R.layout.starred);

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(StarredActivity.this);
		String bib = sp.getString("opac_bib", "");

		StarDataSource data = new StarDataSource(this);
		data.open();
		items = data.getAllItems(bib);
		data.close();

		ListView lv = (ListView) findViewById(R.id.lvStarred);

		if (items.size() == 0) {
			setContentView(R.layout.starred_empty);
		} else {
			lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					Intent intent = new Intent(StarredActivity.this,
							SearchResultDetailsActivity.class);
					intent.putExtra("item_id", items.get(position).getMNr());
					startActivity(intent);
				}
			});
			lv.setClickable(true);
			lv.setAdapter(new StarredAdapter(StarredActivity.this, (items)));
			lv.setTextFilterEnabled(true);
		}
	}

	public void remove(Starred item) {
		items.remove(item);

		StarDataSource data = new StarDataSource(this);
		data.open();
		data.remove(item);
		data.close();
	}
}
