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

import android.annotation.SuppressLint;
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
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import org.apache.http.NoHttpResponseException;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.reminder.ReminderHelper;
import de.geeksfactory.opacclient.reminder.SyncAccountAlarmListener;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.ui.AppCompatProgressDialog;
import de.geeksfactory.opacclient.ui.ExpandingCardListManager;
import de.geeksfactory.opacclient.utils.ErrorReporter;

public class AccountFragment extends Fragment implements
        AccountSelectedListener {

    public static final long MAX_CACHE_AGE = (1000 * 3600 * 2);
    protected AppCompatProgressDialog dialog;
    protected AlertDialog adialog;
    protected OpacClient app;
    protected View view;
    protected ScrollView svAccount;
    protected FrameLayout errorView;
    protected View unsupportedErrorView, answerErrorView;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected Button btSend, btPrefs;
    protected LinearLayout llLent, llRes, llLoading;
    protected TextView tvError, tvResHeader, tvPendingFeesLabel, tvPendingFees, tvValidUntilLabel,
            tvValidUntil, tvAge, tvLentHeader, tvWarning, tvAccCity, tvAccUser, tvAccLabel,
            tvErrBodyA, tvErrHeadA, tvErrBodyU;
    protected ExpandingCardListManager lentManager;
    protected ExpandingCardListManager resManager;
    private LoadTask lt;
    private CancelTask ct;
    private DownloadTask dt;
    private Account account;
    private boolean refreshing = false;
    private long refreshtime;
    private boolean fromcache;
    private boolean supported = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_account, container, false);
        findViews();
        app = (OpacClient) getActivity().getApplication();
        account = app.getAccount();

        swipeRefreshLayout.setColorSchemeResources(R.color.primary_red);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        setHasOptionsMenu(true);

        accountSelected(app.getAccount());

        final Handler handler = new Handler();
        // schedule alarm here and post runnable as soon as scheduled
        handler.post(new Runnable() {
            @Override
            public void run() {
                refreshage();
                handler.postDelayed(this, 60000);
            }
        });

        return view;
    }

    private void findViews() {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        svAccount = (ScrollView) view.findViewById(R.id.svAccount);
        errorView = (FrameLayout) view.findViewById(R.id.error_view);
        unsupportedErrorView = view.findViewById(R.id.unsupported_error);
        answerErrorView = view.findViewById(R.id.answer_error);
        llLoading = (LinearLayout) view.findViewById(R.id.llLoading);
        tvErrBodyU = (TextView) view.findViewById(R.id.tvErrBodyU);
        btSend = (Button) view.findViewById(R.id.btSend);
        btPrefs = (Button) view.findViewById(R.id.btPrefs);
        tvErrHeadA = (TextView) view.findViewById(R.id.tvErrHeadA);
        tvErrBodyA = (TextView) view.findViewById(R.id.tvErrBodyA);
        tvAccLabel = (TextView) view.findViewById(R.id.tvAccLabel);
        tvAccUser = (TextView) view.findViewById(R.id.tvAccUser);
        tvAccCity = (TextView) view.findViewById(R.id.tvAccCity);
        llLent = (LinearLayout) view.findViewById(R.id.llLent);
        tvWarning = (TextView) view.findViewById(R.id.tvWarning);
        tvLentHeader = (TextView) view.findViewById(R.id.tvLentHeader);
        llRes = (LinearLayout) view.findViewById(R.id.llReservations);
        tvError = (TextView) view.findViewById(R.id.tvError);
        tvResHeader = (TextView) view.findViewById(R.id.tvResHeader);
        tvPendingFeesLabel = (TextView) view.findViewById(R.id.tvPendingFeesLabel);
        tvPendingFees = (TextView) view.findViewById(R.id.tvPendingFees);
        tvValidUntilLabel = (TextView) view.findViewById(R.id.tvValidUntilLabel);
        tvValidUntil = (TextView) view.findViewById(R.id.tvValidUntil);
        tvAge = (TextView) view.findViewById(R.id.tvAge);
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_account, menu);
        if (app.getAccount() != null && (
                app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL) != 0) {
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
            if (item.getDeadline() != null)
                appendIfNotEmpty(string, fmt2.print(item.getDeadline()),
                        R.string.accountdata_lent_deadline);
            appendIfNotEmpty(string, item.getHomeBranch(), R.string.accountdata_lent_home_branch);
            appendIfNotEmpty(string, item.getLendingBranch(),
                    R.string.accountdata_lent_lending_branch);
            string.append("\n");
        }

        if (data.getLent().size() == 0) {
            string.append(getResources().getString(R.string.entl_none));
        }

        string.append(getResources().getString(R.string.reservations_head));
        string.append("\n\n");
        for (ReservedItem item : data.getReservations()) {
            appendIfNotEmpty(string, item.getTitle(), R.string.accountdata_title);
            appendIfNotEmpty(string, item.getAuthor(), R.string.accountdata_author);
            appendIfNotEmpty(string, item.getFormat(), R.string.accountdata_format);
            appendIfNotEmpty(string, item.getStatus(), R.string.accountdata_status);
            if (item.getReadyDate() != null)
                appendIfNotEmpty(string, fmt2.print(item.getReadyDate()),
                        R.string.accountdata_reserved_ready_date);
            if (item.getExpirationDate() != null)
                appendIfNotEmpty(string, fmt2.print(item.getExpirationDate()),
                        R.string.accountdata_reserved_expiration_date);
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

        svAccount.setVisibility(View.GONE);
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
        }
        if (api != null && !app.getLibrary().isAccountSupported()) {
            supported = false;
            // Not supported with this api at all
            llLoading.setVisibility(View.GONE);
            unsupportedErrorView.setVisibility(
                    View.VISIBLE);
            tvErrBodyU.setText(R.string.account_unsupported_api);
            btSend.setText(R.string.write_mail);
            btSend.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent emailIntent = new Intent(
                            android.content.Intent.ACTION_SEND);
                    emailIntent.putExtra(
                            android.content.Intent.EXTRA_EMAIL,
                            new String[]{"info@opacapp.de"});
                    emailIntent
                            .putExtra(
                                    android.content.Intent.EXTRA_SUBJECT,
                                    "Bibliothek "
                                            + app.getLibrary()
                                                 .getIdent());
                    emailIntent.putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            getResources().getString(
                                    R.string.interested_to_help));
                    emailIntent.setType("text/plain");
                    startActivity(Intent.createChooser(emailIntent,
                            getString(R.string.write_mail)));
                }
            });

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
                displaydata(adatasource.getCachedAccountData(account), true);
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

    protected void cancel(final String a) {
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

    protected void prolong(final String a) {
        long age = System.currentTimeMillis() - refreshtime;
        if (refreshing || age > MAX_CACHE_AGE) {
            Toast.makeText(getActivity(), R.string.account_no_concurrent,
                    Toast.LENGTH_LONG).show();
            if (!refreshing) {
                refresh();
            }
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.prolong_confirm)
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

    protected void download(final String a) {
        if (app.getApi() instanceof EbookServiceApi) {
            dialog = AppCompatProgressDialog.show(getActivity(), "",
                    getString(R.string.doing_download), true);
            dialog.show();
            dt = new DownloadTask(a);
            dt.execute();
        }
    }

    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
        swipeRefreshLayout.setRefreshing(refreshing);
    }

    public void invalidateData() {
        AccountDataSource adatasource = new AccountDataSource(getActivity());
        adatasource.invalidateCachedAccountData(account);
        svAccount.setVisibility(View.GONE);
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

            if (e != null && e instanceof SSLSecurityException) {
                ((TextView) connError.findViewById(R.id.tvErrBody))
                        .setText(R.string.connection_error_detail_security);
            } else if (e != null && e instanceof NotReachableException) {
                ((TextView) connError.findViewById(R.id.tvErrBody))
                        .setText(R.string.connection_error_detail_nre);
            }
            connError.findViewById(R.id.btRetry)
                     .setOnClickListener(new OnClickListener() {
                         @Override
                         public void onClick(View v) {
                             refresh();
                         }
                     });
            llLoading.setVisibility(View.GONE);
            svAccount.setVisibility(View.GONE);
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

            displaydata(result, false);
        }

    }

    @SuppressWarnings("deprecation")
    public void displaydata(final AccountData result, boolean fromcache) {
        if (getActivity() == null) {
            return;
        }
        svAccount.setVisibility(View.VISIBLE);
        llLoading.setVisibility(View.GONE);
        unsupportedErrorView.setVisibility(View.GONE);
        answerErrorView.setVisibility(View.GONE);
        errorView.removeAllViews();

        this.fromcache = fromcache;

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(app.getApplicationContext());
        final int tolerance = Integer.parseInt(sp.getString("notification_warning", "3"));

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

        /*
            Lent items
         */

        llLent.removeAllViews();

        final boolean notification_on = sp.getBoolean(SyncAccountAlarmListener.PREF_SYNC_SERVICE, false);
        boolean notification_problems = false;

        if (tvWarning != null) {
            if (result.getWarning() != null && result.getWarning().length() > 1) {
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setText(result.getWarning());
            } else {
                tvWarning.setVisibility(View.GONE);
            }
        }

        if (result.getLent().size() == 0) {
            TextView t1 = new TextView(getActivity());
            t1.setText(R.string.entl_none);
            llLent.addView(t1);
            tvLentHeader.setText(getActivity().getString(R.string.lent_head) + " (0)");
        } else {
            tvLentHeader.setText(
                    getActivity().getString(R.string.lent_head) + " (" + result.getLent().size() +
                            ")");

            lentManager = new ExpandingCardListManager(getActivity(), llLent) {
                @Override
                public View getView(final int position, ViewGroup container) {
                    final View v = getLayoutInflater(null)
                            .inflate(R.layout.listitem_account_lent, container, false);
                    LentViewHolder holder = new LentViewHolder();
                    holder.findViews(v);

                    final LentItem item = result.getLent().get(position);

                    // Expanding and closing details
                    v.setClickable(true);
                    v.setFocusable(true);
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (getExpandedPosition() != position) {
                                expand(position);
                            } else {
                                collapse();
                            }
                        }
                    });

                    if (item.getId() != null) {
                        // Connection to detail view
                        holder.ivDetails.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View clicked) {
                                Intent intent = new Intent(getActivity(),
                                        SearchResultDetailActivity.class);
                                intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
                                        item.getId());
                                ActivityOptionsCompat options = ActivityOptionsCompat
                                        .makeScaleUpAnimation(v, v.getLeft(), v.getTop(),
                                                v.getWidth(), v.getHeight());
                                ActivityCompat
                                        .startActivity(getActivity(), intent, options.toBundle());
                            }
                        });
                        holder.hasDetailLink = true;
                    }

                    // Overview (Title/Author, Status/Deadline)
                    if (item.getTitle() != null && item.getAuthor() != null) {
                        holder.tvTitleAndAuthor.setText(item.getTitle() + ", " + item.getAuthor());
                    } else if (item.getTitle() != null) {
                        holder.tvTitleAndAuthor.setText(item.getTitle());
                    } else {
                        setTextOrHide(item.getAuthor(), holder.tvTitleAndAuthor);
                    }

                    DateTimeFormatter fmt = DateTimeFormat.shortDate();

                    if (item.getDeadline() != null && item.getStatus() != null) {
                        holder.tvStatus.setText(fmt.print(item.getDeadline()) + " (" +
                                Html.fromHtml(item.getStatus()) + ")");
                    } else if (item.getDeadline() != null) {
                        holder.tvStatus.setText(fmt.print(new LocalDate(item.getDeadline())));
                    } else {
                        setHtmlTextOrHide(item.getStatus(), holder.tvStatus);
                    }

                    // Detail
                    setTextOrHide(item.getAuthor(), holder.tvAuthorDetail);
                    setHtmlTextOrHide(item.getFormat(), holder.tvFormatDetail);
                    if (item.getLendingBranch() != null && item.getHomeBranch() != null) {
                        holder.tvBranchDetail.setText(Html.fromHtml(
                                item.getLendingBranch() + " / " + item.getHomeBranch()));
                    } else if (item.getLendingBranch() != null) {
                        holder.tvBranchDetail.setText(Html.fromHtml(item.getLendingBranch()));
                    } else {
                        setHtmlTextOrHide(item.getHomeBranch(), holder.tvBranchDetail);
                    }

                    // Color codes for return dates
                    if (item.getDeadline() != null) {
                        if (item.getDeadline().equals(LocalDate.now()) ||
                                item.getDeadline().isBefore(LocalDate.now())) {
                            holder.vStatusColor.setBackgroundColor(
                                    getResources().getColor(R.color.date_overdue));
                        } else if (
                                Days.daysBetween(LocalDate.now(), item.getDeadline()).getDays() <=
                                        tolerance) {
                            holder.vStatusColor.setBackgroundColor(
                                    getResources().getColor(R.color.date_warning));
                        } else if (item.getDownloadData() != null) {
                            holder.vStatusColor.setBackgroundColor(
                                    getResources().getColor(R.color.account_downloadable));
                        }
                    } else if (item.getDownloadData() != null) {
                        holder.vStatusColor.setBackgroundColor(
                                getResources().getColor(R.color.account_downloadable));
                    }

                    if (item.getProlongData() != null) {
                        holder.ivProlong.setTag(item.getProlongData());
                        holder.ivProlong.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                prolong((String) arg0.getTag());
                            }
                        });
                        holder.ivProlong.setVisibility(View.VISIBLE);
                        holder.ivProlong.setAlpha(item.isRenewable() ? 255 : 100);
                    } else if (item.getDownloadData() != null &&
                            app.getApi() instanceof EbookServiceApi) {
                        holder.ivDownload.setTag(item.getDownloadData());
                        holder.ivDownload.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                download((String) arg0.getTag());
                            }
                        });
                        holder.ivProlong.setVisibility(View.GONE);
                        holder.ivDownload.setVisibility(View.VISIBLE);
                    } else {
                        holder.ivProlong.setVisibility(View.INVISIBLE);
                    }
                    v.setTag(holder);
                    return v;
                }

                @Override
                public void expandView(int position, View view) {
                    LentViewHolder holder = (LentViewHolder) view.getTag();
                    LentItem item = result.getLent().get(position);

                    holder.llDetails.setVisibility(View.VISIBLE);
                    setHtmlTextOrHide(item.getTitle(), holder.tvTitleAndAuthor);
                    if (holder.hasDetailLink) holder.ivDetails.setVisibility(View.VISIBLE);
                }

                @Override
                public void collapseView(int position, View view) {
                    LentViewHolder holder = (LentViewHolder) view.getTag();
                    LentItem item = result.getLent().get(position);

                    holder.llDetails.setVisibility(View.GONE);
                    if (item.getTitle() != null && item.getAuthor() != null) {
                        holder.tvTitleAndAuthor.setText(item.getTitle() + ", " + item.getAuthor());
                    } else if (item.getAuthor() != null) {
                        holder.tvTitleAndAuthor.setText(item.getAuthor());
                        holder.tvTitleAndAuthor.setVisibility(View.VISIBLE);
                    }
                    holder.ivDetails.setVisibility(View.GONE);
                }

                @Override
                public int getCount() {
                    return result.getLent().size();
                }
            };
            lentManager
                    .setAnimationInterceptor(new ExpandingCardListManager.AnimationInterceptor() {
                        private float llDataY;
                        private float llDataTranslationY = 0;

                        @Override
                        public void beforeExpand(View unexpandedView) {
                            LentViewHolder holder = (LentViewHolder) unexpandedView.getTag();
                            llDataY = ViewHelper.getY(holder.llData);
                        }

                        @Override
                        public Collection<Animator> getExpandAnimations(int heightDifference,
                                                                        View expandedView) {
                            LentViewHolder holder = (LentViewHolder) expandedView.getTag();
                            Collection<Animator> anims = getAnimations(-heightDifference, 0);
                            // Animate buttons to the side
                            int difference = 2 * (getResources()
                                    .getDimensionPixelSize(R.dimen.card_side_margin_selected) -
                                    getResources().getDimensionPixelSize(
                                            R.dimen.card_side_margin_default));
                            anims.add(ObjectAnimator
                                    .ofFloat(holder.llButtons, "translationX", difference, 0));
                            // Animate llData to the bottom if required
                            if (ViewHelper.getY(holder.llData) != llDataY) {
                                ViewHelper.setY(holder.llData, llDataY);
                                llDataTranslationY = ViewHelper.getTranslationY(holder.llData);
                                anims.add(ObjectAnimator.ofFloat(holder.llData, "translationY", 0));
                            } else {
                                llDataTranslationY = 0;
                            }
                            return anims;
                        }

                        @Override
                        public Collection<Animator> getCollapseAnimations(int heightDifference,
                                                                          View expandedView) {
                            LentViewHolder holder = (LentViewHolder) expandedView.getTag();
                            Collection<Animator> anims = getAnimations(0, heightDifference);
                            // Animate buttons back
                            int difference = 2 * (getResources()
                                    .getDimensionPixelSize(R.dimen.card_side_margin_selected) -
                                    getResources().getDimensionPixelSize(
                                            R.dimen.card_side_margin_default));
                            anims.add(ObjectAnimator
                                    .ofFloat(holder.llButtons, "translationX", 0, difference));
                            // Animate llData back
                            anims.add(ObjectAnimator
                                    .ofFloat(holder.llData, "translationY", llDataTranslationY));
                            return anims;
                        }

                        @Override
                        public void onCollapseAnimationEnd() {
                            if (view.findViewById(R.id.rlMeta) != null) {
                                // tablet
                                ViewHelper.setTranslationY(view.findViewById(R.id.rlMeta), 0);
                            } else {
                                // phone
                                ViewHelper.setTranslationY(tvResHeader, 0);
                                ViewHelper.setTranslationY(llRes, 0);
                                ViewHelper.setTranslationY(tvAge, 0);
                                ViewHelper.setTranslationY(view.findViewById(R.id.tvNoWarranty), 0);
                            }
                        }

                        private Collection<Animator> getAnimations(float from, float to) {
                            List<Animator> animators = new ArrayList<>();
                            if (view.findViewById(R.id.rlMeta) != null) {
                                // tablet
                                if (result.getLent().size() >= result.getReservations().size()) {
                                    animators.add(ObjectAnimator
                                            .ofFloat(view.findViewById(R.id.rlMeta), "translationY",
                                                    from, to));
                                }
                            } else {
                                // phone
                                animators.add(ObjectAnimator
                                        .ofFloat(tvResHeader, "translationY", from, to));
                                animators.add(ObjectAnimator
                                        .ofFloat(llRes, "translationY", from, to));
                                animators.add(ObjectAnimator
                                        .ofFloat(tvAge, "translationY", from, to));
                                animators.add(ObjectAnimator
                                        .ofFloat(view.findViewById(R.id.tvNoWarranty),
                                                "translationY", from, to));
                            }
                            return animators;
                        }
                    });

            for (final LentItem item : result.getLent()) {
                try {
                    if (notification_on && item.getDeadline() == null && !item.isEbook()) {
                        notification_problems = true;
                    }
                } catch (Exception e) {
                    notification_problems = true;
                }
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
        llRes.removeAllViews();
        if (result.getReservations().size() == 0) {
            TextView t1 = new TextView(getActivity());
            t1.setText(R.string.reservations_none);
            llRes.addView(t1);
            tvResHeader.setText(getActivity().getString(R.string.reservations_head) + " (0)");
        } else {
            tvResHeader.setText(getActivity().getString(R.string.reservations_head) + " (" +
                    result.getReservations().size() + ")");
            resManager = new ExpandingCardListManager(getActivity(), llRes) {
                @Override
                public View getView(final int position, ViewGroup container) {
                    final View v = getLayoutInflater(null)
                            .inflate(R.layout.listitem_account_reservation, llRes, false);
                    ReservationViewHolder holder = new ReservationViewHolder();
                    holder.findViews(v);

                    final ReservedItem item = result.getReservations().get(position);

                    // Expanding and closing details
                    v.setClickable(true);
                    v.setFocusable(true);
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (getExpandedPosition() != position) {
                                expand(position);
                            } else {
                                collapse();
                            }
                        }
                    });

                    if (item.getId() != null) {
                        // Connection to detail view
                        holder.ivDetails.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View clicked) {
                                Intent intent = new Intent(getActivity(),
                                        SearchResultDetailActivity.class);
                                intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
                                        item.getId());
                                ActivityOptionsCompat options = ActivityOptionsCompat
                                        .makeScaleUpAnimation(v, v.getLeft(), v.getTop(),
                                                v.getWidth(), v.getHeight());
                                ActivityCompat
                                        .startActivity(getActivity(), intent, options.toBundle());
                            }
                        });
                        holder.hasDetailLink = true;
                    }

                    // Overview (Title/Author, Ready/Expire)
                    if (item.getTitle() != null && item.getAuthor() != null) {
                        holder.tvTitleAndAuthor.setText(item.getTitle() + ", " + item.getAuthor());
                    } else if (item.getTitle() != null) {
                        holder.tvTitleAndAuthor.setText(item.getTitle());
                    } else {
                        setTextOrHide(item.getAuthor(), holder.tvTitleAndAuthor);
                    }

                    DateTimeFormatter fmt = DateTimeFormat.shortDate();

                    StringBuilder status = new StringBuilder();
                    if (item.getStatus() != null) status.append(item.getStatus());
                    boolean needsBraces = item.getStatus() != null &&
                            (item.getReadyDate() != null || item.getExpirationDate() != null);
                    if (needsBraces) status.append(" (");
                    if (item.getReadyDate() != null) {
                        status.append(getString(R.string.reservation_expire_until)).append(" ")
                                .append(fmt.print(item.getReadyDate()));
                    }
                    if (item.getExpirationDate() != null) {
                        if (item.getReadyDate() != null) status.append(", ");
                        status.append(fmt.print(item.getExpirationDate()));
                    }
                    if (needsBraces) status.append(")");
                    if (status.length() > 0) {
                        holder.tvStatus.setText(Html.fromHtml(status.toString()));
                    } else {
                        holder.tvStatus.setVisibility(View.GONE);
                    }

                    // Detail
                    setTextOrHide(item.getAuthor(), holder.tvAuthorDetail);
                    setHtmlTextOrHide(item.getFormat(), holder.tvFormatDetail);
                    setHtmlTextOrHide(item.getBranch(), holder.tvBranchDetail);

                    if (item.getBookingData() != null) {
                        holder.ivBooking.setTag(item.getBookingData());
                        holder.ivBooking.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                bookingStart((String) arg0.getTag());
                            }
                        });
                        holder.ivBooking.setVisibility(View.VISIBLE);
                        holder.ivCancel.setVisibility(View.GONE);
                    } else if (item.getCancelData() != null) {
                        holder.ivCancel.setTag(item.getCancelData());
                        holder.ivCancel.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                cancel((String) arg0.getTag());
                            }
                        });
                        holder.ivCancel.setVisibility(View.VISIBLE);
                        holder.ivBooking.setVisibility(View.GONE);
                    } else {
                        holder.ivCancel.setVisibility(View.INVISIBLE);
                        holder.ivBooking.setVisibility(View.GONE);
                    }
                    v.setTag(holder);
                    return v;
                }

                @Override
                public void expandView(int position, View view) {
                    ReservationViewHolder holder = (ReservationViewHolder) view.getTag();
                    ReservedItem item = result.getReservations().get(position);

                    holder.llDetails.setVisibility(View.VISIBLE);
                    setTextOrHide(item.getTitle(), holder.tvTitleAndAuthor);
                    if (holder.hasDetailLink) holder.ivDetails.setVisibility(View.VISIBLE);
                }

                @Override
                public void collapseView(int position, View view) {
                    ReservationViewHolder holder = (ReservationViewHolder) view.getTag();
                    ReservedItem item = result.getReservations().get(position);

                    holder.llDetails.setVisibility(View.GONE);
                    if (item.getTitle() != null && item.getAuthor() != null) {
                        holder.tvTitleAndAuthor.setText(item.getTitle() + ", " + item.getAuthor());
                    } else if (item.getTitle() != null) {
                        holder.tvTitleAndAuthor.setText(item.getAuthor());
                        holder.tvTitleAndAuthor.setVisibility(View.VISIBLE);
                    }
                    holder.ivDetails.setVisibility(View.GONE);
                }

                @Override
                public int getCount() {
                    return result.getReservations().size();
                }
            };
            resManager.setAnimationInterceptor(new ExpandingCardListManager.AnimationInterceptor() {
                private float llDataY;
                private float llDataTranslationY = 0;

                @Override
                public void beforeExpand(View unexpandedView) {
                    ReservationViewHolder holder = (ReservationViewHolder) unexpandedView.getTag();
                    llDataY = ViewHelper.getY(holder.llData);
                }

                @Override
                public Collection<Animator> getExpandAnimations(int heightDifference,
                                                                View expandedView) {
                    ReservationViewHolder holder = (ReservationViewHolder) expandedView.getTag();
                    Collection<Animator> anims = getAnimations(-heightDifference, 0);
                    // Animate buttons to the side
                    int difference = 2 * (getResources()
                            .getDimensionPixelSize(R.dimen.card_side_margin_selected) -
                            getResources().getDimensionPixelSize(R.dimen.card_side_margin_default));
                    anims.add(ObjectAnimator
                            .ofFloat(holder.llButtons, "translationX", difference, 0));
                    // Animate llData to the bottom if required
                    if (ViewHelper.getY(holder.llData) != llDataY) {
                        ViewHelper.setY(holder.llData, llDataY);
                        llDataTranslationY = ViewHelper.getTranslationY(holder.llData);
                        anims.add(ObjectAnimator.ofFloat(holder.llData, "translationY", 0));
                    } else {
                        llDataTranslationY = 0;
                    }
                    return anims;
                }

                @Override
                public Collection<Animator> getCollapseAnimations(int heightDifference,
                                                                  View expandedView) {
                    ReservationViewHolder holder = (ReservationViewHolder) expandedView.getTag();
                    Collection<Animator> anims = getAnimations(0, heightDifference);
                    // Animate buttons back
                    int difference = 2 * (getResources()
                            .getDimensionPixelSize(R.dimen.card_side_margin_selected) -
                            getResources().getDimensionPixelSize(R.dimen.card_side_margin_default));
                    anims.add(ObjectAnimator
                            .ofFloat(holder.llButtons, "translationX", 0, difference));
                    // Animate llData back
                    anims.add(ObjectAnimator
                            .ofFloat(holder.llData, "translationY", llDataTranslationY));
                    return anims;
                }

                @Override
                public void onCollapseAnimationEnd() {
                    if (view.findViewById(R.id.rlMeta) != null) {
                        // tablet
                        ViewHelper.setTranslationY(view.findViewById(R.id.rlMeta), 0);
                    } else {
                        // phone
                        ViewHelper.setTranslationY(tvAge, 0);
                        ViewHelper.setTranslationY(view.findViewById(R.id.tvNoWarranty), 0);
                    }
                }

                private Collection<Animator> getAnimations(float from, float to) {
                    List<Animator> animators = new ArrayList<>();
                    if (view.findViewById(R.id.rlMeta) != null) {
                        // tablet
                        if (result.getReservations().size() >= result.getLent().size()) {
                            animators.add(ObjectAnimator
                                    .ofFloat(view.findViewById(R.id.rlMeta), "translationY", from,
                                            to));
                        }
                    } else {
                        // phone
                        animators.add(ObjectAnimator.ofFloat(tvAge, "translationY", from, to));
                        animators.add(ObjectAnimator
                                .ofFloat(view.findViewById(R.id.tvNoWarranty), "translationY", from,
                                        to));
                    }
                    return animators;
                }
            });
        }

        if (result.getPendingFees() != null) {
            tvPendingFeesLabel.setVisibility(View.VISIBLE);
            tvPendingFees.setVisibility(View.VISIBLE);
            tvPendingFees.setText(result.getPendingFees());
        } else {
            tvPendingFeesLabel.setVisibility(View.GONE);
            tvPendingFees.setVisibility(View.GONE);
        }
        if (result.getValidUntil() != null) {
            tvValidUntilLabel.setVisibility(View.VISIBLE);
            tvValidUntil.setVisibility(View.VISIBLE);
            tvValidUntil.setText(result.getValidUntil());
        } else {
            tvValidUntilLabel.setVisibility(View.GONE);
            tvValidUntil.setVisibility(View.GONE);
        }
        refreshage();
    }

    private void setHtmlTextOrHide(String value, TextView tv) {
        if (!TextUtils.isEmpty(value)) {
            tv.setText(Html.fromHtml(value));
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void setTextOrHide(String value, TextView tv) {
        if (!TextUtils.isEmpty(value)) {
            tv.setText(value);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    public void refreshage() {
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

    public void bookingStart(String booking_info) {
        long age = System.currentTimeMillis() - refreshtime;
        if (refreshing || fromcache || age > MAX_CACHE_AGE) {
            Toast.makeText(getActivity(), R.string.account_no_concurrent, Toast.LENGTH_LONG).show();
            if (!refreshing) {
                refresh();
            }
            return;
        }
        DetailledItem item = new DetailledItem();
        item.setBookable(true);
        item.setBooking_info(booking_info);
        MultiStepResultHelper<DetailledItem> msrhBooking = new MultiStepResultHelper<>(
                getActivity(), item, R.string.doing_booking);
        msrhBooking.setCallback(new Callback<DetailledItem>() {
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
                                       String selection, DetailledItem argument) {
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

                LayoutInflater inflater = getLayoutInflater(null);

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
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.cancel();
            }
        }

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

    private class LentViewHolder {
        public TextView tvTitleAndAuthor;
        public TextView tvStatus;
        public TextView tvAuthorDetail;
        public TextView tvBranchDetail;
        public TextView tvFormatDetail;
        public ImageView ivProlong;
        public ImageView ivDownload;
        public ImageView ivDetails;
        public View vStatusColor;
        public LinearLayout llData;
        public LinearLayout llDetails;
        public LinearLayout llButtons;
        public boolean hasDetailLink;

        public void findViews(View v) {
            tvTitleAndAuthor = (TextView) v.findViewById(R.id.tvTitleAndAuthor);
            tvStatus = (TextView) v.findViewById(R.id.tvStatus);
            tvAuthorDetail = (TextView) v.findViewById(R.id.tvAuthorDetail);
            tvBranchDetail = (TextView) v.findViewById(R.id.tvBranchDetail);
            tvFormatDetail = (TextView) v.findViewById(R.id.tvFormatDetail);
            ivProlong = (ImageView) v.findViewById(R.id.ivProlong);
            ivDownload = (ImageView) v.findViewById(R.id.ivDownload);
            ivDetails = (ImageView) v.findViewById(R.id.ivDetails);
            vStatusColor = v.findViewById(R.id.vStatusColor);
            llData = (LinearLayout) v.findViewById(R.id.llData);
            llDetails = (LinearLayout) v.findViewById(R.id.llDetails);
            llButtons = (LinearLayout) v.findViewById(R.id.llButtons);
        }
    }

    private class ReservationViewHolder {
        public TextView tvTitleAndAuthor;
        public TextView tvStatus;
        public TextView tvAuthorDetail;
        public TextView tvBranchDetail;
        public TextView tvFormatDetail;
        public ImageView ivCancel;
        public ImageView ivBooking;
        public ImageView ivDetails;
        public LinearLayout llData;
        public LinearLayout llDetails;
        public LinearLayout llButtons;
        public boolean hasDetailLink;

        public void findViews(View v) {
            tvTitleAndAuthor = (TextView) v.findViewById(R.id.tvTitleAndAuthor);
            tvStatus = (TextView) v.findViewById(R.id.tvStatus);
            tvAuthorDetail = (TextView) v.findViewById(R.id.tvAuthorDetail);
            tvBranchDetail = (TextView) v.findViewById(R.id.tvBranchDetail);
            tvFormatDetail = (TextView) v.findViewById(R.id.tvFormatDetail);
            ivCancel = (ImageView) v.findViewById(R.id.ivCancel);
            ivBooking = (ImageView) v.findViewById(R.id.ivBooking);
            ivDetails = (ImageView) v.findViewById(R.id.ivDetails);
            llData = (LinearLayout) v.findViewById(R.id.llData);
            llDetails = (LinearLayout) v.findViewById(R.id.llDetails);
            llButtons = (LinearLayout) v.findViewById(R.id.llButtons);
        }
    }

    public class LoadTask extends AsyncTask<Void, Void, AccountData> {

        private Exception exception;

        @Override
        protected AccountData doInBackground(Void... voids) {
            try {
                AccountData data = app.getApi().account(account);

                if (data == null) {
                    return null;
                }

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

                new ReminderHelper(app).generateAlarms();

                return data;
            } catch (IOException | OpacErrorException e) {
                exception = e;
            } catch (Exception e) {
                ErrorReporter.handleException(e);
                exception = e;
            }
            return null;
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

    public class DownloadTask extends AsyncTask<Void, Void, String> {

        private String itemId;

        public DownloadTask(String itemId) {
            this.itemId = itemId;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return ((EbookServiceApi) app.getApi()).downloadItem(account, itemId);
        }

        @Override
        protected void onPostExecute(final String result) {
            dialog.dismiss();
            if (getActivity() == null) {
                return;
            }
            if (result.contains("acsm")) {
                String[] download_clients = new String[]{
                        "com.android.aldiko", "com.aldiko.android",
                        "com.bluefirereader",
                        "com.mantano.reader.android.lite",
                        "com.datalogics.dlreader",
                        "com.mantano.reader.android.normal",
                        "com.mantano.reader.android", "com.neosoar"};
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            getActivity());
                    builder.setMessage(R.string.reader_needed)
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
                                           i.setData(Uri.parse(result));
                                           sp.edit()
                                             .putBoolean(
                                                     "reader_needed_ignore",
                                                     true).commit();
                                           startActivity(i);
                                       }
                                   })
                           .setPositiveButton(R.string.download,
                                   new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(
                                               DialogInterface dialog, int id) {
                                           dialog.cancel();
                                           Intent i = new Intent(
                                                   Intent.ACTION_VIEW,
                                                   Uri.parse(
                                                           "market://details?id=de" +
                                                                   ".bluefirereader"));
                                           startActivity(i);
                                       }
                                   });
                    AlertDialog alert = builder.create();
                    alert.show();
                    return;
                }
            }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(result));
            startActivity(i);
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
}
