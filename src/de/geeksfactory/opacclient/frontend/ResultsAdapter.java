package de.geeksfactory.opacclient.frontend;

import java.util.HashMap;
import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.R.drawable;
import de.geeksfactory.opacclient.R.id;
import de.geeksfactory.opacclient.R.layout;
import de.geeksfactory.opacclient.storage.SearchResult;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultsAdapter extends ArrayAdapter<SearchResult> {
	private List<SearchResult> objects;
	public HashMap<String, Integer> typemap = new HashMap<String, Integer>();

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

		SearchResult item = objects.get(position);

		if (contentView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.searchresult_listitem,
					viewGroup, false);
		} else {
			view = contentView;
		}

		TextView tv = (TextView) view.findViewById(R.id.tvResult);
		tv.setText(Html.fromHtml(item.getInnerhtml()));

		ImageView iv = (ImageView) view.findViewById(R.id.ivType);

		if (typemap.containsKey(item.getType())) {
			iv.setImageResource(typemap.get(item.getType()));
			iv.setVisibility(View.VISIBLE);
		} else {
			iv.setVisibility(View.INVISIBLE);
		}

		return view;
	}

	public ResultsAdapter(Context context, List<SearchResult> objects) {
		super(context, R.layout.searchresult_listitem, objects);
		this.objects = objects;

		typemap.put("type_mbuchs.png", R.drawable.type_mbuchs);
		typemap.put("type_cdkl.png", R.drawable.type_cdkl);
		typemap.put("type_cdromkl.png", R.drawable.type_cdromkl);
		typemap.put("type_mcdroms.png", R.drawable.type_cdromkl);
		typemap.put("type_ekl.png", R.drawable.type_ekl);
		typemap.put("type_emedium.png", R.drawable.type_ekl);
		typemap.put("type_monleihe.png", R.drawable.type_ekl);
		typemap.put("type_mbmonos.png", R.drawable.type_mbmonos);
		typemap.put("type_mbuechers.png", R.drawable.type_mbuechers);
		typemap.put("type_mdvds.png", R.drawable.type_mdvds);
		typemap.put("type_mdvd.png", R.drawable.type_mdvds);
		typemap.put("type_mfilms.png", R.drawable.type_mfilms);
		typemap.put("type_mvideos.png", R.drawable.type_mfilms);
		typemap.put("type_mhoerbuchs.png", R.drawable.type_mhoerbuchs);
		typemap.put("type_mmusikcds.png", R.drawable.type_mmusikcds);
		typemap.put("type_mcdns.png", R.drawable.type_mmusikcds);
		typemap.put("type_mnoten1s.png", R.drawable.type_mnoten1s);
		typemap.put("type_munselbs.png", R.drawable.type_munselbs);
		typemap.put("type_mztgs.png", R.drawable.type_mztgs);
		typemap.put("type_zeitung.png", R.drawable.type_mztgs);
		typemap.put("type_spielekl.png", R.drawable.type_spielekl);
		typemap.put("type_mspiels.png", R.drawable.type_spielekl);
		typemap.put("type_tafelkl.png", R.drawable.type_tafelkl);
		typemap.put("type_spiel_konsol.png", R.drawable.type_konsol);
		typemap.put("type_wii.png", R.drawable.type_konsol);
		// Hof
		typemap.put("type_buch_rot.png", R.drawable.type_mbuchs);
		typemap.put("type_buch_gelb.png", R.drawable.type_mbuchs);
		typemap.put("type_buch_blau.png", R.drawable.type_mbuchs);
		// Wien
		typemap.put("type_buch_32_32.png", R.drawable.type_mbuchs);
		typemap.put("type_zeitschrift_32_32.png", R.drawable.type_mztgs);
		typemap.put("type_cd_32_32.png", R.drawable.type_cdkl);
		typemap.put("type_dvd_32_32.png", R.drawable.type_mdvds);
		typemap.put("type_sony_playstation_32_32.png", R.drawable.type_konsol);
		typemap.put("type_microsoft_xbox_32_32.png", R.drawable.type_konsol);
		typemap.put("type_nintendo_wii_32_32.png", R.drawable.type_konsol);
		typemap.put("type_nintendo_ds_32_32.png", R.drawable.type_konsol);
		typemap.put("type_cd_rom_32_32.png", R.drawable.type_cdromkl);
		typemap.put("type_video_32_32.png", R.drawable.type_mfilms);
		typemap.put("type_noten_32_32.png", R.drawable.type_mnoten1s);
		// Magdeburg
		typemap.put("type_cd.png", R.drawable.type_cdkl);
		typemap.put("type_cdh.png", R.drawable.type_mhoerbuchs);
		typemap.put("type_cdhk.png", R.drawable.type_mhoerbuchs);
		typemap.put("type_dm.png", R.drawable.type_mnoten1s);
		typemap.put("type_b.png", R.drawable.type_mbuchs);
		typemap.put("type_bk.png", R.drawable.type_mbuchs);
		typemap.put("type_dvd.png", R.drawable.type_mdvds);
		typemap.put("type_dvdk.png", R.drawable.type_mdvds);
		typemap.put("type_rom.png", R.drawable.type_cdromkl);
	}
}
