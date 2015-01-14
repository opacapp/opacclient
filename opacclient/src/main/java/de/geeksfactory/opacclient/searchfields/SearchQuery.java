package de.geeksfactory.opacclient.searchfields;

public class SearchQuery {
	private SearchField field;
	private String value;
	
	public SearchQuery(SearchField field, String value) {
		this.field = field;
		this.value = value;
	}
	
	/**
	 * @return the field
	 */
	public SearchField getSearchField() {
		return field;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	public String getKey() {
		return field.getId();
	}

	@Override
	public String toString() {
		return "SearchQuery [field=" + field + ", value=" + getValue()
				+ ", key=" + getKey() + "]";
	}
	
}
