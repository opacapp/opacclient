package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.AutoCompleteTextView;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.EditText;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filter;
import android.widget.Filterable;
import de.geeksfactory.opacclient.R;

public class SuggestLibraryActivity extends Activity {

	private static final String LOG_TAG = "Opac";
	private AutoCompleteTextView etCity;
	private EditText etName;
	private EditText etComment;
	private Button btnSend;
	private City selectedCity = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_suggest_library);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		etCity = (AutoCompleteTextView) findViewById(R.id.etCity);
		etName = (EditText) findViewById(R.id.etName);
		etComment = (EditText) findViewById(R.id.etComment);
		btnSend = (Button) findViewById(R.id.btnSend);

		if (savedInstanceState != null) {
			etCity.setText(savedInstanceState.getCharSequence("city"));
			etName.setText(savedInstanceState.getCharSequence("name"));
			etComment.setText(savedInstanceState.getCharSequence("comment"));
			selectedCity = (City) savedInstanceState
					.getSerializable("selectedCity");
		}

		final PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(
				this, android.R.layout.simple_dropdown_item_1line);
		etCity.setAdapter(adapter);
		etCity.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				selectedCity = adapter.getCity(position);
			}
		});
		etCity.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				selectedCity = null;
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

		});

		btnSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent send = new Intent(Intent.ACTION_SENDTO);
				String uriText = "mailto:"
						+ Uri.encode("info@opacapp.de")
						+ "?subject="
						+ Uri.encode(getResources().getString(
								R.string.library_suggestion)
								+ " "
								+ etCity.getText().toString()
								+ " "
								+ etName.getText().toString()) + "&body="
						+ Uri.encode(createMessage());
				Uri uri = Uri.parse(uriText);

				send.setData(uri);
				startActivity(Intent.createChooser(send, getResources()
						.getString(R.string.select_mail_app)));
				finish();
			}

		});
	}

	private String createMessage() {
		try {
			return createJSON().toString(4) + "\n\n"
					+ etComment.getText().toString();
		} catch (JSONException e) {
			return "";
		}
	}

	protected JSONObject createJSON() {
		JSONObject json = new JSONObject();
		try {
			if (selectedCity != null) {
				json.put("country", selectedCity.country);
				json.put("state", selectedCity.state);
				json.put("city", selectedCity.name);
				JSONArray geo = new JSONArray();
				geo.put(selectedCity.lat);
				geo.put(selectedCity.lon);
				json.put("geo", geo);
			} else {
				json.put("city", etCity.getText().toString());
			}
			json.put("title", etName.getText().toString());
			return json;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putCharSequence("city", etCity.getText());
		outState.putCharSequence("name", etName.getText());
		outState.putCharSequence("comment", etComment.getText());
		if (selectedCity instanceof Serializable)
			outState.putSerializable("selectedCity", selectedCity);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home:
			Intent intent = NavUtils.getParentActivityIntent(this);
			if (getIntent().hasExtra("welcome"))
				intent.putExtra("welcome", true);
			NavUtils.navigateUpTo(this, intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static final String GEOCODE_API = "https://maps.googleapis.com/maps/api/geocode/json";

	private ArrayList<City> autocomplete(String input) {
		ArrayList<City> resultList = null;

		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();
		try {
			StringBuilder sb = new StringBuilder(GEOCODE_API);
			sb.append("?sensor=false");
			sb.append("&language=de");
			sb.append("&region=de");
			sb.append("&address=" + URLEncoder.encode(input, "utf8"));

			URL url = new URL(sb.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Error processing Places API URL", e);
			return resultList;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error connecting to Places API", e);
			return resultList;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {
			// Create a JSON object hierarchy from the results
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			JSONArray resultsJsonArray = jsonObj.getJSONArray("results");

			// Extract the Place descriptions from the results
			resultList = new ArrayList<City>();
			for (int i = 0; i < resultsJsonArray.length(); i++) {
				JSONObject result = resultsJsonArray.getJSONObject(i);
				if (contains(result.getJSONArray("types"), "locality")) {
					JSONArray addressComponents = result
							.getJSONArray("address_components");
					City city = new City();
					for (int j = 0; j < addressComponents.length(); j++) {
						JSONObject component = addressComponents
								.getJSONObject(j);
						if (contains(component.getJSONArray("types"),
								"locality")) {
							city.name = component.getString("long_name");
						} else if (contains(component.getJSONArray("types"),
								"administrative_area_level_1")) {
							city.state = component.getString("long_name");
						} else if (contains(component.getJSONArray("types"),
								"country")) {
							city.country = component.getString("long_name");
						}
					}
					city.lat = result.getJSONObject("geometry")
							.getJSONObject("location").getDouble("lat");
					city.lon = result.getJSONObject("geometry")
							.getJSONObject("location").getDouble("lng");
					resultList.add(city);
				}
			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}

		return resultList;
	}

	private boolean contains(JSONArray array, String string) {
		boolean found = false;
		int i = 0;
		try {
			while (!found && i < array.length()) {
				if (array.getString(i).equals(string))
					found = true;
				i++;
			}
			return found;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static class City implements Serializable {
		private static final long serialVersionUID = 2697131850736622659L;

		public String name;
		public String state;
		public String country;
		public double lat;
		public double lon;
	}

	private class PlacesAutoCompleteAdapter extends ArrayAdapter<String>
			implements Filterable {
		private ArrayList<City> resultList;

		public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		@Override
		public int getCount() {
			return resultList.size();
		}

		@Override
		public String getItem(int index) {
			return resultList.get(index).name + ", "
					+ resultList.get(index).state + ", "
					+ resultList.get(index).country;
		}

		public City getCity(int index) {
			return resultList.get(index);
		}

		@Override
		public Filter getFilter() {
			Filter filter = new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults filterResults = new FilterResults();
					if (constraint != null) {
						// Retrieve the autocomplete results.
						resultList = autocomplete(constraint.toString());

						// Assign the data to the FilterResults
						filterResults.values = resultList;
						filterResults.count = resultList.size();
					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					if (results != null && results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
			};
			return filter;
		}
	}
}
