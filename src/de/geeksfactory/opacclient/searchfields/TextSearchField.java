package de.geeksfactory.opacclient.searchfields;

public class TextSearchField extends SearchField {
	protected String hint;
	protected boolean freeSearch;
	protected boolean number;
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
	 * @param freeSearch
	 *            Set to true if this is the "free search" field. There may only
	 *            be one or none of those in one library
	 * @param number
	 *            Set to true if only numbers are allowed in this field
	 */
	public TextSearchField(String id, String displayName, boolean halfWidth,
			String hint, boolean freeSearch, boolean number) {
		super(id, displayName);
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
}
