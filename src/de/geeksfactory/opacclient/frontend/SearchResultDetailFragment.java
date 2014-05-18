package de.geeksfactory.opacclient.frontend;

import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.app.ProgressDialog;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.apis.EbookServiceApi.BookingResult;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.Callback;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.StepTask;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.storage.AccountDataSource;
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
	 * The detailled item that this fragment represents.
	 */
	private DetailledItem item;
	private String title;
	private String id;
	private Integer nr;

	private OpacClient app;
	private View view;

	private FetchTask ft;
	private FetchSubTask fst;
	private ProgressDialog dialog;
	private AlertDialog adialog;

	private boolean account_switched = false;
	private boolean invalidated = false;
	private boolean progress = false;

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

		setRetainInstance(true);

		if (getArguments().containsKey(ARG_ITEM_ID)
				|| getArguments().containsKey(ARG_ITEM_NR)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			load(getArguments().getInt(ARG_ITEM_NR),
					getArguments().getString(ARG_ITEM_ID));
		}
	}

	public void setProgress(boolean show, boolean animate) {
		progress = show;

		if (view != null) {
			ProgressBar progress = (ProgressBar) view
					.findViewById(R.id.progress);
			View content = view.findViewById(R.id.rootView);

			if (show) {
				if (animate) {
					progress.startAnimation(AnimationUtils.loadAnimation(
							getActivity(), R.anim.fade_in));
					content.startAnimation(AnimationUtils.loadAnimation(
							getActivity(), R.anim.fade_out));
				} else {
					progress.clearAnimation();
					content.clearAnimation();
				}
				progress.setVisibility(View.VISIBLE);
				content.setVisibility(View.GONE);
			} else {
				if (animate) {
					progress.startAnimation(AnimationUtils.loadAnimation(
							getActivity(), R.anim.fade_out));
					content.startAnimation(AnimationUtils.loadAnimation(
							getActivity(), R.anim.fade_in));
				} else {
					progress.clearAnimation();
					content.clearAnimation();
				}
				progress.setVisibility(View.GONE);
				content.setVisibility(View.VISIBLE);
			}
		}
	}

	public void showConnectivityError() {
		ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
		final FrameLayout errorView = (FrameLayout) view
				.findViewById(R.id.error_view);
		errorView.removeAllViews();
		View connError = getActivity().getLayoutInflater().inflate(
				R.layout.error_connectivity, errorView);

		((Button) connError.findViewById(R.id.btRetry))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						errorView.removeAllViews();
						reload();
					}
				});

		progress.startAnimation(AnimationUtils.loadAnimation(getActivity(),
				R.anim.fade_out));
		connError.startAnimation(AnimationUtils.loadAnimation(getActivity(),
				R.anim.fade_in));
		progress.setVisibility(View.GONE);
		connError.setVisibility(View.VISIBLE);
	}

	public void setProgress() {
		setProgress(progress, false);
	}

	private void load(int nr, String id) {
		setProgress(true, true);
		this.id = id;
		this.nr = nr;
		if (id != null && !id.equals("")) {
			fst = new FetchSubTask();
			fst.execute(app, id);
		} else {
			ft = new FetchTask();
			ft.execute(app, nr);
		}
	}

	private void reload() {
		load(nr, id);
	}

	@Override
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
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (item != null) {
			display();
		}
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
		setHasOptionsMenu(true);
		setRetainInstance(true);
		setProgress();
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
						if (mIcon_val.getHeight() > 1
								&& mIcon_val.getWidth() > 1) {
							res.setCoverBitmap(mIcon_val);
						} else {
							// When images embedded from Amazon aren't
							// available, a 1x1
							// pixel image is returned (iOPAC)
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
			} catch (InterruptedIOException e) {
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
			if (getActivity() == null)
				return;

			if (!success || result == null) {
				showConnectivityError();
				return;
			}

			item = result;

			display();

			if (getActivity().getIntent().hasExtra("reservation")
					&& getActivity().getIntent().getBooleanExtra("reservation",
							false))
				reservationStart();
		}
	}

	protected void display() {
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

		LinearLayout llDetails = (LinearLayout) view
				.findViewById(R.id.llDetails);
		llDetails.removeAllViews();
		for (Detail detail : item.getDetails()) {
			View v = getLayoutInflater()
					.inflate(R.layout.listitem_detail, null);
			((TextView) v.findViewById(R.id.tvDesc)).setText(detail.getDesc());
			((TextView) v.findViewById(R.id.tvContent)).setText(detail
					.getContent());
			Linkify.addLinks((TextView) v.findViewById(R.id.tvContent),
					Linkify.WEB_URLS);
			llDetails.addView(v);
		}

		LinearLayout llCopies = (LinearLayout) view.findViewById(R.id.llCopies);
		llCopies.removeAllViews();
		if (item.getVolumesearch() != null) {
			TextView tvC = (TextView) view.findViewById(R.id.tvCopies);
			tvC.setText(R.string.baende);
			Button btnVolume = new Button(getActivity());
			btnVolume.setText(R.string.baende_volumesearch);
			btnVolume.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.startSearch(getActivity(), getItem().getVolumesearch());
				}
			});
			llCopies.addView(btnVolume);

		} else if (item.getBaende().size() > 0) {
			TextView tvC = (TextView) view.findViewById(R.id.tvCopies);
			tvC.setText(R.string.baende);

			for (final Map<String, String> band : item.getBaende()) {
				View v = getLayoutInflater().inflate(R.layout.listitem_volume,
						null);
				((TextView) v.findViewById(R.id.tvTitel)).setText(band
						.get(DetailledItem.KEY_CHILD_TITLE));

				v.findViewById(R.id.llItem).setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(getActivity(),
										SearchResultDetailActivity.class);
								intent.putExtra(ARG_ITEM_ID,
										band.get(DetailledItem.KEY_CHILD_ID));
								intent.putExtra("from_collection", true);
								startActivity(intent);
							}
						});
				llCopies.addView(v);
			}
		} else {
			if (item.getCopies().size() == 0) {
				view.findViewById(R.id.tvCopies).setVisibility(View.GONE);
			} else {
				for (Map<String, String> copy : item.getCopies()) {
					View v = getLayoutInflater().inflate(
							R.layout.listitem_copy, null);

					if (v.findViewById(R.id.tvBranch) != null) {
						if (copy.containsKey(DetailledItem.KEY_COPY_BRANCH)) {
							((TextView) v.findViewById(R.id.tvBranch))
									.setText(copy
											.get(DetailledItem.KEY_COPY_BRANCH));
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
											.get(DetailledItem.KEY_COPY_DEPARTMENT));
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
											.get(DetailledItem.KEY_COPY_LOCATION));
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
											.get(DetailledItem.KEY_COPY_SHELFMARK));
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
											.get(DetailledItem.KEY_COPY_STATUS));
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
											+ copy.get(DetailledItem.KEY_COPY_RESERVATIONS));
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
										.get(DetailledItem.KEY_COPY_RETURN))) {
							((TextView) v.findViewById(R.id.tvReturndate))
									.setText(getString(R.string.ret)
											+ ": "
											+ copy.get(DetailledItem.KEY_COPY_RETURN));
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

		setProgress(false, true);

		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (item != null)
			display();
	}

	protected void dialog_wrong_credentials(String s, final boolean finish) {
		if (getActivity() == null)
			return;
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
								Intent intent = new Intent(getActivity(),
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

				if (getActivity().getIntent().hasExtra("reservation")
						&& getActivity().getIntent().getBooleanExtra(
								"reservation", false))
					app.getApi().start();

				DetailledItem res = app.getApi().getResultById(a, homebranch);
				if (res.getId() == null) {
					res.setId(a);
				}
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

	public class ResTask extends StepTask<ReservationResult> {

		@Override
		protected ReservationResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			DetailledItem item = (DetailledItem) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				ReservationResult res = app.getApi().reservation(item,
						app.getAccount(), useraction, selection);
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.net.SocketException e) {
				e.printStackTrace();
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(ReservationResult res) {
			if (getActivity() == null)
				return;

			if (res == null) {
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

			super.onPostExecute(res);
		}
	}

	public class BookingTask extends StepTask<BookingResult> {

		@Override
		protected BookingResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			DetailledItem item = (DetailledItem) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				BookingResult res = ((EbookServiceApi) app.getApi()).booking(
						item, app.getAccount(), useraction, selection);
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.net.SocketException e) {
				e.printStackTrace();
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(BookingResult res) {
			if (getActivity() == null)
				return;

			if (res == null) {
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

			super.onPostExecute(res);
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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.search_result_details_activity, menu);

		if (item != null) {
			if (item.isReservable()) {
				menu.findItem(R.id.action_reservation).setVisible(true);
			} else {
				menu.findItem(R.id.action_reservation).setVisible(false);
			}
			if (item.isBookable() && app.getApi() instanceof EbookServiceApi) {
				if (((EbookServiceApi) app.getApi()).isEbook(item))
					menu.findItem(R.id.action_lendebook).setVisible(true);
				else
					menu.findItem(R.id.action_lendebook).setVisible(false);
			} else {
				menu.findItem(R.id.action_lendebook).setVisible(false);
			}
			menu.findItem(R.id.action_tocollection).setVisible(
					item.getCollectionId() != null);
		} else {
			menu.findItem(R.id.action_reservation).setVisible(false);
			menu.findItem(R.id.action_lendebook).setVisible(false);
			menu.findItem(R.id.action_tocollection).setVisible(false);
		}

		String bib = app.getLibrary().getIdent();
		StarDataSource data = new StarDataSource(getActivity());
		if ((id == null || id.equals("")) && item != null) {
			if (data.isStarredTitle(bib, title)) {
				menu.findItem(R.id.action_star).setIcon(
						R.drawable.ic_action_star_1);
			}
		} else {
			if (data.isStarred(bib, id)) {
				menu.findItem(R.id.action_star).setIcon(
						R.drawable.ic_action_star_1);
			}
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		final String bib = app.getLibrary().getIdent();
		if (item.getItemId() == R.id.action_reservation) {
			reservationStart();
			return true;
		} else if (item.getItemId() == R.id.action_lendebook) {
			bookingStart();
			return true;
		} else if (item.getItemId() == R.id.action_tocollection) {
			if (getActivity().getIntent().getBooleanExtra("from_collection",
					false)) {
				getActivity().finish();
			} else {
				Intent intent = new Intent(getActivity(),
						SearchResultDetailActivity.class);
				intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
						getItem().getCollectionId());
				startActivity(intent);
				getActivity().finish();
			}
			return true;
		} else if (item.getItemId() == R.id.action_share) {
			if (getItem() == null) {
				Toast toast = Toast.makeText(getActivity(),
						getString(R.string.share_wait), Toast.LENGTH_SHORT);
				toast.show();
			} else {
				final String title = getItem().getTitle();
				final String id = getItem().getId();
				final CharSequence[] items = { getString(R.string.share_link),
						getString(R.string.share_details) };

				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setTitle(R.string.share_dialog_select);
				builder.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int di) {
						String bib = app.getLibrary().getIdent();
						if (di == 0) {
							// Share link
							Intent intent = new Intent(
									android.content.Intent.ACTION_SEND);
							intent.setType("text/plain");
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

							// Add data to the intent, the receiving app will
							// decide
							// what to do with it.
							intent.putExtra(Intent.EXTRA_SUBJECT, title);

							String t = title;
							try {
								bib = java.net.URLEncoder.encode(app
										.getLibrary().getIdent(), "UTF-8");
								t = java.net.URLEncoder.encode(t, "UTF-8");
							} catch (UnsupportedEncodingException e) {
							}

							String shareUrl = app.getApi().getShareUrl(id,
									title);
							if (shareUrl != null) {
								intent.putExtra(Intent.EXTRA_TEXT, shareUrl);
								startActivity(Intent.createChooser(intent,
										getResources()
												.getString(R.string.share)));
							} else {
								Toast toast = Toast.makeText(getActivity(),
										getString(R.string.share_notsupported),
										Toast.LENGTH_SHORT);
								toast.show();
							}
						} else { // Share details
							Intent intent = new Intent(
									android.content.Intent.ACTION_SEND);
							intent.setType("text/plain");
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

							// Add data to the intent, the receiving app will
							// decide
							// what to do with it.
							intent.putExtra(Intent.EXTRA_SUBJECT, title);

							String t = title;
							try {
								bib = java.net.URLEncoder.encode(app
										.getLibrary().getIdent(), "UTF-8");
								t = java.net.URLEncoder.encode(t, "UTF-8");
							} catch (UnsupportedEncodingException e) {
							}

							String text = title + "\n\n";

							for (Detail detail : getItem().getDetails()) {
								String colon = "";
								if (!detail.getDesc().endsWith(":"))
									colon = ":";
								text += detail.getDesc() + colon + "\n"
										+ detail.getContent() + "\n\n";
							}

							String shareUrl = app.getApi().getShareUrl(id,
									title);
							if (shareUrl != null)
								text += shareUrl;

							intent.putExtra(Intent.EXTRA_TEXT, text);
							startActivity(Intent.createChooser(intent,
									getResources().getString(R.string.share)));
						}
					}
				});
				AlertDialog alert = builder.create();

				alert.show();
			}

			return true;
		} else if (item.getItemId() == R.id.action_star) {
			StarDataSource star = new StarDataSource(getActivity());
			if (getItem() == null) {
				Toast toast = Toast.makeText(getActivity(),
						getString(R.string.star_wait), Toast.LENGTH_SHORT);
				toast.show();
			} else if (getItem().getId() == null
					|| getItem().getId().equals("")) {
				final String title = getItem().getTitle();
				if (star.isStarredTitle(bib, title)) {
					star.remove(star.getItemByTitle(bib, title));
					item.setIcon(R.drawable.ic_action_star_0);
				} else {
					star.star(null, title, bib);
					Toast toast = Toast.makeText(getActivity(),
							getString(R.string.starred), Toast.LENGTH_SHORT);
					toast.show();
					item.setIcon(R.drawable.ic_action_star_1);
				}
			} else {
				final String title = getItem().getTitle();
				final String id = getItem().getId();
				if (star.isStarred(bib, id)) {
					star.remove(star.getItem(bib, id));
					item.setIcon(R.drawable.ic_action_star_0);
				} else {
					star.star(id, title, bib);
					Toast toast = Toast.makeText(getActivity(),
							getString(R.string.starred), Toast.LENGTH_SHORT);
					toast.show();
					item.setIcon(R.drawable.ic_action_star_1);
				}
			}
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	public DetailledItem getItem() {
		return item;
	}

	protected void dialog_no_credentials() {
		if (getActivity() == null)
			return;
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.status_nouser)
				.setCancelable(false)
				.setNegativeButton(R.string.dismiss,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						})
				.setPositiveButton(R.string.accounts_edit,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(getActivity(),
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

	protected void reservationStart() {
		if (invalidated) {
			new RestoreSessionTask().execute(false);
		}
		if (app.getApi() instanceof EbookServiceApi) {
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(getActivity());
			if (sp.getString("email", "").equals("")
					&& ((EbookServiceApi) app.getApi()).isEbook(item)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(getString(R.string.opac_error_email))
						.setCancelable(false)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								})
						.setPositiveButton(R.string.prefs,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.dismiss();
										app.toPrefs(getActivity());
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
				return;
			}
		}

		AccountDataSource data = new AccountDataSource(getActivity());
		data.open();
		final List<Account> accounts = data.getAccountsWithPassword(app
				.getLibrary().getIdent());
		data.close();
		if (accounts.size() == 0) {
			dialog_no_credentials();
			return;
		} else if (accounts.size() > 1
				&& !getActivity().getIntent().getBooleanExtra("reservation",
						false)
				&& (app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_CHANGE_ACCOUNT) != 0
				&& !(SearchResultDetailFragment.this.id == null
						|| SearchResultDetailFragment.this.id.equals("null") || SearchResultDetailFragment.this.id
							.equals(""))) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			// Get the layout inflater
			LayoutInflater inflater = getLayoutInflater();

			View view = inflater.inflate(R.layout.dialog_simple_list, null);

			ListView lv = (ListView) view.findViewById(R.id.lvBibs);
			AccountListAdapter adapter = new AccountListAdapter(getActivity(),
					accounts);
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					if (accounts.get(position).getId() != app.getAccount()
							.getId() || account_switched) {

						if (SearchResultDetailFragment.this.id == null
								|| SearchResultDetailFragment.this.id
										.equals("null")
								|| SearchResultDetailFragment.this.id
										.equals("")) {
							Toast.makeText(getActivity(),
									R.string.accchange_sorry, Toast.LENGTH_LONG)
									.show();
						} else {
							app.setAccount(accounts.get(position).getId());
							Intent intent = new Intent(getActivity(),
									SearchResultDetailActivity.class);
							intent.putExtra(
									SearchResultDetailFragment.ARG_ITEM_ID,
									SearchResultDetailFragment.this.id);
							// TODO: refresh fragment instead
							intent.putExtra("reservation", true);
							startActivity(intent);
						}
					} else {
						reservationDo();
					}
					adialog.dismiss();
				}
			});
			builder.setTitle(R.string.account_select)
					.setView(view)
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									adialog.cancel();
								}
							})
					.setNeutralButton(R.string.accounts_edit,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									adialog.dismiss();
									app.openAccountList(getActivity());
								}
							});
			adialog = builder.create();
			adialog.show();
		} else {
			reservationDo();
		}
	}

	public void reservationDo() {
		MultiStepResultHelper msrhReservation = new MultiStepResultHelper(
				getSupportActivity(), item, R.string.doing_res);
		msrhReservation.setCallback(new Callback() {
			@Override
			public void onSuccess(MultiStepResult result) {
				AccountDataSource adata = new AccountDataSource(getActivity());
				adata.open();
				adata.invalidateCachedAccountData(app.getAccount());
				adata.close();
				if (result.getMessage() != null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							getActivity());
					builder.setMessage(result.getMessage())
							.setCancelable(false)
							.setNegativeButton(R.string.dismiss,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									})
							.setPositiveButton(R.string.account,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											Intent intent = new Intent(
													getActivity(), app
															.getMainActivity());
											intent.putExtra("fragment",
													"account");
											getActivity().startActivity(intent);
											getActivity().finish();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
				} else {
					Intent intent = new Intent(getActivity(), app
							.getMainActivity());
					intent.putExtra("fragment", "account");
					getActivity().startActivity(intent);
					getActivity().finish();
				}
			}

			@Override
			public void onError(MultiStepResult result) {
				dialog_wrong_credentials(result.getMessage(), false);
			}

			@Override
			public void onUnhandledResult(MultiStepResult result) {
			}

			@Override
			public void onUserCancel() {
			}

			@Override
			public StepTask<?> newTask() {
				return new ResTask();
			}
		});
		msrhReservation.start();
	}

	protected void bookingStart() {
		AccountDataSource data = new AccountDataSource(getActivity());
		data.open();
		final List<Account> accounts = data.getAccountsWithPassword(app
				.getLibrary().getIdent());
		data.close();
		if (accounts.size() == 0) {
			dialog_no_credentials();
			return;
		} else if (accounts.size() > 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			// Get the layout inflater
			LayoutInflater inflater = getLayoutInflater();

			View view = inflater.inflate(R.layout.dialog_simple_list, null);

			ListView lv = (ListView) view.findViewById(R.id.lvBibs);
			AccountListAdapter adapter = new AccountListAdapter(getActivity(),
					accounts);
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					bookingDo();
					adialog.dismiss();
				}
			});
			builder.setTitle(R.string.account_select)
					.setView(view)
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									adialog.cancel();
								}
							})
					.setNeutralButton(R.string.accounts_edit,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									adialog.dismiss();
									app.openAccountList(getActivity());
								}
							});
			adialog = builder.create();
			adialog.show();
		} else {
			bookingDo();
		}
	}

	public void bookingDo() {
		MultiStepResultHelper msrhBooking = new MultiStepResultHelper(
				getSupportActivity(), item, R.string.doing_res);
		msrhBooking.setCallback(new Callback() {
			@Override
			public void onSuccess(MultiStepResult result) {
				if (getActivity() == null)
					return;
				AccountDataSource adata = new AccountDataSource(getActivity());
				adata.open();
				adata.invalidateCachedAccountData(app.getAccount());
				adata.close();
				Intent intent = new Intent(getActivity(), app.getMainActivity());
				intent.putExtra("fragment", "account");
				getActivity().startActivity(intent);
				getActivity().finish();
			}

			@Override
			public void onError(MultiStepResult result) {
				if (getActivity() == null)
					return;
				dialog_wrong_credentials(result.getMessage(), false);
			}

			@Override
			public void onUnhandledResult(MultiStepResult result) {
			}

			@Override
			public void onUserCancel() {
			}

			@Override
			public StepTask<?> newTask() {
				return new BookingTask();
			}
		});
		msrhBooking.start();
	}

	public class RestoreSessionTask extends OpacTask<Integer> {
		private boolean reservation = true;

		@Override
		protected Integer doInBackground(Object... arg0) {
			reservation = (Boolean) arg0[0];
			try {
				if (id != null) {
					SharedPreferences sp = PreferenceManager
							.getDefaultSharedPreferences(getActivity());
					String homebranch = sp.getString(
							OpacClient.PREF_HOME_BRANCH_PREFIX
									+ app.getAccount().getId(), null);
					app.getApi().getResultById(id, homebranch);
					return 0;
				} else
					ACRA.getErrorReporter().handleException(
							new Throwable("No ID supplied"));
			} catch (java.net.SocketException e) {
				e.printStackTrace();
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			} catch (java.net.UnknownHostException e) {
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			return 1;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (getActivity() == null)
				return;
			if (reservation) {
				reservationDo();
			}
		}

	}

}
