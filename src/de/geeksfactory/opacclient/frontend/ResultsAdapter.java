package de.geeksfactory.opacclient.frontend;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.SearchResult;

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

		typemap.put("mbuchs.png", R.drawable.type_mbuchs);
		typemap.put("cdkl.png", R.drawable.type_cdkl);
		typemap.put("cdromkl.png", R.drawable.type_cdromkl);
		typemap.put("mcdroms.png", R.drawable.type_cdromkl);
		typemap.put("ekl.png", R.drawable.type_ekl);
		typemap.put("emedium.png", R.drawable.type_ekl);
		typemap.put("monleihe.png", R.drawable.type_ekl);
		typemap.put("mbmonos.png", R.drawable.type_mbmonos);
		typemap.put("mbuechers.png", R.drawable.type_mbuechers);
		typemap.put("mdvds.png", R.drawable.type_mdvds);
		typemap.put("mdvd.png", R.drawable.type_mdvds);
		typemap.put("mfilms.png", R.drawable.type_mfilms);
		typemap.put("mvideos.png", R.drawable.type_mfilms);
		typemap.put("mhoerbuchs.png", R.drawable.type_mhoerbuchs);
		typemap.put("mmusikcds.png", R.drawable.type_mmusikcds);
		typemap.put("mcdns.png", R.drawable.type_mmusikcds);
		typemap.put("mnoten1s.png", R.drawable.type_mnoten1s);
		typemap.put("munselbs.png", R.drawable.type_munselbs);
		typemap.put("mztgs.png", R.drawable.type_mztgs);
		typemap.put("zeitung.png", R.drawable.type_mztgs);
		typemap.put("spielekl.png", R.drawable.type_spielekl);
		typemap.put("mspiels.png", R.drawable.type_spielekl);
		typemap.put("tafelkl.png", R.drawable.type_tafelkl);
		typemap.put("spiel_konsol.png", R.drawable.type_konsol);
		typemap.put("wii.png", R.drawable.type_konsol);
		// Hof
		typemap.put("buch_rot.png", R.drawable.type_mbuchs);
		typemap.put("buch_gelb.png", R.drawable.type_mbuchs);
		typemap.put("buch_blau.png", R.drawable.type_mbuchs);
		// Wien
		typemap.put("buch_32_32.png", R.drawable.type_mbuchs);
		typemap.put("zeitschrift_32_32.png", R.drawable.type_mztgs);
		typemap.put("cd_32_32.png", R.drawable.type_cdkl);
		typemap.put("dvd_32_32.png", R.drawable.type_mdvds);
		typemap.put("sony_playstation_32_32.png", R.drawable.type_konsol);
		typemap.put("microsoft_xbox_32_32.png", R.drawable.type_konsol);
		typemap.put("nintendo_wii_32_32.png", R.drawable.type_konsol);
		typemap.put("nintendo_ds_32_32.png", R.drawable.type_konsol);
		typemap.put("cd_rom_32_32.png", R.drawable.type_cdromkl);
		typemap.put("video_32_32.png", R.drawable.type_mfilms);
		typemap.put("noten_32_32.png", R.drawable.type_mnoten1s);
		// Magdeburg
		typemap.put("cd.png", R.drawable.type_cdkl);
		typemap.put("cdh.png", R.drawable.type_mhoerbuchs);
		typemap.put("cdhk.png", R.drawable.type_mhoerbuchs);
		typemap.put("dm.png", R.drawable.type_mnoten1s);
		typemap.put("b.png", R.drawable.type_mbuchs);
		typemap.put("bk.png", R.drawable.type_mbuchs);
		typemap.put("dvd.png", R.drawable.type_mdvds);
		typemap.put("dvdk.png", R.drawable.type_mdvds);
		typemap.put("rom.png", R.drawable.type_cdromkl);
		// Bremen
		typemap.put("97.png", R.drawable.type_ekl);
		typemap.put("15.png", R.drawable.type_mdvds);
		typemap.put("16.png", R.drawable.type_mfilms);
		typemap.put("17.png", R.drawable.type_mfilms);
		typemap.put("18.png", R.drawable.type_mfilms);
		typemap.put("19.png", R.drawable.type_mfilms);
		typemap.put("96.png", R.drawable.type_ekl);
		typemap.put("27.png", R.drawable.type_cdkl);
		typemap.put("7.png", R.drawable.type_mmusikcds);
		typemap.put("8.png", R.drawable.type_mmusikcds);
		typemap.put("13.png", R.drawable.type_spielekl);
		typemap.put("22.png", R.drawable.type_spielekl);
		typemap.put("21.png", R.drawable.type_mnoten1s);
		typemap.put("buch01.png", R.drawable.type_mbuchs);
		typemap.put("buch02.png", R.drawable.type_mbuechers);
		typemap.put("buch03.png", R.drawable.type_mbuechers);
		typemap.put("buch04.png", R.drawable.type_mbuechers);
		typemap.put("21.png", R.drawable.type_mnoten1s);
		typemap.put("21.png", R.drawable.type_mnoten1s);
		typemap.put("21.png", R.drawable.type_mnoten1s);
		// Hamburg
		typemap.put("Buch", R.drawable.type_mbuchs);
		typemap.put("Buch Kinder/Jugendliche", R.drawable.type_mbuchs);
		typemap.put("DVD", R.drawable.type_mdvds);
	}
}
