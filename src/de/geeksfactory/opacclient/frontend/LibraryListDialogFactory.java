/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.acra.ACRA;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.ExpandableListView;
import org.holoeverywhere.widget.ExpandableListView.OnChildClickListener;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.WrapperListAdapter;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class LibraryListDialogFactory {

	public static AlertDialog newInstance(final Activity ctx,
			final OpacClient app, final boolean welcome) {

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		// Get the layout inflater
		LayoutInflater inflater = ctx.getLayoutInflater();

		final List<Library> libraries;

		View view = inflater.inflate(R.layout.dialog_library_select, null);

		final ExpandableListView lv = (ExpandableListView) view
				.findViewById(R.id.lvBibs);
		final ListView slv = (ListView) view.findViewById(R.id.lvSimple);
		try {
			libraries = app.getLibraries();
		} catch (IOException e) {
			ACRA.getErrorReporter().handleException(e);
			return null;
		}
		final LibraryListAdapter la = new LibraryListAdapter(ctx);
		Collections.sort(libraries);
		for (Library lib : libraries) {
			la.addItem(lib);
		}
		lv.setAdapter(la);

		final TextView tvLocateString = (TextView) view
				.findViewById(R.id.tvLocateString);

		final ImageView ivLocationIcon = (ImageView) view
				.findViewById(R.id.ivLocationIcon);

		final LinearLayout llLocate = (LinearLayout) view
				.findViewById(R.id.llLocate);

		final LocationManager locationManager = (LocationManager) ctx
				.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); // no GPS
		final String provider = locationManager.getBestProvider(criteria, true);
		if (provider == null) // no geolocation available
			llLocate.setVisibility(View.GONE);

		llLocate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (lv.getVisibility() == View.VISIBLE) {
					tvLocateString.setText(R.string.geolocate_progress);
					ivLocationIcon.setImageResource(R.drawable.ic_locate);

					if (provider == null)
						return;
					locationManager.requestLocationUpdates(provider, 0, 0,
							new LocationListener() {
								@Override
								public void onStatusChanged(String provider,
										int status, Bundle extras) {
								}

								@Override
								public void onProviderEnabled(String provider) {
								}

								@Override
								public void onProviderDisabled(String provider) {
								}

								@Override
								public void onLocationChanged(Location location) {
									if (location != null) {
										double lat = location.getLatitude();
										double lon = location.getLongitude();
										// Calculate distances
										List<Library> distancedlibs = new ArrayList<Library>();
										for (Library lib : libraries) {
											float[] result = new float[1];
											double[] geo = lib.getGeo();
											if (geo == null)
												continue;
											Location.distanceBetween(lat, lon,
													geo[0], geo[1], result);
											lib.setGeo_distance(result[0]);
											distancedlibs.add(lib);
										}
										Collections.sort(distancedlibs,
												new DistanceComparator());
										distancedlibs = distancedlibs.subList(
												0, 20);
										PlainLibraryListAdapter la = new PlainLibraryListAdapter(
												ctx, distancedlibs);
										slv.setAdapter(la);
										tvLocateString
												.setText(R.string.alphabetic_list);
										ivLocationIcon
												.setImageResource(R.drawable.ic_list);
										slv.setVisibility(View.VISIBLE);
										lv.setVisibility(View.GONE);

									}
								}
							});
				} else {
					lv.setVisibility(View.VISIBLE);
					slv.setVisibility(View.GONE);
					tvLocateString.setText(R.string.geolocate);
					ivLocationIcon.setImageResource(R.drawable.ic_locate);
				}
			}
		});

		builder.setView(view).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		final AlertDialog dialog = builder.create();

		lv.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView arg0, View arg1,
					int groupPosition, int childPosition, long arg4) {
				AccountDataSource data = new AccountDataSource(ctx);
				data.open();
				Account acc = new Account();
				acc.setLibrary(la.getChild(groupPosition, childPosition)
						.getIdent());
				acc.setLabel(ctx.getString(R.string.default_account_name));
				long insertedid = data.addAccount(acc);
				data.close();
				dialog.dismiss();

				app.setAccount(insertedid);

				Intent i = new Intent(ctx, AccountEditActivity.class);
				i.putExtra("id", insertedid);
				i.putExtra("adding", true);
				if (welcome)
					i.putExtra("welcome", true);
				ctx.startActivity(i);
				return false;
			}
		});
		slv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (getAdapter(slv) instanceof ArrayAdapter) {
					AccountDataSource data = new AccountDataSource(ctx);
					data.open();
					Account acc = new Account();
					acc.setLibrary(((Library) getAdapter(slv).getItem(position))
							.getIdent());
					acc.setLabel(ctx.getString(R.string.default_account_name));
					long insertedid = data.addAccount(acc);
					data.close();
					dialog.dismiss();

					app.setAccount(insertedid);

					Intent i = new Intent(ctx, AccountEditActivity.class);
					i.putExtra("id", insertedid);
					i.putExtra("adding", true);
					if (welcome)
						i.putExtra("welcome", true);
					ctx.startActivity(i);
				}
			}
		});
		return dialog;
	}

	public static class DistanceComparator implements Comparator<Library> {
		@Override
		public int compare(Library o1, Library o2) {
			return ((Float) o1.getGeo_distance()).compareTo(o2
					.getGeo_distance());
		}
	}

	public static Adapter getAdapter(ListView lv) {
		Adapter a = lv.getAdapter();
		if (a instanceof WrapperListAdapter) {
			a = ((WrapperListAdapter) a).getWrappedAdapter();
		}

		return a;
	}
}
