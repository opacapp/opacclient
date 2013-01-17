package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import com.actionbarsherlock.ActionBarSherlock.Implementation;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NavigationFragment extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.sliding_navigation, null);

		v.findViewById(R.id.llNavSearch).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								SearchActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavAccount).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								AccountActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavStarred).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								StarredActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavInfo).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								InfoActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavSettings).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								MainPreferenceActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavAbout).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								AboutActivity.class);
						startActivity(intent);
					}
				});

		return v;
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		reload();
	}

	public void reload() {
		Library lib = ((OpacClient) getActivity().getApplication())
				.getLibrary();
		if (lib != null) {
			TextView tvBn = (TextView) getView().findViewById(R.id.tvLibrary);
			if (lib.getTitle() != null && !lib.getTitle().equals("null"))
				tvBn.setText(lib.getCity() + " · " + lib.getTitle());
			else
				tvBn.setText(lib.getCity());

			try {
				if (lib.getData().getString("information") != null) {
					if (!lib.getData().getString("information").equals("null")) {
						getView().findViewById(R.id.llNavInfo).setVisibility(
								View.VISIBLE);
					} else {
						getView().findViewById(R.id.llNavInfo).setVisibility(
								View.GONE);
					}
				} else {
					getView().findViewById(R.id.llNavInfo).setVisibility(
							View.GONE);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		LinearLayout llAccountlist = (LinearLayout) getView().findViewById(
				R.id.llAccountlist);
		llAccountlist.removeAllViews();
		AccountDataSource aData = new AccountDataSource(getActivity());
		aData.open();
		List<Account> accounts = aData.getAllAccounts();
		for (final Account account : accounts) {
			View v = getActivity().getLayoutInflater().inflate(
					R.layout.account_listitem_nav, null);
			((TextView) v.findViewById(R.id.tvLabel)).setText(account
					.getLabel());
			Library library;
			try {
				library = ((OpacClient) getActivity().getApplication())
						.getLibrary(account.getBib());
				TextView tvCity = (TextView) v.findViewById(R.id.tvCity);
				if (library.getTitle() != null
						&& !library.getTitle().equals("null")) {
					tvCity.setText(library.getCity() + " · "
							+ library.getTitle());
				} else {
					tvCity.setText(library.getCity());
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			v.findViewById(R.id.llAccount).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							selectaccount(account.getId());
						}
					});
			llAccountlist.addView(v);
		}
		aData.close();
	}

	public void selectaccount(long id) {
		((OpacClient) getActivity().getApplication()).setAccount(id);
		if (getActivity() instanceof OpacActivity) {
			((OpacActivity) getActivity()).accountSelected();
			((OpacActivity) getActivity()).showContent();
			reload();
		}
	}
}
