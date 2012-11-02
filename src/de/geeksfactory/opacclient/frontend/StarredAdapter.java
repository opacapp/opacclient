package de.geeksfactory.opacclient.frontend;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Starred;

public class StarredAdapter extends ArrayAdapter<Starred> {
	private List<Starred> objects;
	private StarredActivity ctx;

	@Override
	public View getView(int position, View contentView, ViewGroup viewGroup) {
		View view = null;

		// position always 0-7
		if (objects.get(position) == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.searchresult_listitem,
					viewGroup, false);
			return view;
		}

		Starred item = objects.get(position);

		if (contentView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.starred_item, viewGroup,
					false);
		} else {
			view = contentView;
		}

		TextView tv = (TextView) view.findViewById(R.id.tvTitle);
		if(item.getTitle() != null)
			tv.setText(Html.fromHtml(item.getTitle()));
		else
			tv.setText("");

		ImageView iv = (ImageView) view.findViewById(R.id.ivDelete);
		iv.setFocusableInTouchMode(false);
		iv.setFocusable(false);
		iv.setTag(item);
		iv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				objects.remove((Starred) arg0.getTag());
				ctx.remove((Starred) arg0.getTag());
				notifyDataSetChanged();
			}
		});
		return view;
	}

	public StarredAdapter(Context context, List<Starred> objects) {
		super(context, R.layout.starred_item, objects);
		this.objects = objects;
		this.ctx = (StarredActivity) context;
	}
}
