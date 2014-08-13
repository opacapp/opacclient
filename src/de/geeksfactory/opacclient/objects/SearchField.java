package de.geeksfactory.opacclient.objects;

import java.util.Map;

public class SearchField {
	private String id;
	private String displayName;
	private Type type;
	private boolean halfWidth;

	private Map<String, String> dropdownValues;

	private String hint;

	public enum Type {
		TEXT, DROPDOWN, BARCODE
	}

	/**
	 * @return A new text SearchField
	 * @param id
	 *            ID of the search field, later given to your search() function
	 * @param displayName
	 *            The name to display for the search field
	 * @param hint
	 *            The hint to display inside the search field
	 * @param halfWidth
	 *            Set to true to make the field appear next to the one before
	 *            (only needed on the second field)
	 */
	public static SearchField getTextInstance(String id, String displayName,
			String hint, boolean halfWidth) {
		SearchField field = new SearchField();
		field.setType(Type.TEXT);
		field.setId(id);
		field.setDisplayName(displayName);
		field.setHint(hint);
		field.setHalfWidth(halfWidth);
		return field;
	}
	
	/**
	 * @return A new barcode SearchField
	 * @param id
	 *            ID of the search field, later given to your search() function
	 * @param displayName
	 *            The name to display for the search field
	 * @param hint
	 *            The hint to display inside the search field
	 * @param halfWidth
	 *            Set to true to make the field appear next to the one before
	 *            (only needed on the second field)
	 */
	public static SearchField getBarcodeInstance(String id, String displayName,
			String hint, boolean halfWidth) {
		SearchField field = new SearchField();
		field.setType(Type.BARCODE);
		field.setId(id);
		field.setDisplayName(displayName);
		field.setHint(hint);
		field.setHalfWidth(halfWidth);
		return field;
	}

	/**
	 * @return A new dropdown SearchField
	 * @param id
	 *            ID of the search field, later given to your search() function
	 * @param displayName
	 *            The name to display for the search field
	 * @param dropdownValues
	 *            The values to show in the dropdown and their keys. If you
	 *            include one with an empty key, this is going to be the default
	 *            value and will not be given to the search() function
	 */
	public static SearchField getDropdownInstance(String id,
			String displayName, Map<String, String> dropdownValues) {
		SearchField field = new SearchField();
		field.setType(Type.DROPDOWN);
		field.setId(id);
		field.setDisplayName(displayName);
		field.setDropdownValues(dropdownValues);
		return field;
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
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(Type type) {
		this.type = type;
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

	/**
	 * @return the dropdownValues
	 */
	public Map<String, String> getDropdownValues() {
		return dropdownValues;
	}

	/**
	 * @param dropdownValues
	 *            the dropdownValues to set
	 */
	public void setDropdownValues(Map<String, String> dropdownValues) {
		this.dropdownValues = dropdownValues;
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

}
