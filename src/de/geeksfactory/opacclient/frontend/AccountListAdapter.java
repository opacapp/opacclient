package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;

public class AccountListAdapter extends ArrayAdapter<Account> {
	private List<Account> objects;
	private Context context;

	@Override
	public View getView(int position, View contentView, ViewGroup viewGroup) {
		View view = null;

		// position always 0-7
		if (objects.get(position) == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.account_listitem,
					viewGroup, false);
			return view;
		}

		Account item = objects.get(position);

		if (contentView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.account_listitem,
					viewGroup, false);
		} else {
			view = contentView;
		}

		Library lib;
		try {
			lib = ((OpacClient) ((Activity) context).getApplication()).getLibrary(item.getBib());
			TextView tvCity = (TextView) view.findViewById(R.id.tvCity);
			if (lib.getTitle() != null && !lib.getTitle().equals("null")) {
				tvCity.setText(lib.getCity() + "\n" + lib.getTitle());
			} else {
				tvCity.setText(lib.getCity());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		TextView tvName = (TextView) view.findViewById(R.id.tvName);
		if (item.getName() != null)
			tvName.setText(item.getName());
		TextView tvLabel = (TextView) view.findViewById(R.id.tvLabel);
		if (item.getLabel() != null)
			tvLabel.setText(item.getLabel());
		return view;
	}

	public AccountListAdapter(Context context, List<Account> objects) {
		super(context, R.layout.account_listitem, objects);
		this.context = context;
		this.objects = objects;
	}
}