package de.geeksfactory.opacclient.frontend;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;

import de.geeksfactory.opacclient.ui.AppCompatProgressDialog;

public class ProgressDialogFragment extends DialogFragment {
    private static final String ARG_MESSAGE = "message";
    private int message;

    public static ProgressDialogFragment getInstance(@StringRes int message) {
        ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE, message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        message = getArguments().getInt(ARG_MESSAGE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AppCompatProgressDialog dialog = new AppCompatProgressDialog(getActivity(), getTheme());
        dialog.setMessage(getString(message));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        return dialog;
    }
}