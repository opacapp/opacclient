package de.geeksfactory.opacclient.frontend;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Copy;

public class CopiesAdapter extends RecyclerView.Adapter<CopiesAdapter.ViewHolder> {
    protected List<Copy> copies;
    protected Context context;

    public CopiesAdapter(List<Copy> copies, Context context) {
        this.copies = copies;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.listitem_copy, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return copies.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
