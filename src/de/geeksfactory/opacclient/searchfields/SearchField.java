package de.geeksfactory.opacclient.searchfields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class SearchField {
	protected String id;
	protected String displayName;
	protected boolean advanced;

	public SearchField(String id, String displayName, boolean advanced) {
		this.id = id;
		this.displayName = displayName;
		this.advanced = advanced;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName
	 *            the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the advanced
	 */
	public boolean isAdvanced() {
		return advanced;
	}

	/**
	 * @param advanced
	 *            the advanced to set
	 */
	public void setAdvanced(boolean advanced) {
		this.advanced = advanced;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("displayName", displayName);
		json.put("advanced", advanced);
		return json;
	}

	public static SearchField fromJSON(JSONObject json) throws JSONException {
		String id = json.getString("id");
		String type = json.getString("type");
		String displayName = json.getString("displayName");
		boolean advanced = json.getBoolean("advanced");

		if (type.equals("text")) {
			String hint = json.getString("hint");
			boolean freeSearch = json.getBoolean("freeSearch");
			boolean number = json.getBoolean("number");
			boolean halfWidth = json.getBoolean("halfWidth");
			return new TextSearchField(id, displayName, advanced, halfWidth,
					hint, freeSearch, number);
		} else if (type.equals("barcode")) {
			String hint = json.getString("hint");
			boolean halfWidth = json.getBoolean("halfWidth");
			return new BarcodeSearchField(id, displayName, advanced,
					halfWidth, hint);
		} else if (type.equals("checkbox")) {
			return new CheckboxSearchField(id, displayName, advanced);
		} else if (type.equals("dropdown")) {
			List<Map<String, String>> dropdownValues = new ArrayList<Map<String, String>>();
			JSONArray array = json.getJSONArray("dropdownValues");
			for (int i = 0; i < array.length(); i++) {
				JSONObject value = array.getJSONObject(i);
				Map<String, String> map = new HashMap<String, String>();
				map.put("key", value.getString("key"));
				map.put("value", value.getString("value"));
				dropdownValues.add(map);
			}
			return new DropdownSearchField(id, displayName, advanced,
					dropdownValues);
		}
		return null;
	}

}
