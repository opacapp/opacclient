package de.geeksfactory.opacclient.searchfields;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A CheckboxSearchField is a boolean search field.
 */
public class CheckboxSearchField extends SearchField {

    public CheckboxSearchField() {

    }

    /**
     * A new dropdown SearchField
     *
     * @param id          ID of the search field, later given to your search() function
     * @param displayName The name to display for the search field
     * @param advanced    Set if this field should only be shown when showing the
     *                    advanced search form
     */
    public CheckboxSearchField(String id, String displayName, boolean advanced) {
        super(id, displayName, advanced);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("type", "checkbox");
        return json;
    }

}
