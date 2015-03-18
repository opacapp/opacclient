package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Map;

import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;

public class MultiStepResultHelper {

    protected Activity context;
    protected Object argument;
    protected StepTask<?> task;
    protected Callback callback;
    protected int loadingstring;

    protected ProgressDialog pdialog;
    protected AlertDialog adialog;

    public MultiStepResultHelper(Activity context, Object argument,
                                 int loadingstring) {
        super();
        this.context = context;
        this.argument = argument;
        this.loadingstring = loadingstring;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void start() {
        doStep(0, null);
    }

    public void doStep(int useraction, String selection) {
        if (pdialog == null || !pdialog.isShowing()) {
            pdialog = ProgressDialog.show(context, "",
                    context.getString(loadingstring), true);
            pdialog.show();
        }
        if (callback == null) {
            throw new IllegalStateException("Callback not set!");
        }
        task = callback.newTask();
        task.execute((context.getApplication()), argument, useraction,
                selection, this);
    }

    public void handleResult(MultiStepResult result) {
        switch (result.getStatus()) {
            case CONFIRMATION_NEEDED:
                askForConfirmation(result);
                break;
            case SELECTION_NEEDED:
                askForSelection(result);
                break;
            case EMAIL_NEEDED:
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                doStep(result.getActionIdentifier(), sp.getString("email", ""));
                break;
            case ERROR:
                if (callback != null)
                    callback.onError(result);
                break;
            case OK:
                if (callback != null)
                    callback.onSuccess(result);
                break;
            case UNSUPPORTED:
                // TODO: Show dialog
                break;
            default:
                if (callback != null)
                    callback.onUnhandledResult(result);
                break;
        }
    }

    public void askForConfirmation(final MultiStepResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = context.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_reservation_details, null, false);

        TableLayout table = (TableLayout) view.findViewById(R.id.tlDetails);

        if (result.getDetails().size() == 1
                && result.getDetails().get(0).length == 1) {
            ((ViewGroup) view.findViewById(R.id.rlConfirm))
                    .removeView(table);
            TextView tv = new TextView(context);
            tv.setText(result.getDetails().get(0)[0]);
            tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            ((ViewGroup) view.findViewById(R.id.rlConfirm)).addView(tv);
        } else {
            for (String[] detail : result.getDetails()) {
                TableRow tr = new TableRow(context);
                if (detail.length == 2) {
                    TextView tv1 = new TextView(context);
                    tv1.setText(Html.fromHtml(detail[0]));
                    tv1.setTypeface(null, Typeface.BOLD);
                    tv1.setPadding(0, 0, 8, 0);
                    TextView tv2 = new TextView(context);
                    tv2.setText(Html.fromHtml(detail[1]));
                    tv2.setEllipsize(TruncateAt.END);
                    tv2.setSingleLine(false);
                    tr.addView(tv1);
                    tr.addView(tv2);
                } else if (detail.length == 1) {
                    TextView tv1 = new TextView(context);
                    tv1.setText(Html.fromHtml(detail[0]));
                    tv1.setPadding(0, 2, 0, 2);
                    TableRow.LayoutParams params = new TableRow.LayoutParams(0);
                    params.span = 2;
                    tv1.setLayoutParams(params);
                    tr.addView(tv1);
                }
                table.addView(tr);
            }
        }

        builder.setTitle(R.string.confirm_title)
                .setView(view)
                .setPositiveButton(R.string.confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                doStep(MultiStepResult.ACTION_CONFIRMATION,
                                        "confirmed");
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                adialog.cancel();
                                if (callback != null)
                                    callback.onUserCancel();
                            }
                        });
        adialog = builder.create();
        adialog.show();
    }

    public void askForSelection(final MultiStepResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = context.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_simple_list, null, false);

        ListView lv = (ListView) view.findViewById(R.id.lvBibs);
        final Object[] possibilities = result.getSelection().toArray();

        lv.setAdapter(new SelectionAdapter(context, possibilities));
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                adialog.dismiss();

                doStep(result.getActionIdentifier(),
                        ((Map<String, String>) possibilities[position])
                                .get("key"));
            }
        });

        if (result.getMessage() != null) {
            ((android.widget.TextView) view.findViewById(R.id.tvMessage))
                    .setText(result.getMessage());
            view.findViewById(R.id.tvMessage).setVisibility(View.VISIBLE);
        }

        switch (result.getActionIdentifier()) {
            case ReservationResult.ACTION_BRANCH:
                builder.setTitle(R.string.zweigstelle);
        }
        builder.setView(view).setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        adialog.cancel();
                        if (callback != null)
                            callback.onUserCancel();
                    }
                });
        adialog = builder.create();
        adialog.show();
    }

    public interface Callback {
        public void onSuccess(MultiStepResult result);

        public void onError(MultiStepResult result);

        public void onUnhandledResult(MultiStepResult result);

        public void onUserCancel();

        public StepTask<?> newTask();
    }

    public static abstract class StepTask<T extends MultiStepResult> extends
            OpacTask<T> {
        protected MultiStepResultHelper helper;

        @Override
        protected void onPostExecute(T res) {
            super.onPostExecute(res);
            if (helper.pdialog != null)
                helper.pdialog.dismiss();
            if (res != null)
                helper.handleResult(res);
        }

        @Override
        protected T doInBackground(Object... arg0) {
            helper = (MultiStepResultHelper) arg0[4];
            return super.doInBackground(arg0);
        }
    }

    public static class SelectionAdapter extends ArrayAdapter<Object> {

        private Object[] objects;

        public SelectionAdapter(Context context, Object[] objects) {
            super(context, R.layout.simple_spinner_item, objects);
            this.objects = objects;
        }

        @Override
        public View getView(int position, View contentView, ViewGroup viewGroup) {
            View view = null;

            if (objects[position] == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.listitem_branch,
                        viewGroup, false);
                return view;
            }

            String item = ((Map<String, String>) objects[position])
                    .get("value").toString();

            if (contentView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.listitem_branch,
                        viewGroup, false);
            } else {
                view = contentView;
            }

            TextView tvText = (TextView) view.findViewById(android.R.id.text1);
            tvText.setText(item);
            return view;
        }

    }

}
