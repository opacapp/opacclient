package de.geeksfactory.opacclient.frontend;

import java.io.UnsupportedEncodingException;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.Toast;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.storage.StarDataSource;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An activity representing a single SearchResult detail screen. This activity
 * is only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a
 * {@link SearchResultListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link SearchResultDetailFragment}.
 */
public class SearchResultDetailActivity extends OpacActivity implements SearchResultDetailFragment.Callbacks {

	private OpacClient app;
	SearchResultDetailFragment detailFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = (OpacClient) getApplication();

		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putInt(
					SearchResultDetailFragment.ARG_ITEM_NR,
					getIntent().getIntExtra(
							SearchResultDetailFragment.ARG_ITEM_NR, 0));
			if(getIntent().getStringExtra(
					SearchResultDetailFragment.ARG_ITEM_ID) != null)
				arguments.putString(
						SearchResultDetailFragment.ARG_ITEM_ID,
						getIntent().getStringExtra(
								SearchResultDetailFragment.ARG_ITEM_ID));
			detailFragment = new SearchResultDetailFragment();
			detailFragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.searchresult_detail_container, detailFragment).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.search_result_details_activity, menu);

//TODO:		if (item != null) {
//			if (item.isReservable()) {
//				menu.findItem(R.id.action_reservation).setVisible(true);
//			} else {
//				menu.findItem(R.id.action_reservation).setVisible(false);
//			}
//			if (item.isBookable() && app.getApi() instanceof EbookServiceApi) {
//				if (((EbookServiceApi) app.getApi()).isEbook(item))
//					menu.findItem(R.id.action_lendebook).setVisible(true);
//				else
//					menu.findItem(R.id.action_lendebook).setVisible(false);
//			} else {
//				menu.findItem(R.id.action_lendebook).setVisible(false);
//			}
//			menu.findItem(R.id.action_tocollection).setVisible(
//					item.getCollectionId() != null);
//		} else {
//			menu.findItem(R.id.action_reservation).setVisible(false);
//			menu.findItem(R.id.action_lendebook).setVisible(false);
//			menu.findItem(R.id.action_tocollection).setVisible(false);
//		}
//
//		String bib = app.getLibrary().getIdent();
//		StarDataSource data = new StarDataSource(this);
//		if ((id == null || id.equals("")) && item != null) {
//			if (data.isStarredTitle(bib, title)) {
//				menu.findItem(R.id.action_star).setIcon(
//						R.drawable.ic_action_star_1);
//			}
//		} else {
//			if (data.isStarred(bib, id)) {
//				menu.findItem(R.id.action_star).setIcon(
//						R.drawable.ic_action_star_1);
//			}
//		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void removeFragment() {
		finish();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		final String bib = app.getLibrary().getIdent();
		if (item.getItemId() == R.id.action_reservation) {
//			reservationStart();
			return true;
		} else if (item.getItemId() == R.id.action_lendebook) {
//			bookingStart();
			return true;
		} else if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		} else if (item.getItemId() == R.id.action_tocollection) {
			if (getIntent().getBooleanExtra("from_collection", false)) {
				finish();
			} else {
				Intent intent = new Intent(this,
						SearchResultDetailActivity.class);
				intent.putExtra("item_id", detailFragment.getItem().getCollectionId());
				startActivity(intent);
				finish();
			}
			return true;
		} else if (item.getItemId() == R.id.action_share) {
			if (detailFragment.getItem() == null) {
				Toast toast = Toast.makeText(this,
						getString(R.string.share_wait), Toast.LENGTH_SHORT);
				toast.show();
			} else {
				final String title = detailFragment.getItem().getTitle();
				final String id = detailFragment.getItem().getId();
				final CharSequence[] items = { getString(R.string.share_link),
						getString(R.string.share_details) };

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.share_dialog_select);
				builder.setItems(items, new DialogInterface.OnClickListener() {
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
								Toast toast = Toast.makeText(
										SearchResultDetailActivity.this,
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

							for (Detail detail : detailFragment.getItem()
									.getDetails()) {
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
							else
								text += "http://opacapp.de/:" + bib + ":" + id
										+ ":" + t;

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
			StarDataSource star = new StarDataSource(
					SearchResultDetailActivity.this);
			if (detailFragment.getItem() == null) {
				Toast toast = Toast.makeText(SearchResultDetailActivity.this,
						getString(R.string.star_wait), Toast.LENGTH_SHORT);
				toast.show();
			} else if (detailFragment.getItem().getId() == null || detailFragment.getItem().getId().equals("")) {
				final String title = detailFragment.getItem().getTitle();
				final String id = detailFragment.getItem().getId();
				if (star.isStarredTitle(bib, title)) {
					star.remove(star.getItemByTitle(bib, title));
					item.setIcon(R.drawable.ic_action_star_0);
				} else {
					star.star(null, title, bib);
					Toast toast = Toast.makeText(
							SearchResultDetailActivity.this,
							getString(R.string.starred), Toast.LENGTH_SHORT);
					toast.show();
					item.setIcon(R.drawable.ic_action_star_1);
				}
			} else {
				final String title = detailFragment.getItem().getTitle();
				final String id = detailFragment.getItem().getId();
				if (star.isStarred(bib, id)) {
					star.remove(star.getItem(bib, id));
					item.setIcon(R.drawable.ic_action_star_0);
				} else {
					star.star(id, title, bib);
					Toast toast = Toast.makeText(
							SearchResultDetailActivity.this,
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

	@Override
	protected int getContentView() {
		return R.layout.activity_searchresult_detail;
	}
}
