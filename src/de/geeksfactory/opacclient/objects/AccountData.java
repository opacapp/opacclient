package de.geeksfactory.opacclient.objects;

import java.util.List;

import android.content.ContentValues;

/**
 * Object representing details of an library account
 * 
 * @author Raphael Michel
 */
public class AccountData {
	private List<ContentValues> lent;
	private List<ContentValues> reservations;
	private long account;

	/**
	 * Title of a lent item.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_TITLE = "title";

	/**
	 * Barcode/unique identifier of a lent item. Should be set.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_BARCODE = "barcode";

	/**
	 * Author of a lent item. Optional.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_AUTHOR = "author";

	/**
	 * Return date for a lent item. Should be set.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_DEADLINE = "returndate";

	/**
	 * Return date for a lent item, converted to a unix timestamp in
	 * milliseconds (comparable to <code>System.currentTimeMillis()</code>). Not
	 * displayed, but REQUIRED for notifications!!
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_DEADLINE_TIMESTAMP = "deadline_ts";

	/**
	 * Status of a lent item. Some libraries use codes like "E" for
	 * "first lending period", "1" for "lending period extended once", etc.
	 * Optional.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_STATUS = "status";

	/**
	 * Library branch the item belongs to. Optional.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_BRANCH = "homebranch";

	/**
	 * Library branch the item was lent from. Optional.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_LENDING_BRANCH = "lendingbranch";

	/**
	 * Internal identifier which will be supplied to your
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#prolong(Account, String)}
	 * implementation for prolonging. Button for prolonging will only be
	 * displayed if this is set.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_LINK = "prolongurl";

	/**
	 * Internal identifier which will be supplied to your
	 * {@link de.geeksfactory.opacclient.apis.EbookServiceApi#downloadItem(String)}
	 * implementation for download. Button for download will only be
	 * displayed if this is set.
	 * 
	 * ContentValues key for {@link #setLent(List)}
	 */
	public static final String KEY_LENT_DOWNLOAD = "download";

	/**
	 * Title of an ordered item. Should be set.
	 * 
	 * ContentValues key for {@link #setReservations(List)}
	 */
	public static final String KEY_RESERVATION_TITLE = "title";

	/**
	 * Author of an ordered item. Optional.
	 */
	public static final String KEY_RESERVATION_AUTHOR = "author";

	/**
	 * Expected date for an ordered item to arrive. Optional.
	 * 
	 * ContentValues key for {@link #setReservations(List)}
	 */
	public static final String KEY_RESERVATION_READY = "availability";

	/**
	 * Date of expiration. Optional.
	 * 
	 * ContentValues key for {@link #setReservations(List)}
	 * 
	 * @since 2.0.6
	 */
	public static final String KEY_RESERVATION_EXPIRE = "expirationdate";

	/**
	 * Library branch an item is ordered to. Optional, but should be set if your
	 * library has multiple branches.
	 * 
	 * ContentValues key for {@link #setReservations(List)}
	 */
	public static final String KEY_RESERVATION_BRANCH = "branch";

	/**
	 * Internal identifier which will be supplied to your
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#cancel(Account, String)}
	 * implementation when the user wants to cancel the order. Cancel button
	 * won't be displayed if this is not set.
	 * 
	 * ContentValues key for {@link #setReservations(List)}
	 */
	public static final String KEY_RESERVATION_CANCEL = "cancelurl";

	/**
	 * Internal identifier which will be supplied to your
	 * {@link de.geeksfactory.opacclient.apis.EbookServiceApi#booking(String, Account, int, String)}
	 * implementation when the user wants to cancel the order. Cancel button
	 * won't be displayed if this is not set.
	 * 
	 * ContentValues key for {@link #setReservations(List)}
	 */
	public static final String KEY_RESERVATION_BOOKING = "bookingurl";

	/**
	 * Get lent items
	 * 
	 * Each <code>ContentValues</code> can contain any of the
	 * <code>KEY_LENT_*</code> constants.
	 * 
	 * @return List of lent items
	 * @see #setLent(List)
	 */
	public List<ContentValues> getLent() {
		return lent;
	}

	/**
	 * Set lent items
	 * 
	 * Each <code>ContentValues</code> can contain any of the
	 * <code>KEY_LENT_*</code> constants.
	 * 
	 * @param lent
	 *            List of items, see above
	 */
	public void setLent(List<ContentValues> lent) {
		this.lent = lent;
	}

	/**
	 * Get ordered/reserved items
	 * 
	 * Each <code>ContentValues</code> can contain any of the
	 * <code>KEY_RESERVATION_*</code> constants.
	 * 
	 * @return List of reservations
	 * @see #setReservations(List)
	 */
	public List<ContentValues> getReservations() {
		return reservations;
	}

	/**
	 * Set ordered/reserved items
	 * 
	 * Each <code>ContentValues</code> can contain any of the
	 * <code>KEY_RESERVATION_*</code> constants.
	 * 
	 * @param reservations
	 *            List of reservations, see above
	 */
	public void setReservations(List<ContentValues> reservations) {
		this.reservations = reservations;
	}

	/**
	 * Get account ID associated with this dataset.
	 * 
	 * @return An ID
	 * @since 2.0.0
	 */
	public long getAccount() {
		return account;
	}

	/**
	 * Set account ID associated with this dataset.
	 * 
	 * @param account
	 *            An ID
	 * @since 2.0.0
	 */
	public void setAccount(long account) {
		this.account = account;
	}

	/**
	 * Create a new AccountData object
	 * 
	 * @param account
	 *            Account ID associated with this dataset.
	 * @since 2.0.0
	 */
	public AccountData(long account) {
		super();
		this.account = account;
	}

}
