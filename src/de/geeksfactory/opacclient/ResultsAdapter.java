package de.geeksfactory.opacclient;

import java.util.List;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultsAdapter extends ArrayAdapter<SearchResult> {
	private List<SearchResult> objects;

	@Override
	public View getView(int position, View contentView, ViewGroup viewGroup) {
		View view = null;
		
		// position always 0-7
		if(objects.get(position) == null){
			LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    view = layoutInflater.inflate(R.layout.searchresult_listitem, viewGroup, false);
		    return view;
		}
		
		SearchResult item = objects.get(position);
		
		if (contentView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    view = layoutInflater.inflate(R.layout.searchresult_listitem, viewGroup, false);
		}else{
			view = contentView;
		}
		
		TextView tv = (TextView) view.findViewById(R.id.tvResult);
		tv.setText(Html.fromHtml(item.getInnerhtml()));
		
		ImageView iv = (ImageView) view.findViewById(R.id.ivType);
		try {
			iv.setImageDrawable(view.getResources().getDrawable(view.getResources().getIdentifier(item.getType().replace(".png", ""), "drawable", "de.geeksfactory.opacclient")));
		} catch(NotFoundException e) {
			iv.setVisibility(View.INVISIBLE);
		}
			
		return view;
	}

	public ResultsAdapter(Context context,
			List<SearchResult> objects) {
		super(context, R.layout.searchresult_listitem, objects);
		this.objects = objects;
	}
}
