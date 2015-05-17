package de.geeksfactory.opacclient.searchfields;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A BarcodeSearchField is a SearchField with the additional feature of a
 * barcode scanner button next to it to fill it with scanned data (e.g. ISBN
 * codes).
 */
public class BarcodeSearchField extends SearchField {
    protected String hint;
    protected boolean halfWidth;

    /**
     * @param id          ID of the search field, later given to your search() function
     * @param displayName The name to display for the search field
     * @param advanced    Set if this field should only be shown when showing the
     *                    advanced search form
     * @param hint        The hint to display inside the search field
     * @param halfWidth   Set to true to make the field appear next to the one before
     *                    (only needed on the second field). The displayName will not be
     *                    shown.
     */
    public BarcodeSearchField(String id, String displayName, boolean advanced,
                              boolean halfWidth, String hint) {
        super(id, displayName, advanced);
        this.halfWidth = halfWidth;
        this.hint = hint;
    }

    /**
     * Gets the hint to be displayed as a placeholder inside the field.
     */
    public String getHint() {
        return hint;
    }

    /**
     * Sets the hint to be displayed as a placeholder inside the field.
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * Gets whether this should share its row with another field.
     */
    public boolean isHalfWidth() {
        return halfWidth;
    }

    /**
     * Sets whether this should share its row with another field. (only needed
     * on the second field)
     */
    public void setHalfWidth(boolean halfWidth) {
        this.halfWidth = halfWidth;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("type", "barcode");
        json.put("hint", hint);
        json.put("halfWidth", halfWidth);
        return json;
    }
}
