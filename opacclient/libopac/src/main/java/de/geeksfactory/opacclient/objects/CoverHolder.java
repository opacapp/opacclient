package de.geeksfactory.opacclient.objects;

/**
 * Any object which can hold a cover should implement this interface. This might
 * be a {@link SearchResult} or {@link DetailedItem}.
 */
public interface CoverHolder {
    /**
     * Get the cover bitmap
     */
    public byte[] getCoverBitmap();

    /**
     * Set the cover as a bitmap
     */
    public void setCoverBitmap(byte[] coverBitmap);

    /**
     * Get the cover URL
     */
    public String getCover();

    /**
     * Set the cover by URL
     */
    public void setCover(String cover);
}
