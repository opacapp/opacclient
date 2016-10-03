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
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.List;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountListActivity extends AppCompatActivity {

    private static final int ACCOUNT_EDIT_REQUEST_CODE = 0;
    protected List<Account> accounts;
    protected FloatingActionButton fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fab = (FloatingActionButton) findViewById(R.id.add_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        float density = getResources().getDisplayMetrics().density;
        float dpWidth = displayMetrics.widthPixels / density;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams
                (Math.round(72 * density), Math.round(72 * density));

        if (dpWidth >= 864) {
            params.addRule(RelativeLayout.BELOW, R.id.toolbar);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, Math.round(-36 * density), Math.round(36 * density), 0);
            ViewCompat.setElevation(fab, 4 * density);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.setMargins(0, 0, Math.round(36 * density), Math.round(36 * density));
            ViewCompat.setElevation(fab, 12 * density);
        }
        fab.setLayoutParams(params);
    }

    public void add() {
        Intent i = new Intent(this, LibraryListActivity.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(fab,
                fab.getLeft(), fab.getTop(), fab.getWidth(), fab.getHeight());
        ActivityCompat.startActivityForResult(this, i, ACCOUNT_EDIT_REQUEST_CODE,
                options.toBundle());
    }

    public void refreshLv() {
        ListView lvAccounts = (ListView) findViewById(R.id.lvAccounts);
        AccountDataSource data = new AccountDataSource(this);
        accounts = data.getAllAccounts();
        AccountListAdapter adapter = new AccountListAdapter(this, accounts);
        lvAccounts.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        ListView lvAccounts = (ListView) findViewById(R.id.lvAccounts);
        refreshLv();

        lvAccounts.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent i = new Intent(AccountListActivity.this,
                        AccountEditActivity.class);
                i.putExtra(AccountEditActivity.EXTRA_ACCOUNT_ID, accounts.get(position).getId());
                i.putExtra(AccountEditActivity.EXTRA_EDITING, true);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(view,
                        view.getLeft(), view.getTop(), view.getWidth(), view.getHeight());
                ActivityCompat.startActivityForResult(AccountListActivity.this, i,
                        ACCOUNT_EDIT_REQUEST_CODE, options.toBundle());
            }
        });
        lvAccounts.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                ((OpacClient) getApplication()).setAccount(accounts.get(
                        position).getId());
                refreshLv();
                return true;
            }

        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACCOUNT_EDIT_REQUEST_CODE) {
            refreshLv();
        }
    }

}
