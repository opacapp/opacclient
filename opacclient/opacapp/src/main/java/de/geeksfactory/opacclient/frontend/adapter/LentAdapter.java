package de.geeksfactory.opacclient.frontend.adapter;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.LentItem;

public class LentAdapter extends AccountAdapter<LentItem, LentAdapter.ViewHolder> {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.listitem_account_lent, parent, false);
        return new ViewHolder(view);
    }

    public static class ViewHolder extends AccountAdapter.ViewHolder<LentItem> {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void setItem(LentItem item) {
            super.setItem(item);
            DateTimeFormatter fmt = DateTimeFormat.shortDate();

            if (item.getDeadline() != null && item.getStatus() != null) {
                tvStatus.setText(fmt.print(item.getDeadline()) + " (" +
                        Html.fromHtml(item.getStatus()) + ")");
            } else if (item.getDeadline() != null) {
                tvStatus.setText(fmt.print(new LocalDate(item.getDeadline())));
            } else {
                setHtmlTextOrHide(item.getStatus(), tvStatus);
            }
        }
    }
}

