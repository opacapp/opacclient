package de.geeksfactory.opacclient.frontend;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import org.acra.ACRA;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class LibraryListActivity extends ActionBarActivity {

    public static final int LEVEL_COUNTRY = 0;
    public static final int LEVEL_STATE = 1;
    public static final int LEVEL_CITY = 2;
    public static final int LEVEL_LIBRARY = 3;

    protected List<Library> libraries;
    protected LibraryListFragment fragment;
    protected LibraryListFragment fragment2;
    protected LibraryListFragment fragment3;
    protected LibraryListFragment fragment4;
    protected boolean visible;

    protected ProgressDialog dialog;

    protected SearchView searchView;
    protected MenuItem searchItem;

    @Override
    protected void onPause() {
        visible = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        visible = true;
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_library_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        if (!getIntent().hasExtra("welcome")) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setHomeButtonEnabled(false);
        }

        new LoadLibrariesTask().execute((OpacClient) getApplication());
        final TextView tvLocateString = (TextView) findViewById(R.id.tvLocateString);
        final ImageView ivLocationIcon = (ImageView) findViewById(R.id.ivLocationIcon);
        final LinearLayout llLocate = (LinearLayout) findViewById(R.id.llLocate);

        final LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); // no GPS
        final String provider = locationManager.getBestProvider(criteria, true);
        if (provider == null) // no geolocation available
        {
            llLocate.setVisibility(View.GONE);
        }

        llLocate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (fragment instanceof LocatedLibraryListFragment) {
                    MenuItemCompat.collapseActionView(searchItem);
                    showListCountries(true);
                    tvLocateString.setText(R.string.geolocate);
                    ivLocationIcon.setImageResource(R.drawable.ic_locate);
                } else {
                    tvLocateString.setText(R.string.geolocate_progress);
                    ivLocationIcon.setImageResource(R.drawable.ic_locate);
                    showListGeo();
                }
            }
        });

        final RelativeLayout rlSuggestLibrary =
                (RelativeLayout) findViewById(R.id.rlSuggestLibrary);
        rlSuggestLibrary.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LibraryListActivity.this,
                        SuggestLibraryActivity.class);
                if (getIntent().hasExtra("welcome")) {
                    intent.putExtra("welcome", true);
                }
                ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation
                        (rlSuggestLibrary, rlSuggestLibrary.getLeft(), rlSuggestLibrary.getTop(),
                                rlSuggestLibrary.getWidth(), rlSuggestLibrary.getHeight());
                ActivityCompat.startActivity(LibraryListActivity.this, intent, options.toBundle());
            }

        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_library_list, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        setSearchTextColor(searchView);

        return true;
    }

    private void setSearchTextColor(SearchView searchView) {
        EditText searchPlate = (EditText) searchView
                .findViewById(R.id.search_src_text);
        searchPlate
                .setTextColor(getResources().getColor(android.R.color.white));
    }

    public boolean isTablet() {
        return findViewById(R.id.container2) != null;
    }

    public void showListGeo() {
        final TextView tvLocateString = (TextView) findViewById(R.id.tvLocateString);
        final ImageView ivLocationIcon = (ImageView) findViewById(R.id.ivLocationIcon);

        final LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); // no GPS
        final String provider = locationManager.getBestProvider(criteria, true);

        if (provider == null) {
            return;
        }
        locationManager.requestLocationUpdates(provider, 0, 0,
                new LocationListener() {
                    @Override
                    public void onStatusChanged(String provider, int status,
                            Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }

                    @Override
                    public void onLocationChanged(Location location) {
                        if (!visible) {
                            return;
                        }
                        fragment = new LocatedLibraryListFragment();
                        Bundle args = new Bundle();
                        args.putInt("level", LEVEL_LIBRARY);
                        fragment.setArguments(args);

                        if (location != null) {
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();
                            // Calculate distances
                            List<Library> distancedlibs = new ArrayList<>();
                            for (Library lib : libraries) {
                                float[] result = new float[1];
                                double[] geo = lib.getGeo();
                                if (geo == null) {
                                    continue;
                                }
                                Location.distanceBetween(lat, lon, geo[0],
                                        geo[1], result);
                                lib.setGeo_distance(result[0]);
                                distancedlibs.add(lib);
                            }
                            Collections.sort(distancedlibs,
                                    new DistanceComparator());
                            distancedlibs = distancedlibs.subList(0, 20);

                            LibraryAdapter adapter = new LibraryAdapter(
                                    LibraryListActivity.this,
                                    R.layout.listitem_library, R.id.tvTitle,
                                    distancedlibs);
                            fragment.setListAdapter(adapter);
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .addToBackStack(null)
                                    .setTransition(
                                            FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                    .replace(R.id.container, fragment).commit();
                            if (fragment2 != null) {
                                getSupportFragmentManager().beginTransaction()
                                                           .detach(fragment2).commit();
                            }
                            if (fragment3 != null) {
                                getSupportFragmentManager().beginTransaction()
                                                           .detach(fragment3).commit();
                            }
                            if (fragment4 != null) {
                                getSupportFragmentManager().beginTransaction()
                                                           .detach(fragment4).commit();
                            }

                            tvLocateString.setText(R.string.alphabetic_list);
                            ivLocationIcon.setImageResource(R.drawable.ic_list);
                        }
                    }
                });
    }

    public void showListCountries(boolean fade) {
        fragment = new LibraryListFragment();
        Bundle args = new Bundle();
        args.putInt("level", LEVEL_COUNTRY);
        fragment.setArguments(args);
        Set<String> data = new HashSet<>();
        for (Library lib : libraries) {
            if (!data.contains(lib.getCountry())) {
                data.add(lib.getCountry());
            }
        }
        List<String> list = new ArrayList<>(data);
        Collator deCollator = Collator.getInstance(Locale.GERMAN);
        deCollator.setStrength(Collator.TERTIARY);
        Collections.sort(list, deCollator);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.listitem_simple, R.id.txtText,
                list.toArray(new String[list.size()]));
        fragment.setListAdapter(adapter);
        if (findViewById(R.id.llFragments) != null) {
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.container, fragment).commit();
            if (fragment2 != null) {
                getSupportFragmentManager().beginTransaction()
                                           .detach(fragment2).commit();
            }
            if (fragment3 != null) {
                getSupportFragmentManager().beginTransaction()
                                           .detach(fragment3).commit();
            }
            if (fragment4 != null) {
                getSupportFragmentManager().beginTransaction()
                                           .detach(fragment4).commit();
            }
        } else {
            if (fade) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setTransition(
                                FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.container, fragment).commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                                           .replace(R.id.container, fragment).commit();
            }
        }
    }

    public void showListStates(String country) {
        LibraryListFragment fragment = new LibraryListFragment();
        Bundle args = new Bundle();
        args.putInt("level", LEVEL_STATE);
        args.putString("country", country);
        fragment.setArguments(args);
        Set<String> data = new HashSet<>();
        for (Library lib : libraries) {
            if (country.equals(lib.getCountry())
                    && !data.contains(lib.getState())) {
                data.add(lib.getState());
            }
        }
        List<String> list = new ArrayList<>(data);
        if (data.size() == 1) {
            showListCities(country, list.get(0));
        }
        Collator deCollator = Collator.getInstance(Locale.GERMAN);
        deCollator.setStrength(Collator.TERTIARY);
        Collections.sort(list, deCollator);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.listitem_simple, R.id.txtText, list);
        fragment.setListAdapter(adapter);
        if (findViewById(R.id.llFragments) != null) {
            fragment2 = fragment;
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.container2, fragment2).commit();
            if (fragment3 != null) {
                getSupportFragmentManager().beginTransaction()
                                           .detach(fragment3).commit();
            }
            if (fragment4 != null) {
                getSupportFragmentManager().beginTransaction()
                                           .detach(fragment4).commit();
            }
        } else if (data.size() > 1) {
            this.fragment = fragment;
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.container, fragment).addToBackStack(null)
                                       .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                       .commit();
        }
    }

    public void showListCities(String country, String state) {
        LibraryListFragment fragment = new LibraryListFragment();
        Bundle args = new Bundle();
        args.putInt("level", LEVEL_CITY);
        args.putString("country", country);
        args.putString("state", state);
        fragment.setArguments(args);
        Set<String> data = new HashSet<>();
        for (Library lib : libraries) {
            if (country.equals(lib.getCountry())
                    && state.equals(lib.getState())
                    && !data.contains(lib.getCity())) {
                data.add(lib.getCity());
            }
        }
        List<String> list = new ArrayList<>(data);
        if (data.size() == 1 && list.get(0).equals(state)) { // City states
            showListLibraries(country, state, list.get(0));
        }
        Collator deCollator = Collator.getInstance(Locale.GERMAN);
        deCollator.setStrength(Collator.TERTIARY);
        Collections.sort(list, deCollator);
        ArrayAdapter<String> adapter = new CityAdapter(this,
                R.layout.listitem_simple, R.id.txtText, list);
        fragment.setListAdapter(adapter);
        if (findViewById(R.id.llFragments) != null) {
            fragment3 = fragment;
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.container3, fragment3).commit();
            if (fragment4 != null) {
                getSupportFragmentManager().beginTransaction()
                                           .detach(fragment4).commit();
            }
        } else if (data.size() > 1 || !list.get(0).equals(state)) {
            this.fragment = fragment;
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.container, fragment).addToBackStack(null)
                                       .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                       .commit();
        }
    }

    public void showListLibraries(String country, String state, String city) {
        LibraryListFragment fragment = new LibraryListFragment();
        Bundle args = new Bundle();
        args.putInt("level", LEVEL_LIBRARY);
        args.putString("country", country);
        args.putString("state", state);
        args.putString("city", city);
        fragment.setArguments(args);
        Set<Library> data = new HashSet<>();
        for (Library lib : libraries) {
            if (country.equals(lib.getCountry())
                    && state.equals(lib.getState())
                    && city.equals(lib.getCity()) && !data.contains(lib)) {
                data.add(lib);
            }
        }
        List<Library> list = new ArrayList<>(data);
        Collections.sort(list);
        LibraryAdapter adapter = new LibraryAdapter(this,
                R.layout.listitem_library_in_city, R.id.tvTitle, list);
        fragment.setListAdapter(adapter);
        if (findViewById(R.id.llFragments) != null) {
            fragment4 = fragment;
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.container4, fragment4).commit();
        } else {
            this.fragment = fragment;
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.container, fragment).addToBackStack(null)
                                       .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                       .commit();
        }
    }

    public void search(String query) {
        if (libraries == null) return;

        fragment = new LocatedLibraryListFragment();
        Bundle args = new Bundle();
        args.putInt("level", LEVEL_LIBRARY);
        fragment.setArguments(args);
        Set<LibrarySearchResult> data = new HashSet<>();
        query = query.toLowerCase(Locale.GERMAN);
        for (Library lib : libraries) {
            int rank = 0;
            if (lib.getCity().toLowerCase(Locale.GERMAN).contains(query)) {
                rank += 3;
            }
            if (lib.getTitle().toLowerCase(Locale.GERMAN).contains(query)) {
                rank += 3;
            }
            if (lib.getState().toLowerCase(Locale.GERMAN).contains(query)) {
                rank += 2;
            }
            if (lib.getCountry().toLowerCase(Locale.GERMAN).contains(query)) {
                rank += 1;
            }
            if (rank > 0) {
                data.add(new LibrarySearchResult(lib, rank));
            }
        }
        List<LibrarySearchResult> list = new ArrayList<>(
                data);
        Collections.sort(list);
        List<Library> libraries = new ArrayList<>();
        for (LibrarySearchResult sr : list) {
            libraries.add(sr.getLibrary());
        }
        LibraryAdapter adapter = new LibraryAdapter(this,
                R.layout.listitem_library, R.id.tvTitle, libraries);
        fragment.setListAdapter(adapter);
        getSupportFragmentManager().beginTransaction().addToBackStack(null)
                                   .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                   .replace(R.id.container, fragment).commit();
        if (fragment2 != null) {
            getSupportFragmentManager().beginTransaction().detach(fragment2)
                                       .commit();
        }
        if (fragment3 != null) {
            getSupportFragmentManager().beginTransaction().detach(fragment3)
                                       .commit();
        }
        if (fragment4 != null) {
            getSupportFragmentManager().beginTransaction().detach(fragment4)
                                       .commit();
        }

        TextView tvLocateString = (TextView) findViewById(R.id.tvLocateString);
        ImageView ivLocationIcon = (ImageView) findViewById(R.id.ivLocationIcon);
        tvLocateString.setText(R.string.alphabetic_list);
        ivLocationIcon.setImageResource(R.drawable.ic_list);
    }

    private static class LibrarySearchResult implements
            Comparable<LibrarySearchResult> {
        private Library library;
        private int rank;

        public LibrarySearchResult(Library library, int rank) {
            this.library = library;
            this.rank = rank;
        }

        public Library getLibrary() {
            return library;
        }

        public int getRank() {
            return rank;
        }

        @Override
        public int hashCode() {
            return library.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LibrarySearchResult &&
                    library.equals(((LibrarySearchResult) obj).getLibrary());
        }

        @Override
        public int compareTo(@NonNull LibrarySearchResult another) {
            if (another.rank == rank) {
                return library.getCity().compareTo(
                        another.getLibrary().getCity());
            } else if (another.rank < rank) {
                return -1;
            } else {
                return 1;
            }
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class LibraryListFragment extends ListFragment {
        public int level;
        public View view;

        public LibraryListFragment() {
            super();
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            switch (level) {
                case LEVEL_COUNTRY:
                    ((LibraryListActivity) getActivity())
                            .showListStates((String) getListAdapter().getItem(
                                    position));
                    break;
                case LEVEL_STATE:
                    ((LibraryListActivity) getActivity()).showListCities(
                            getArguments().getString("country"),
                            (String) getListAdapter().getItem(position));
                    break;
                case LEVEL_CITY:
                    ((LibraryListActivity) getActivity()).showListLibraries(
                            getArguments().getString("country"), getArguments()
                                    .getString("state"), (String) getListAdapter()
                                    .getItem(position));
                    break;
                case LEVEL_LIBRARY:
                    Library lib = (Library) getListAdapter().getItem(position);
                    AccountDataSource data = new AccountDataSource(getActivity());
                    data.open();
                    Account acc = new Account();
                    acc.setLibrary(lib.getIdent());
                    acc.setLabel(getActivity().getString(
                            R.string.default_account_name));
                    long insertedid = data.addAccount(acc);
                    data.close();

                    ((OpacClient) getActivity().getApplication())
                            .setAccount(insertedid);

                    Intent i = new Intent(getActivity(), AccountEditActivity.class);
                    i.putExtra("id", insertedid);
                    i.putExtra("adding", true);
                    if (getActivity().getIntent().hasExtra("welcome")) {
                        i.putExtra("welcome", true);
                    }
                    getActivity().startActivity(i);
                    getActivity().finish();
                    break;
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public View onCreateView(LayoutInflater inflater,
                ViewGroup container, Bundle savedInstanceState) {
            level = getArguments().getInt("level", LEVEL_COUNTRY);
            setRetainInstance(true);
            view = super.onCreateView(inflater, container, savedInstanceState);
            ListView lv = ((ListView) view.findViewById(android.R.id.list));
            lv.setChoiceMode(((LibraryListActivity) getActivity()).isTablet() ?
                    AbsListView.CHOICE_MODE_SINGLE
                    : AbsListView.CHOICE_MODE_NONE);
            if (level == LEVEL_CITY) {
                lv.setFastScrollEnabled(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    lv.setFastScrollAlwaysVisible(true);
                }
            }
            lv.setSelector(R.drawable.ripple);
            return view;
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            setEmptyText(getResources().getString(R.string.no_results));
        }
    }

    public static class LocatedLibraryListFragment extends LibraryListFragment {

        public LocatedLibraryListFragment() {
            super();
        }

    }

    public static class CityAdapter extends ArrayAdapter<String> implements
            SectionIndexer {

        private final static String[] letters = {"A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
                "S", "T", "U", "V", "W", "X", "Y", "Z", "Ä", "Ö", "Ü"};

        public CityAdapter(Context context, int resource,
                int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            boolean found = false;
            int i = 0;
            while (i < getCount() && !found) {
                String city = getItem(i);
                String letter = city.substring(0, 1).toUpperCase(Locale.GERMAN);
                if (Arrays.asList(letters).indexOf(letter) >= sectionIndex) {
                    found = true;
                } else {
                    i++;
                }
            }
            return i;
        }

        @Override
        public int getSectionForPosition(int position) {
            String letter = getItem(position).substring(0, 1).toUpperCase(
                    Locale.GERMAN);
            return Arrays.asList(letters).indexOf(letter);
        }

        @Override
        public Object[] getSections() {
            return letters;
        }
    }

    public static class DistanceComparator implements Comparator<Library> {
        @Override
        public int compare(Library o1, Library o2) {
            return ((Float) o1.getGeo_distance()).compareTo(o2
                    .getGeo_distance());
        }
    }

    public class LibraryAdapter extends ArrayAdapter<Library> {

        private Context context;
        private int resource;

        public LibraryAdapter(Context context, int resource,
                int textViewResourceId, List<Library> objects) {
            super(context, resource, textViewResourceId, objects);
            this.context = context;
            this.resource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(resource, parent,
                        false);
            } else {
                view = convertView;
            }
            Library item = getItem(position);

            TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
            TextView tvSupport = (TextView) view.findViewById(R.id.tvSupport);
            tvTitle.setText(item.getTitle());
            tvSupport.setText(getResources().getString(
                    item.isAccountSupported() ? R.string.support_search_account
                            : R.string.support_search));
            if (view.findViewById(R.id.tvCity) != null) {
                TextView tvCity = (TextView) view.findViewById(R.id.tvCity);
                tvCity.setText(item.getCity());
            }
            return view;
        }

    }

    protected class LoadLibrariesTask extends
            AsyncTask<OpacClient, Double, List<Library>> {
        private long startTime;
        private int progressUpdateCount = 0;

        @Override
        protected void onProgressUpdate(Double... progress) {
            if (progressUpdateCount == 0) {
                startTime = System.currentTimeMillis();
            } else if (progressUpdateCount == 1) {
                double timeElapsed = System.currentTimeMillis() - startTime;
                double expectedTime = timeElapsed / progress[0];
                if (expectedTime > 300) {
                    dialog = ProgressDialog.show(LibraryListActivity.this, "",
                            getString(R.string.loading_libraries), true, false);
                    dialog.show();
                }
            }
            progressUpdateCount++;
        }

        @Override
        protected List<Library> doInBackground(OpacClient... arg0) {
            OpacClient app = arg0[0];
            try {
                return app.getLibraries(new OpacClient.ProgressCallback() {
                    @Override
                    public void publishProgress(double progress) {
                        LoadLibrariesTask.this.publishProgress(progress);
                    }
                });
            } catch (IOException e) {
                ACRA.getErrorReporter().handleException(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Library> result) {
            libraries = result;
            if (dialog != null) dialog.dismiss();
            if (libraries == null) return;
            if (!visible) return;

            // Get the intent, verify the action and get the query
            Intent intent = getIntent();
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                search(query);
            } else {
                showListCountries(false);
            }
        }
    }

}
