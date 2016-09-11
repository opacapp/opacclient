package de.geeksfactory.opacclient.frontend.adapter;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.ReservedItem;

public class ReservationsAdapter
        extends AccountAdapter<ReservedItem, ReservationsAdapter.ViewHolder> {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.listitem_account_reservation, parent, false);
        return new ViewHolder(view);
    }

    public static class ViewHolder extends AccountAdapter.ViewHolder<ReservedItem> {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void setItem(ReservedItem item) {
            super.setItem(item);
            DateTimeFormatter fmt = DateTimeFormat.shortDate();

            StringBuilder status = new StringBuilder();
            if (item.getStatus() != null) status.append(item.getStatus());
            boolean needsBraces = item.getStatus() != null &&
                    (item.getReadyDate() != null || item.getExpirationDate() != null);
            if (needsBraces) status.append(" (");
            if (item.getReadyDate() != null) {
                status.append(context.getString(R.string.reservation_expire_until)).append(" ")
                      .append(fmt.print(item.getReadyDate()));
            }
            if (item.getExpirationDate() != null) {
                if (item.getReadyDate() != null) status.append(", ");
                status.append(fmt.print(item.getExpirationDate()));
            }
            if (needsBraces) status.append(")");
            if (status.length() > 0) {
                tvStatus.setText(Html.fromHtml(status.toString()));
            } else {
                tvStatus.setVisibility(View.GONE);
            }
        }
    }
}
