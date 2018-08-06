package de.geeksfactory.opacclient.objects;

/**
 * Object representing a tag in a starred item.
 */

public class Tag {

    private int id;
    private String tagName;
    private int starredItemRefId;

    @Override
    public String toString() {
        return "Tag [id=" + id + ", tag=" + tagName + "]";
    }

    /**
     * Get this tag's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set this tag's ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get this tag's name
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Set this tag's name
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * Get this tag's reference ID to the starred item it is attached to
     */
    public int getStarredItemRefId() {
        return starredItemRefId;
    }

    /**
     * Set this tag's reference ID to the starred item it is attached to
     */
    public void setStarredItemRefId(int starredItemRefId) {
        this.starredItemRefId = starredItemRefId;
    }
}
