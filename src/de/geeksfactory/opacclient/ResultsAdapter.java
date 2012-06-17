package de.geeksfactory.opacclient;

import java.util.List;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.text.Html;
import android.util.Log;
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
		if(item.getType().equals("type_mbuchs.png")){
			iv.setImageResource(R.drawable.type_mbuchs);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_cdkl.png")){
			iv.setImageResource(R.drawable.type_cdkl);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_cdromkl.png")){
			iv.setImageResource(R.drawable.type_cdromkl);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_ekl.png")){
			iv.setImageResource(R.drawable.type_ekl);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mbmonos.png")){
			iv.setImageResource(R.drawable.type_mbmonos);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mbuechers.png")){
			iv.setImageResource(R.drawable.type_mbuechers);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mdvds.png")){
			iv.setImageResource(R.drawable.type_mdvds);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mfilms.png")){
			iv.setImageResource(R.drawable.type_mfilms);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mhoerbuchs.png")){
			iv.setImageResource(R.drawable.type_mhoerbuchs);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mmusikcds.png")){
			iv.setImageResource(R.drawable.type_mmusikcds);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mnoten1s.png")){
			iv.setImageResource(R.drawable.type_mnoten1s);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_munselbs.png")){
			iv.setImageResource(R.drawable.type_munselbs);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_mztgs.png")){
			iv.setImageResource(R.drawable.type_mztgs);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_spielekl.png")){
			iv.setImageResource(R.drawable.type_spielekl);
			iv.setVisibility(View.VISIBLE);
		}else if(item.getType().equals("type_tafelkl.png")){
			iv.setImageResource(R.drawable.type_tafelkl);
			iv.setVisibility(View.VISIBLE);
		}else{
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
