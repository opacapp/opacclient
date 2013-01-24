package de.geeksfactory.opacclient;

import android.os.AsyncTask;

public abstract class OpacTask<Result> extends
		AsyncTask<Object, Object, Result> {
	protected OpacClient a;

	@Override
	protected Result doInBackground(Object... arg0) {
		@SuppressWarnings("unused")
		OpacClient a = (OpacClient) arg0[0];
		return null;
	}
}
