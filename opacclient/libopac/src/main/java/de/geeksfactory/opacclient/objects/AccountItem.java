package de.geeksfactory.opacclient.objects;

public abstract class AccountItem {
    private long account;
    private String title;
    private String author;
    private String format;
    private String id;
    private String status;
    private Long dbId;

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
     */
    public void setFormat(String format) {
        this.format = format;
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
            default:
                throw new IllegalArgumentException("unknown key: " + key);
        }
    }
}
