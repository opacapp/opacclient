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

import java.io.InterruptedIOException;

import org.acra.ACRA;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;

public class WelcomeActivity extends Activity {
	protected OpacClient app;
	protected AlertDialog dialog;

	public static int getLayoutResource() {
		return R.layout.activity_welcome;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OpacClient) getApplication();
		setContentView(getLayoutResource());

		Button btAddAccount = (Button) findViewById(R.id.btAddAccount);
		btAddAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				add();
			}
		});
	}

	@Override
	public void onBackPressed() {
		System.exit(0);
		super.onBackPressed();
	}

	public void add() {
		Intent i = new Intent(this, LibraryListActivity.class);
		i.putExtra("welcome", true);
		startActivity(i);
	}

	public class InitTask extends OpacTask<Integer> {
		@Override
		protected Integer doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			try {
				app.getApi().start();
			} catch (java.net.UnknownHostException e) {
				e.printStackTrace();
			} catch (java.net.SocketException e) {
				e.printStackTrace();
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
			startActivity(intent);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (dialog != null) {
			if (dialog.isShowing()) {
				dialog.cancel();
			}
		}
	}

}
