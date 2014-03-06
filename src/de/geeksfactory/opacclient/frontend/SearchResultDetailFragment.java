package de.geeksfactory.opacclient.frontend;

import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.acra.ACRA;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.app.ProgressDialog;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.R.id;
import de.geeksfactory.opacclient.R.layout;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.apis.EbookServiceApi.BookingResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;
import de.geeksfactory.opacclient.frontend.SearchResultListFragment.Callbacks;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.storage.StarDataSource;

/**
 * A fragment representing a single SearchResult detail screen. This fragment is
 * either contained in a {@link SearchResultListActivity} in two-pane mode (on
 * tablets) or a {@link SearchResultDetailActivity} on handsets.
 */
public class SearchResultDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	public static final String ARG_ITEM_NR = "item_nr";

	/**
	 * The detailled item that this fragment
	 * represents.
	 */
	private DetailledItem item;	
	private String title;
	private String id;
	
	private OpacClient app;
	private View view;
	
	private FetchTask ft;
	private FetchSubTask fst;
	private ResTask rt;
	private BookingTask bt;
	private ProgressDialog dialog;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SearchResultDetailFragment() {
	}
	
	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;
	
	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when the fragment should be deleted
		 */
		public void removeFragment();
	}
	
	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void removeFragment() {
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID) || getArguments().containsKey(ARG_ITEM_NR)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			load(getArguments().getInt(ARG_ITEM_NR), getArguments().getString(ARG_ITEM_ID));
		}
	}
	
	private void load(int nr, String id) {
		if (id != null && !id.equals("")) {
			Log.d("Opac", "load id");
			this.id = id;
			fst = new FetchSubTask();
			fst.execute(app, id);
		} else {
			Log.d("Opac", "load nr");
			ft = new FetchTask();
			ft.execute(app, nr);
		}
	}
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OpacClient) activity.getApplication();
		
		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_searchresult_detail,
				container, false);
		view = rootView;
		return rootView;
	}
	
	
	public class FetchTask extends OpacTask<DetailledItem> {
		protected boolean success = true;

		@Override
		protected DetailledItem doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			Integer nr = (Integer) arg0[1];

			try {
				DetailledItem res = app.getApi().getResult(nr);
				URL newurl;
				if (res.getCover() != null && res.getCoverBitmap() == null) {
					try {
						newurl = new URL(res.getCover());
						Bitmap mIcon_val = BitmapFactory.decodeStream(newurl
								.openConnection().getInputStream());
						if(mIcon_val.getHeight() > 1 && mIcon_val.getWidth() > 1) {
							res.setCoverBitmap(mIcon_val);
						} else {
							//When images embedded from Amazon aren't available, a 1x1
							//pixel image is returned (iOPAC)
							res.setCover(null);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				success = false;
				e.printStackTrace();
			} catch (java.net.SocketException e) {
				success = false;
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return null;
		}

		@Override
		@SuppressLint("NewApi")
		protected void onPostExecute(DetailledItem result) {
//			if (!success || result == null) {
//				setContentView(R.layout.connectivity_error);
//				((Button) findViewById(R.id.btRetry))
//						.setOnClickListener(new OnClickListener() {
//							@Override
//							public void onClick(View v) {
//								load();
//							}
//						});
//				return;
//			}

			item = result;

			try {
				Log.i("result", getItem().toString());
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			ImageView iv = (ImageView) view.findViewById(R.id.ivCover);

			if (getItem().getCoverBitmap() != null) {
				iv.setVisibility(View.VISIBLE);
				iv.setImageBitmap(getItem().getCoverBitmap());
			} else {
				iv.setVisibility(View.GONE);
			}

			TextView tvTitel = (TextView) view.findViewById(R.id.tvTitle);
			tvTitel.setText(getItem().getTitle());
			getActivity().setTitle(getItem().getTitle());

			LinearLayout llDetails = (LinearLayout) view.findViewById(R.id.llDetails);
			for (Detail detail : result.getDetails()) {
				View v = getLayoutInflater().inflate(R.layout.detail_listitem,
						null);
				((TextView) v.findViewById(R.id.tvDesc)).setText(detail
						.getDesc());
				((TextView) v.findViewById(R.id.tvContent)).setText(detail
						.getContent());
				Linkify.addLinks((TextView) v.findViewById(R.id.tvContent),
						Linkify.WEB_URLS);
				llDetails.addView(v);
			}

			LinearLayout llCopies = (LinearLayout) view.findViewById(R.id.llCopies);
			if (result.getVolumesearch() != null) {
				TextView tvC = (TextView) view.findViewById(R.id.tvCopies);
				tvC.setText(R.string.baende);
				Button btnVolume = new Button(getActivity());
				btnVolume.setText(R.string.baende_volumesearch);
				btnVolume.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						app.startSearch(getActivity(),
								getItem().getVolumesearch());
					}
				});
				llCopies.addView(btnVolume);

			} else if (result.getBaende().size() > 0) {
				TextView tvC = (TextView) view.findViewById(R.id.tvCopies);
				tvC.setText(R.string.baende);

				for (final ContentValues band : result.getBaende()) {
					View v = getLayoutInflater().inflate(
							R.layout.band_listitem, null);
					((TextView) v.findViewById(R.id.tvTitel)).setText(band
							.getAsString(DetailledItem.KEY_CHILD_TITLE));

					v.findViewById(R.id.llItem).setOnClickListener(
							new OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent intent = new Intent(
											getActivity(),
											SearchResultDetailActivity.class);
									intent.putExtra(
											ARG_ITEM_ID,
											band.getAsString(DetailledItem.KEY_CHILD_ID));
									intent.putExtra("from_collection", true);
									startActivity(intent);
								}
							});
					llCopies.addView(v);
				}
			} else {
				if (result.getCopies().size() == 0) {
					view.findViewById(R.id.tvCopies).setVisibility(View.GONE);
				} else {
					for (ContentValues copy : result.getCopies()) {
						View v = getLayoutInflater().inflate(
								R.layout.copy_listitem, null);

						if (v.findViewById(R.id.tvBranch) != null) {
							if (copy.containsKey(DetailledItem.KEY_COPY_BRANCH)) {
								((TextView) v.findViewById(R.id.tvBranch))
										.setText(copy
												.getAsString(DetailledItem.KEY_COPY_BRANCH));
								((TextView) v.findViewById(R.id.tvBranch))
										.setVisibility(View.VISIBLE);
							} else {
								((TextView) v.findViewById(R.id.tvBranch))
										.setVisibility(View.GONE);
							}
						}
						if (v.findViewById(R.id.tvDepartment) != null) {
							if (copy.containsKey(DetailledItem.KEY_COPY_DEPARTMENT)) {
								((TextView) v.findViewById(R.id.tvDepartment))
										.setText(copy
												.getAsString(DetailledItem.KEY_COPY_DEPARTMENT));
								((TextView) v.findViewById(R.id.tvDepartment))
										.setVisibility(View.VISIBLE);
							} else {
								((TextView) v.findViewById(R.id.tvDepartment))
										.setVisibility(View.GONE);
							}
						}
						if (v.findViewById(R.id.tvLocation) != null) {
							if (copy.containsKey(DetailledItem.KEY_COPY_LOCATION)) {
								((TextView) v.findViewById(R.id.tvLocation))
										.setText(copy
												.getAsString(DetailledItem.KEY_COPY_LOCATION));
								((TextView) v.findViewById(R.id.tvLocation))
										.setVisibility(View.VISIBLE);
							} else {
								((TextView) v.findViewById(R.id.tvLocation))
										.setVisibility(View.GONE);
							}
						}
						if (v.findViewById(R.id.tvShelfmark) != null) {
							if (copy.containsKey(DetailledItem.KEY_COPY_SHELFMARK)) {
								((TextView) v.findViewById(R.id.tvShelfmark))
										.setText(copy
												.getAsString(DetailledItem.KEY_COPY_SHELFMARK));
								((TextView) v.findViewById(R.id.tvShelfmark))
										.setVisibility(View.VISIBLE);
							} else {
								((TextView) v.findViewById(R.id.tvShelfmark))
										.setVisibility(View.GONE);
							}
						}
						if (v.findViewById(R.id.tvStatus) != null) {
							if (copy.containsKey(DetailledItem.KEY_COPY_STATUS)) {
								((TextView) v.findViewById(R.id.tvStatus))
										.setText(copy
												.getAsString(DetailledItem.KEY_COPY_STATUS));
								((TextView) v.findViewById(R.id.tvStatus))
										.setVisibility(View.VISIBLE);
							} else {
								((TextView) v.findViewById(R.id.tvStatus))
										.setVisibility(View.GONE);
							}
						}

						if (v.findViewById(R.id.tvReservations) != null) {
							if (copy.containsKey(DetailledItem.KEY_COPY_RESERVATIONS)) {
								((TextView) v.findViewById(R.id.tvReservations))
										.setText(getString(R.string.res)
												+ ": "
												+ copy.getAsString(DetailledItem.KEY_COPY_RESERVATIONS));
								((TextView) v.findViewById(R.id.tvReservations))
										.setVisibility(View.VISIBLE);
							} else {
								((TextView) v.findViewById(R.id.tvReservations))
										.setVisibility(View.GONE);
							}
						}
						if (v.findViewById(R.id.tvReturndate) != null) {
							if (copy.containsKey(DetailledItem.KEY_COPY_RETURN)
									&& !"".equals(copy
											.getAsString(DetailledItem.KEY_COPY_RETURN))) {
								((TextView) v.findViewById(R.id.tvReturndate))
										.setText(getString(R.string.ret)
												+ ": "
												+ copy.getAsString(DetailledItem.KEY_COPY_RETURN));
								((TextView) v.findViewById(R.id.tvReturndate))
										.setVisibility(View.VISIBLE);
							} else {
								((TextView) v.findViewById(R.id.tvReturndate))
										.setVisibility(View.GONE);
							}
						}

						llCopies.addView(v);
					}
				}
			}

			if (id == null || id.equals("")) {
				id = getItem().getId();
			}

//TODO:			if (getIntent().hasExtra("reservation")
//					&& getIntent().getBooleanExtra("reservation", false))
//				reservationStart();
		}
	}

	protected void dialog_wrong_credentials(String s, final boolean finish) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.opac_error) + " " + s)
				.setCancelable(false)
				.setNegativeButton(R.string.dismiss,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								if (finish)
									mCallbacks.removeFragment();
							}
						})
				.setPositiveButton(R.string.prefs,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(
										getActivity(),
										AccountEditActivity.class);
								intent.putExtra(
										AccountEditActivity.EXTRA_ACCOUNT_ID,
										app.getAccount().getId());
								startActivity(intent);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public class FetchSubTask extends FetchTask {
		@Override
		protected DetailledItem doInBackground(Object... arg0) {
			this.a = (OpacClient) arg0[0];
			String a = (String) arg0[1];

			try {
				SharedPreferences sp = PreferenceManager
						.getDefaultSharedPreferences(getActivity());
				String homebranch = sp.getString(
						OpacClient.PREF_HOME_BRANCH_PREFIX
								+ app.getAccount().getId(), null);

//TODO:				if (getIntent().hasExtra("reservation")
//						&& getIntent().getBooleanExtra("reservation", false))
//					app.getApi().start();

				DetailledItem res = app.getApi().getResultById(a, homebranch);
				URL newurl;
				try {
					newurl = new URL(res.getCover());
					Bitmap mIcon_val = BitmapFactory.decodeStream(newurl
							.openConnection().getInputStream());
					res.setCoverBitmap(mIcon_val);
				} catch (Exception e) {
					e.printStackTrace();
				}
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
				e.printStackTrace();
			} catch (java.lang.IllegalStateException e) {
				success = false;
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				publishProgress(e, "ioerror");
			}

			return null;
		}
	}

	public class ResTask extends OpacTask<ReservationResult> {
		private boolean success;

		@Override
		protected ReservationResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			DetailledItem item = (DetailledItem) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				ReservationResult res = app.getApi().reservation(
						item, app.getAccount(), useraction,
						selection);
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.net.SocketException e) {
				success = false;
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(ReservationResult res) {
			dialog.dismiss();

			if (!success || res == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(R.string.connection_error)
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
				return;
			}

			//TODO: reservationResult(res);
		}
	}

	public class BookingTask extends OpacTask<BookingResult> {
		private boolean success;

		@Override
		protected BookingResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			String reservation_info = (String) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				BookingResult res = ((EbookServiceApi) app.getApi()).booking(
						reservation_info, app.getAccount(), useraction,
						selection);
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.net.SocketException e) {
				success = false;
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(BookingResult res) {
			dialog.dismiss();

			if (!success || res == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(R.string.connection_error)
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
				return;
			}

			//TODO:bookingResult(res);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (dialog != null) {
			if (dialog.isShowing()) {
				dialog.cancel();
			}
		}
		try {
			if (ft != null) {
				if (!ft.isCancelled()) {
					ft.cancel(true);
				}
			}
			if (fst != null) {
				if (!fst.isCancelled()) {
					fst.cancel(true);
				}
			}
			if (rt != null) {
				if (!rt.isCancelled()) {
					rt.cancel(true);
				}
			}
			if (bt != null) {
				if (!bt.isCancelled()) {
					bt.cancel(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		OpacActivity.unbindDrawables(view.findViewById(R.id.rootView));
		System.gc();
	}

	
	public DetailledItem getItem() {
		return item;
	}


	
}
