package de.geeksfactory.opacclient.objects;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Bundle;
import de.geeksfactory.opacclient.apis.OpacApi;

/**
 * Object representing all details of a media item
 * 
 * @author Raphael Michel
 */
public class DetailledItem {
	private List<Detail> details = new ArrayList<Detail>();
	private List<ContentValues> copies = new ArrayList<ContentValues>();
	private List<ContentValues> baende = new ArrayList<ContentValues>();
	private String cover;
	private String title;
	private Bitmap coverBitmap;
	private boolean reservable;
	private String reservation_info;
	private boolean bookable;
	private String booking_info;
	private String id;
	private Bundle volumesearch;
	private String collectionid;

	/**
	 * The barcode of a copy. Optional.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}:
	 */
	public static final String KEY_COPY_BARCODE = "barcode";

	/**
	 * The location (like "third floor") of a copy. Optional.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}:
	 */
	public static final String KEY_COPY_LOCATION = "location";

	/**
	 * The department (like "music library") of a copy. Optional.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}:
	 */
	public static final String KEY_COPY_DEPARTMENT = "department";

	/**
	 * The branch a copy is in. Should be set, if your library has more than one
	 * branch.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}:
	 */
	public static final String KEY_COPY_BRANCH = "branch";

	/**
	 * Current status of a copy ("lent", "free", ...). Should be set.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}:
	 */
	public static final String KEY_COPY_STATUS = "status";

	/**
	 * Expected date of return if a copy is lent out. Optional.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}:
	 */
	public static final String KEY_COPY_RETURN = "returndate";

	/**
	 * Number of pending reservations if a copy is currently lent out. Optional.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}:
	 */
	public static final String KEY_COPY_RESERVATIONS = "reservations";

	/**
	 * Identification in the libraries' shelf system. Optional.
	 * 
	 * ContentValues key for {@link #addCopy(ContentValues)}.
	 * 
	 * @since 2.0.10
	 */
	public static final String KEY_COPY_SHELFMARK = "signature";

	/**
	 * Unique media identifier of a child item for
	 * {@link OpacApi#getResultById(String, String)}. Required.
	 * 
	 * ContentValues key for {@link #addBand(ContentValues)}:
	 */
	public static final String KEY_CHILD_ID = "id";

	/**
	 * Title of a child item. Required.
	 * 
	 * ContentValues key for {@link #addBand(ContentValues)}:
	 */
	public static final String KEY_CHILD_TITLE = "titel";

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
	 * @param id
	 *            media ID
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
	public Bitmap getCoverBitmap() {
		return coverBitmap;
	}

	/**
	 * Set cover image bitmap
	 */
	public void setCoverBitmap(Bitmap coverBitmap) {
		this.coverBitmap = coverBitmap;
	}

	/**
	 * Get cover image URL
	 */
	public String getCover() {
		return cover;
	}

	/**
	 * Set cover image URL
	 */
	public void setCover(String cover) {
		this.cover = cover;
	}

	/**
	 * Returns all data stored in this object, serialized as a human-readable
	 * string.
	 */
	@Override
	public String toString() {
		return "DetailledItem [details=" + details + ", copies=" + copies
				+ ", baende=" + baende + ", cover=" + cover + ", title="
				+ title + ", coverBitmap=" + coverBitmap + ", reservable="
				+ reservable + ", reservation_info=" + reservation_info
				+ ", id=" + id + ", volumesearch=" + volumesearch + "]";
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
	 * @see #addCopy(ContentValues)
	 */
	public List<ContentValues> getCopies() {
		return copies;
	}

	/**
	 * Set list of copies of this item available
	 * 
	 * @param copies
	 *            List of copies
	 * @see #addCopy(ContentValues)
	 */
	public void setCopies(List<ContentValues> copies) {
		this.copies = copies;
	}

	/**
	 * List of child items available
	 * 
	 * @return List of child items available
	 * @see #addBand(ContentValues)
	 */
	public List<ContentValues> getBaende() {
		return baende;
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
	 * Add a copy. <code>copy</code> may contain any of the
	 * <code>KEY_COPY_*</code> constants as keys.
	 * 
	 * @param copy
	 *            An object representing a copy
	 * @see Detail
	 */
	public void addCopy(ContentValues copy) {
		copies.add(copy);
	}

	/**
	 * Add a child item. <code>child</code> must contain all of the
	 * <code>KEY_CHILD_*</code> constants as keys. This is to be used, if a
	 * search result is not a real item but more like a "meta item" for a
	 * collection, for example a "Harry Potter" item containing a collection of
	 * all seven Harry Potter books as child items.
	 * 
	 * @see Detail
	 */
	public void addBand(ContentValues child) {
		baende.add(child);
	}

	/**
	 * Can return a
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#search(Bundle)} query
	 * <code>Bundle</code> for a volume search based on this item.
	 * 
	 * @return Search query or <code>null</code> if not applicable
	 * @see Detail
	 */
	public Bundle getVolumesearch() {
		return volumesearch;
	}

	/**
	 * Sets a search query which is passed back to your
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#search(Bundle)}
	 * implementation for a volume search based on this item-
	 * 
	 * @param volumesearch
	 *            Search query
	 */
	public void setVolumesearch(Bundle volumesearch) {
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
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#reservation(String, Account, int, String)}
	 * implementation.
	 * 
	 * @return Some custom information.
	 */
	public String getReservation_info() {
		return reservation_info;
	}

	/**
	 * Set extra information stored to be returned to your
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#reservation(String, Account, int, String)}
	 * implementation.
	 * 
	 * @param reservation_info
	 *            Some custom information.
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
	 * @param bookable
	 *            the bookable to set
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
	 * @param booking_info
	 *            the booking_info to set
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
	 * @param collectionid
	 *            the collectionid to set
	 * @since 2.0.17
	 */
	public void setCollectionId(String collectionid) {
		this.collectionid = collectionid;
	}
}
