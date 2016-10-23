package de.geeksfactory.opacclient.objects;

import java.io.Serializable;

public abstract class AccountItem implements Serializable {
    protected long account;
    protected String title;
    protected String author;
    protected String format;
    protected SearchResult.MediaType mediaType;
    protected String id;
    protected String status;
    protected Long dbId;
    protected String cover;

    /**
     * @return The ID of the account this item is associated with
     */
    public long getAccount() {
        return account;
    }

    /**
     * Set the ID of the account this item is associated with. Does not need to be set by the API
     * implementation.
     */
    public void setAccount(long account) {
        this.account = account;
    }

    /**
     * @return The Title of this item
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The author of this item. Optional.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set author. Optional.
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return The format of this item. Optional.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Set item format. Optional.
     * Use this if the OPAC shows a string representation of the item format. If you can recognize
     * the media type, use {@link #setMediaType(SearchResult.MediaType)} instead.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return The media type of this item. Optional.
     */
    public SearchResult.MediaType getMediaType() {
        return mediaType;
    }

    /**
     * Set the media type of this item. Optional.
     */
    public void setMediaType(SearchResult.MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * @return media ID to open detail page, if possible.
     */
    public String getId() {
        return id;
    }

    /**
     * Set media ID to open detail page, if possible.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return status of an item. Some libraries use codes like "E" for "first lending period", "1"
     * for "lending period extended once", etc. Optional.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set status of an item. Some libraries use codes like "E" for "first lending period", "1" for
     * "lending period extended once", etc. Optional.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return ID for this item in the local account database. Is only available when it was
     * retreived from the database.
     */
    public Long getDbId() {
        return dbId;
    }

    /**
     * Set ID for this item in the local account database. Must not be set by the API
     * implementation.
     */
    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    /**
     * @return A cover URL for this item. Optional.
     */
    public String getCover() {
        return cover;
    }

    /**
     * Set a cover URL for this item. Optional.
     */
    public void setCover(String cover) {
        this.cover = cover;
    }

    /**
     * Set property using the following keys: LentItem: barcode, returndate, homebranch,
     * lendingbranch, prolongurl, renewable, download
     *
     * ReservedItem: availability, expirationdate, branch, cancelurl, bookingurl
     *
     * Both: title, author, format, id, status
     *
     * @param key   one of the keys mentioned above
     * @param value the value to set. Dates must be in ISO-8601 format (yyyy-MM-dd) and booleans as
     *              "Y"/"N".
     */
    public void set(String key, String value) {
        if ("".equals(value)) {
            value = null;
        }
        switch (key) {
            case "title":
                setTitle(value);
                break;
            case "author":
                setAuthor(value);
                break;
            case "format":
                setFormat(value);
                break;
            case "id":
                setId(value);
                break;
            case "status":
                setStatus(value);
                break;
            case "cover":
                setCover(value);
                break;
            case "mediatype":
                setMediaType(SearchResult.MediaType.valueOf(value));
                break;
            default:
                throw new IllegalArgumentException("unknown key: " + key);
        }
    }

    @Override
    public String toString() {
        return "AccountItem{" +
                "account=" + account +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", format='" + format + '\'' +
                ", mediaType=" + mediaType +
                ", id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", dbId=" + dbId +
                ", cover='" + cover + '\'' +
                '}';
    }
}
