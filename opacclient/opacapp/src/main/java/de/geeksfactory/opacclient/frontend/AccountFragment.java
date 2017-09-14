/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NoHttpResponseException;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.apis.EbookServiceApi.BookingResult;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.CancelResult;
import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.apis.OpacApi.ProlongAllResult;
import de.geeksfactory.opacclient.apis.OpacApi.ProlongResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.Callback;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.StepTask;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.frontend.adapter.AccountAdapter;
import de.geeksfactory.opacclient.frontend.adapter.LentAdapter;
import de.geeksfactory.opacclient.frontend.adapter.ReservationsAdapter;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.reminder.ReminderHelper;
import de.geeksfactory.opacclient.reminder.SyncAccountAlarmListener;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.ui.AccountDividerItemDecoration;
import de.geeksfactory.opacclient.utils.ErrorReporter;
import su.j2e.rvjoiner.JoinableAdapter;
import su.j2e.rvjoiner.JoinableLayout;
import su.j2e.rvjoiner.RvJoiner;

public class AccountFragment extends Fragment implements
        AccountSelectedListener, LentAdapter.Callback, ReservationsAdapter.Callback {

    public static final long MAX_CACHE_AGE = (1000 * 3600 * 2);
    private static final int REQUEST_DETAIL = 1;
    protected AlertDialog adialog;
    protected OpacClient app;
    protected View view;
    protected FrameLayout errorView;
    protected View unsupportedErrorView, answerErrorView;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected Button btPrefs;
    protected LinearLayout llLoading;
    protected TextView tvError, tvResHeader, tvPendingFeesLabel, tvPendingFees, tvValidUntilLabel,
            tvValidUntil, tvAge, tvLentHeader, tvWarning, tvAccCity, tvAccUser, tvAccLabel,
            tvErrBodyA, tvErrHeadA, tvErrBodyU;
    protected LentAdapter lentAdapter;
    protected RelativeLayout rlReplaced;
    protected ImageView ivReplacedStore;
    protected ReservationsAdapter resAdapter;
    protected AccountData accountData;
    private LoadTask lt;
    private CancelTask ct;
    private DownloadTask dt;
    private Account account;
    private boolean refreshing = false;
    private long refreshtime;
    private boolean fromcache;
    private boolean supported = true;
    private JoinableLayout lentEmpty;
    private JoinableLayout reservationsEmpty;
    private TextView tvLentEmpty;
    private TextView tvReservationsEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_account, container, false);
        app = (OpacClient) getActivity().getApplication();
        account = app.getAccount();
        findViews();

        swipeRefreshLayout.setColorSchemeResources(R.color.primary_red);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        setHasOptionsMenu(true);

        final Handler handler = new Handler();
        // schedule alarm here and post runnable as soon as scheduled
        handler.post(new Runnable() {
            @Override
            public void run() {
                displayAge();
                handler.postDelayed(this, 60000);
            }
        });

        return view;
    }

    private void findViews() {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        errorView = (FrameLayout) view.findViewById(R.id.error_view);
        unsupportedErrorView = view.findViewById(R.id.unsupported_error);
        answerErrorView = view.findViewById(R.id.answer_error);
        llLoading = (LinearLayout) view.findViewById(R.id.llLoading);
        tvErrBodyU = (TextView) view.findViewById(R.id.tvErrBodyU);
        btPrefs = (Button) view.findViewById(R.id.btPrefs);
        tvErrHeadA = (TextView) view.findViewById(R.id.tvErrHeadA);
        tvErrBodyA = (TextView) view.findViewById(R.id.tvErrBodyA);
        tvLentEmpty = (TextView) view.findViewById(R.id.emptyLent);
        rlReplaced = (RelativeLayout) view.findViewById(R.id.rlReplaced);
        ivReplacedStore = (ImageView) view.findViewById(R.id.ivReplacedStore);
        tvReservationsEmpty = (TextView) view.findViewById(R.id.emptyReservations);

        lentAdapter = new LentAdapter(this);
        try {
            lentAdapter.setApi(app.getApi());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        } catch (OpacClient.LibraryRemovedException e) {
            e.printStackTrace();
        }
        displayLentItems();
        resAdapter = new ReservationsAdapter(this);
        try {
            resAdapter.setApi(app.getApi());
        } catch (OpacClient.LibraryRemovedException e) {
            e.printStackTrace();
        }
        displayReservedItems();

        if (view.findViewById(R.id.rlAccHeader) != null) {
            // tablet
            RecyclerView rvLent = (RecyclerView) view.findViewById(R.id.rvLent);
            rvLent.setLayoutManager(new LinearLayoutManager(getActivity()));
            rvLent.setAdapter(lentAdapter);
            rvLent.addItemDecoration(new AccountDividerItemDecoration(getContext(), null));

            RecyclerView rvReservations = (RecyclerView) view.findViewById(R.id.rvReservations);
            rvReservations.setLayoutManager(new LinearLayoutManager(getActivity()));
            rvReservations.setAdapter(resAdapter);
            rvReservations.addItemDecoration(new AccountDividerItemDecoration(getContext(), null));

            findHeaderViews(view);
            findErrorWarningViews(view);
            findLentHeader(view);
            findResHeader(view);
            findFooterViews(view);
        } else {
            // phone
            RecyclerView rv = (RecyclerView) view.findViewById(R.id.rvAccountData);
            rv.setLayoutManager(new LinearLayoutManager(getActivity()));
            RvJoiner joiner = new RvJoiner();
            rv.addItemDecoration(new AccountDividerItemDecoration(getContext(), joiner));

            joiner.add(new JoinableLayout(R.layout.account_header, new JoinableLayout.Callback() {
                @Override
                public void onInflateComplete(View view, ViewGroup parent) {
                    findHeaderViews(view);
                }
            }));
            joiner.add(
                    new JoinableLayout(R.layout.account_error_warning,
                            new JoinableLayout.Callback() {
                                @Override
                                public void onInflateComplete(View view, ViewGroup parent) {
                                    findErrorWarningViews(view);
                                }
                            }));
            joiner.add(
                    new JoinableLayout(R.layout.account_header_lent, new JoinableLayout.Callback() {
                        @Override
                        public void onInflateComplete(View view, ViewGroup parent) {
                            findLentHeader(view);
                        }
                    }));
            lentEmpty = new JoinableLayout(R.layout.listitem_account_empty_lent);
            joiner.add(lentEmpty);
            joiner.add(new JoinableAdapter(lentAdapter));
            joiner.add(new JoinableLayout(R.layout.account_header_reservations,
                    new JoinableLayout.Callback() {
                        @Override
                        public void onInflateComplete(View view, ViewGroup parent) {
                            findResHeader(view);
                        }
                    }));
            joiner.add(new JoinableAdapter(resAdapter));
            reservationsEmpty = new JoinableLayout(R.layout.listitem_account_empty_reservations);
            joiner.add(reservationsEmpty);
            joiner.add(new JoinableLayout(R.layout.account_footer, new JoinableLayout.Callback() {
                @Override
                public void onInflateComplete(View view, ViewGroup parent) {
                    findFooterViews(view);
                }
            }));

            rv.setAdapter(joiner.getAdapter());
        }
    }

    private void findFooterViews(View view) {
        tvAge = (TextView) view.findViewById(R.id.tvAge);
        displayAge();
    }

    private void findResHeader(View view) {
        tvResHeader = (TextView) view.findViewById(R.id.tvResHeader);
        displayResHeader();
    }

    private void findLentHeader(View view) {
        tvLentHeader = (TextView) view.findViewById(R.id.tvLentHeader);
        displayLentHeader();
    }

    private void findErrorWarningViews(View view) {
        tvError = (TextView) view.findViewById(R.id.tvError);
        tvWarning = (TextView) view.findViewById(R.id.tvWarning);
    }

    private void findHeaderViews(View view) {
        tvAccLabel = (TextView) view.findViewById(R.id.tvAccLabel);
        tvAccUser = (TextView) view.findViewById(R.id.tvAccUser);
        tvAccCity = (TextView) view.findViewById(R.id.tvAccCity);
        tvPendingFeesLabel = (TextView) view.findViewById(R.id.tvPendingFeesLabel);
        tvPendingFees = (TextView) view.findViewById(R.id.tvPendingFees);
        tvValidUntilLabel = (TextView) view.findViewById(R.id.tvValidUntilLabel);
        tvValidUntil = (TextView) view.findViewById(R.id.tvValidUntil);
        displayHeader();
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_account, menu);
        OpacApi api;
        try {
            api = app.getApi();
        } catch (OpacClient.LibraryRemovedException e) {
            return;
        }
        if (app.getAccount() != null && (
                api.getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL) != 0) {
            menu.findItem(R.id.action_prolong_all).setVisible(true);
        } else {
            menu.findItem(R.id.action_prolong_all).setVisible(false);
        }
        menu.findItem(R.id.action_refresh).setVisible(supported);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            refresh();
        } else if (item.getItemId() == R.id.action_prolong_all) {
            prolongAllStart();
        } else if (item.getItemId() == R.id.action_export) {
            export();
        }
        return super.onOptionsItemSelected(item);
    }

    private void export() {
        if (refreshing) {
            Toast.makeText(getActivity(), R.string.account_no_concurrent,
                    Toast.LENGTH_LONG).show();
            if (!refreshing) {
                refresh();
            }
            return;
        }

        Context ctx = getActivity() != null ? getActivity() : OpacClient
                .getEmergencyContext();
        AccountDataSource adatasource = new AccountDataSource(ctx);
        AccountData data = adatasource.getCachedAccountData(account);
        LocalDateTime dt = new LocalDateTime(adatasource.getCachedAccountDataTime(account));

        if (data == null) return;

        StringBuilder string = new StringBuilder();

        DateTimeFormatter fmt1 = DateTimeFormat.shortDateTime().withLocale(
                getResources().getConfiguration().locale);
        DateTimeFormatter fmt2 = DateTimeFormat.shortDate().withLocale(
                getResources().getConfiguration().locale);
        String dateStr = fmt1.print(dt);
        string.append(getResources()
                .getString(R.string.accountdata_export_header, account.getLabel(), dateStr));
        string.append("\n\n");
        string.append(getResources().getString(R.string.lent_head));
        string.append("\n\n");
        for (LentItem item : data.getLent()) {
            appendIfNotEmpty(string, item.getTitle(), R.string.accountdata_title);
            appendIfNotEmpty(string, item.getAuthor(), R.string.accountdata_author);
            appendIfNotEmpty(string, item.getFormat(), R.string.accountdata_format);
            appendIfNotEmpty(string, item.getStatus(), R.string.accountdata_status);
            appendIfNotEmpty(string, item.getBarcode(), R.string.accountdata_lent_barcode);
            if (item.getDeadline() != null) {
                appendIfNotEmpty(string, fmt2.print(item.getDeadline()),
                        R.string.accountdata_lent_deadline);
            }
            appendIfNotEmpty(string, item.getHomeBranch(), R.string.accountdata_lent_home_branch);
            appendIfNotEmpty(string, item.getLendingBranch(),
                    R.string.accountdata_lent_lending_branch);
            string.append("\n");
        }

        if (data.getLent().size() == 0) {
            string.append(getResources().getString(R.string.lent_none));
        }

        string.append(getResources().getString(R.string.reservations_head));
        string.append("\n\n");
        for (ReservedItem item : data.getReservations()) {
            appendIfNotEmpty(string, item.getTitle(), R.string.accountdata_title);
            appendIfNotEmpty(string, item.getAuthor(), R.string.accountdata_author);
            appendIfNotEmpty(string, item.getFormat(), R.string.accountdata_format);
            appendIfNotEmpty(string, item.getStatus(), R.string.accountdata_status);
            if (item.getReadyDate() != null) {
                appendIfNotEmpty(string, fmt2.print(item.getReadyDate()),
                        R.string.accountdata_reserved_ready_date);
            }
            if (item.getExpirationDate() != null) {
                appendIfNotEmpty(string, fmt2.print(item.getExpirationDate()),
                        R.string.accountdata_reserved_expiration_date);
            }
            appendIfNotEmpty(string, item.getBranch(), R.string.accountdata_reserved_branch);
            string.append("\n");
        }

        if (data.getReservations().size() == 0) {
            string.append(getResources().getString(R.string.reservations_none));
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, string.toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent,
                getResources().getText(R.string.share_dialog_select)));
    }

    private void appendIfNotEmpty(StringBuilder string, String text, int id) {
        if (text != null && !text.equals("")) {
            string.append(getResources().getString(id)).append(": ")
                  .append(text).append("\n");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        account = app.getAccount();
        accountSelected(account);
    }

    @Override
    public void accountSelected(Account account) {

        swipeRefreshLayout.setVisibility(View.GONE);
        unsupportedErrorView.setVisibility(View.GONE);
        answerErrorView.setVisibility(View.GONE);
        errorView.removeAllViews();
        llLoading.setVisibility(View.VISIBLE);

        setRefreshing(false);
        supported = true;

        this.account = app.getAccount();
        OpacApi api;
        try {
            api = app.getApi();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        } catch (OpacClient.LibraryRemovedException e) {
            show_connectivity_error(e);
            return;
        }
        if (api != null && !app.getLibrary().isAccountSupported()) {

            if (app.getLibrary().getReplacedBy() != null && !"".equals(app.getLibrary().getReplacedBy()) && app.promotePlusApps()) {
                rlReplaced.setVisibility(View.VISIBLE);
                tvErrBodyU.setVisibility(View.GONE);
                ivReplacedStore.setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    Intent i = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(app.getLibrary().getReplacedBy()
                                                         .replace("https://play.google.com/store/apps/details?id=", "market://details?id=")));
                                    startActivity(i);
                                } catch (ActivityNotFoundException e) {
                                    Intent i = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(app.getLibrary().getReplacedBy()));
                                    startActivity(i);
                                }
                            }
                        });
            } else {
                rlReplaced.setVisibility(View.GONE);
                tvErrBodyU.setVisibility(View.VISIBLE);
            }

            supported = false;
            // Not supported with this api at all
            llLoading.setVisibility(View.GONE);
            unsupportedErrorView.setVisibility(
                    View.VISIBLE);
            tvErrBodyU.setText(R.string.account_unsupported_api);

        } else if (account.getPassword() == null
                || account.getPassword().equals("null")
                || account.getPassword().equals("")
                || account.getName() == null
                || account.getName().equals("null")
                || account.getName().equals("")) {
            // No credentials entered
            llLoading.setVisibility(View.GONE);
            answerErrorView.setVisibility(View.VISIBLE);
            btPrefs.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(),
                            AccountEditActivity.class);
                    intent.putExtra(
                            AccountEditActivity.EXTRA_ACCOUNT_ID, app
                                    .getAccount().getId());
                    startActivity(intent);
                }
            });
            tvErrHeadA.setText("");
            tvErrBodyA.setText(R.string.status_nouser);

        } else {
            // Supported
            Context ctx = getActivity() != null ? getActivity() : OpacClient
                    .getEmergencyContext();
            AccountDataSource adatasource = new AccountDataSource(ctx);
            refreshtime = adatasource.getCachedAccountDataTime(account);
            if (refreshtime > 0) {
                display(adatasource.getCachedAccountData(account), true);
                if (System.currentTimeMillis() - refreshtime > MAX_CACHE_AGE) {
                    refresh();
                }
            } else {
                refresh();
            }
        }
    }

    public void refresh() {

        if ((!app.getLibrary().isAccountSupported())
                || account.getPassword() == null
                || account.getPassword().equals("null")
                || account.getPassword().equals("")
                || account.getName() == null
                || account.getName().equals("null")
                || account.getName().equals("")) {
            return;
        }

        setRefreshing(true);
        lt = new LoadTask();
        lt.execute();
    }

    public void cancel(final String a) {
        long age = System.currentTimeMillis() - refreshtime;
        if (refreshing || fromcache || age > MAX_CACHE_AGE) {
            Toast.makeText(getActivity(), R.string.account_no_concurrent,
                    Toast.LENGTH_LONG).show();
            if (!refreshing) {
                refresh();
            }
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.cancel_confirm)
               .setCancelable(true)
               .setNegativeButton(R.string.no,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface d, int id) {
                               d.cancel();
                           }
                       })
               .setPositiveButton(R.string.yes,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface d, int id) {
                               d.dismiss();

                               MultiStepResultHelper<String> msrhCancel =
                                       new MultiStepResultHelper<>(
                                               getActivity(), a,
                                               R.string.doing_cancel);
                               msrhCancel.setCallback(new Callback<String>() {
                                   @Override
                                   public void onSuccess(MultiStepResult result) {
                                       invalidateData();
                                   }

                                   @Override
                                   public void onError(MultiStepResult result) {
                                       AlertDialog.Builder builder = new AlertDialog.Builder(
                                               getActivity());
                                       builder.setMessage(result.getMessage())
                                              .setCancelable(true)
                                              .setNegativeButton(
                                                      R.string.close,
                                                      new DialogInterface.OnClickListener() {
                                                          @Override
                                                          public void onClick(
                                                                  DialogInterface d,
                                                                  int id) {
                                                              d.cancel();
                                                          }
                                                      })
                                              .setOnCancelListener(
                                                      new DialogInterface.OnCancelListener() {
                                                          @Override
                                                          public void onCancel(
                                                                  DialogInterface d) {
                                                              if (d != null) {
                                                                  d.cancel();
                                                              }
                                                          }
                                                      });
                                       AlertDialog alert = builder.create();
                                       alert.show();
                                   }

                                   @Override
                                   public void onUnhandledResult(
                                           MultiStepResult result) {
                                   }

                                   @Override
                                   public void onUserCancel() {
                                   }

                                   @Override
                                   public StepTask<?> newTask(MultiStepResultHelper helper,
                                           int useraction, String selection, String argument) {
                                       return ct = new CancelTask(helper, useraction, selection,
                                               argument);
                                   }
                               });
                               msrhCancel.start();
                           }
                       })
               .setOnCancelListener(new DialogInterface.OnCancelListener() {
                   @Override
                   public void onCancel(DialogInterface d) {
                       if (d != null) {
                           d.cancel();
                       }
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void prolong(final String a) {
        long age = System.currentTimeMillis() - refreshtime;
        if (refreshing || age > MAX_CACHE_AGE) {
            Toast.makeText(getActivity(), R.string.account_no_concurrent,
                    Toast.LENGTH_LONG).show();
            if (!refreshing) {
                refresh();
            }
            return;
        }
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        OpacApi api;
        try {
            api = app.getApi();
        } catch (OpacClient.LibraryRemovedException e) {
            return;
        }
        if (sp.getBoolean("prolong_fee_warning_ignore", false) ||
                app.getLibrary().isSuppressFeeWarnings() ||
                (api.getSupportFlags() & OpacApi.SUPPORT_FLAG_WARN_PROLONG_FEES) > 0) {
            prolongPerform(a);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    getActivity());
            View content = getActivity().getLayoutInflater()
                                        .inflate(R.layout.dialog_prolong_confirm, null);
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
                                       sp.edit().putBoolean("prolong_fee_warning_ignore", true)
                                         .apply();
                                   }
                                   prolongPerform(a);
                               }
                           });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    protected void prolongPerform(final String a) {
        MultiStepResultHelper<String> msrhProlong =
                new MultiStepResultHelper<>(
                        getActivity(), a, R.string.doing_prolong);
        msrhProlong.setCallback(new Callback<String>() {
            @Override
            public void onSuccess(MultiStepResult result) {
                if (getActivity() == null) {
                    return;
                }
                invalidateData();

                if (result.getMessage() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            getActivity());
                    builder.setMessage(result.getMessage())
                           .setCancelable(false)
                           .setNegativeButton(R.string.close,
                                   new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(
                                               DialogInterface dialog,
                                               int id) {
                                           dialog.cancel();
                                       }
                                   });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }

            @Override
            public void onError(MultiStepResult result) {
                if (getActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setMessage(result.getMessage())
                       .setCancelable(true)
                       .setNegativeButton(R.string.close,
                               new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface d,
                                           int id) {
                                       d.cancel();
                                   }
                               })
                       .setOnCancelListener(
                               new DialogInterface.OnCancelListener() {
                                   @Override
                                   public void onCancel(DialogInterface d) {
                                       if (d != null) {
                                           d.cancel();
                                       }
                                   }
                               });
                AlertDialog alert = builder.create();
                alert.show();
            }

            @Override
            public void onUnhandledResult(MultiStepResult result) {
            }

            @Override
            public void onUserCancel() {
            }

            @Override
            public StepTask<?> newTask(MultiStepResultHelper helper,
                    int useraction,
                    String selection, String argument) {
                return new ProlongTask(helper, useraction, selection,
                        argument);
            }
        });
        msrhProlong.start();

    }

    public void download(final String a) {
        MultiStepResultHelper<String> msrhDownload = new MultiStepResultHelper<>(
                getActivity(), a, R.string.doing_download);
        msrhDownload.setCallback(new Callback<String>() {
            @Override
            public void onSuccess(MultiStepResult res) {
                final EbookServiceApi.DownloadResult result = (EbookServiceApi.DownloadResult) res;
                if (result.getUrl() != null) {
                    if (result.getUrl().contains("acsm") || (a.contains("overdrive") && !result.getUrl().contains("epub-sample") && (result.getUrl().contains(".odm") || result.getUrl().contains(".epub")))) {
                        String[] download_clients = new String[]{
                                "com.android.aldiko", "com.aldiko.android",
                                "com.bluefirereader",
                                "com.mantano.reader.android.lite",
                                "com.overdrive.mobile.android.mediaconsole",
                                "com.datalogics.dlreader",
                                "com.mantano.reader.android.normal",
                                "com.mantano.reader.android", "com.neosoar"};
                        if (a.contains("overdrive") && result.getUrl().contains(".odm")) {
                            download_clients = new String[] {
                                    "com.overdrive.mobile.android.mediaconsole",
                            };
                        }
                        boolean found = false;
                        PackageManager pm = getActivity().getPackageManager();
                        for (String id : download_clients) {
                            try {
                                pm.getPackageInfo(id, 0);
                                found = true;
                            } catch (NameNotFoundException e) {
                            }
                        }
                        final SharedPreferences sp = PreferenceManager
                                .getDefaultSharedPreferences(getActivity());
                        if (!found && !sp.contains("reader_needed_ignore")) {

                            int msg = R.string.reader_needed;
                            if (a.contains("overdrive")) {
                                msg = R.string.reader_needed_overdrive;
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(
                                    getActivity());
                            builder.setMessage(msg)
                                   .setCancelable(true)
                                   .setNegativeButton(R.string.cancel,
                                           new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(
                                                       DialogInterface dialog, int id) {
                                                   dialog.cancel();
                                               }
                                           })
                                   .setNeutralButton(R.string.reader_needed_ignore,
                                           new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(
                                                       DialogInterface dialog, int id) {
                                                   Intent i = new Intent(
                                                           Intent.ACTION_VIEW);
                                                   i.setData(Uri.parse(result.getUrl()));
                                                   sp.edit()
                                                     .putBoolean("reader_needed_ignore", true)
                                                     .commit();
                                                   startActivity(i);
                                               }
                                           })
                                   .setPositiveButton(R.string.download,
                                           new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(
                                                       DialogInterface dialog, int id) {
                                                   dialog.cancel();
                                                   String reader = "com.bluefirereader";
                                                   if (a.toLowerCase().contains("overdrive")) {
                                                       reader = "com.overdrive.mobile.android.mediaconsole";
                                                   }
                                                   Intent i = new Intent(
                                                           Intent.ACTION_VIEW,
                                                           Uri.parse(
                                                                   "market://details?id=" + reader));
                                                   startActivity(i);
                                               }
                                           });
                            AlertDialog alert = builder.create();
                            alert.show();
                            return;
                        }
                    }
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(result.getUrl()));
                    startActivity(i);
                }
            }

            @Override
            public void onError(MultiStepResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setMessage(result.getMessage())
                       .setCancelable(true)
                       .setNegativeButton(
                               R.string.close,
                               new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(
                                           DialogInterface d,
                                           int id) {
                                       d.cancel();
                                   }
                               })
                       .setOnCancelListener(
                               new DialogInterface.OnCancelListener() {
                                   @Override
                                   public void onCancel(
                                           DialogInterface d) {
                                       if (d != null) {
                                           d.cancel();
                                       }
                                   }
                               });
                AlertDialog alert = builder.create();
                alert.show();
            }

            @Override
            public void onUnhandledResult(
                    MultiStepResult result) {
            }

            @Override
            public void onUserCancel() {
            }

            @Override
            public StepTask<?> newTask(MultiStepResultHelper helper,
                    int useraction, String selection, String argument) {
                return dt = new DownloadTask(helper, useraction, selection,
                        argument);
            }
        });
        msrhDownload.start();
    }

    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
        swipeRefreshLayout.setRefreshing(refreshing);
    }

    public void invalidateData() {
        AccountDataSource adatasource = new AccountDataSource(getActivity());
        adatasource.invalidateCachedAccountData(account);
        swipeRefreshLayout.setVisibility(View.GONE);
        accountSelected(account);
    }

    public void show_connectivity_error(Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
        if (getActivity() == null) {
            return;
        }
        if (e instanceof OpacErrorException) {
            AccountDataSource adatasource = new AccountDataSource(getActivity());
            adatasource.invalidateCachedAccountData(account);
            dialog_wrong_credentials(e.getMessage());
            return;
        }
        if (getView() != null) {
            final FrameLayout errorView = (FrameLayout) getView().findViewById(
                    R.id.error_view);
            errorView.removeAllViews();
            View connError = getActivity().getLayoutInflater().inflate(
                    R.layout.error_connectivity, errorView);

            TextView tvErrBody = (TextView) connError.findViewById(R.id.tvErrBody);
            Button btnRetry = (Button) connError.findViewById(R.id.btRetry);
            btnRetry.setVisibility(View.VISIBLE);
            if (e != null && e instanceof SSLSecurityException) {
                tvErrBody.setText(R.string.connection_error_detail_security);
            } else if (e != null && e instanceof NotReachableException) {
                tvErrBody.setText(R.string.connection_error_detail_nre);
            } else if (e != null && e instanceof OpacClient.LibraryRemovedException) {
                tvErrBody.setText(R.string.library_removed_error);
                btnRetry.setVisibility(View.GONE);
            }
            btnRetry.setOnClickListener(new OnClickListener() {
                         @Override
                         public void onClick(View v) {
                             refresh();
                         }
                     });
            llLoading.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.GONE);
            connError.setVisibility(View.VISIBLE);
        }
    }

    protected void dialog_wrong_credentials(String s) {
        llLoading.setVisibility(View.GONE);
        answerErrorView.setVisibility(View.VISIBLE);
        btPrefs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AccountEditActivity.class);
                intent.putExtra(AccountEditActivity.EXTRA_ACCOUNT_ID, account.getId());
                startActivity(intent);
            }
        });
        tvErrBodyA.setText(s);
    }

    public void loaded(final AccountData result) {
        if (result.getAccount() == account.getId()) {
            // The account this data is for is still visible

            setRefreshing(false);

            refreshtime = System.currentTimeMillis();

            display(result, false);
        }

    }

    protected void displayHeader() {
        if (accountData == null || tvAccLabel == null || tvAccUser == null || tvAccCity == null ||
                tvPendingFees == null || tvPendingFeesLabel == null || tvValidUntil == null ||
                tvValidUntilLabel == null) {
            return;
        }

        tvAccLabel.setText(account.getLabel());
        tvAccUser.setText(account.getName());
        Library lib;
        try {
            lib = app.getLibrary(account.getLibrary());
            tvAccCity.setText(lib.getDisplayName());
        } catch (IOException e) {
            ErrorReporter.handleException(e);
            e.printStackTrace();
        } catch (JSONException e) {
            ErrorReporter.handleException(e);
        }

        if (accountData != null) {
            if (accountData.getPendingFees() != null) {
                tvPendingFeesLabel.setVisibility(View.VISIBLE);
                tvPendingFees.setVisibility(View.VISIBLE);
                tvPendingFees.setText(accountData.getPendingFees());
            } else {
                tvPendingFeesLabel.setVisibility(View.GONE);
                tvPendingFees.setVisibility(View.GONE);
            }
            if (accountData.getValidUntil() != null) {
                tvValidUntilLabel.setVisibility(View.VISIBLE);
                tvValidUntil.setVisibility(View.VISIBLE);
                tvValidUntil.setText(accountData.getValidUntil());
            } else {
                tvValidUntilLabel.setVisibility(View.GONE);
                tvValidUntil.setVisibility(View.GONE);
            }
        }
    }

    protected void displayResHeader() {
        if (accountData != null && tvResHeader != null) {
            tvResHeader.setText(getActivity().getString(R.string.reservations_head) + " (" +
                    accountData.getReservations().size() + ")");
        }
    }

    protected void displayLentHeader() {
        if (accountData != null && tvLentHeader != null) {
            tvLentHeader.setText(getActivity().getString(R.string.lent_head) + " (" +
                    accountData.getLent().size() + ")");
        }
    }

    private void displayWarning() {
        if (accountData != null && tvWarning != null) {
            if (accountData.getWarning() != null && accountData.getWarning().length() > 1) {
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setText(accountData.getWarning());
            } else {
                tvWarning.setVisibility(View.GONE);
            }
        }
    }

    public void displayAge() {
        try {
            if (tvAge == null) {
                return;
            }

            long age = System.currentTimeMillis() - refreshtime;
            if (age < 60 * 1000) {
                tvAge.setText(getResources().getString(R.string.account_up_to_date));
            } else if (age < (3600 * 1000)) {
                tvAge.setText(getResources()
                        .getQuantityString(R.plurals.account_age_minutes, (int) (age / (60 * 1000)),
                                (int) (age / (60 * 1000))));
            } else if (age < 24 * 3600 * 1000) {
                tvAge.setText(getResources()
                        .getQuantityString(R.plurals.account_age_hours, (int) (age / (3600 * 1000)),
                                (int) (age / (3600 * 1000))));

            } else {
                tvAge.setText(getResources().getQuantityString(R.plurals.account_age_days,
                        (int) (age / (24 * 3600 * 1000)), (int) (age / (24 * 3600 * 1000))));
            }
        } catch (java.lang.IllegalStateException e) {
            // as this is called from a handler it may be called
            // without an activity attached to this fragment
            // we do nothing about it
        }
    }

    private void displayReservedItems() {
        if (accountData != null && resAdapter != null) {
            resAdapter.setItems(accountData.getReservations());
        }
    }

    private void displayLentItems() {
        if (accountData != null && lentAdapter != null) {
            lentAdapter.setItems(accountData.getLent());
        }
    }

    public void display(final AccountData result, boolean fromcache) {
        accountData = result;
        if (getActivity() == null) {
            return;
        }
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        llLoading.setVisibility(View.GONE);
        unsupportedErrorView.setVisibility(View.GONE);
        answerErrorView.setVisibility(View.GONE);
        errorView.removeAllViews();

        this.fromcache = fromcache;

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(app.getApplicationContext());
        displayHeader();

        /*
            Lent items
         */
        final boolean notification_on =
                sp.getBoolean(SyncAccountAlarmListener.PREF_SYNC_SERVICE, false);
        boolean notification_problems = false;

        displayWarning();
        displayLentHeader();
        displayLentItems();
        if (lentEmpty != null) {
            // phone
            lentEmpty.setVisible(result.getLent().size() == 0);
        } else {
            // tablet
            tvLentEmpty.setVisibility(result.getLent().size() == 0 ? View.VISIBLE : View.GONE);
        }
        for (final LentItem item : result.getLent()) {
            try {
                if (notification_on && item.getDeadline() == null && !item.isEbook()) {
                    notification_problems = true;
                }
            } catch (Exception e) {
                notification_problems = true;
            }
        }

        if (notification_problems) {
            if (tvError != null) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(R.string.notification_problems);
            }
        }

        /*
            Reservations
         */
        displayResHeader();
        displayReservedItems();
        if (reservationsEmpty != null) {
            // phone
            reservationsEmpty.setVisible(result.getReservations().size() == 0);
        } else {
            // tablet
            tvReservationsEmpty
                    .setVisibility(result.getReservations().size() == 0 ? View.VISIBLE : View.GONE);
        }
        displayAge();

        boolean hideCovers = true;
        for (LentItem item : result.getLent()) {
            if (item.getMediaType() != null || item.getCover() != null) hideCovers = false;
        }
        for (ReservedItem item : result.getReservations()) {
            if (item.getMediaType() != null || item.getCover() != null) hideCovers = false;
        }
        lentAdapter.setCoversHidden(hideCovers);
        resAdapter.setCoversHidden(hideCovers);
    }

    public void bookingStart(String booking_info) {
        long age = System.currentTimeMillis() - refreshtime;
        if (refreshing || fromcache || age > MAX_CACHE_AGE) {
            Toast.makeText(getActivity(), R.string.account_no_concurrent, Toast.LENGTH_LONG).show();
            if (!refreshing) {
                refresh();
            }
            return;
        }
        DetailedItem item = new DetailedItem();
        item.setBookable(true);
        item.setBooking_info(booking_info);
        MultiStepResultHelper<DetailedItem> msrhBooking = new MultiStepResultHelper<>(
                getActivity(), item, R.string.doing_booking);
        msrhBooking.setCallback(new Callback<DetailedItem>() {
            @Override
            public void onSuccess(MultiStepResult result) {
                invalidateData();
            }

            @Override
            public void onError(MultiStepResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(result.getMessage()).setCancelable(true)
                       .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface d, int id) {
                               d.cancel();
                           }
                       }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface d) {
                        if (d != null) {
                            d.cancel();
                        }
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }

            @Override
            public void onUnhandledResult(MultiStepResult result) {
            }

            @Override
            public void onUserCancel() {
            }

            @Override
            public StepTask<?> newTask(MultiStepResultHelper helper, int useraction,
                    String selection, DetailedItem argument) {
                return new BookingTask(helper, useraction, selection, argument);
            }
        });
        msrhBooking.start();
    }

    public void prolongAllStart() {
        if (refreshing) {
            Toast.makeText(getActivity(), R.string.account_no_concurrent,
                    Toast.LENGTH_LONG).show();
            if (!refreshing) {
                refresh();
            }
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.prolong_all_confirm).setCancelable(true)
               .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface d, int id) {
                       d.cancel();
                   }
               }).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int id) {
                prolongAllDo();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void prolongAllDo() {

        MultiStepResultHelper<Void> msrhProlong = new MultiStepResultHelper<>(getActivity(), null,
                R.string.doing_prolong_all);
        msrhProlong.setCallback(new Callback<Void>() {
            @Override
            public void onSuccess(MultiStepResult result) {
                if (getActivity() == null) {
                    return;
                }
                ProlongAllResult res = (ProlongAllResult) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                if (res.getResults() != null) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    View view = inflater.inflate(R.layout.dialog_simple_list, null, false);

                    ListView lv = (ListView) view.findViewById(R.id.lvBibs);

                    lv.setAdapter(new ProlongAllResultAdapter(getActivity(), res.getResults()));
                    switch (result.getActionIdentifier()) {
                        case ReservationResult.ACTION_BRANCH:
                            builder.setTitle(R.string.branch);
                    }
                    builder.setView(view)
                           .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int id) {
                                   adialog.cancel();
                                   invalidateData();
                               }
                           });
                } else {
                    builder.setMessage(result.getMessage())
                           .setCancelable(true)
                           .setNegativeButton(R.string.close,
                                   new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface d,
                                               int id) {
                                           d.cancel();
                                       }
                                   })
                           .setOnCancelListener(
                                   new DialogInterface.OnCancelListener() {
                                       @Override
                                       public void onCancel(DialogInterface d) {
                                           if (d != null) {
                                               d.cancel();
                                           }
                                       }
                                   });
                }
                adialog = builder.create();
                adialog.show();
            }

            @Override
            public void onError(MultiStepResult result) {
                if (getActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setMessage(result.getMessage())
                       .setCancelable(true)
                       .setNegativeButton(R.string.close,
                               new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface d,
                                           int id) {
                                       d.cancel();
                                   }
                               })
                       .setOnCancelListener(
                               new DialogInterface.OnCancelListener() {
                                   @Override
                                   public void onCancel(DialogInterface d) {
                                       if (d != null) {
                                           d.cancel();
                                       }
                                   }
                               });
                AlertDialog alert = builder.create();
                alert.show();
            }

            @Override
            public void onUnhandledResult(MultiStepResult result) {
            }

            @Override
            public void onUserCancel() {
            }

            @Override
            public StepTask<?> newTask(MultiStepResultHelper helper, int useraction,
                    String selection, Void argument) {
                return new ProlongAllTask(helper, useraction, selection);
            }
        });
        msrhProlong.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (lt != null) {
                if (!lt.isCancelled()) {
                    lt.cancel(true);
                }
            }
            if (ct != null) {
                if (!ct.isCancelled()) {
                    ct.cancel(true);
                }
            }
            if (dt != null) {
                if (!dt.isCancelled()) {
                    dt.cancel(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class LoadTask extends AsyncTask<Void, Void, AccountData> {

        private Exception exception;

        @Override
        protected AccountData doInBackground(Void... voids) {
            AccountData data;
            try {
                data = app.getApi().account(account);

                if (data == null) {
                    return null;
                }
            } catch (IOException | OpacErrorException | OpacClient.LibraryRemovedException e) {
                exception = e;
                return null;
            } catch (Exception e) {
                ErrorReporter.handleException(e);
                exception = e;
                return null;
            }

            try {
                // save data
                AccountDataSource adatasource;
                if (getActivity() == null && OpacClient.getEmergencyContext() != null) {
                    adatasource = new AccountDataSource(OpacClient.getEmergencyContext());
                } else {
                    adatasource = new AccountDataSource(getActivity());
                }

                account.setPasswordKnownValid(true);
                adatasource.update(account);
                adatasource.storeCachedAccountData(adatasource.getAccount(data.getAccount()), data);
            } finally {
                new ReminderHelper(app).generateAlarms();
            }

            return data;
        }

        @Override
        protected void onPostExecute(AccountData result) {
            if (exception == null && result != null) {
                loaded(result);
            } else {
                setRefreshing(false);

                show_connectivity_error(exception);
            }
        }
    }

    public class CancelTask extends StepTask<CancelResult> {
        private String itemId;

        public CancelTask(MultiStepResultHelper helper, int useraction, String selection,
                String itemId) {
            super(helper, useraction, selection);
            this.itemId = itemId;
        }

        @Override
        protected CancelResult doInBackground(Void... voids) {
            try {
                return app.getApi().cancel(itemId, account, useraction, selection);
            } catch (java.net.UnknownHostException | NoHttpResponseException | java.net
                    .SocketException e) {
                e.printStackTrace();
            } catch (Exception e) {
                ErrorReporter.handleException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(CancelResult result) {
            if (getActivity() == null) {
                return;
            }

            if (result == null) {
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
            }

            super.onPostExecute(result);
        }
    }

    public class DownloadTask extends StepTask<EbookServiceApi.DownloadResult> {
        private String itemId;

        public DownloadTask(MultiStepResultHelper helper, int useraction, String selection,
                String itemId) {
            super(helper, useraction, selection);
            this.itemId = itemId;
        }

        @Override
        protected EbookServiceApi.DownloadResult doInBackground(Void... voids) {
            try {
                return ((EbookServiceApi) app.getApi()).downloadItem(account, itemId, useraction, selection);
            } catch (java.net.UnknownHostException | NoHttpResponseException
                    | java.net.SocketException e) {
                e.printStackTrace();
            } catch (Exception e) {
                ErrorReporter.handleException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(EbookServiceApi.DownloadResult result) {
            if (getActivity() == null) {
                return;
            }
            if (getActivity() == null || result == null) {
                return;
            }
            super.onPostExecute(result);
        }
    }

    public class ProlongTask extends
            MultiStepResultHelper.StepTask<ProlongResult> {
        private boolean success = true;
        private String itemId;

        public ProlongTask(MultiStepResultHelper helper, int useraction, String selection,
                String itemId) {
            super(helper, useraction, selection);
            this.itemId = itemId;
        }

        @Override
        protected ProlongResult doInBackground(Void... voids) {
            try {
                ProlongResult res = app.getApi().prolong(itemId, account,
                        useraction, selection);
                success = true;
                return res;
            } catch (java.net.UnknownHostException | NoHttpResponseException e) {
                publishProgress(e, "ioerror");
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            } catch (Exception e) {
                ErrorReporter.handleException(e);
                success = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(ProlongResult res) {
            if (getActivity() == null) {
                return;
            }

            super.onPostExecute(res);

            if (!success || res == null) {
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
            }
        }
    }

    public class ProlongAllTask extends
            MultiStepResultHelper.StepTask<ProlongAllResult> {

        public ProlongAllTask(MultiStepResultHelper helper, int useraction, String selection) {
            super(helper, useraction, selection);
        }

        @Override
        protected ProlongAllResult doInBackground(Void... voids) {
            try {
                return app.getApi().prolongAll(account,
                        useraction, selection);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                ErrorReporter.handleException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ProlongAllResult result) {
            if (getActivity() == null) {
                return;
            }

            if (result == null) {
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
            }

            super.onPostExecute(result);
        }
    }

    public class ProlongAllResultAdapter extends ArrayAdapter<Map<String, String>> {

        private List<Map<String, String>> objects;

        public ProlongAllResultAdapter(Context context, List<Map<String, String>> objects) {
            super(context, R.layout.simple_spinner_item, objects);
            this.objects = objects;
        }

        @Override
        public View getView(int position, View contentView, ViewGroup viewGroup) {
            View view;

            if (objects.get(position) == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(
                        R.layout.listitem_prolongall_result, viewGroup, false);
                return view;
            }

            Map<String, String> item = objects.get(position);

            if (contentView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(
                        R.layout.listitem_prolongall_result, viewGroup, false);
            } else {
                view = contentView;
            }

            TextView tvAuthor = (TextView) view.findViewById(R.id.tvAuthor);
            TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
            TextView tvOld = (TextView) view.findViewById(R.id.tvOld);
            TextView tvNew = (TextView) view.findViewById(R.id.tvNew);
            TextView tvMsg = (TextView) view.findViewById(R.id.tvMsg);

            tvAuthor.setVisibility(item
                    .containsKey(ProlongAllResult.KEY_LINE_AUTHOR) ? View.VISIBLE
                    : View.GONE);
            tvAuthor.setText(item.get(ProlongAllResult.KEY_LINE_AUTHOR));
            tvTitle.setText(item.get(ProlongAllResult.KEY_LINE_TITLE));
            tvOld.setText(item.get(ProlongAllResult.KEY_LINE_OLD_RETURNDATE));
            tvNew.setText(item.get(ProlongAllResult.KEY_LINE_NEW_RETURNDATE));

            tvMsg.setText(item.get(ProlongAllResult.KEY_LINE_MESSAGE));
            return view;
        }

    }

    public class BookingTask extends StepTask<BookingResult> {
        private DetailedItem item;

        public BookingTask(MultiStepResultHelper helper, int useraction, String selection,
                DetailedItem item) {
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


    @Override
    public void onClick(LentItem item, LentAdapter.ViewHolder view) {
        showDetailActivity(item, view);
    }

    @Override
    public void onClick(ReservedItem item, ReservationsAdapter.ViewHolder view) {
        showDetailActivity(item, view);
    }

    private void showDetailActivity(AccountItem item, AccountAdapter.ViewHolder view) {
        Intent intent = new Intent(getContext(), AccountItemDetailActivity.class);
        intent.putExtra(AccountItemDetailActivity.EXTRA_ITEM, item);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(getActivity(), view.itemView,
                        getString(R.string.transition_background));
        ActivityCompat
                .startActivityForResult(getActivity(), intent, REQUEST_DETAIL, options.toBundle());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_DETAIL && intent != null &&
                intent.hasExtra(AccountItemDetailActivity.EXTRA_DATA)) {
            String data = intent.getStringExtra(AccountItemDetailActivity.EXTRA_DATA);
            switch (resultCode) {
                case AccountItemDetailActivity.RESULT_PROLONG:
                    prolong(data);
                    break;
                case AccountItemDetailActivity.RESULT_DOWNLOAD:
                    download(data);
                    break;
                case AccountItemDetailActivity.RESULT_CANCEL:
                    cancel(data);
                    break;
                case AccountItemDetailActivity.RESULT_BOOKING:
                    bookingStart(data);
                    break;
            }
        }
    }
}
