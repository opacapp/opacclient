package de.geeksfactory.opacclient.frontend.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;

import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.frontend.ResultsAdapter;
import de.geeksfactory.opacclient.i18n.AndroidStringProvider;
import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.utils.ISBNTools;

public abstract class AccountAdapter<I extends AccountItem, VH extends AccountAdapter.ViewHolder<I>>
        extends RecyclerView.Adapter<VH> {
    protected List<I> items;
    protected OpacApi api;
    private boolean coversHidden;

    public AccountAdapter() {
        super();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.setCoversHidden(coversHidden);
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

    public void setCoversHidden(boolean coversHidden) {
        if (this.coversHidden != coversHidden) {
            this.coversHidden = coversHidden;
            notifyDataSetChanged();
        }
    }

    public void setApi(OpacApi api) {
        this.api = api;
    }

    public static class ViewHolder<I extends AccountItem> extends RecyclerView.ViewHolder {
        protected Context context;
        public TextView tvTitleAndAuthor;
        protected TextView tvStatus;
        protected View vStatusColor;
        protected ImageButton ivProlong;
        protected ImageButton ivDownload;
        protected ImageButton ivCancel;
        protected ImageButton ivBooking;
        protected ImageView ivCover;
        protected ImageView ivMediaType;

        protected int textColorPrimary;
        private AndroidStringProvider sp;
        private boolean coversHidden;

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
            ivCover = (ImageView) itemView.findViewById(R.id.ivCover);
            ivMediaType = (ImageView) itemView.findViewById(R.id.ivMediaType);

            TypedArray a =
                    context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
            textColorPrimary = a.getColor(0, 0);
            a.recycle();
            sp = new AndroidStringProvider();
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

            if (coversHidden) {
                ivMediaType.setVisibility(View.GONE);
                ivCover.setVisibility(View.GONE);
            } else {
                if (item.getCover() != null) {
                    ivCover.setVisibility(View.VISIBLE);
                    ivMediaType.setVisibility(View.GONE);

                    Drawable loading = VectorDrawableCompat.create(context.getResources(), R.drawable.ic_loading, null);
                    Glide.with(context).using(new ISBNToolsUrlLoader(context))
                         .load(item.getCover())
                         .placeholder(loading)
                         .crossFade()
                         .into(ivCover);
                } else {
                    ivCover.setVisibility(View.GONE);
                    Glide.clear(ivCover);
                    if (item.getMediaType() != null) {
                        ivMediaType.setImageResource(
                                ResultsAdapter.getResourceByMediaType(item.getMediaType
                                        ()));
                        ivMediaType.setContentDescription(sp.getMediaTypeName(item.getMediaType()));
                        ivMediaType.setVisibility(View.VISIBLE);
                    } else {
                        ivMediaType.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

        public void setCoversHidden(boolean coversHidden) {
            this.coversHidden = coversHidden;
        }

        public boolean isCoversHidden() {
            return coversHidden;
        }
    }

    protected static void setTextOrHide(CharSequence value, TextView tv) {
        if (!TextUtils.isEmpty(value)) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(value);
        } else {
            tv.setVisibility(View.GONE);
        }
    }


    private static class ISBNToolsUrlLoader extends BaseGlideUrlLoader<String> {
        public ISBNToolsUrlLoader(Context context) {
            super(context);
        }

        @Override
        protected String getUrl(String url, int width, int height) {
            return ISBNTools.getBestSizeCoverUrl(url, width, height);
        }
    }
}
