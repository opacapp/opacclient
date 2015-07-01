package de.geeksfactory.opacclient.frontend;

import android.content.Context;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import de.geeksfactory.opacclient.R;

public class NavigationAdapter extends BaseAdapter {

    protected ArrayList<Item> data = new ArrayList<>();
    protected LayoutInflater inflater;
    protected Context context;

    public NavigationAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addSeperatorItem(final String text) {
        Item item = new Item();
        item.type = Item.TYPE_SEPARATOR;
        item.text = text;
        data.add(item);
        notifyDataSetChanged();
    }

    public void addTextItem(final String text, String tag) {
        Item item = new Item();
        item.type = Item.TYPE_TEXT;
        item.text = text;
        item.tag = tag;
        data.add(item);
        notifyDataSetChanged();
    }

    public void addTextItemWithIcon(final String text, final int iconDrawable,
            String tag) {
        Item item = new Item();
        item.type = Item.TYPE_TEXT;
        item.text = text;
        item.tag = tag;
        item.iconDrawable = iconDrawable;
        data.add(item);
        notifyDataSetChanged();
    }

    public void addLibraryItem(final String text, final String smallText,
            final String number, final long accountId) {
        Item item = new Item();
        item.type = Item.TYPE_ACCOUNT;
        item.text = text;
        item.smallText = smallText;
        item.number = number;
        item.accountId = accountId;
        data.add(item);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).type;
    }

    @Override
    public int getViewTypeCount() {
        return Item.TYPE_MAX_COUNT;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Item getItem(int position) {
        return data.get(position);
    }

    public int getPositionByTag(String tag) {
        for (int i = 0; i < data.size(); i++) {
            if (data != null && data.get(i) != null
                    && tag.equals(data.get(i).tag)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        int type = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();
            switch (type) {
                case Item.TYPE_TEXT:
                    convertView = inflater
                            .inflate(R.layout.drawer_item_text, null);
                    holder.text = (TextView) convertView.findViewById(R.id.text);
                    holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    break;
                case Item.TYPE_ACCOUNT:
                    convertView = inflater.inflate(R.layout.drawer_item_library,
                            null);
                    holder.text = (TextView) convertView.findViewById(R.id.text);
                    holder.smallText = (TextView) convertView
                            .findViewById(R.id.smallText);
                    holder.number = (TextView) convertView
                            .findViewById(R.id.number);
                    break;
                case Item.TYPE_SEPARATOR:
                    convertView = inflater.inflate(R.layout.drawer_item_separator,
                            null);
                    holder.text = (TextView) convertView.findViewById(R.id.text);
                    break;
            }
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        switch (type) {
            case Item.TYPE_TEXT:
                holder.text.setText(data.get(position).text);
                if (data.get(position).iconDrawable != null) {
                    holder.icon.setVisibility(View.VISIBLE);
                    holder.icon.setImageDrawable(ResourcesCompat
                            .getDrawable(context.getResources(), data.get(position).iconDrawable,
                                    context.getTheme()));
                } else {
                    holder.icon.setVisibility(View.INVISIBLE);
                }
                break;
            case Item.TYPE_ACCOUNT:
                holder.text.setText(data.get(position).text);
                holder.smallText.setText(data.get(position).smallText);
                holder.number.setText(data.get(position).number);
                break;
            case Item.TYPE_SEPARATOR:
                holder.text.setText(data.get(position).text);
                break;
        }
        if (convertView != null) convertView.setTag(holder);
        return convertView;
    }

    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public TextView text;
        public TextView smallText;
        public TextView number;
        public ImageView icon;
    }

    public static class Item {
        public static final int TYPE_TEXT = 0;
        public static final int TYPE_ACCOUNT = 1;
        public static final int TYPE_SEPARATOR = 2;
        public static int TYPE_MAX_COUNT = TYPE_SEPARATOR + 1;
        public int type;
        public Integer iconDrawable;
        public String text;
        public String smallText;
        public String number;
        public String tag;
        public long accountId;
    }

}
