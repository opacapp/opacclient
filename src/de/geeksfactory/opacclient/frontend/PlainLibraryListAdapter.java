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

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Library;

public class PlainLibraryListAdapter extends ArrayAdapter<Library> {
	private Context context;
	private List<Library> objects;

	public PlainLibraryListAdapter(Context context, List<Library> objects) {
		super(context, R.layout.listitem_library, objects);
		this.objects = objects;
		this.context = context;
	}

	@Override
	public View getView(int position, View contentView, ViewGroup viewGroup) {
		Library item = objects.get(position);

		if (contentView == null) {
			LayoutInflater infalInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			contentView = infalInflater
					.inflate(R.layout.listitem_library, null);
		}

		TextView tvCity = (TextView) contentView.findViewById(R.id.tvCity);
		tvCity.setText(item.getCity());
		TextView tvTitle = (TextView) contentView.findViewById(R.id.tvTitle);
		if (item.getTitle() != null && !item.getTitle().equals("null")) {
			tvTitle.setText(item.getTitle());
			tvTitle.setVisibility(View.VISIBLE);
		} else
			tvTitle.setVisibility(View.GONE);
		TextView tvSupport = (TextView) contentView
				.findViewById(R.id.tvSupport);
		if (item.getSupport() != null && !item.getSupport().equals("null")) {
			tvSupport.setText(item.getSupport());
			tvSupport.setVisibility(View.VISIBLE);
		} else
			tvSupport.setVisibility(View.GONE);

		return contentView;
	}
}
