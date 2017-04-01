/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Object representing all details of a media item
 *
 * @author Raphael Michel
 */
public class DetailedItem implements CoverHolder {
    private List<Detail> details = new ArrayList<>();
    private List<Copy> copies = new ArrayList<>();
    private List<Volume> volumes = new ArrayList<>();
    private String cover;
    private String title;
    private SearchResult.MediaType mediaType;
    private byte[] coverBitmap;
    private boolean reservable;
    private String reservation_info;
    private boolean bookable;
    private String booking_info;
    private String id;
    private Map<String, String> volumesearch;
    private String collectionid;

    /**
     * Get unique media identifier
     *
     * @return media ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set unique media identifier
     *
     * @param id media ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get media title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set media title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get cover image bitmap
     */
    @Override
    public byte[] getCoverBitmap() {
        return coverBitmap;
    }

    /**
     * Set cover image bitmap
     */
    @Override
    public void setCoverBitmap(byte[] coverBitmap) {
        this.coverBitmap = coverBitmap;
    }

    /**
     * Get cover image URL
     */
    @Override
    public String getCover() {
        return cover;
    }

    /**
     * Set cover image URL
     */
    @Override
    public void setCover(String cover) {
        this.cover = cover;
    }

    /**
     * Returns all data stored in this object, serialized as a human-readable
     * string.
     */
    @Override
    public String toString() {
        return "DetailedItem [details=" + details + ", copies=" + copies
                + ", volumes=" + volumes + ", cover=" + cover + ", title="
                + title + ", coverBitmap=" + coverBitmap + ", reservable="
                + reservable + ", reservation_info=" + reservation_info
                + ", id=" + id + ", volumesearch=" + volumesearch + ", mediatype=" + mediaType +
                "]";
    }

    /**
     * Set details (like author, summary, …).
     *
     * @return List of Details
     * @see Detail
     */
    public List<Detail> getDetails() {
        return details;
    }

    /**
     * List of copies of this item available
     *
     * @return List of copies
     * @see #addCopy(Copy)
     */
    public List<Copy> getCopies() {
        return copies;
    }

    /**
     * Set list of copies of this item available
     *
     * @param copies List of copies
     * @see #addCopy(Copy)
     */
    public void setCopies(List<Copy> copies) {
        this.copies = copies;
    }

    /**
     * List of child items (e.g. volumes of a series) available
     *
     * @return List of child items available
     * @see #addVolume(Volume)
     */
    public List<Volume> getVolumes() {
        return volumes;
    }

    /**
     * Add a detail
     *
     * @see Detail
     */
    public void addDetail(Detail detail) {
        details.add(detail);
    }

    /**
     * Add a copy.
     *
     * @param copy An object representing a copy
     * @see Detail
     */
    public void addCopy(Copy copy) {
        copies.add(copy);
    }

    /**
     * Add a child item.
     */
    public void addVolume(Volume child) {
        volumes.add(child);
    }

    /**
     * Can return a
     * {@link de.geeksfactory.opacclient.apis.OpacApi#search(List)} query
     * <code>List</code> for a volume search based on this item.
     *
     * @return Search query or <code>null</code> if not applicable
     * @see Detail
     */
    public Map<String, String> getVolumesearch() {
        return volumesearch;
    }

    /**
     * Sets a search query which is passed back to your
     * {@link de.geeksfactory.opacclient.apis.OpacApi#search(List)}
     * implementation for a volume search based on this item-
     *
     * @param volumesearch Search query
     */
    public void setVolumesearch(Map<String, String> volumesearch) {
        this.volumesearch = volumesearch;
    }

    /**
     * Returns whether it is possible to order this item through the app
     *
     * @return <code>true</code> if possible, <code>false</code> otherwise.
     */
    public boolean isReservable() {
        return reservable;
    }

    /**
     * Specifies whether it is possible to order this item through the app
     */
    public void setReservable(boolean reservable) {
        this.reservable = reservable;
    }

    /**
     * Get extra information stored to be returned to your
     * {@link de.geeksfactory.opacclient.apis.OpacApi#reservation(DetailedItem, Account, int, String)}
     * implementation.
     *
     * @return Some custom information.
     */
    public String getReservation_info() {
        return reservation_info;
    }

    /**
     * Set extra information stored to be returned to your
     * {@link de.geeksfactory.opacclient.apis.OpacApi#reservation(DetailedItem, Account, int, String)}
     * implementation.
     *
     * @param reservation_info Some custom information.
     */
    public void setReservation_info(String reservation_info) {
        this.reservation_info = reservation_info;
    }

    /**
     * @return the bookable
     */
    public boolean isBookable() {
        return bookable;
    }

    /**
     * @param bookable Some custom information.
     */
    public void setBookable(boolean bookable) {
        this.bookable = bookable;
    }

    /**
     * @return the booking_info
     */
    public String getBooking_info() {
        return booking_info;
    }

    /**
     * @param booking_info Some custom information.
     */
    public void setBooking_info(String booking_info) {
        this.booking_info = booking_info;
    }

    /**
     * Get the ID of the item which is a collection containing this item as a
     * child item
     *
     * @since 2.0.17
     */
    public String getCollectionId() {
        return collectionid;
    }

    /**
     * Sets the ID of the item which is a collection containing this item as a
     * child item
     *
     * @param collectionid the collectionid to set
     * @since 2.0.17
     */
    public void setCollectionId(String collectionid) {
        this.collectionid = collectionid;
    }

    /**
     * @return this item's media type
     */
    public SearchResult.MediaType getMediaType() {
        return mediaType;
    }

    /**
     * @param mediaType the media type to set
     */
    public void setMediaType(SearchResult.MediaType mediaType) {
        this.mediaType = mediaType;
    }
}
