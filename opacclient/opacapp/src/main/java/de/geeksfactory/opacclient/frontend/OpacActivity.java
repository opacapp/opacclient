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
import android.support.design.widget.NavigationView;
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
import android.widget.ImageView;
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
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public abstract class OpacActivity extends AppCompatActivity {
    protected OpacClient app;
    protected AlertDialog adialog;
    protected AccountDataSource aData;

    protected int selectedItemId;

    protected NavigationView drawer;
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerToggle;
    protected FloatingActionButton fab;
    protected CharSequence title;

    protected Fragment fragment;
    protected boolean hasDrawer = false;
    protected Toolbar toolbar;
    private boolean twoPane;
    private boolean fabVisible;

    protected List<Account> accounts;
    protected ImageView accountExpand;
    protected TextView accountTitle;
    protected TextView accountSubtitle;
    protected TextView accountWarning;
    protected LinearLayout accountData;
    protected boolean accountSwitcherVisible = false;

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
        setupAccountSwitcher();

        if (savedInstanceState != null) {
            setTwoPane(savedInstanceState.getBoolean("twoPane"));
            setFabVisible(savedInstanceState.getBoolean("fabVisible"));
            selectedItemId = savedInstanceState.getInt("selectedItemId");
            setFabOnClickListener(selectedItemId);
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

    private void setupAccountSwitcher() {
        aData.open();
        accounts = aData.getAllAccounts();
        aData.close();

        Account selectedAccount = app.getAccount();

        View header = drawer.getHeaderView(0);
        accountExpand = (ImageView) header.findViewById(R.id.account_expand);
        accountTitle = (TextView) header.findViewById(R.id.account_title);
        accountSubtitle = (TextView) header.findViewById(R.id.account_subtitle);
        accountWarning = (TextView) header.findViewById(R.id.account_warning);
        accountData = (LinearLayout) header.findViewById(R.id.account_data);

        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAccountSwitcher();
            }
        };
        accountData.setOnClickListener(l);
        //accountExpand.setOnClickListener(l);

        updateAccountSwitcher(selectedAccount);
    }

    private void toggleAccountSwitcher() {
        setAccountSwitcherVisible(!accountSwitcherVisible);
    }

    private void setAccountSwitcherVisible(boolean accountSwitcherVisible) {
        if (accountSwitcherVisible == this.accountSwitcherVisible) return;

        this.accountSwitcherVisible = accountSwitcherVisible;

        if (accountSwitcherVisible) {
            accountExpand.setActivated(true);
            // touch feedback and animation only work when drawer update is delayed
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawer.getMenu().clear();
                    SharedPreferences sp =
                            PreferenceManager.getDefaultSharedPreferences(OpacActivity.this);
                    long tolerance = Long.decode(sp.getString("notification_warning", "367200000"));

                    aData.open();
                    for (Account account : OpacActivity.this.accounts) {
                        // don't show the currently selected account in the list
                        if (account.getId() == app.getAccount().getId()) continue;

                        int id = calculateAccountMenuItemId(account);
                        drawer.getMenu().add(R.id.nav_group_accounts, id, 0, "");
                        drawer.getMenu().findItem(id).setActionView(
                                R.layout.navigation_drawer_item_account);
                        View view = drawer.getMenu().findItem(id).getActionView();
                        String title = getAccountTitle(account);
                        String subtitle = getAccountSubtitle(account);
                        int expiring = aData.getExpiring(account, tolerance);
                        ((TextView) view.findViewById(R.id.account_title)).setText(title);
                        ((TextView) view.findViewById(R.id.account_subtitle)).setText(subtitle);
                        TextView tvWarning = (TextView) view.findViewById(R.id.account_warning);
                        if (expiring > 0) {
                            tvWarning.setText(String.valueOf(expiring));
                            tvWarning.setVisibility(View.VISIBLE);
                        } else {
                            tvWarning.setVisibility(View.INVISIBLE);
                        }
                    }
                    drawer.inflateMenu(R.menu.navigation_drawer_accounts);
                }
            }, 200);
        } else {
            accountExpand.setActivated(false);
            // touch feedback and animation only work when drawer update is delayed
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawer.getMenu().clear();
                    drawer.inflateMenu(R.menu.navigation_drawer);
                    fixNavigationSelection();
                }
            }, 200);
        }
    }

    /**
     * Generates menu item IDs for accounts. They won't collide with statically generated IDs
     * because they are <= 0x00FFFFFF.
     *
     * @param account The account to generate an ID for
     * @return The unique menu item ID for this account
     */
    private int calculateAccountMenuItemId(Account account) {
        return (int) (account.getId() % 0x00FFFFFF);
    }

    private String getAccountTitle(Account account) {
        if (getString(R.string.default_account_name).equals(
                account.getLabel())) {
            try {
                return app.getLibrary(account.getLibrary()).getCity();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return account.getLabel();
        }
    }

    private String getAccountSubtitle(Account account) {
        try {
            Library library = app.getLibrary(account.getLibrary());
            if (getString(R.string.default_account_name).equals(
                    account.getLabel())) {
                return library.getTitle();
            } else {
                return library.getCity() + " · " + library.getTitle();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
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
            drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(View drawerView, float slideOffset) {
                    drawerToggle.onDrawerSlide(drawerView, slideOffset);
                }

                @Override
                public void onDrawerOpened(View drawerView) {
                    drawerToggle.onDrawerOpened(drawerView);
                }

                @Override
                public void onDrawerClosed(View drawerView) {
                    drawerToggle.onDrawerClosed(drawerView);
                    setAccountSwitcherVisible(false);
                }

                @Override
                public void onDrawerStateChanged(int newState) {
                    drawerToggle.onDrawerStateChanged(newState);
                }
            });
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            drawer = (NavigationView) findViewById(R.id.navdrawer);

            drawer.setNavigationItemSelectedListener(
                    new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(MenuItem item) {
                            selectItem(item);
                            return true;
                        }
                    });

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
            drawer.setCheckedItem(R.id.nav_search);
        } else if (fragment instanceof AccountFragment) {
            drawer.setCheckedItem(R.id.nav_account);
        } else if (fragment instanceof StarredFragment) {
            drawer.setCheckedItem(R.id.nav_starred);
        } else if (fragment instanceof InfoFragment) {
            drawer.setCheckedItem(R.id.nav_info);
        }
        if (app.getLibrary() != null) {
            getSupportActionBar().setSubtitle(app.getLibrary().getDisplayName());
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    protected void selectItem(MenuItem item) {
        try {
            setSupportProgressBarIndeterminateVisibility(false);
        } catch (Exception e) {
        }
        Fragment previousFragment = fragment;
        switch (item.getItemId()) {
            case R.id.nav_search:
                fragment = new SearchFragment();
                setTwoPane(false);
                setFabVisible(true);
                break;
            case R.id.nav_account:
                fragment = new AccountFragment();
                setTwoPane(false);
                setFabVisible(false);
                break;
            case R.id.nav_starred:
                fragment = new StarredFragment();
                setTwoPane(true);
                setFabVisible(false);
                break;
            case R.id.nav_info:
                fragment = new InfoFragment();
                setTwoPane(false);
                setFabVisible(false);
                break;
            case R.id.nav_settings: {
                Intent intent = new Intent(this, MainPreferenceActivity.class);
                startActivity(intent);
                return;
            }
            case R.id.nav_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return;
            }
            case R.id.nav_manage_accounts: {
                Intent intent = new Intent(this, AccountListActivity.class);
                startActivity(intent);
                return;
            }
            case R.id.nav_add_account: {
                Intent intent = new Intent(this, LibraryListActivity.class);
                startActivity(intent);
                return;
            }
            default:
                selectaccount(item.getItemId());
                drawerLayout.closeDrawer(drawer);
                setAccountSwitcherVisible(false);
                return;
        }
        setFabOnClickListener(item.getItemId());

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

        // Highlight the selected item, update the title, and close the drawer
        drawer.setCheckedItem(item.getItemId());
        selectedItemId = item.getItemId();
        setTitle(item.getTitle());
        drawerLayout.closeDrawer(drawer);
        setAccountSwitcherVisible(false);
        return;
    }


    protected void selectItem(String tag) {
        int id;
        switch (tag) {
            case "search":
                id = R.id.nav_search;
                break;
            case "account":
                id = R.id.nav_account;
                break;
            case "starred":
                id = R.id.nav_starred;
                break;
            case "info":
                id = R.id.nav_info;
                break;
            default:
                return;
        }
        selectItem(drawer.getMenu().findItem(id));
    }

    protected void selectItem(int pos) {
        selectItem(drawer.getMenu().getItem(pos));
    }

    protected void setFabOnClickListener(int id) {
        if (isTablet()) {
            if (id == R.id.nav_search) {
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
        updateAccountSwitcher(account);
    }

    private void updateAccountSwitcher(Account account) {
        if (account == null) return;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        long tolerance = Long.decode(sp.getString("notification_warning", "367200000"));

        aData.open();
        int expiring = aData.getExpiring(account, tolerance);
        aData.close();

        accountTitle.setText(getAccountTitle(account));
        accountSubtitle.setText(getAccountSubtitle(account));
        if (expiring > 0) {
            accountWarning.setText(String.valueOf(expiring));
            accountWarning.setVisibility(View.VISIBLE);
        } else {
            accountWarning.setVisibility(View.GONE);
        }
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
        outState.putInt("selectedItemId", selectedItemId);
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

    public class MetaAdapter<T extends Map.Entry<?, String>> extends ArrayAdapter<T> {

        private List<T> objects;
        private int spinneritem;

        public MetaAdapter(Context context, List<T> objects,
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

            T item = objects.get(position);

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
            tvText.setText(item.getValue());
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

            T item = objects.get(position);

            if (contentView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(spinneritem, viewGroup, false);
            } else {
                view = contentView;
            }

            TextView tvText = (TextView) view.findViewById(android.R.id.text1);
            tvText.setText(item.getValue());
            return view;
        }

    }
}
