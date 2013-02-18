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

public class LibraryListAdapter extends ArrayAdapter<Library> {
	private List<Library> objects;

	@Override
	public View getView(int position, View contentView, ViewGroup viewGroup) {
		View view = null;

		// position always 0-7
		if (objects.get(position) == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.library_listitem, viewGroup,
					false);
			return view;
		}

		Library item = objects.get(position);

		if (contentView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.library_listitem, viewGroup,
					false);
		} else {
			view = contentView;
		}

		TextView tvCity = (TextView) view.findViewById(R.id.tvCity);
		tvCity.setText(item.getCity());
		TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
		if (item.getTitle() != null && !item.getTitle().equals("null")) {
			tvTitle.setText(item.getTitle());
			tvTitle.setVisibility(View.VISIBLE);
		} else
			tvTitle.setVisibility(View.GONE);
		TextView tvSupport = (TextView) view.findViewById(R.id.tvSupport);
		if (item.getSupport() != null && !item.getSupport().equals("null")) {
			tvSupport.setText(item.getSupport());
			tvSupport.setVisibility(View.VISIBLE);
		} else
			tvSupport.setVisibility(View.GONE);

		return view;
	}

	public LibraryListAdapter(Context context, List<Library> objects) {
		super(context, R.layout.library_listitem, objects);
		this.objects = objects;
	}
}
