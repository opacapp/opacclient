package de.geeksfactory.opacclient.frontend.adapter;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.objects.LentItem;

public class LentAdapter extends AccountAdapter<LentItem, LentAdapter.ViewHolder> {

    public interface Callback {
        void prolong(String prolongData);

        void download(String downloadData);

        void onClick(LentItem item, ViewHolder view);
    }

    private Callback callback;

    public LentAdapter(Callback callback) {
        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.listitem_account_item, parent, false);
        return new ViewHolder(view);
    }

    public class ViewHolder extends AccountAdapter.ViewHolder<LentItem> {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void setItem(final LentItem item) {
            super.setItem(item);
            DateTimeFormatter fmt = DateTimeFormat.shortDate();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            final int tolerance = Integer.parseInt(sp.getString("notification_warning", "3"));

            SpannableStringBuilder builder = new SpannableStringBuilder();
            if (item.getDeadline() != null) {
                int start = builder.length();
                builder.append(fmt.print(item.getDeadline()));
                // setSpan with a span argument is not supported before API 21
                builder.setSpan(new ForegroundColorSpan(textColorPrimary),
                        start, start + fmt.print(item.getDeadline()).length(), 0);
                if (item.getStatus() != null) builder.append(" – ");
            }
            if (item.getStatus() != null) {
                builder.append(Html.fromHtml(item.getStatus()));
            }
            setTextOrHide(builder, tvStatus);

            // Color codes for return dates
            if (item.getDeadline() != null) {
                if (item.getDeadline().equals(LocalDate.now()) ||
                        item.getDeadline().isBefore(LocalDate.now())) {
                    vStatusColor.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.date_overdue));
                } else if (Days.daysBetween(LocalDate.now(), item.getDeadline()).getDays() <=
                        tolerance) {
                    vStatusColor.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.date_warning));
                } else if (item.getDownloadData() != null) {
                    vStatusColor.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.account_downloadable));
                } else {
                    vStatusColor.setBackgroundResource(0);
                }
            } else if (item.getDownloadData() != null) {
                vStatusColor.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.account_downloadable));
            } else {
                vStatusColor.setBackgroundResource(0);
            }

            ivCancel.setVisibility(View.GONE);
            ivBooking.setVisibility(View.GONE);
            if (item.getProlongData() != null) {
                ivProlong.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        callback.prolong(item.getProlongData());
                    }
                });
                ivProlong.setVisibility(View.VISIBLE);
                ViewCompat.setAlpha(ivProlong, item.isRenewable() ? 1f : 0.4f);
                ivDownload.setVisibility(View.GONE);
            } else if (item.getDownloadData() != null &&
                    api != null && api instanceof EbookServiceApi) {
                ivDownload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        callback.download(item.getDownloadData());
                    }
                });
                ivProlong.setVisibility(View.GONE);
                ivDownload.setVisibility(View.VISIBLE);
            } else {
                ivProlong.setVisibility(View.INVISIBLE);
                ivDownload.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onClick(item, ViewHolder.this);
                }
            });
        }
    }
}

