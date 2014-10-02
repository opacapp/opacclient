package de.geeksfactory.opacclient.searchfields;

import org.json.JSONException;
import org.json.JSONObject;

public class BarcodeSearchField extends SearchField {
	protected String hint;
	protected boolean halfWidth;

	/**
	 * @param id
	 *            ID of the search field, later given to your search() function
	 * @param displayName
	 *            The name to display for the search field
	 * @param advanced
	 * 			  Set if this field should only be shown when showing the advanced
	 *            search form
	 * @param hint
	 *            The hint to display inside the search field
	 * @param halfWidth
	 *            Set to true to make the field appear next to the one before
	 *            (only needed on the second field). The displayName will not be
	 *            shown.
	 */
	public BarcodeSearchField(String id, String displayName, boolean advanced, boolean halfWidth,
			String hint) {
		super(id, displayName, advanced);
		this.halfWidth = halfWidth;
		this.hint = hint;
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
	 * @return the halfWidth
	 */
	public boolean isHalfWidth() {
		return halfWidth;
	}

	/**
	 * @param halfWidth the halfWidth to set
	 */
	public void setHalfWidth(boolean halfWidth) {
		this.halfWidth = halfWidth;
	}
	
	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject json = super.toJSON();
		json.put("type", "barcode");
		json.put("hint", hint);
		json.put("halfWidth", halfWidth);
		return json;
	}
}
