package de.geeksfactory.opacclient.searchfields;

import org.json.JSONException;
import org.json.JSONObject;

public class TextSearchField extends SearchField {
	protected String hint;
	protected boolean freeSearch;
	protected boolean number;
	protected boolean halfWidth;

	public TextSearchField() {
		
	}
	
	/**
	 * @param id
	 *            ID of the search field, later given to your search() function
	 * @param displayName
	 *            The name to display for the search field
	 * @param advanced
	 * 			  Set if this field should only be shown when showing the advanced
	 *            search form
	 * @param halfWidth
	 *            Set to true to make the field appear next to the one before
	 *            (only needed on the second field). The displayName will not be
	 *            shown.
	 * @param hint
	 *            The hint to display inside the search field
	 * @param freeSearch
	 *            Set to true if this is the "free search" field. There may only
	 *            be one or none of those in one library
	 * @param number
	 *            Set to true if only numbers are allowed in this field
	 */
	public TextSearchField(String id, String displayName, boolean advanced, 
			boolean halfWidth, String hint, boolean freeSearch, boolean number) {
		super(id, displayName, advanced);
		this.halfWidth = halfWidth;
		this.hint = hint;
		this.freeSearch = freeSearch;
		this.number = number;
	}

	/**
	 * @return the hint
	 */
	public String getHint() {
		return hint;
	}

	/**
	 * @param hint
	 *            the hint to set
	 */
	public void setHint(String hint) {
		this.hint = hint;
	}

	/**
	 * @return the freeSearch
	 */
	public boolean isFreeSearch() {
		return freeSearch;
	}

	/**
	 * @param freeSearch
	 *            the freeSearch to set
	 */
	public void setFreeSearch(boolean freeSearch) {
		this.freeSearch = freeSearch;
	}

	/**
	 * @return the number
	 */
	public boolean isNumber() {
		return number;
	}

	/**
	 * @param number
	 *            the number to set
	 */
	public void setNumber(boolean number) {
		this.number = number;
	}

	/**
	 * @return the halfWidth
	 */
	public boolean isHalfWidth() {
		return halfWidth;
	}

	/**
	 * @param halfWidth
	 *            the halfWidth to set
	 */
	public void setHalfWidth(boolean halfWidth) {
		this.halfWidth = halfWidth;
	}

	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject json = super.toJSON();
		json.put("type", "text");
		json.put("hint", hint);
		json.put("freeSearch", freeSearch);
		json.put("number", number);
		json.put("halfWidth", halfWidth);
		return json;
	}
}
