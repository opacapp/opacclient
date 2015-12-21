package de.geeksfactory.opacclient.frontend;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.apis.EbookServiceApi.BookingResult;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.Callback;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.StepTask;
import de.geeksfactory.opacclient.networking.CoverDownloadTask;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.CoverHolder;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.StarDataSource;
import de.geeksfactory.opacclient.ui.AppCompatProgressDialog;
import de.geeksfactory.opacclient.ui.WhitenessUtils;
import de.geeksfactory.opacclient.utils.CompatibilityUtils;
import de.geeksfactory.opacclient.utils.ErrorReporter;

/**
 * A fragment representing a single SearchResult detail screen. This fragment is either contained in
 * a {@link SearchResultListActivity} in two-pane mode (on tablets) or a {@link
 * SearchResultDetailActivity} on handsets.
 */
public class SearchResultDetailFragment extends Fragment
        implements Toolbar.OnMenuItemClickListener {
    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    public static final String ARG_ITEM_NR = "item_nr";
    public static final String ARG_ITEM_COVER_BITMAP = "item_cover_bitmap";
    public static final String ARG_ITEM_MEDIATYPE = "item_mediatype";

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when
     * this fragment is not attached to an activity.
     */
    private static Callbacks dummyCallbacks = new Callbacks() {
        @Override
        public void removeFragment() {
        }
    };
    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private Callbacks callbacks = dummyCallbacks;
    protected boolean back_button_visible = false;
    protected boolean image_analyzed = false;

    protected Toolbar toolbar;
    protected ImageView ivCover;
    protected FrameLayout coverWrapper;
    protected NestedScrollView scrollView;
    protected View gradientBottom, gradientTop;
    protected TextView tvCopies, tvVolumes;
    protected LinearLayout llDetails, llCopies, llVolumes;
    protected ProgressBar progressBar;
    protected RelativeLayout detailsLayout;
    protected FrameLayout errorView;
    protected CollapsingToolbarLayout collapsingToolbar;
    protected AppBarLayout appBarLayout;

    /**
     * The detailled item that this fragment represents.
     */
    private DetailledItem item;
    private String id;
    private Integer nr;
    private OpacClient app;
    private View view;
    private FetchTask ft;
    private AppCompatProgressDialog dialog;
    private AlertDialog adialog;
    private boolean account_switched = false;
    private boolean invalidated = false;
    private boolean progress = false;
    private Boolean[] cardAnimations;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public SearchResultDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    public void setProgress(boolean show, boolean animate) {
        progress = show;

        if (view != null) {
            View content = detailsLayout;

            if (scrollView != null) {
                scrollView.setVisibility(View.VISIBLE);
            }

            if (show) {
                if (animate) {
                    progressBar.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_in));
                    content.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_out));
                } else {
                    progressBar.clearAnimation();
                    content.clearAnimation();
                }
                progressBar.setVisibility(View.VISIBLE);
                content.setVisibility(View.GONE);
            } else {
                if (animate) {
                    progressBar.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_out));
                    content.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_in));
                } else {
                    progressBar.clearAnimation();
                    content.clearAnimation();
                }
                progressBar.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
            }
        }
    }

    public void showConnectivityError() {
        errorView.removeAllViews();
        View connError = getActivity().getLayoutInflater().inflate(
                R.layout.error_connectivity, errorView);

        connError.findViewById(R.id.btRetry)
                 .setOnClickListener(new OnClickListener() {
                     @Override
                     public void onClick(View v) {
                         errorView.removeAllViews();
                         reload();
                     }
                 });

        progressBar.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                android.R.anim.fade_out));
        scrollView.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                android.R.anim.fade_out));
        connError.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                android.R.anim.fade_in));
        progressBar.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        connError.setVisibility(View.VISIBLE);
    }

    public void setProgress() {
        setProgress(progress, false);
    }

    private void load(int nr, String id) {
        setProgress(true, true);
        this.id = id;
        this.nr = nr;
        ft = new FetchTask(nr, id);
        ft.execute();
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

        callbacks = (Callbacks) activity;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (item != null) {
            display();
        } else if (getArguments().containsKey(ARG_ITEM_ID)
                || getArguments().containsKey(ARG_ITEM_NR)) {
            load(getArguments().getInt(ARG_ITEM_NR),
                    getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        callbacks = dummyCallbacks;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_searchresult_detail,
                container, false);
        view = rootView;
        findViews();
        setHasOptionsMenu(false);

        if (getActivity() instanceof SearchResultDetailActivity) {
            // This applies on phones, where the Toolbar is also the
            // main ActionBar of the Activity and needs a back button
            toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().supportFinishAfterTransition();
                }
            });
            back_button_visible = true;
        }

        toolbar.inflateMenu(R.menu.search_result_details_activity);
        refreshMenu(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this);

        setRetainInstance(true);
        setProgress();

        if (getArguments().containsKey(ARG_ITEM_COVER_BITMAP)) {
            Bitmap bitmap = getArguments().getParcelable(ARG_ITEM_COVER_BITMAP);
            ivCover.setImageBitmap(bitmap);
            analyzeCover(bitmap);
            showCoverView(true);
        } else {
            showCoverView(false);
        }

        return rootView;
    }

    private void analyzeCover(Bitmap bitmap) {
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                if (swatch == null) swatch = palette.getDarkMutedSwatch();
                if (swatch != null) {
                    appBarLayout.setBackgroundColor(swatch.getRgb());
                    collapsingToolbar.setContentScrimColor(swatch.getRgb());
                    if (getActivity() != null &&
                            getActivity() instanceof SearchResultDetailActivity &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // show darkened color in status bar
                        float[] hsv = swatch.getHsl();
                        hsv[2] *= 0.95f;
                        getActivity().getWindow().setStatusBarColor(Color.HSVToColor(hsv));
                    }
                }
            }
        });
        analyzeWhitenessOfCoverAsync(bitmap);
        image_analyzed = true;
    }

    private void findViews() {
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        scrollView = (NestedScrollView) view.findViewById(R.id.rootView);
        ivCover = (ImageView) view.findViewById(R.id.ivCover);
        coverWrapper = (FrameLayout) view.findViewById(R.id.coverWrapper);
        gradientBottom = view.findViewById(R.id.gradient_bottom);
        gradientTop = view.findViewById(R.id.gradient_top);
        collapsingToolbar = (CollapsingToolbarLayout) view.findViewById(R.id.collapsingToolbar);
        appBarLayout = (AppBarLayout) view.findViewById(R.id.appBarLayout);
        llDetails = (LinearLayout) view.findViewById(R.id.llDetails);
        llCopies = (LinearLayout) view.findViewById(R.id.llCopies);
        llVolumes = (LinearLayout) view.findViewById(R.id.llVolumes);
        progressBar = (ProgressBar) view.findViewById(R.id.progress);
        detailsLayout = (RelativeLayout) view.findViewById(R.id.detailsLayout);
        errorView = (FrameLayout) view.findViewById(R.id.error_view);
        tvCopies = (TextView) view.findViewById(R.id.tvCopies);
        tvVolumes = (TextView) view.findViewById(R.id.tvVolumes);
    }

    /**
     * Examine how many white pixels are in the bitmap in order to determine whether or not we need
     * gradient overlays on top of the image.
     */
    @SuppressLint("NewApi")
    private void analyzeWhitenessOfCoverAsync(final Bitmap bitmap) {
        AnalyzeWhitenessTask task = new AnalyzeWhitenessTask();
        // Execute in parallel with FetchTask
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
        } else {
            task.execute(bitmap);
        }
    }

    protected void display() {
        try {
            Log.i("result", getItem().toString());
        } catch (Exception e) {
            ErrorReporter.handleException(e);
        }
        collapsingToolbar.setTitle(getItem().getTitle());
        llDetails.removeAllViews();
        for (Detail detail : item.getDetails()) {
            View v = getLayoutInflater(null)
                    .inflate(R.layout.listitem_detail, null);
            ((TextView) v.findViewById(R.id.tvDesc)).setText(detail.getDesc());
            ((TextView) v.findViewById(R.id.tvContent)).setText(detail
                    .getContent());
            Linkify.addLinks((TextView) v.findViewById(R.id.tvContent),
                    Linkify.WEB_URLS);
            llDetails.addView(v);
        }

        llVolumes.removeAllViews();
        llCopies.removeAllViews();
        if (item.getVolumesearch() != null) {
            Button btnVolume = new Button(getActivity());
            btnVolume.setText(R.string.volumesearch);
            btnVolume.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    app.startVolumeSearch(getActivity(), getItem().getVolumesearch());
                }
            });
            llVolumes.addView(btnVolume);
        } else if (item.getVolumes().size() > 0) {
            for (final Map<String, String> band : item.getVolumes()) {
                View v = getLayoutInflater(null).inflate(R.layout.listitem_volume,
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
                llVolumes.addView(v);
            }
        } else {
            tvVolumes.setVisibility(View.GONE);
        }

        if (item.getCopies().size() == 0) {
            tvCopies.setVisibility(View.GONE);
        } else {
            for (Map<String, String> copy : item.getCopies()) {
                View v = getLayoutInflater(null).inflate(
                        R.layout.listitem_copy, llCopies, false);

                if (v.findViewById(R.id.tvBranch) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_BRANCH)) {
                        ((TextView) v.findViewById(R.id.tvBranch))
                                .setText(copy
                                        .get(DetailledItem.KEY_COPY_BRANCH));
                        v.findViewById(R.id.tvBranch).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvBranch).setVisibility(View.GONE);
                    }
                }
                if (v.findViewById(R.id.tvDepartment) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_DEPARTMENT)) {
                        ((TextView) v.findViewById(R.id.tvDepartment))
                                .setText(copy
                                        .get(DetailledItem.KEY_COPY_DEPARTMENT));
                        v.findViewById(R.id.tvDepartment).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvDepartment).setVisibility(View.GONE);
                    }
                }
                if (v.findViewById(R.id.tvLocation) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_LOCATION)) {
                        ((TextView) v.findViewById(R.id.tvLocation))
                                .setText(copy
                                        .get(DetailledItem.KEY_COPY_LOCATION));
                        v.findViewById(R.id.tvLocation).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvLocation).setVisibility(View.GONE);
                    }
                }
                if (v.findViewById(R.id.tvShelfmark) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_SHELFMARK)) {
                        ((TextView) v.findViewById(R.id.tvShelfmark))
                                .setText(copy
                                        .get(DetailledItem.KEY_COPY_SHELFMARK));
                        v.findViewById(R.id.tvShelfmark).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvShelfmark).setVisibility(View.GONE);
                    }
                }
                if (v.findViewById(R.id.tvStatus) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_STATUS)) {
                        ((TextView) v.findViewById(R.id.tvStatus))
                                .setText(copy
                                        .get(DetailledItem.KEY_COPY_STATUS));
                        v.findViewById(R.id.tvStatus).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvStatus).setVisibility(View.GONE);
                    }
                }

                if (v.findViewById(R.id.tvReservations) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_RESERVATIONS)) {
                        ((TextView) v.findViewById(R.id.tvReservations))
                                .setText(copy.get(DetailledItem.KEY_COPY_RESERVATIONS));
                        v.findViewById(R.id.tvReservations).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvReservations).setVisibility(View.GONE);
                    }
                }
                if (v.findViewById(R.id.tvReturndate) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_RETURN)) {
                        ((TextView) v.findViewById(R.id.tvReturndate))
                                .setText(copy.get(DetailledItem.KEY_COPY_RETURN));
                        v.findViewById(R.id.tvReturndate).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvReturndate).setVisibility(View.GONE);
                    }
                }
                if (v.findViewById(R.id.tvUrl) != null) {
                    if (containsAndNotEmpty(copy, DetailledItem.KEY_COPY_URL)) {
                        ((TextView) v.findViewById(R.id.tvUrl))
                                .setText(copy.get(DetailledItem.KEY_COPY_URL));
                        v.findViewById(R.id.tvUrl).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.tvUrl).setVisibility(View.GONE);
                    }
                }

                llCopies.addView(v);
            }
        }

        if (id == null || id.equals("")) {
            id = getItem().getId();
        }

        refreshMenu(toolbar.getMenu());

        setProgress(false, true);
    }

    private void displayCover() {
        if (getItem().getCoverBitmap() != null) {
            coverWrapper.setVisibility(View.VISIBLE);
            ivCover.setImageBitmap(getItem().getCoverBitmap());
            if (!image_analyzed) {
                analyzeCover(getItem().getCoverBitmap());
            }
            showCoverView(true);
        } else if (getArguments().containsKey(ARG_ITEM_COVER_BITMAP)) {
            showCoverView(true);
        } else {
            showCoverView(false);
            collapsingToolbar.setTitle(getItem().getTitle());
        }
    }

    private boolean containsAndNotEmpty(Map<String, String> map, String key) {
        return map != null && map.containsKey(key) && !"".equals(map.get(key));
    }

    private void showCoverView(boolean b) {
        if (getActivity() == null) {
            return;
        }
        coverWrapper.setVisibility(b ? View.VISIBLE : View.GONE);
        if (!b) {
            appBarLayout.setBackgroundResource(getToolbarBackgroundColor());
        }
    }

    private int getToolbarBackgroundColor() {
        if (getActivity() != null) {
            if (getActivity() instanceof SearchResultDetailActivity) {
                return R.color.primary_red;
            } else {
                return R.color.primary_red_dark;
            }
        } else {
            return R.color.primary_red;
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (item != null) {
            display();
        }
    }

    protected void dialog_wrong_credentials(String s, final boolean finish) {
        if (getActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.opac_error) + " " + s)
               .setCancelable(false)
               .setNegativeButton(R.string.close,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.cancel();
                               if (finish) {
                                   callbacks.removeFragment();
                               }
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
        refreshMenu(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    protected void refreshMenu(Menu menu) {
        if (item != null) {
            if (item.isReservable()) {
                menu.findItem(R.id.action_reservation).setVisible(true);
            } else {
                menu.findItem(R.id.action_reservation).setVisible(false);
            }
            if (item.isBookable() && app.getApi() instanceof EbookServiceApi) {
                if (((EbookServiceApi) app.getApi()).isEbook(item)) {
                    menu.findItem(R.id.action_lendebook).setVisible(true);
                } else {
                    menu.findItem(R.id.action_lendebook).setVisible(false);
                }
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
            if (data.isStarredTitle(bib, item.getTitle())) {
                menu.findItem(R.id.action_star).setIcon(
                        R.drawable.ic_action_star_1);
            }
        } else {
            if (data.isStarred(bib, id)) {
                menu.findItem(R.id.action_star).setIcon(
                        R.drawable.ic_action_star_1);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
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
                final CharSequence[] items = {getString(R.string.share_link),
                        getString(R.string.share_details)};

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setTitle(R.string.share_dialog_select);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int di) {
                        if (di == 0) {
                            // Share link
                            Intent intent = new Intent(
                                    android.content.Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.addFlags(CompatibilityUtils.getNewDocumentIntentFlag());

                            // Add data to the intent, the receiving app will
                            // decide
                            // what to do with it.
                            intent.putExtra(Intent.EXTRA_SUBJECT, title);

                            String t = title;
                            try {
                                t = java.net.URLEncoder.encode(t, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                            }

                            String shareUrl = app.getApi().getShareUrl(id, t);
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
                            intent.addFlags(CompatibilityUtils.getNewDocumentIntentFlag());

                            // Add data to the intent, the receiving app will
                            // decide
                            // what to do with it.
                            intent.putExtra(Intent.EXTRA_SUBJECT, title);

                            String t = title;
                            try {
                                t = t != null ? java.net.URLEncoder.encode(t, "UTF-8") : "";
                            } catch (UnsupportedEncodingException e) {
                            }

                            String text = t + "\n\n";

                            for (Detail detail : getItem().getDetails()) {
                                String colon = "";
                                if (!detail.getDesc().endsWith(":")) {
                                    colon = ":";
                                }
                                text += detail.getDesc() + colon + "\n"
                                        + detail.getContent() + "\n\n";
                            }

                            String shareUrl = app.getApi().getShareUrl(id, t);
                            if (shareUrl != null) {
                                text += shareUrl;
                            }

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
                    star.star(null, title, bib, getItem().getMediaType());
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
                    star.star(id, title, bib, getItem().getMediaType());
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
        if (getActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.status_nouser)
               .setCancelable(false)
               .setNegativeButton(R.string.close,
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
            new RestoreSessionTask(false).execute();
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
                       .setNegativeButton(R.string.close,
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
        } else if (accounts.size() > 1
                && !getActivity().getIntent().getBooleanExtra("reservation", false)
                && (app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_CHANGE_ACCOUNT) != 0
                && !(SearchResultDetailFragment.this.id == null
                || SearchResultDetailFragment.this.id.equals("null") ||
                SearchResultDetailFragment.this.id
                        .equals(""))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getLayoutInflater(null);

            View view = inflater.inflate(R.layout.dialog_simple_list, null, false);

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
                           });
            adialog = builder.create();
            adialog.show();
        } else {
            reservationDo();
        }
    }

    public void reservationDo() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (sp.getBoolean("reservation_fee_warning_ignore", false) ||
                (app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_WARN_RESERVATION_FEES) > 0) {
            reservationPerform();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    getActivity());
            View content = getLayoutInflater(null).inflate(R.layout.dialog_reservation_fees, null);
            final CheckBox check = (CheckBox) content.findViewById(R.id.check_box1);
            builder.setView(content)
                   .setCancelable(false)
                   .setNegativeButton(R.string.cancel,
                           new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(
                                       DialogInterface dialog, int id) {
                                   dialog.cancel();
                               }
                           })
                   .setPositiveButton(R.string.reservation_fee_continue,
                           new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(
                                       DialogInterface dialog, int id) {
                                   if (check.isChecked()) {
                                       sp.edit().putBoolean("reservation_fee_warning_ignore", true).apply();
                                   }
                                   reservationPerform();
                               }
                           });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void reservationPerform() {
        MultiStepResultHelper<DetailledItem> msrhReservation = new MultiStepResultHelper<>(
                getActivity(), item, R.string.doing_res);
        msrhReservation.setCallback(new Callback<DetailledItem>() {
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
                           .setNegativeButton(R.string.close,
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
            public StepTask<?> newTask(MultiStepResultHelper helper, int useraction,
                    String selection, DetailledItem item) {
                return new ResTask(helper, useraction, selection, item);
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
        } else if (accounts.size() > 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getLayoutInflater(null);

            View view = inflater.inflate(R.layout.dialog_simple_list, null, false);

            ListView lv = (ListView) view.findViewById(R.id.lvBibs);
            AccountListAdapter adapter = new AccountListAdapter(getActivity(),
                    accounts);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    app.setAccount(accounts.get(position).getId());
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
                           });
            adialog = builder.create();
            adialog.show();
        } else {
            bookingDo();
        }
    }

    public void bookingDo() {
        MultiStepResultHelper<DetailledItem> msrhBooking = new MultiStepResultHelper<>(
                getActivity(), item, R.string.doing_booking);
        msrhBooking.setCallback(new Callback<DetailledItem>() {
            @Override
            public void onSuccess(MultiStepResult result) {
                if (getActivity() == null) {
                    return;
                }
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
                if (getActivity() == null) {
                    return;
                }
                dialog_wrong_credentials(result.getMessage(), false);
            }

            @Override
            public void onUnhandledResult(MultiStepResult result) {
            }

            @Override
            public void onUserCancel() {
            }

            @Override
            public StepTask<?> newTask(MultiStepResultHelper helper, int useraction,
                    String selection, DetailledItem item) {
                return new BookingTask(helper, useraction, selection, item);
            }
        });
        msrhBooking.start();
    }

    /**
     * A callback interface that all activities containing this fragment must implement. This
     * mechanism allows activities to be notified of item selections.
     */
    public interface Callbacks {
        /**
         * Callback for when the fragment should be deleted
         */
        public void removeFragment();
    }

    public class LoadCoverTask extends CoverDownloadTask {

        public LoadCoverTask(CoverHolder item, int width, int height) {
            super(getActivity(), item);
            this.width = width;
            this.height = height;
        }

        protected void onPostExecute(CoverHolder item) {
            displayCover();
        }
    }

    public class FetchTask extends AsyncTask<Void, Void, DetailledItem> {
        protected boolean success = true;
        protected Integer nr;
        protected String id;

        public FetchTask(Integer nr, String id) {
            this.nr = nr;
            this.id = id;
        }

        @Override
        protected DetailledItem doInBackground(Void... voids) {
            try {
                DetailledItem res;
                if (id != null && !id.equals("")) {
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    String homebranch = sp.getString(
                            OpacClient.PREF_HOME_BRANCH_PREFIX
                                    + app.getAccount().getId(), null);

                    if (getActivity().getIntent().hasExtra("reservation")
                            && getActivity().getIntent().getBooleanExtra(
                            "reservation", false)) {
                        app.getApi().start();
                    }

                    res = app.getApi().getResultById(id, homebranch);
                    if (res.getId() == null) res.setId(id);
                } else {
                    res = app.getApi().getResult(nr);
                }
                if (res.getMediaType() == null && getArguments().containsKey(ARG_ITEM_MEDIATYPE)) {
                    res.setMediaType(SearchResult.MediaType
                            .valueOf(getArguments().getString(ARG_ITEM_MEDIATYPE)));
                }
                success = true;
                return res;
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        @SuppressLint("NewApi")
        protected void onPostExecute(DetailledItem result) {
            if (getActivity() == null) {
                return;
            }

            if (!success || result == null) {
                showConnectivityError();
                return;
            }

            item = result;

            if (item.getCover() != null && item.getCoverBitmap() == null) {
                new LoadCoverTask(item, coverWrapper.getWidth(), coverWrapper.getHeight()).execute();
            } else {
                displayCover();
            }

            display();

            if (getActivity().getIntent().hasExtra("reservation")
                    && getActivity().getIntent().getBooleanExtra("reservation",
                    false)) {
                reservationStart();
            }
        }
    }

    private class AnalyzeWhitenessTask extends AsyncTask<Bitmap, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Bitmap... params) {
            return WhitenessUtils.isBitmapWhiteAtTopOrBottom(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean isWhite) {
            super.onPostExecute(isWhite);
            gradientBottom.setVisibility(isWhite ? View.VISIBLE : View.GONE);
            gradientTop.setVisibility(isWhite ? View.VISIBLE : View.GONE);
        }
    }

    public class ResTask extends StepTask<ReservationResult> {
        private DetailledItem item;

        public ResTask(MultiStepResultHelper helper, int useraction, String selection,
                DetailledItem item) {
            super(helper, useraction, selection);
            this.item = item;
        }

        @Override
        protected ReservationResult doInBackground(Void... voids) {
            try {
                return app.getApi().reservation(item,
                        app.getAccount(), useraction, selection);
            } catch (IOException e) {
                publishProgress(e, "ioerror");
            } catch (Exception e) {
                ErrorReporter.handleException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ReservationResult res) {
            if (getActivity() == null) {
                return;
            }

            if (res == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setMessage(R.string.error)
                       .setCancelable(true)
                       .setNegativeButton(R.string.close,
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

        private DetailledItem item;

        public BookingTask(MultiStepResultHelper helper, int useraction, String selection,
                DetailledItem item) {
            super(helper, useraction, selection);
            this.item = item;
        }

        @Override
        protected BookingResult doInBackground(Void... voids) {
            try {
                return ((EbookServiceApi) app.getApi()).booking(
                        item, app.getAccount(), useraction, selection);
            } catch (IOException e) {
                publishProgress(e, "ioerror");
            } catch (Exception e) {
                ErrorReporter.handleException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(BookingResult res) {
            if (getActivity() == null) {
                return;
            }

            if (res == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setMessage(R.string.error)
                       .setCancelable(true)
                       .setNegativeButton(R.string.close,
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

    public class RestoreSessionTask extends AsyncTask<Void, Void, Integer> {
        private boolean reservation;

        public RestoreSessionTask(boolean reservation) {
            this.reservation = reservation;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                if (id != null) {
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    String homebranch = sp.getString(
                            OpacClient.PREF_HOME_BRANCH_PREFIX
                                    + app.getAccount().getId(), null);
                    app.getApi().getResultById(id, homebranch);
                    return 0;
                } else {
                    ErrorReporter.handleException(
                            new Throwable("No ID supplied"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                ErrorReporter.handleException(e);
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (getActivity() == null) {
                return;
            }
            if (reservation) {
                reservationDo();
            }
        }

    }

}
