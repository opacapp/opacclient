package de.geeksfactory.opacclient.frontend.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.AccountItem;

public abstract class AccountAdapter<I extends AccountItem, VH extends AccountAdapter.ViewHolder<I>>
        extends RecyclerView.Adapter<VH> {
    protected List<I> items;
    protected OpacApi api;

    public AccountAdapter() {
        super();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.setItem(items.get(position));
    }

    @Override
    public int getItemCount() {
        if (items == null) {
            return 0;
        } else {
            return items.size();
        }
    }

    public void setItems(List<I> items) {
        this.items = items;
        notifyDataSetChanged();
    }


    public void setApi(OpacApi api) {
        this.api = api;
    }

    public static class ViewHolder<I extends AccountItem> extends RecyclerView.ViewHolder {
        protected Context context;
        protected TextView tvTitleAndAuthor;
        protected TextView tvStatus;
        protected View vStatusColor;
        protected ImageButton ivProlong;
        protected ImageButton ivDownload;
        protected ImageButton ivCancel;
        protected ImageButton ivBooking;
        protected int textColorPrimary;

        public ViewHolder(View itemView) {
            super(itemView);
            this.context = itemView.getContext();
            tvTitleAndAuthor = (TextView) itemView.findViewById(R.id.tvTitleAndAuthor);
            tvStatus = (TextView) itemView.findViewById(R.id.tvStatus);
            vStatusColor = itemView.findViewById(R.id.vStatusColor);
            ivProlong = (ImageButton) itemView.findViewById(R.id.ivProlong);
            ivDownload = (ImageButton) itemView.findViewById(R.id.ivDownload);
            ivCancel = (ImageButton) itemView.findViewById(R.id.ivCancel);
            ivBooking = (ImageButton) itemView.findViewById(R.id.ivBooking);

            TypedArray a =
                    context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
            textColorPrimary = a.getColor(0, 0);
            a.recycle();
        }

        public void setItem(I item) {
            // Overview (Title/Author, Status/Deadline)
            if (item.getTitle() != null && item.getAuthor() != null) {
                tvTitleAndAuthor.setText(item.getTitle() + ", " + item.getAuthor());
            } else if (item.getTitle() != null) {
                tvTitleAndAuthor.setText(item.getTitle());
            } else {
                setTextOrHide(item.getAuthor(), tvTitleAndAuthor);
            }
        }
    }

    protected static void setHtmlTextOrHide(String value, TextView tv) {
        if (!TextUtils.isEmpty(value)) {
            tv.setText(Html.fromHtml(value));
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    protected static void setTextOrHide(CharSequence value, TextView tv) {
        if (!TextUtils.isEmpty(value)) {
            tv.setText(value);
        } else {
            tv.setVisibility(View.GONE);
        }
    }
}
