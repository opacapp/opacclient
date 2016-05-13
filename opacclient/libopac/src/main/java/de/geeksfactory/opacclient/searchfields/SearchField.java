package de.geeksfactory.opacclient.searchfields;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

/**
 * A SearchField is the abstract representation of a criteria input available in the search form.
 */
public abstract class SearchField {
    protected String id;
    protected String displayName;
    protected boolean advanced;
    protected boolean visible = true;
    /**
     * Optional attribute, describes the meaning of the search field. Used for sorting the search
     * fields in the form. Will be assigned automatically by the MeaningDetector if you use it.
     */
    protected Meaning meaning;
    /**
     * A JSONObject where you can save arbitrary data about this Search field. If you add a
     * "meaning" attribute, MeaningDetector will search for that string in the meanings list instead
     * of the displayName.
     */
    protected JSONObject data;

    public SearchField() {

    }

    public SearchField(String id, String displayName, boolean advanced) {
        this.id = id;
        this.displayName = displayName;
        this.advanced = advanced;
    }

    /**
     * Initialize a SearchField from its JSON-serialized counterpart
     */
    public static SearchField fromJSON(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String type = json.getString("type");
        String displayName = json.getString("displayName");
        JSONObject data = null;
        if (json.has("data")) {
            data = json.getJSONObject("data");
        }
        Meaning meaning = null;
        if (json.has("meaning")) {
            meaning = Meaning.valueOf(json.getString("meaning"));
        }
        boolean advanced = json.getBoolean("advanced");
        boolean visible = json.getBoolean("visible");

        SearchField field = null;
        switch (type) {
            case "text": {
                String hint = json.getString("hint");
                boolean freeSearch = json.getBoolean("freeSearch");
                boolean number = json.getBoolean("number");
                boolean halfWidth = json.getBoolean("halfWidth");
                field = new TextSearchField(id, displayName, advanced, halfWidth,
                        hint, freeSearch, number);
                break;
            }
            case "barcode": {
                String hint = json.getString("hint");
                boolean halfWidth = json.getBoolean("halfWidth");
                field = new BarcodeSearchField(id, displayName, advanced,
                        halfWidth, hint);
                break;
            }
            case "checkbox":
                field = new CheckboxSearchField(id, displayName, advanced);
                break;
            case "dropdown":
                JSONArray array = json.getJSONArray("dropdownValues");
                field = new DropdownSearchField(id, displayName, advanced, null);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject value = array.getJSONObject(i);
                    ((DropdownSearchField) field).addDropdownValue(
                            value.getString("key"), value.getString("value"));
                }
                break;
        }
        if (field != null) {
            field.setData(data);
            field.setMeaning(meaning);
            field.setVisible(visible);
        }
        return field;
    }

    /**
     * Get this field's internal ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set this field's internal ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the name of this field to be displayed
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set the name of this field to be displayed
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get whether this field should only be displayed in "advanced mode".
     */
    public boolean isAdvanced() {
        return advanced;
    }

    /**
     * Set whether this field should only be displayed in "advanced mode".
     */
    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }

    /**
     * Get whether this field is visible on the UI
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set whether this field is visible on the UI
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Serialize a SearchField to a JSONObject.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("displayName", displayName);
        json.put("advanced", advanced);
        json.put("visible", visible);
        if (data != null) {
            json.put("data", data);
        }
        if (meaning != null) {
            json.put("meaning", meaning.toString());
        }
        return json;
    }

    /**
     * Get raw additional SearchField data
     */
    public JSONObject getData() {
        return data;
    }

    /**
     * Set raw additional SearchField data
     */
    public void setData(JSONObject data) {
        this.data = data;
    }

    /**
     * Returns the SearchField's guessed or specified meaning
     */
    public Meaning getMeaning() {
        return meaning;
    }

    /**
     * Set the SearchField's meaning
     */
    public void setMeaning(Meaning meaning) {
        this.meaning = meaning;
    }

    @Override
    public String toString() {
        return "SearchField [id=" + id + "]";
    }

    /**
     * A SearchField can have one of the following meanings. They are used for field ordering and
     * providing additional UI features.
     */
    public enum Meaning {
        FREE, TITLE, AUTHOR, DIGITAL, AVAILABLE, ISBN, BARCODE, YEAR, BRANCH, HOME_BRANCH, CATEGORY,
        PUBLISHER, KEYWORD, SYSTEM, AUDIENCE, LOCATION, ORDER, DATABASE
    }

    /**
     * Sorts search fields by the order of the {@link SearchField.Meaning} enum.
     */
    public static class OrderComparator implements Comparator<SearchField> {
        @Override
        public int compare(SearchField a, SearchField b) {
            if (a.getMeaning() == null || b.getMeaning() == null) {
                return 0;
            } else {
                return a.getMeaning().compareTo(b.getMeaning());
            }
        }
    }

}
