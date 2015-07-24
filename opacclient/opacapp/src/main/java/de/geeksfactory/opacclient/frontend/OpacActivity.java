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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.NavigationAdapter.Item;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public abstract class OpacActivity extends AppCompatActivity {
    protected OpacClient app;
    protected AlertDialog adialog;
    protected AccountDataSource aData;

    protected int selectedItemPos;
    protected String selectedItemTag;

    protected NavigationAdapter navAdapter;
    protected ListView drawerList;
    protected View drawer;
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerToggle;
    protected FloatingActionButton fab;
    protected CharSequence title;

    protected List<Account> accounts;

    protected Fragment fragment;
    protected boolean hasDrawer = false;
    protected Toolbar toolbar;
    private boolean twoPane;
    private boolean fabVisible;

    protected static void unbindDrawables(View view) {
        if (view == null) {
            return;
        }
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            if (!(view instanceof AdapterView)) {
                ((ViewGroup) view).removeAllViews();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(android.view.Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(getContentView());

        app = (OpacClient) getApplication();

        aData = new AccountDataSource(this);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        fab = (FloatingActionButton) findViewById(R.id.search_fab);
        setupDrawer();

        if (savedInstanceState != null) {
            setTwoPane(savedInstanceState.getBoolean("twoPane"));
            setFabVisible(savedInstanceState.getBoolean("fabVisible"));
            selectedItemTag = savedInstanceState.getString("selectedItemTag");
            setFabOnClickListener(selectedItemTag);
            if (savedInstanceState.containsKey("title")) {
                setTitle(savedInstanceState.getCharSequence("title"));
            }
            if (savedInstanceState.containsKey("fragment")) {
                fragment = getSupportFragmentManager().getFragment(
                        savedInstanceState, "fragment");
                getSupportFragmentManager().beginTransaction()
                                           .replace(R.id.content_frame, fragment).commit();
            }
        }
        fixStatusBarFlashing();
    }

    /**
     * Fix status bar flashing problem during transitions by excluding the status bar background from transitions
     */
    @TargetApi(21)
    private void fixStatusBarFlashing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getEnterTransition().excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().getReenterTransition().excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().getReturnTransition().excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().getExitTransition().excludeTarget(android.R.id.statusBarBackground, true);
        }
    }

    protected abstract int getContentView();

    protected void setupDrawer() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            hasDrawer = true;
            drawerLayout.setStatusBarBackground(R.color.primary_red_dark);
            drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);
            drawerToggle =
                    new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open,
                            R.string.drawer_close) {
                        /**
                         * Called when a drawer has settled in a completely closed
                         * state.
                         */
                        @Override
                        public void onDrawerClosed(View view) {
                            super.onDrawerClosed(view);
                            getSupportActionBar().setTitle(title);
                        }

                        /** Called when a drawer has settled in a completely open state. */
                        @Override
                        public void onDrawerOpened(View drawerView) {
                            super.onDrawerOpened(drawerView);
                            getSupportActionBar().setTitle(
                                    app.getResources().getString(R.string.app_name));
                            if (getCurrentFocus() != null) {
                                InputMethodManager imm = (InputMethodManager) getSystemService(
                                        Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(getCurrentFocus()
                                        .getWindowToken(), 0);
                            }
                        }
                    };

            // Set the drawer toggle as the DrawerListener
            drawerLayout.setDrawerListener(drawerToggle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            drawer = findViewById(R.id.navdrawer);
            drawerList = (ListView) findViewById(R.id.drawer_list);
            navAdapter = new NavigationAdapter(this);
            drawerList.setAdapter(navAdapter);
            navAdapter.addSeperatorItem(getString(R.string.nav_hl_library));
            navAdapter.addTextItemWithIcon(getString(R.string.nav_search),
                    R.drawable.ic_action_search, "search");
            navAdapter.addTextItemWithIcon(getString(R.string.nav_account),
                    R.drawable.ic_action_account, "account");
            navAdapter.addTextItemWithIcon(getString(R.string.nav_starred),
                    R.drawable.ic_action_star_1, "starred");
            navAdapter.addTextItemWithIcon(getString(R.string.nav_info),
                    R.drawable.ic_action_info, "info");

            aData.open();
            accounts = aData.getAllAccounts();
            if (accounts.size() > 1) {
                navAdapter
                        .addSeperatorItem(getString(R.string.nav_hl_accountlist));

                long tolerance = Long.decode(sp.getString(
                        "notification_warning", "367200000"));
                int selected = -1;
                for (final Account account : accounts) {
                    Library library;
                    try {
                        library = ((OpacClient) getApplication())
                                .getLibrary(account.getLibrary());
                        int expiring = aData.getExpiring(account, tolerance);
                        String expiringText = "";
                        if (expiring > 0) {
                            expiringText = String.valueOf(expiring);
                        }
                        if (getString(R.string.default_account_name).equals(
                                account.getLabel())) {
                            navAdapter.addLibraryItem(library.getCity(),
                                    library.getTitle(), expiringText,
                                    account.getId());
                        } else {
                            navAdapter.addLibraryItem(
                                    account.getLabel(),
                                    library.getCity() + " · "
                                            + library.getTitle(), expiringText,
                                    account.getId());
                        }
                        if (account.getId() == app.getAccount().getId()) {
                            selected = navAdapter.getCount() - 1;
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (selected > 0) {
                    drawerList.setItemChecked(selected, true);
                }
            }

            navAdapter.addSeperatorItem(getString(R.string.nav_hl_other));
            navAdapter.addTextItemWithIcon(getString(R.string.nav_settings),
                    R.drawable.ic_action_settings, "settings");
            navAdapter.addTextItemWithIcon(getString(R.string.nav_about),
                    R.drawable.ic_action_help, "about");

            drawerList.setOnItemClickListener(new DrawerItemClickListener());

            if (!sp.getBoolean("version2.0.0-introduced", false)
                    && app.getSlidingMenuEnabled()) {
                final Handler handler = new Handler();
                // Just show the menu to explain that is there if people start
                // version 2 for the first time.
                // We need a handler because if we just put this in onCreate
                // nothing
                // happens. I don't have any idea, why.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences sp = PreferenceManager
                                .getDefaultSharedPreferences(OpacActivity.this);
                        drawerLayout.openDrawer(drawer);
                        sp.edit().putBoolean("version2.0.0-introduced", true)
                          .commit();
                    }
                }, 500);

            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setSupportProgressBarIndeterminateVisibility(false);
        if (hasDrawer) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (hasDrawer) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    protected void selectItem(String tag) {
        int pos = navAdapter.getPositionByTag(tag);
        if (pos >= 0) {
            selectItem(pos);
        }
    }

    @Override
    protected void onResume() {
        setupDrawer();

        fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);

        if (hasDrawer) {
            drawerToggle.syncState();
        }
        setTwoPane(twoPane);
        super.onResume();
        fixNavigationSelection();
    }

    protected void fixNavigationSelection() {
        if (fragment == null) {
            return;
        }
        if (fragment instanceof SearchFragment) {
            drawerList.setItemChecked(navAdapter.getPositionByTag("search"),
                    true);
        } else if (fragment instanceof AccountFragment) {
            drawerList.setItemChecked(navAdapter.getPositionByTag("account"),
                    true);
        } else if (fragment instanceof StarredFragment) {
            drawerList.setItemChecked(navAdapter.getPositionByTag("starred"),
                    true);
        } else if (fragment instanceof InfoFragment) {
            drawerList
                    .setItemChecked(navAdapter.getPositionByTag("info"), true);
        }
        if (app.getLibrary() != null) {
            getSupportActionBar()
                    .setSubtitle(app.getLibrary().getDisplayName());
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    @SuppressLint("NewApi")
    protected void selectItem(int position) {
        try {
            setSupportProgressBarIndeterminateVisibility(false);
        } catch (Exception e) {
        }
        Item item = navAdapter.getItem(position);
        if (item.type == Item.TYPE_TEXT) {
            Fragment previousFragment = fragment;
            switch (item.tag) {
                case "search":
                    fragment = new SearchFragment();
                    setTwoPane(false);
                    setFabVisible(true);
                    break;
                case "account":
                    fragment = new AccountFragment();
                    setTwoPane(false);
                    setFabVisible(false);
                    break;
                case "starred":
                    fragment = new StarredFragment();
                    setTwoPane(true);
                    setFabVisible(false);
                    break;
                case "info":
                    fragment = new InfoFragment();
                    setTwoPane(false);
                    setFabVisible(false);
                    break;
                case "settings": {
                    Intent intent = new Intent(this, MainPreferenceActivity.class);
                    startActivity(intent);
                    drawerList.setItemChecked(position, false);
                    return;
                }
                case "about": {
                    Intent intent = new Intent(this, AboutActivity.class);
                    startActivity(intent);
                    drawerList.setItemChecked(position, false);
                    return;
                }
            }
            setFabOnClickListener(item.tag);

            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction()
                                                             .replace(R.id.content_frame, fragment);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fragment.setSharedElementEnterTransition(
                        TransitionInflater.from(this).inflateTransition
                                (android.R.transition.move));
                fragment.setEnterTransition(TransitionInflater.from(this).inflateTransition
                        (android.R.transition.fade));
            }

            try {
                if (previousFragment instanceof SearchFragment &&
                        fragment instanceof AccountFragment && previousFragment.getView() != null) {
                    transaction.addSharedElement(previousFragment.getView().findViewById(R.id
                            .rlSimpleSearch), getString(R.string.transition_gray_box));
                } else if (previousFragment instanceof AccountFragment &&
                        fragment instanceof SearchFragment && previousFragment.getView() != null) {
                    transaction.addSharedElement(previousFragment.getView().findViewById(R.id
                            .rlAccHeader), getString(R.string.transition_gray_box));
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            transaction.commit();

            // Highlight the selected item, update the title, and close the
            // drawer
            deselectItemsByType(Item.TYPE_TEXT);
            drawerList.setItemChecked(selectedItemPos, false);
            drawerList.setItemChecked(position, true);
            selectedItemPos = position;
            selectedItemTag = item.tag;
            setTitle(navAdapter.getItem(position).text);
            drawerLayout.closeDrawer(drawer);

        } else if (item.type == Item.TYPE_ACCOUNT) {
            deselectItemsByType(Item.TYPE_ACCOUNT);
            drawerList.setItemChecked(position, true);
            selectaccount(item.accountId);
            drawerLayout.closeDrawer(drawer);
        }
    }

    protected void setFabOnClickListener(String tag) {
        if (isTablet()) {
            if (tag.equals("search")) {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation
                                (v, Math.round(v.getLeft()), Math.round(v.getTop()), v.getWidth(),
                                        v.getHeight());
                        ((SearchFragment) fragment).go(options.toBundle());
                    }
                });
            } else {
                fab.setOnClickListener(null);
            }
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        this.title = title;
    }

    protected void deselectItemsByType(int type) {
        for (int i = 0; i < navAdapter.getCount(); i++) {
            if (navAdapter.getItemViewType(i) == type) {
                drawerList.setItemChecked(i, false);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (app.getLibrary() == null) {
            // Create new
            if (app.getAccount() != null) {
                try {
                    InputStream stream = getAssets().open(
                            OpacClient.ASSETS_BIBSDIR + "/"
                                    + app.getAccount().getLibrary() + ".json");
                    stream.close();
                } catch (IOException e) {
                    AccountDataSource data = new AccountDataSource(this);
                    data.open();
                    data.remove(app.getAccount());
                    List<Account> available_accounts = data.getAllAccounts();
                    if (available_accounts.size() > 0) {
                        ((OpacClient) getApplication())
                                .setAccount(available_accounts.get(0).getId());
                    }
                    data.close();
                    if (app.getLibrary() != null) {
                        return;
                    }
                }
            }
            app.addFirstAccount(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = new MenuInflater(this);
        mi.inflate(R.menu.activity_opac, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void accountSelected(Account account) {
    }

    public void selectaccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_simple_list, null);

        ListView lv = (ListView) view.findViewById(R.id.lvBibs);
        AccountDataSource data = new AccountDataSource(this);
        data.open();
        final List<Account> accounts = data.getAllAccounts();
        data.close();
        AccountListAdapter adapter = new AccountListAdapter(this, accounts);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {

                app.setAccount(accounts.get(position).getId());

                adialog.dismiss();

                ((AccountSelectedListener) fragment).accountSelected(accounts
                        .get(position));
            }
        });
        builder.setTitle(R.string.account_select)
               .setView(view)
               .setNegativeButton(R.string.cancel,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int id) {
                               adialog.cancel();
                           }
                       })
               .setNeutralButton(R.string.accounts_edit,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.dismiss();
                               Intent intent = new Intent(OpacActivity.this,
                                       AccountListActivity.class);
                               startActivity(intent);
                           }
                       });
        adialog = builder.create();
        adialog.show();
    }

    public void selectaccount(long id) {
        ((OpacClient) getApplication()).setAccount(id);
        accountSelected(((OpacClient) getApplication()).getAccount());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return hasDrawer && drawerToggle.onOptionsItemSelected(item) ||
                super.onOptionsItemSelected(item);
    }

    protected boolean isTablet() {
        return findViewById(R.id.content_frame_right) != null;
    }

    protected void setTwoPane(boolean active) {
        twoPane = active;
        if (isTablet()) {
            findViewById(R.id.content_frame_right).setVisibility(
                    active ? View.VISIBLE : View.GONE);
            findViewById(R.id.twopane_wrapper).getLayoutParams().width =
                    active ? LinearLayout.LayoutParams.MATCH_PARENT :
                            getResources().getDimensionPixelSize(R.dimen.single_pane_max_width);
        }
    }

    protected void setFabVisible(boolean visible) {
        fabVisible = visible;
        if (isTablet()) {
            fab.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
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
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("twoPane", twoPane);
        outState.putBoolean("fabVisible", fabVisible);
        outState.putString("selectedItemTag", selectedItemTag);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, "fragment",
                    fragment);
        }
        if (title != null) {
            outState.putCharSequence("title", title);
        }
    }

    public interface AccountSelectedListener {
        void accountSelected(Account account);
    }

    public class DrawerItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            selectItem(position);
        }
    }

    public class MetaAdapter extends ArrayAdapter<Map<String, String>> {

        private List<Map<String, String>> objects;
        private int spinneritem;

        public MetaAdapter(Context context, List<Map<String, String>> objects,
                int spinneritem) {
            super(context, R.layout.simple_spinner_item, objects);
            this.objects = objects;
            this.spinneritem = spinneritem;
        }

        @Override
        public View getDropDownView(int position, View contentView,
                ViewGroup viewGroup) {
            View view;

            if (objects.get(position) == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater
                        .inflate(R.layout.simple_spinner_dropdown_item,
                                viewGroup, false);
                return view;
            }

            Map<String, String> item = objects.get(position);

            if (contentView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater
                        .inflate(R.layout.simple_spinner_dropdown_item,
                                viewGroup, false);
            } else {
                view = contentView;
            }

            TextView tvText = (TextView) view.findViewById(android.R.id.text1);
            tvText.setText(item.get("value"));
            return view;
        }

        @Override
        public View getView(int position, View contentView, ViewGroup viewGroup) {
            View view;

            if (objects.get(position) == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(spinneritem, viewGroup, false);
                return view;
            }

            Map<String, String> item = objects.get(position);

            if (contentView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(spinneritem, viewGroup, false);
            } else {
                view = contentView;
            }

            TextView tvText = (TextView) view.findViewById(android.R.id.text1);
            tvText.setText(item.get("value"));
            return view;
        }

    }
}
