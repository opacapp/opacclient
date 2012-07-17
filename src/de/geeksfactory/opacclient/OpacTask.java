package de.geeksfactory.opacclient;

import android.os.AsyncTask;

public abstract class OpacTask<Result> extends
		AsyncTask<Object, Object, Result> {
	OpacClient app;

	protected Result doInBackground(Object... arg0) {
		OpacClient app = (OpacClient) arg0[0];
		return null;
	}

	protected void onProgressUpdate(Object... arg0) {
		if (((String) arg0[1]).equals("ioerror")) {
			app.web_error((Exception) arg0[0], "ioerror");
		}
	}
}
