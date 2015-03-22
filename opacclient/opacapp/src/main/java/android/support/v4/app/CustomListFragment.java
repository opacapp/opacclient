package android.support.v4.app;

import android.view.View;

import de.geeksfactory.opacclient.R;

public class CustomListFragment extends ListFragment {
    @SuppressWarnings("ResourceType")
    protected void setupIds(View view) {
        view.findViewById(R.id.internalEmpty).setId(INTERNAL_EMPTY_ID);
        view.findViewById(R.id.progressContainer).setId(INTERNAL_PROGRESS_CONTAINER_ID);
        view.findViewById(R.id.listContainer).setId(INTERNAL_LIST_CONTAINER_ID);
    }
}
