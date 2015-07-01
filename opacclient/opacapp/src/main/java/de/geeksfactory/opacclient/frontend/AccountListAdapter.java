/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;

public class AccountListAdapter extends ArrayAdapter<Account> {
    private List<Account> objects;
    private Context context;
    private boolean highlight = true;

    public AccountListAdapter(Context context, List<Account> objects) {
        super(context, R.layout.listitem_account, objects);
        this.context = context;
        this.objects = objects;
    }

    public AccountListAdapter setHighlightActiveAccount(boolean highlight) {
        this.highlight = highlight;
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public View getView(int position, View contentView, ViewGroup viewGroup) {
        View view;

        // position always 0-7
        if (objects.get(position) == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.listitem_account, viewGroup,
                    false);
            return view;
        }

        Account item = objects.get(position);

        if (contentView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.listitem_account, viewGroup,
                    false);
        } else {
            view = contentView;
        }

        if (((OpacClient) ((Activity) context).getApplication()).getAccount()
                                                                .getId() == item.getId() &&
                highlight) {
            view.findViewById(R.id.rlItem).setBackgroundColor(
                    context.getResources().getColor(R.color.accent_red));
        } else {
            // should be replaced by setBackground which is not available before API level 16
            view.findViewById(R.id.rlItem).setBackgroundDrawable(null);
        }

        Library lib;
        try {
            lib = ((OpacClient) ((Activity) context).getApplication())
                    .getLibrary(item.getLibrary());
            TextView tvCity = (TextView) view.findViewById(R.id.tvCity);
            if (lib.getTitle() != null && !lib.getTitle().equals("null")) {
                tvCity.setText(lib.getCity() + "\n" + lib.getTitle());
            } else {
                tvCity.setText(lib.getCity());
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        TextView tvName = (TextView) view.findViewById(R.id.tvName);
        if (item.getName() != null) {
            tvName.setText(item.getName());
        }
        TextView tvLabel = (TextView) view.findViewById(R.id.tvLabel);
        if (item.getLabel() != null) {
            tvLabel.setText(item.getLabel());
        }
        return view;
    }
}