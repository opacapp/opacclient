package de.geeksfactory.opacclient.searchfields;

public class BarcodeSearchField extends SearchField {
	protected String hint;
	protected boolean halfWidth;

	/**
	 * @param id
	 *            ID of the search field, later given to your search() function
	 * @param displayName
	 *            The name to display for the search field
	 * @param hint
	 *            The hint to display inside the search field
	 * @param halfWidth
	 *            Set to true to make the field appear next to the one before
	 *            (only needed on the second field). The displayName will not be
	 *            shown.
	 */
	public BarcodeSearchField(String id, String displayName, boolean halfWidth,
			String hint) {
		super(id, displayName);
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
}
