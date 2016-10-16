package de.geeksfactory.opacclient.frontend;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.databinding.AccountItemDetailActivityBinding;
import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.objects.SearchResult;

public class AccountItemDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ITEM = "item";
    public static final int RESULT_PROLONG = 1;
    public static final int RESULT_DOWNLOAD = 2;
    public static final int RESULT_CANCEL = 3;
    public static final int RESULT_BOOKING = 4;
    public static final String EXTRA_DATA = "data";
    private AccountItemDetailActivityBinding binding;
    private AccountItem item = null;

    @Override
    @TargetApi(21)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accountitem_detail);
        binding = DataBindingUtil.bind(findViewById(R.id.content));

        setSupportActionBar(binding.toolbar);

        item = (AccountItem) getIntent().getSerializableExtra(EXTRA_ITEM);
        binding.setItem(item);
        binding.btnDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AccountItemDetailActivity.this,
                        SearchResultDetailActivity.class);
                intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
                        item.getId());
                startActivity(intent);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Transition enter = new ChangeBounds()
                    .setInterpolator(new LinearOutSlowInInterpolator())
                    .setDuration(225);
            getWindow().setSharedElementEnterTransition(enter);
            enter.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
                    sv.scrollTo(0, 0);
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
                    sv.scrollTo(0, 0);
                }

                @Override
                public void onTransitionCancel(Transition transition) {}

                @Override
                public void onTransitionPause(Transition transition) {}

                @Override
                public void onTransitionResume(Transition transition) {}
            });
            Transition exit = new ChangeBounds()
                    .setInterpolator(new FastOutLinearInInterpolator())
                    .setDuration(195);
            getWindow().setSharedElementReturnTransition(exit);
        }

        View outside = findViewById(R.id.outside);
        // finish when clicking outside dialog
        outside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.finishAfterTransition(AccountItemDetailActivity.this);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_account_item_detail, menu);

        MenuItem prolong = menu.findItem(R.id.action_prolong);
        MenuItem download = menu.findItem(R.id.action_download);
        MenuItem cancel = menu.findItem(R.id.action_cancel);
        MenuItem booking = menu.findItem(R.id.action_booking);

        OpacClient app = (OpacClient) getApplication();
        OpacApi api = null;
        try {
            api = app.getApi();
        } catch (OpacClient.LibraryRemovedException e) {
            e.printStackTrace();
        }

        if (item instanceof LentItem) {
            final LentItem i = (LentItem) item;
            cancel.setVisible(false);
            booking.setVisible(false);
            if (i.getProlongData() != null) {
                prolong.setVisible(true);
                //ViewCompat.setAlpha(prolong, item.isRenewable() ? 1f : 0.4f);
                download.setVisible(false);
            } else if (i.getDownloadData() != null &&
                    api != null && api instanceof EbookServiceApi) {
                prolong.setVisible(false);
                download.setVisible(true);
            } else {
                prolong.setVisible(false);
                download.setVisible(false);
            }
        } else if (item instanceof ReservedItem) {
            final ReservedItem i = (ReservedItem) item;
            prolong.setVisible(false);
            download.setVisible(false);
            if (i.getBookingData() != null) {
                booking.setVisible(true);
                cancel.setVisible(false);
            } else if (i.getCancelData() != null) {
                cancel.setVisible(true);
                booking.setVisible(false);
            } else {
                cancel.setVisible(false);
                booking.setVisible(false);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent intent = new Intent();
        int resultCode;
        int item_id = menuItem.getItemId();
        if (item_id == R.id.action_prolong) {
            resultCode = RESULT_PROLONG;
            intent.putExtra(EXTRA_DATA, ((LentItem) item).getProlongData());
        } else if (item_id == R.id.action_download) {
            resultCode = RESULT_DOWNLOAD;
            intent.putExtra(EXTRA_DATA, ((LentItem) item).getDownloadData());
        } else if (item_id == R.id.action_cancel) {
            resultCode = RESULT_CANCEL;
            intent.putExtra(EXTRA_DATA, ((ReservedItem) item).getCancelData());
        } else if (item_id == R.id.action_booking) {
            resultCode = RESULT_BOOKING;
            intent.putExtra(EXTRA_DATA, ((ReservedItem) item).getBookingData());
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        setResult(resultCode, intent);
        supportFinishAfterTransition();
        return true;
    }

    public static CharSequence getBranch(AccountItem item, String format) {
        if (item instanceof LentItem) {
            LentItem lentItem = (LentItem) item;
            if (lentItem.getLendingBranch() != null && lentItem.getHomeBranch() != null) {
                return fromHtml(String.format(format, lentItem.getLendingBranch(),
                        lentItem.getHomeBranch()));
            } else if (lentItem.getLendingBranch() != null) {
                return fromHtml(lentItem.getLendingBranch());
            } else if (lentItem.getHomeBranch() != null) {
                return fromHtml(lentItem.getHomeBranch());
            } else {
                return null;
            }
        } else {
            return fromHtml(((ReservedItem) item).getBranch());
        }
    }

    private static CharSequence fromHtml(@Nullable String text) {
        return text != null ? Html.fromHtml(text) : null;
    }

    public static String getMediaTypeName(SearchResult.MediaType mediaType, Context context) {
        int id = context.getResources().getIdentifier("mediatype_"
                        + mediaType.toString().toLowerCase(), "string",
                context.getPackageName());
        return context.getResources().getString(id);
    }

    public static CharSequence getFormat(AccountItem item, Context context) {
        if (item.getFormat() != null) {
            return Html.fromHtml(item.getFormat());
        } else if (item.getMediaType() != null) {
            return getMediaTypeName(item.getMediaType(), context);
        } else {
            return null;
        }
    }
}
