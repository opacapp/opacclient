package de.geeksfactory.opacclient.searchfields;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A DropdownSearchField allows the user to select from a list of values, e.g.
 * library branches or item formats.
 */
public class DropdownSearchField extends SearchField {

    /**
     * Represents a dropdown option.
     */
    public static class Option implements Map.Entry<String, String> {
        private final String key;
        private final String value;

        public Option(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String value) {
            throw new UnsupportedOperationException();
        }
    }

    protected List<Option> dropdownValues;

    public DropdownSearchField() {

    }

    /**
     * A new dropdown SearchField
     *
     * @param id             ID of the search field, later given to your search() function
     * @param displayName    The name to display for the search field
     * @param advanced       Set if this field should only be shown when showing the
     *                       advanced search form
     * @param dropdownValues The values to show in the dropdown and their keys. If you
     *                       include one with an empty key, this is going to be the default
     *                       value and will not be given to the search() function
     */
    public DropdownSearchField(String id, String displayName, boolean advanced,
                               List<Option> dropdownValues) {
        super(id, displayName, advanced);
        this.dropdownValues = dropdownValues;
    }

    /**
     * Get the list of selectable values.
     */
    public List<Option> getDropdownValues() {
        return dropdownValues;
    }

    /**
     * Set a list of values for the dropdown list.
     */
    public void setDropdownValues(List<Option> dropdownValues) {
        this.dropdownValues = dropdownValues;
    }

    public void addDropdownValue(String key, String value) {
        if (dropdownValues == null) {
            dropdownValues = new ArrayList<>();
        }
        dropdownValues.add(new Option(key, value));
    }

    public void addDropdownValue(int index, String key, String value) {
        if (dropdownValues == null) {
            dropdownValues = new ArrayList<>();
        }
        dropdownValues.add(index, new Option(key, value));
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("type", "dropdown");
        JSONArray values = new JSONArray();
        for (Option map : dropdownValues) {
            JSONObject value = new JSONObject();
            value.put("key", map.getKey());
            value.put("value", map.getValue());
            values.put(value);
        }
        json.put("dropdownValues", values);
        return json;
    }

}
