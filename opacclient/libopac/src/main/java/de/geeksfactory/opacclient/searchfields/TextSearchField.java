package de.geeksfactory.opacclient.searchfields;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A TextSearchField is a SearchField allowing free text input.
 */
public class TextSearchField extends SearchField {
    protected String hint;
    protected boolean freeSearch;
    protected boolean number;
    protected boolean halfWidth;

    public TextSearchField() {

    }

    /**
     * @param id          ID of the search field, later given to your search() function
     * @param displayName The name to display for the search field
     * @param advanced    Set if this field should only be shown when showing the
     *                    advanced search form
     * @param halfWidth   Set to true to make the field appear next to the one before
     *                    (only needed on the second field). The displayName will not be
     *                    shown.
     * @param hint        The hint to display inside the search field
     * @param freeSearch  Set to true if this is the "free search" field. There may only
     *                    be one or none of those in one library
     * @param number      Set to true if only numbers are allowed in this field
     */
    public TextSearchField(String id, String displayName, boolean advanced,
                           boolean halfWidth, String hint, boolean freeSearch, boolean number) {
        super(id, displayName, advanced);
        this.halfWidth = halfWidth;
        this.hint = hint;
        this.freeSearch = freeSearch;
        this.number = number;
    }

    /**
     * The hint to display inside the search field
     */
    public String getHint() {
        return hint;
    }

    /**
     * The hint to display inside the search field
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * Returns true if this is the "free search" field. There may only be one or
     * none of those in one library
     */
    public boolean isFreeSearch() {
        return freeSearch;
    }

    /**
     * Set to true if this is the "free search" field. There may only be one or
     * none of those in one library
     */
    public void setFreeSearch(boolean freeSearch) {
        this.freeSearch = freeSearch;
    }

    /**
     * Set to true if only numbers are allowed in this field
     */
    public boolean isNumber() {
        return number;
    }

    /**
     * Set to true if only numbers are allowed in this field
     */
    public void setNumber(boolean number) {
        this.number = number;
    }

    /**
     * Set to true to make the field appear next to the one before (only needed
     * on the second field). The displayName will not be shown.
     */
    public boolean isHalfWidth() {
        return halfWidth;
    }

    /**
     * Set to true to make the field appear next to the one before (only needed
     * on the second field). The displayName will not be shown.
     */
    public void setHalfWidth(boolean halfWidth) {
        this.halfWidth = halfWidth;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("type", "text");
        json.put("hint", hint);
        json.put("freeSearch", freeSearch);
        json.put("number", number);
        json.put("halfWidth", halfWidth);
        return json;
    }
}
