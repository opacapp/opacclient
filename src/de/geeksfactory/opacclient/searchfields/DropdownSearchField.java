package de.geeksfactory.opacclient.searchfields;

import java.util.List;
import java.util.Map;

public class DropdownSearchField extends SearchField {

	protected List<Map<String, String>> dropdownValues;

	/**
	 * @return A new dropdown SearchField
	 * @param id
	 *            ID of the search field, later given to your search() function
	 * @param displayName
	 *            The name to display for the search field
	 * @param advanced
	 * 			  Set if this field should only be shown when showing the advanced
	 *            search form
	 * @param dropdownValues
	 *            The values to show in the dropdown and their keys. If you
	 *            include one with an empty key, this is going to be the default
	 *            value and will not be given to the search() function
	 */
	public DropdownSearchField(String id, String displayName,
			boolean advanced, List<Map<String, String>> dropdownValues) {
		super(id, displayName, advanced);
		this.dropdownValues = dropdownValues;
	}

	/**
	 * @return the dropdownValues
	 */
	public List<Map<String, String>> getDropdownValues() {
		return dropdownValues;
	}

	/**
	 * @param dropdownValues
	 *            the dropdownValues to set
	 */
	public void setDropdownValues(List<Map<String, String>> dropdownValues) {
		this.dropdownValues = dropdownValues;
	}

}
