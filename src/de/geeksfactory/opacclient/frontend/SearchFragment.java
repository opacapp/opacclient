package de.geeksfactory.opacclient.frontend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.CheckBox;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.Spinner;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;

public class SearchFragment extends Fragment implements AccountSelectedListener {
	protected SharedPreferences sp;

	public interface Callback {
		public void scanBarcode();
	}

	protected Callback mCallback;
	protected View view;
	protected OpacClient app;
	protected Bundle savedState;

	protected boolean advanced = false;
	protected List<SearchField> fields;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_search, container, false);

		setHasOptionsMenu(true);

		setRetainInstance(true);

		sp = ((OpacActivity) getActivity()).getDefaultSharedPreferences();
		app = (OpacClient) getActivity().getApplication();

		// if (getIntent().getBooleanExtra("barcode", false)) {
		// BarcodeScanIntegrator integrator = new BarcodeScanIntegrator(
		// SearchActivity.this);
		// integrator.initiateScan();
		// } else {
		// ArrayAdapter<CharSequence> order_adapter = ArrayAdapter
		// .createFromResource(this, R.array.orders,
		// R.layout.simple_spinner_item);
		// order_adapter
		// .setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		// ((Spinner) SearchActivity.this.findViewById(R.id.cbOrder))
		// .setAdapter(order_adapter);
		// }

