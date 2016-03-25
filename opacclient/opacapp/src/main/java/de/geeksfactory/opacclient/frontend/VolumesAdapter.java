package de.geeksfactory.opacclient.frontend;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Volume;

public class VolumesAdapter extends RecyclerView.Adapter<VolumesAdapter.ViewHolder> {
    private final List<Volume> volumes;
    private final Context context;

    public VolumesAdapter(List<Volume> volumes, Context context) {
        this.volumes = volumes;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.listitem_volume, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Volume volume = volumes.get(position);
        holder.tvTitle.setText(volume.getTitle());

        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, SearchResultDetailActivity.class);
                        intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID, volume.getId());
                        intent.putExtra("from_collection", true);
                        context.startActivity(intent);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return volumes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle;
        public View itemView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
        }
    }
}
