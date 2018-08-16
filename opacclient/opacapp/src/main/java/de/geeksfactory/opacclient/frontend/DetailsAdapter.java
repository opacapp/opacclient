package de.geeksfactory.opacclient.frontend;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Detail;

public class DetailsAdapter extends RecyclerView.Adapter<DetailsAdapter.ViewHolder> {
    private final Context context;
    private final List<Detail> details;

    public DetailsAdapter(List<Detail> details, Context context) {
        this.details = details;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.listitem_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Detail detail = details.get(position);
        holder.desc.setText(detail.getDesc());
        holder.content.setText(detail.getContent());
        Linkify.addLinks(holder.content, Linkify.WEB_URLS);
    }

    @Override
    public int getItemCount() {
        return details.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView desc;
        public TextView content;

        public ViewHolder(View itemView) {
            super(itemView);
            this.desc = (TextView) itemView.findViewById(R.id.tvDesc);
            this.content = (TextView) itemView.findViewById(R.id.tvContent);
        }
    }
}
