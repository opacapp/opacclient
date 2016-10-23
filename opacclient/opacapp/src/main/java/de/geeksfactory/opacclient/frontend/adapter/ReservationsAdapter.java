package de.geeksfactory.opacclient.frontend.adapter;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.ReservedItem;

public class ReservationsAdapter
        extends AccountAdapter<ReservedItem, ReservationsAdapter.ViewHolder> {

    public interface Callback {
        void cancel(String prolongData);

        void bookingStart(String downloadData);

        void onClick(ReservedItem item, ViewHolder view);
    }

    private Callback callback;

    public ReservationsAdapter(Callback callback) {
        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.listitem_account_item, parent, false);
        return new ViewHolder(view);
    }

    public class ViewHolder extends AccountAdapter.ViewHolder<ReservedItem> {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void setItem(final ReservedItem item) {
            super.setItem(item);
            DateTimeFormatter fmt = DateTimeFormat.shortDate();

            SpannableStringBuilder status = new SpannableStringBuilder();
            if (item.getStatus() != null) {
                int start = status.length();
                status.append(Html.fromHtml(item.getStatus()));
                // setSpan with a span argument is not supported before API 21
                status.setSpan(new ForegroundColorSpan(textColorPrimary),
                        start, start + Html.fromHtml(item.getStatus()).length(), 0);
                if (item.getReadyDate() != null || item.getExpirationDate() != null) {
                    status.append(" – ");
                }
            }
            if (item.getReadyDate() != null) {
                status.append(fmt.print(item.getReadyDate()));
            }
            if (item.getExpirationDate() != null) {
                if (item.getReadyDate() != null) status.append(", ");
                status.append(context.getString(R.string.reservation_expire_until)).append(" ")
                      .append(fmt.print(item.getExpirationDate()));
            }
            setTextOrHide(status, tvStatus);

            ivProlong.setVisibility(View.GONE);
            ivDownload.setVisibility(View.GONE);
            if (item.getBookingData() != null) {
                ivBooking.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        callback.bookingStart(item.getBookingData());
                    }
                });
                ivBooking.setVisibility(View.VISIBLE);
                ivCancel.setVisibility(View.GONE);
            } else if (item.getCancelData() != null) {
                ivCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        callback.cancel(item.getCancelData());
                    }
                });
                ivCancel.setVisibility(View.VISIBLE);
                ivBooking.setVisibility(View.GONE);
            } else {
                ivCancel.setVisibility(View.INVISIBLE);
                ivBooking.setVisibility(View.GONE);
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
