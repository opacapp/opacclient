package de.geeksfactory.opacclient.objects;

/**
 * Object representing a tag in a starred item.
 */

public class Tag {

    private int id;
    private String tagName;

    @Override
    public String toString() {
        return "Tag [id=" + id + ", tag=" + tagName + "]";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