//TODO:		ImageView ivBarcode = (ImageView) view.findViewById(R.id.ivBarcode);
//		ivBarcode.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
//				mCallback.scanBarcode();
//			}
//		});

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (!(app.getLibrary() == null)) {
			accountSelected(app.getAccount());
		}
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("query")) {
			savedState = savedInstanceState.getBundle("query");
		}
	}

	public void clear() {
//		TODO: rewrite for new SearchField implementation
//		((EditText) view.findViewById(R.id.etSimpleSearch)).setText("");
//		((EditText) view.findViewById(R.id.etTitel)).setText("");
//		((EditText) view.findViewById(R.id.etVerfasser)).setText("");
//		((EditText) view.findViewById(R.id.etSchlagA)).setText("");
//		((EditText) view.findViewById(R.id.etSchlagB)).setText("");
//		((EditText) view.findViewById(R.id.etBarcode)).setText("");
//		((EditText) view.findViewById(R.id.etISBN)).setText("");
//		((EditText) view.findViewById(R.id.etJahr)).setText("");
//		((EditText) view.findViewById(R.id.etJahrBis)).setText("");
//		((EditText) view.findViewById(R.id.etJahrVon)).setText("");
//		((EditText) view.findViewById(R.id.etSystematik)).setText("");
//		((EditText) view.findViewById(R.id.etInteressenkreis)).setText("");
//		((EditText) view.findViewById(R.id.etVerlag)).setText("");
//		((CheckBox) view.findViewById(R.id.cbDigital)).setChecked(false);
//		((CheckBox) view.findViewById(R.id.cbAvailable)).setChecked(false);
//		((Spinner) view.findViewById(R.id.cbBranch)).setSelection(0);
//		((Spinner) view.findViewById(R.id.cbHomeBranch)).setSelection(0);
//		((Spinner) view.findViewById(R.id.cbMediengruppe)).setSelection(0);
	}

	protected void manageVisibility() {
		if (app.getLibrary().getReplacedBy() != null
				&& sp.getInt("annoyed", 0) < 5) {
			view.findViewById(R.id.rlReplaced).setVisibility(View.VISIBLE);
			view.findViewById(R.id.ivReplacedStore).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							try {
								Intent i = new Intent(Intent.ACTION_VIEW, Uri
										.parse("market://details?id="
												+ app.getLibrary()
														.getReplacedBy()));
								startActivity(i);
							} catch (ActivityNotFoundException e) {
								Log.i("play", "no market installed");
							}
						}
					});
			sp.edit().putInt("annoyed", sp.getInt("annoyed", 0) + 1).commit();
		} else {
			view.findViewById(R.id.rlReplaced).setVisibility(View.GONE);
		}
		
		LinearLayout llFormFields = (LinearLayout) view.findViewById(R.id.llFormFields);
		llFormFields.removeAllViews();
		
		RelativeLayout rlSimpleSearch = (RelativeLayout) view.findViewById(R.id.rlSimpleSearch);
		TextView tvSearchAdvHeader = (TextView) view.findViewById(R.id.tvSearchAdvHeader);
		EditText etSimpleSearch = (EditText) view.findViewById(R.id.etSimpleSearch);
		rlSimpleSearch.setVisibility(View.GONE);
		tvSearchAdvHeader.setVisibility(View.GONE);
		
		for (SearchField field:fields) {
			View v = null;
			if (field instanceof TextSearchField) {
				TextSearchField textSearchField = (TextSearchField) field;
				if (textSearchField.isFreeSearch()) {
					rlSimpleSearch.setVisibility(View.VISIBLE);
					tvSearchAdvHeader.setVisibility(View.VISIBLE);
					etSimpleSearch.setHint(textSearchField.getHint());
				} else {
					v = getLayoutInflater().inflate(
							R.layout.searchfield_text, llFormFields, false);
					TextView title = (TextView) v.findViewById(R.id.title);
					title.setText(textSearchField.getDisplayName());
					EditText edittext = (EditText) v.findViewById(R.id.edittext);
					edittext.setHint(textSearchField.getHint());
					//TODO: Implementation for half-width search fields
				}
			} else if (field instanceof BarcodeSearchField) {
				BarcodeSearchField bcSearchField = (BarcodeSearchField) field;
				v = getLayoutInflater().inflate(
						R.layout.searchfield_barcode, llFormFields, false);
				TextView title = (TextView) v.findViewById(R.id.title);
				title.setText(bcSearchField.getDisplayName());
				EditText edittext = (EditText) v.findViewById(R.id.edittext);
				edittext.setHint(bcSearchField.getHint());
				//TODO: Implementation for half-width search fields
			} else if (field instanceof DropdownSearchField) {
				DropdownSearchField ddSearchField = (DropdownSearchField) field;
				v = getLayoutInflater().inflate(
						R.layout.searchfield_dropdown, llFormFields, false);
				TextView title = (TextView) v.findViewById(R.id.title);
				title.setText(ddSearchField.getDisplayName());
				Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				spinner.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
						getActivity(), ddSearchField.getDropdownValues(), R.layout.simple_spinner_item));
			} else if (field instanceof CheckboxSearchField) {
				CheckboxSearchField cbSearchField = (CheckboxSearchField) field;
				v = getLayoutInflater().inflate(
						R.layout.searchfield_checkbox, llFormFields, false);
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				checkbox.setText(cbSearchField.getDisplayName());
			}
			if (v != null) {
				v.setTag(field.getId());
				llFormFields.addView(v);
			}
		}
	}

	@Override
	public void accountSelected(Account account) {
		advanced = sp.getBoolean("advanced", false);
		new LoadSearchFieldsTask().execute();
	}
	
	protected void progress(boolean on) {
		view.findViewById(R.id.progressBar).setVisibility(on ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.scroll).setVisibility(on ? View.GONE : View.VISIBLE);
	}
	
	protected class LoadSearchFieldsTask extends AsyncTask<Void, Void, List<SearchField>> {

		@Override
		protected void onPreExecute() {
			progress(true);
		}
		
		@Override
		protected List<SearchField> doInBackground(Void... arg0) {
			try {
				return app.getApi().getSearchFields(new SQLMetaDataSource(app), app.getLibrary());
			} catch (OpacErrorException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<SearchField> fields) {
			progress(false);
			if (fields != null) {
				SearchFragment.this.fields = fields;
				manageVisibility();
				if (savedState != null)
					loadQuery(savedState);
			}
		}
		
	}

	public void go() {
		app.startSearch(getActivity(), saveQuery());
	}

	public Map<String, String> saveQuery() {
		Map<String, String> query = new HashMap<String, String>();
		for (SearchField field:fields) {
			ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
			if (field instanceof TextSearchField) {
				EditText text;
				if (((TextSearchField) field).isFreeSearch()) {
					text = (EditText) view.findViewById(R.id.etSimpleSearch);
				} else {
					text = (EditText) v.findViewById(R.id.edittext);
				}
				query.put(field.getId(), text.getEditableText().toString());
			} else if (field instanceof BarcodeSearchField) {
				EditText text = (EditText) v.findViewById(R.id.edittext);
				query.put(field.getId(), text.getEditableText().toString());
			} else if (field instanceof DropdownSearchField) {
				Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				query.put(field.getId(), 
						((DropdownSearchField) field).getDropdownValues()
						.get(spinner.getSelectedItemPosition()).get("key"));
			} else if (field instanceof CheckboxSearchField) {
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				query.put(field.getId(), String.valueOf(checkbox.isChecked()));
			}
		}
		return query;
	}

	public void loadQuery(Bundle query) {
		for (SearchField field:fields) {
			ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
			if (field instanceof TextSearchField) {
				EditText text;
				if (((TextSearchField) field).isFreeSearch()) {
					text = (EditText) view.findViewById(R.id.etSimpleSearch);
				} else {
					text = (EditText) v.findViewById(R.id.edittext);				
				}
				text.setText(query.getString(field.getId()));
			} else if (field instanceof BarcodeSearchField) {
				EditText text = (EditText) v.findViewById(R.id.edittext);
				text.setText(query.getString(field.getId()));
			} else if (field instanceof DropdownSearchField) {
				Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				int i = 0;
				for (Map<String, String> map:((DropdownSearchField) field).getDropdownValues()) {
					if (map.get("key").equals(query.getString(field.getId()))) {
						spinner.setSelection(i);
						break;
					}	
					i++;
				}
			} else if (field instanceof CheckboxSearchField) {
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				checkbox.setChecked(Boolean.valueOf(query.getString(field.getId())));
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallback = (Callback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement SearchFragment.Callback");
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_search, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_search_go) {
			go();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		savedState = OpacClient.mapToBundle(saveQuery());
		outState.putBundle("query", savedState);
		super.onSaveInstanceState(outState);
	}

}
