package de.geeksfactory.opacclient.searchfields;

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
	 * @param advanced the advanced to set
	 */
	public void setAdvanced(boolean advanced) {
		this.advanced = advanced;
	}

}
