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
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.SSLSecurityException;
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
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
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
    private LoadTask lt;
    private CancelTask ct;
    private DownloadTask dt;
    private Account account;
    private boolean refreshing = false;
    private long refreshtime;
    private boolean fromcache;
    private boolean supported = true;

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

        if (getActivity().getIntent().getExtras() != null) {
            if (getActivity().getIntent().getExtras()
                             .containsKey("notifications")) {
                AccountDataSource adata = new AccountDataSource(getActivity());
                adata.open();
                Bundle notif = getActivity().getIntent().getExtras()
                                            .getBundle("notifications");
                Set<String> keys = notif.keySet();
                for (String key : keys) {
                    long[] val = notif.getLongArray(key);
                    adata.notificationSave(val[0], val[1]);
                }
                adata.close();

                if (getActivity().getIntent().getExtras().getLong("account") != app
                        .getAccount().getId()) {
                    app.setAccount(getActivity().getIntent().getExtras()
                                                .getLong("account"));
                    ((OpacActivity) getActivity()).accountSelected(app
                            .getAccount());
                }
                NotificationManager nMgr = (NotificationManager) getActivity()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                nMgr.cancel(OpacClient.NOTIF_ID);
            }
        }

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
        if ((app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL) != 0) {
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
        }
        return super.onOptionsItemSelected(item);
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
        if (api != null
                && !app.getLibrary().isAccountSupported()
                && (api.getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_EXTENDABLE) == 0) {
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

        } else if (api != null && !app.getLibrary().isAccountSupported()) {
            supported = false;

            // We need help
            llLoading.setVisibility(View.GONE);
            unsupportedErrorView.setVisibility(
                    View.VISIBLE);

            tvErrBodyU.setText(R.string.account_unsupported);
            btSend.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog = AppCompatProgressDialog.show(getActivity(), "",
                            getString(R.string.report_sending), true,
                            true, new OnCancelListener() {
                                @Override
                                public void onCancel(
                                        DialogInterface arg0) {
                                }
                            });
                    dialog.show();
                    new SendTask().execute();
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
            adatasource.open();
            refreshtime = adatasource.getCachedAccountDataTime(account);
            if (refreshtime > 0) {
                displaydata(adatasource.getCachedAccountData(account), true);
                if (System.currentTimeMillis() - refreshtime > MAX_CACHE_AGE) {
                    refresh();
                }
            } else {
                refresh();
            }
            adatasource.close();
        }
    }

    public void refresh() {

        if ((!app.getLibrary().isAccountSupported() && (app
                .getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_EXTENDABLE) == 0)
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

        MultiStepResultHelper<String> msrhProlong = new MultiStepResultHelper<>(
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
                                               DialogInterface dialog, int id) {
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
            public StepTask<?> newTask(MultiStepResultHelper helper, int useraction,
                    String selection, String argument) {
                return new ProlongTask(helper, useraction, selection, argument);
            }
        });
        msrhProlong.start();
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
        adatasource.open();
        adatasource.invalidateCachedAccountData(account);
        adatasource.close();
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
            adatasource.open();
            adatasource.invalidateCachedAccountData(account);
            adatasource.close();
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
                Intent intent = new Intent(getActivity(),
                        AccountEditActivity.class);
                intent.putExtra(AccountEditActivity.EXTRA_ACCOUNT_ID,
                        account.getId());
                startActivity(intent);
            }
        });
        tvErrBodyA.setText(s);
    }

    public void loaded(final AccountData result) {
        AccountDataSource adatasource;
        if (getActivity() == null && OpacClient.getEmergencyContext() != null) {
            adatasource = new AccountDataSource(
                    OpacClient.getEmergencyContext());
        } else {
            adatasource = new AccountDataSource(getActivity());
        }

        adatasource.open();
        adatasource.storeCachedAccountData(
                adatasource.getAccount(result.getAccount()), result);
        adatasource.close();

        if (result.getAccount() == account.getId()) {
            // The account this data is for is still visible

            setRefreshing(false);

            refreshtime = System.currentTimeMillis();

            displaydata(result, false);
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
        public ImageView ivClose;
        public View vStatusColor;
        public LinearLayout llDetails;

        public void findViews(View v) {
            tvTitleAndAuthor = (TextView) v.findViewById(R.id.tvTitleAndAuthor);
            tvStatus = (TextView) v.findViewById(R.id.tvStatus);
            tvAuthorDetail = (TextView) v.findViewById(R.id.tvAuthorDetail);
            tvBranchDetail = (TextView) v.findViewById(R.id.tvBranchDetail);
            tvFormatDetail = (TextView) v.findViewById(R.id.tvFormatDetail);
            ivProlong = (ImageView) v.findViewById(R.id.ivProlong);
            ivDownload = (ImageView) v.findViewById(R.id.ivDownload);
            ivDetails = (ImageView) v.findViewById(R.id.ivDetails);
            ivClose = (ImageView) v.findViewById(R.id.ivClose);
            vStatusColor = v.findViewById(R.id.vStatusColor);
            llDetails = (LinearLayout) v.findViewById(R.id.llDetails);
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
        public ImageView ivClose;
        public LinearLayout llDetails;

        public void findViews(View v) {
            tvTitleAndAuthor = (TextView) v.findViewById(R.id.tvTitleAndAuthor);
            tvStatus = (TextView) v.findViewById(R.id.tvStatus);
            tvAuthorDetail = (TextView) v.findViewById(R.id.tvAuthorDetail);
            tvBranchDetail = (TextView) v.findViewById(R.id.tvBranchDetail);
            tvFormatDetail = (TextView) v.findViewById(R.id.tvFormatDetail);
            ivCancel = (ImageView) v.findViewById(R.id.ivCancel);
            ivBooking = (ImageView) v.findViewById(R.id.ivBooking);
            ivDetails = (ImageView) v.findViewById(R.id.ivDetails);
            ivClose = (ImageView) v.findViewById(R.id.ivClose);
            llDetails = (LinearLayout) v.findViewById(R.id.llDetails);
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
        final long tolerance = Long.decode(sp.getString("notification_warning",
                "367200000"));

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

        final boolean notification_on = sp.getBoolean("notification_service", false);
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
                    View v = getLayoutInflater(null)
                            .inflate(R.layout.listitem_account_lent, container, false);
                    LentViewHolder holder = new LentViewHolder();
                    holder.findViews(v);

                    final Map<String, String> item = result.getLent().get(position);

                    if (item.containsKey(AccountData.KEY_LENT_ID)) {
                        // Connection to detail view
                        View.OnClickListener gotoDetails = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent =
                                        new Intent(getActivity(), SearchResultDetailActivity.class);
                                intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
                                        item.get(AccountData.KEY_LENT_ID));
                                ActivityOptionsCompat options = ActivityOptionsCompat
                                        .makeScaleUpAnimation(v, v.getLeft(), v.getTop(), v.getWidth(),
                                                v.getHeight());
                                ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
                            }
                        };
                        v.setClickable(true);
                        v.setFocusable(true);
                        v.setOnClickListener(gotoDetails);
                    }
                    // Expanding and closing details
                    holder.ivDetails.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View clicked) {
                            expand(position);
                        }
                    });
                    holder.ivClose.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View clicked) {
                            collapse();
                        }
                    });

                    // Overview (Title/Author, Status/Deadline)
                    CharSequence title = item.containsKey(AccountData.KEY_LENT_TITLE) ?
                            Html.fromHtml(item.get(AccountData.KEY_LENT_TITLE)) : null;
                    CharSequence author = item.containsKey(AccountData.KEY_LENT_AUTHOR) ?
                            Html.fromHtml(item.get(AccountData.KEY_LENT_AUTHOR)) : null;
                    if (title != null && author != null) {
                        holder.tvTitleAndAuthor.setText(title + ", " + author);
                    } else if (title != null) {
                        holder.tvTitleAndAuthor.setText(title);
                    } else if (author != null) {
                        holder.tvTitleAndAuthor.setText(author);
                    } else {
                        holder.tvTitleAndAuthor.setVisibility(View.GONE);
                    }

                    if (item.containsKey(AccountData.KEY_LENT_DEADLINE) &&
                            item.containsKey(AccountData.KEY_LENT_STATUS)) {
                        holder.tvStatus.setText(Html.fromHtml(item.get(AccountData.KEY_LENT_DEADLINE)) + " (" +
                                Html.fromHtml(item.get(AccountData.KEY_LENT_STATUS)) + ")");
                    } else if (item.containsKey(AccountData.KEY_LENT_DEADLINE)) {
                        holder.tvStatus.setText(Html.fromHtml(item.get(AccountData.KEY_LENT_DEADLINE)));
                    } else if (item.containsKey(AccountData.KEY_LENT_STATUS)) {
                        holder.tvStatus.setText(Html.fromHtml(item.get(AccountData.KEY_LENT_STATUS)));
                    } else {
                        holder.tvStatus.setVisibility(View.GONE);
                    }

                    // Detail
                    boolean hasDetails = false;
                    if (author != null) {
                        holder.tvAuthorDetail.setText(author);
                        hasDetails = true;
                    } else {
                        holder.tvAuthorDetail.setVisibility(View.GONE);
                    }
                    if (item.containsKey(AccountData.KEY_LENT_FORMAT)) {
                        holder.tvFormatDetail.setText(Html.fromHtml(item.get(AccountData.KEY_LENT_FORMAT)));
                        hasDetails = true;
                    } else {
                        holder.tvFormatDetail.setVisibility(View.GONE);
                    }
                    if (item.containsKey(AccountData.KEY_LENT_LENDING_BRANCH)) {
                        holder.tvBranchDetail
                                .setText(Html.fromHtml(item.get(AccountData.KEY_LENT_LENDING_BRANCH)));
                        hasDetails = true;
                    } else if (item.containsKey(AccountData.KEY_LENT_BRANCH)) {
                        holder.tvBranchDetail.setText(Html.fromHtml(item.get(AccountData.KEY_LENT_BRANCH)));
                        hasDetails = true;
                    } else {
                        holder.tvBranchDetail.setVisibility(View.GONE);
                    }

                    // If there are no more details, we don't need the button for them
                    if (!hasDetails) {
                        holder.ivDetails.setVisibility(View.GONE);
                    }

                    // Color codes for return dates
                    if (item.containsKey(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) {
                        if (Long.parseLong(item
                                .get(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) < System
                                .currentTimeMillis()) {
                            holder.vStatusColor
                                    .setBackgroundColor(getResources().getColor(R.color.date_overdue));
                        } else if ((Long.parseLong(item
                                .get(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) - System
                                .currentTimeMillis()) <= tolerance) {
                            holder.vStatusColor
                                    .setBackgroundColor(getResources().getColor(R.color.date_warning));
                        } else if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)) {
                            holder.vStatusColor.setBackgroundColor(
                                    getResources().getColor(R.color.account_downloadable));
                        }
                    } else if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)) {
                        holder.vStatusColor.setBackgroundColor(
                                getResources().getColor(R.color.account_downloadable));
                    }

                    if (item.containsKey(AccountData.KEY_LENT_LINK)) {
                        holder.ivProlong.setTag(item.get(AccountData.KEY_LENT_LINK));
                        holder.ivProlong.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                prolong((String) arg0.getTag());
                            }
                        });
                        holder.ivProlong.setVisibility(View.VISIBLE);
                        if (item.containsKey(AccountData.KEY_LENT_RENEWABLE)) {
                            holder.ivProlong.setAlpha(
                                    item.get(AccountData.KEY_LENT_RENEWABLE).equals("Y") ? 255 : 100);
                        }
                    } else if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)
                            && app.getApi() instanceof EbookServiceApi) {
                        holder.ivDownload.setTag(item.get(AccountData.KEY_LENT_DOWNLOAD));
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
                    Map<String, String> item = result.getLent().get(position);

                    holder.llDetails.setVisibility(View.VISIBLE);
                    if (item.containsKey(AccountData.KEY_LENT_TITLE)) {
                        holder.tvTitleAndAuthor.setText(Html.fromHtml(item.get(AccountData.KEY_LENT_TITLE)));
                    } else {
                        holder.tvTitleAndAuthor.setVisibility(View.GONE);
                    }
                    holder.ivDetails.setVisibility(View.GONE);
                    holder.ivClose.setVisibility(View.VISIBLE);
                }

                @Override
                public void collapseView(int position, View view) {
                    LentViewHolder holder = (LentViewHolder) view.getTag();
                    Map<String, String> item = result.getLent().get(position);

                    CharSequence title = item.containsKey(AccountData.KEY_LENT_TITLE) ?
                            Html.fromHtml(item.get(AccountData.KEY_LENT_TITLE)) : null;
                    CharSequence author = item.containsKey(AccountData.KEY_LENT_AUTHOR) ?
                            Html.fromHtml(item.get(AccountData.KEY_LENT_AUTHOR)) : null;

                    holder.llDetails.setVisibility(View.GONE);
                    if (title != null && author != null) {
                        holder.tvTitleAndAuthor.setText(title + ", " + author);
                    } else if (author != null) {
                        holder.tvTitleAndAuthor.setText(author);
                        holder.tvTitleAndAuthor.setVisibility(View.VISIBLE);
                    }
                    holder.ivDetails.setVisibility(View.VISIBLE);
                    holder.ivClose.setVisibility(View.GONE);
                }

                @Override
                public int getCount() {
                    return result.getLent().size();
                }
            };
            lentManager.setAnimationInterceptor(new ExpandingCardListManager.AnimationInterceptor() {

                @Override
                public Collection<Animator> getExpandAnimations(int heightDifference) {
                    return getAnimations(-heightDifference, 0);
                }

                @Override
                public Collection<Animator> getCollapseAnimations(int heightDifference) {
                    return getAnimations(0, heightDifference);
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
                                    .ofFloat(view.findViewById(R.id.rlMeta), "translationY", from,
                                            to));
                        }
                    } else {
                        // phone
                        animators.add(ObjectAnimator.ofFloat(tvResHeader, "translationY", from, to));
                        animators.add(ObjectAnimator.ofFloat(llRes, "translationY", from, to));
                        animators.add(ObjectAnimator.ofFloat(tvAge, "translationY", from, to));
                        animators.add(ObjectAnimator.ofFloat(view.findViewById(R.id.tvNoWarranty), "translationY", from, to));
                    }
                    return animators;
                }
            });

            for (final Map<String, String> item : result.getLent()) {
                try {
                    if (notification_on && item.containsKey(AccountData.KEY_LENT_DEADLINE)) {
                        if (!item.get(AccountData.KEY_LENT_DEADLINE).equals("")) {
                            if ((!item
                                    .containsKey(AccountData.KEY_LENT_DEADLINE_TIMESTAMP) || Long
                                    .parseLong(item
                                            .get(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) < 1)
                                    && !"Onleihe".equals(item
                                    .get(AccountData.KEY_LENT_BRANCH))) {
                                notification_problems = true;
                            }
                        }
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
            tvResHeader.setText(getActivity()
                    .getString(R.string.reservations_head) + " (0)");
        } else {
            tvResHeader.setText(getActivity()
                    .getString(R.string.reservations_head) + " (" +
                    result.getReservations().size() + ")");
            resManager = new ExpandingCardListManager(getActivity(), llRes) {
                @Override
                public View getView(final int position, ViewGroup container) {
                    final View v = getLayoutInflater(null).inflate(
                            R.layout.listitem_account_reservation, llRes, false);
                    ReservationViewHolder holder = new ReservationViewHolder();
                    holder.findViews(v);

                    final Map<String, String> item = result.getReservations().get(position);

                    if (item.containsKey(AccountData.KEY_RESERVATION_ID)) {
                        // Connection to detail view
                        View.OnClickListener gotoDetails = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent =
                                        new Intent(getActivity(), SearchResultDetailActivity.class);
                                intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
                                        item.get(AccountData.KEY_RESERVATION_ID));
                                ActivityOptionsCompat options = ActivityOptionsCompat
                                        .makeScaleUpAnimation(v, v.getLeft(), v.getTop(), v.getWidth(),
                                                v.getHeight());
                                ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
                            }
                        };
                        v.setClickable(true);
                        v.setFocusable(true);
                        v.setOnClickListener(gotoDetails);
                    }
                    // Expanding and closing details
                    holder.ivDetails.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View clicked) {
                            expand(position);
                        }
                    });
                    holder.ivClose.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View clicked) {
                            collapse();
                        }
                    });

                    // Overview (Title/Author, Ready/Expire)
                    final CharSequence title = item.containsKey(AccountData.KEY_RESERVATION_TITLE) ?
                            Html.fromHtml(item.get(AccountData.KEY_RESERVATION_TITLE)) : null;
                    final CharSequence author = item.containsKey(AccountData.KEY_RESERVATION_AUTHOR) ?
                            Html.fromHtml(item.get(AccountData.KEY_RESERVATION_AUTHOR)) : null;
                    if (title != null && author != null) {
                        holder.tvTitleAndAuthor.setText(title + ", " + author);
                    } else if (title != null) {
                        holder.tvTitleAndAuthor.setText(title);
                    } else if (author != null) {
                        holder.tvTitleAndAuthor.setText(author);
                    } else {
                        holder.tvTitleAndAuthor.setVisibility(View.GONE);
                    }

                    if (item.containsKey(AccountData.KEY_RESERVATION_READY)) {
                        holder.tvStatus.setText(Html.fromHtml(item.get(AccountData.KEY_RESERVATION_READY)));
                    } else if (item.containsKey(AccountData.KEY_RESERVATION_EXPIRE) &&
                            item.get(AccountData.KEY_RESERVATION_EXPIRE).length() > 6) {
                        holder.tvStatus.setText(
                                Html.fromHtml(getString(R.string.reservation_expire_until) + " "
                                        + item.get(AccountData.KEY_RESERVATION_EXPIRE)));
                    } else {
                        holder.tvStatus.setVisibility(View.GONE);
                    }

                    // Detail
                    boolean hasDetails = false;
                    if (author != null) {
                        holder.tvAuthorDetail.setText(author);
                        hasDetails = true;
                    } else {
                        holder.tvAuthorDetail.setVisibility(View.GONE);
                    }
                    if (item.containsKey(AccountData.KEY_RESERVATION_FORMAT)) {
                        holder.tvFormatDetail
                                .setText(Html.fromHtml(item.get(AccountData.KEY_RESERVATION_FORMAT)));
                        hasDetails = true;
                    } else {
                        holder.tvFormatDetail.setVisibility(View.GONE);
                    }
                    if (item.containsKey(AccountData.KEY_RESERVATION_BRANCH)) {
                        holder.tvBranchDetail
                                .setText(Html.fromHtml(item.get(AccountData.KEY_RESERVATION_BRANCH)));
                        hasDetails = true;
                    } else {
                        holder.tvBranchDetail.setVisibility(View.GONE);
                    }

                    // If there are no more details, we don't need the button for them
                    if (!hasDetails) {
                        holder.ivDetails.setVisibility(View.GONE);
                    }

                    if (item.containsKey(AccountData.KEY_RESERVATION_BOOKING)) {
                        holder.ivBooking.setTag(item.get(AccountData.KEY_RESERVATION_BOOKING));
                        holder.ivBooking.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                bookingStart((String) arg0.getTag());
                            }
                        });
                        holder.ivBooking.setVisibility(View.VISIBLE);
                        holder.ivCancel.setVisibility(View.GONE);
                    } else if (item.containsKey(AccountData.KEY_RESERVATION_CANCEL)) {
                        holder.ivCancel.setTag(item.get(AccountData.KEY_RESERVATION_CANCEL));
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
                    Map<String, String> item = result.getReservations().get(position);

                    holder.llDetails.setVisibility(View.VISIBLE);
                    if (item.containsKey(AccountData.KEY_RESERVATION_TITLE)) {
                        holder.tvTitleAndAuthor.setText(item.get(
                                AccountData.KEY_RESERVATION_TITLE));
                    } else {
                        holder.tvTitleAndAuthor.setVisibility(View.GONE);
                    }
                    holder.ivDetails.setVisibility(View.GONE);
                    holder.ivClose.setVisibility(View.VISIBLE);
                }

                @Override
                public void collapseView(int position, View view) {
                    ReservationViewHolder holder = (ReservationViewHolder) view.getTag();
                    Map<String, String> item = result.getReservations().get(position);

                    holder.llDetails.setVisibility(View.GONE);
                    if (item.containsKey(AccountData.KEY_RESERVATION_TITLE) && item.containsKey(AccountData.KEY_RESERVATION_AUTHOR)) {
                        holder.tvTitleAndAuthor.setText(item.get(
                                AccountData.KEY_RESERVATION_TITLE) + ", " + item.get(
                                AccountData.KEY_RESERVATION_AUTHOR));
                    } else if (item.containsKey(AccountData.KEY_RESERVATION_AUTHOR)) {
                        holder.tvTitleAndAuthor.setText(item.get(AccountData.KEY_RESERVATION_AUTHOR));
                        holder.tvTitleAndAuthor.setVisibility(View.VISIBLE);
                    }
                    holder.ivDetails.setVisibility(View.VISIBLE);
                    holder.ivClose.setVisibility(View.GONE);
                }

                @Override
                public int getCount() {
                    return result.getReservations().size();
                }
            };
            resManager.setAnimationInterceptor(new ExpandingCardListManager.AnimationInterceptor() {
                @Override
                public Collection<Animator> getExpandAnimations(int heightDifference) {
                    return getAnimations(-heightDifference, 0);
                }

                @Override
                public Collection<Animator> getCollapseAnimations(int heightDifference) {
                    return getAnimations(0, heightDifference);
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
                        animators.add(ObjectAnimator.ofFloat(view.findViewById(R.id.tvNoWarranty), "translationY", from, to));
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
            tvValidUntilLabel.setVisibility(
                    View.VISIBLE);
            tvValidUntil.setVisibility(View.VISIBLE);
            tvValidUntil.setText(result.getValidUntil());
        } else {
            tvValidUntilLabel.setVisibility(View.GONE);
            tvValidUntil.setVisibility(View.GONE);
        }
        refreshage();
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
                tvAge.setText(getResources().getQuantityString(R.plurals.account_age_minutes,
                        (int) (age / (60 * 1000)), (int) (age / (60 * 1000))));
            } else if (age < 24 * 3600 * 1000) {
                tvAge.setText(getResources().getQuantityString(R.plurals.account_age_hours,
                        (int) (age / (3600 * 1000)), (int) (age / (3600 * 1000))));

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
            Toast.makeText(getActivity(), R.string.account_no_concurrent,
                    Toast.LENGTH_LONG).show();
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
        builder.setMessage(R.string.prolong_all_confirm)
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
                               prolongAllDo();
                           }
                       });
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void prolongAllDo() {

        MultiStepResultHelper<Void> msrhProlong = new MultiStepResultHelper<>(
                getActivity(), null, R.string.doing_prolong_all);
        msrhProlong.setCallback(new Callback<Void>() {
            @Override
            public void onSuccess(MultiStepResult result) {
                if (getActivity() == null) {
                    return;
                }
                ProlongAllResult res = (ProlongAllResult) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());

                LayoutInflater inflater = getLayoutInflater(null);

                View view = inflater.inflate(R.layout.dialog_simple_list, null, false);

                ListView lv = (ListView) view.findViewById(R.id.lvBibs);

                lv.setAdapter(new ProlongAllResultAdapter(getActivity(), res
                        .getResults()));
                switch (result.getActionIdentifier()) {
                    case ReservationResult.ACTION_BRANCH:
                        builder.setTitle(R.string.branch);
                }
                builder.setView(view).setNeutralButton(R.string.close,
                        new DialogInterface.OnClickListener() {
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

    public class SendTask extends AsyncTask<Void, Object, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            HttpClient dc = HttpClientBuilder.create().build();
            HttpPost httppost = new HttpPost(
                    "http://opacapp.de/crashreport.php");
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("traceback", ""));
            try {
                nameValuePairs
                        .add(new BasicNameValuePair("version",
                                getActivity().getPackageManager()
                                             .getPackageInfo(
                                                     getActivity().getPackageName(),
                                                     0).versionName));
            } catch (Exception e) {
                e.printStackTrace();
            }

            nameValuePairs.add(new BasicNameValuePair("android", android.os.Build.VERSION.RELEASE));
            nameValuePairs
                    .add(new BasicNameValuePair("sdk", "" + android.os.Build.VERSION.SDK_INT));
            nameValuePairs.add(new BasicNameValuePair("device", android.os.Build.MANUFACTURER + " "
                    + android.os.Build.MODEL));
            nameValuePairs.add(new BasicNameValuePair("bib", app.getLibrary().getIdent()));

            try {
                nameValuePairs.add(new BasicNameValuePair("html", app.getApi()
                                                                     .getAccountExtendableInfo(
                                                                             app.getAccount())));
            } catch (Exception e1) {
                e1.printStackTrace();
                return 1;
            }

            try {
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            HttpResponse response;
            try {
                response = dc.execute(httppost);
                response.getEntity().consumeContent();
            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (getActivity() == null) {
                return;
            }

            dialog.dismiss();
            btSend.setEnabled(false);
            if (result == 0) {
                Toast toast = Toast.makeText(getActivity(),
                        getString(R.string.report_sent), Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getActivity(),
                        getString(R.string.report_error), Toast.LENGTH_SHORT);
                toast.show();
            }

        }
    }

    public class LoadTask extends AsyncTask<Void, Void, AccountData> {

        private Exception exception;

        @Override
        protected AccountData doInBackground(Void... voids) {
            try {
                return app.getApi().account(app.getAccount());
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
                return;
            }

            super.onPostExecute(res);
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
