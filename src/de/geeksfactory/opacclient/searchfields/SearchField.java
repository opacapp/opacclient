package de.geeksfactory.opacclient.searchfields;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class SearchField {
	public static class OrderComparator implements Comparator<SearchField> {
		@Override
		public int compare(SearchField a, SearchField b) {
			if (a.getMeaning() == null || b.getMeaning() == null) {
				return 0;
			} else {
				return a.getMeaning().compareTo(b.getMeaning());
			}
		}
	}

	protected String id;
	protected String displayName;
	protected boolean advanced;
	/**
	 * Optional attribute, describes the meaning of the search field.
	 * Used for sorting the search fields in the form.
	 * Will be assigned automatically by the MeaningDetector if you use it.
	 */
	protected Meaning meaning;

	public enum Meaning {
		FREE, TITLE, AUTHOR, DIGITAL, AVAILABLE, ISBN, BARCODE, YEAR, BRANCH, HOME_BRANCH,
		CATEGORY, PUBLISHER, KEYWORD, SYSTEM, AUDIENCE, LOCATION, ORDER
	}

	/**
	 * A JSONObject where you can save arbitrary data about this Search field
	 */
	protected JSONObject data;

	public SearchField() {

	}

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
		if (data != null)
			json.put("data", data);
		if (meaning != null)
			json.put("meaning", meaning.toString());
		return json;
	}

	public static SearchField fromJSON(JSONObject json) throws JSONException {
		String id = json.getString("id");
		String type = json.getString("type");
		String displayName = json.getString("displayName");
		JSONObject data = null;
		if (json.has("data"))
			data = json.getJSONObject("data");
		Meaning meaning = null;
		if (json.has("meaning"))
			meaning = Meaning.valueOf(json.getString("meaning"));
		boolean advanced = json.getBoolean("advanced");

		SearchField field = null;
		if (type.equals("text")) {
			String hint = json.getString("hint");
			boolean freeSearch = json.getBoolean("freeSearch");
			boolean number = json.getBoolean("number");
			boolean halfWidth = json.getBoolean("halfWidth");
			field = new TextSearchField(id, displayName, advanced, halfWidth,
					hint, freeSearch, number);
		} else if (type.equals("barcode")) {
			String hint = json.getString("hint");
			boolean halfWidth = json.getBoolean("halfWidth");
			field = new BarcodeSearchField(id, displayName, advanced,
					halfWidth, hint);
		} else if (type.equals("checkbox")) {
			field = new CheckboxSearchField(id, displayName, advanced);
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
			field = new DropdownSearchField(id, displayName, advanced,
					dropdownValues);
		}
		if (field != null) {
			field.setData(data);
			field.setMeaning(meaning);
		}
		return field;
	}

	/**
	 * @return the data
	 */
	public JSONObject getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(JSONObject data) {
		this.data = data;
	}

	/**
	 * @return the meaning
	 */
	public Meaning getMeaning() {
		return meaning;
	}

	/**
	 * @param meaning the meaning to set
	 */
	public void setMeaning(Meaning meaning) {
		this.meaning = meaning;
	}

}
