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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.IOException;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.utils.ErrorReporter;

public class WelcomeActivity extends AppCompatActivity {
    protected OpacClient app;

    @SuppressWarnings("SameReturnValue") // Plus Edition compatibility
    public static int getLayoutResource() {
        return R.layout.activity_welcome;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (OpacClient) getApplication();
        setContentView(getLayoutResource());

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

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

    public class InitTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                app.getApi().start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                ErrorReporter.handleException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

}
