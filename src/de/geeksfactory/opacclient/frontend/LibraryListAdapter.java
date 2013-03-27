package de.geeksfactory.opacclient.frontend;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Library;

public class LibraryListAdapter extends BaseExpandableListAdapter {
	private Context context;
	private ArrayList<String> groups;
	private ArrayList<ArrayList<Library>> children;

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	public LibraryListAdapter(Context context) {
		this.context = context;
		this.groups = new ArrayList<String>();
		this.children = new ArrayList<ArrayList<Library>>();
	}

	/**
	 * Searches the adapter for the position of a given library
	 * 
	 * @param library
	 *            Library object to search for
	 * @return Array of groupPosition and childPosition
	 */
	public int[] findPosition(Library library) {
		if (library == null)
			return null;
		int groups_len = children.size();
		for (int i = 0; i < groups_len; i++) {
			int childs_len = children.get(i).size();
			ArrayList<Library> child = children.get(i);
			for (int j = 0; j < childs_len; j++) {
				if (child.get(j) != null) {
					if (child.get(j) == library || child.get(j).equals(library))
						return new int[] { i, j };
				}
			}
		}
		return null;
	}

	public void addItem(Library library) {
		if (!groups.contains(library.getGroup())) {
			groups.add(library.getGroup());
		}
		int index = groups.indexOf(library.getGroup());
		if (children.size() < index + 1) {
			children.add(new ArrayList<Library>());
		}
		children.get(index).add(library);
	}

	@Override
	public Library getChild(int groupPosition, int childPosition) {
		return children.get(groupPosition).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		Library item = (Library) getChild(groupPosition, childPosition);

		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater
					.inflate(R.layout.library_listitem, null);
		}

		TextView tvCity = (TextView) convertView.findViewById(R.id.tvCity);
		tvCity.setText(item.getCity());
		TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
		if (item.getTitle() != null && !item.getTitle().equals("null")) {
			tvTitle.setText(item.getTitle());
			tvTitle.setVisibility(View.VISIBLE);
		} else
			tvTitle.setVisibility(View.GONE);
		TextView tvSupport = (TextView) convertView
				.findViewById(R.id.tvSupport);
		if (item.getSupport() != null && !item.getSupport().equals("null")) {
			tvSupport.setText(item.getSupport());
			tvSupport.setVisibility(View.VISIBLE);
		} else
			tvSupport.setVisibility(View.GONE);

		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return children.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return groups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		String group = (String) getGroup(groupPosition);
		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.group_listitem, null);
		}
		TextView tv = (TextView) convertView.findViewById(R.id.tvTitle);
		tv.setText(group);
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}
}
