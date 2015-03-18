package android.support.v4.app;

import android.view.View;

import de.geeksfactory.opacclient.R;

/**
 * Created by Johan on 14.01.2015.
 */
public class CustomListFragment extends ListFragment {
    protected void setupIds(View view) {
        view.findViewById(R.id.internalEmpty).setId(INTERNAL_EMPTY_ID);
        view.findViewById(R.id.progressContainer).setId(INTERNAL_PROGRESS_CONTAINER_ID);
        view.findViewById(R.id.listContainer).setId(INTERNAL_LIST_CONTAINER_ID);
    }
}
