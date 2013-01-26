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
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Item's title
	 */
	public static final String KEY_LENT_TITLE = "titel";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Item's title
	 */
	public static final String KEY_LENT_BARCODE = "barcode";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Item's title
	 */
	public static final String KEY_LENT_AUTHOR = "verfasser";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Return date
	 */
	public static final String KEY_LENT_DEADLINE = "frist";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Return date (as a timestamp in milliseconds)
	 */
	public static final String KEY_LENT_DEADLINE_TIMESTAMP = "deadline_ts";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Status
	 */
	public static final String KEY_LENT_STATUS = "status";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Branch the item belongs to
	 */
	public static final String KEY_LENT_BRANCH = "zst";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Branch were the item was lent
	 */
	public static final String KEY_LENT_LENDING_BRANCH = "ast";

	/**
	 * ContentValues key for {@link #setLent(List)}:
	 * 
	 * Identifier (see
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#prolong(Account, String)}
	 */
	public static final String KEY_LENT_LINK = "link";

	/**
	 * ContentValues key for {@link #setReservations(List)}:
	 * 
	 * Item's title
	 */
	public static final String KEY_RESERVATION_TITLE = "titel";

	/**
	 * ContentValues key for {@link #setReservations(List)}:
	 * 
	 * 
	 * Item's author
	 */
	public static final String KEY_RESERVATION_AUTHOR = "verfasser";

	/**
	 * ContentValues key for {@link #setReservations(List)}:
	 * 
	 * Expected date
	 */
	public static final String KEY_RESERVATION_READY = "bereit";

	/**
	 * ContentValues key for {@link #setReservations(List)}:
	 * 
	 * branch
	 */
	public static final String KEY_RESERVATION_BRANCH = "zst";

	/**
	 * ContentValues key for {@link #setReservations(List)}:
	 * 
	 * Identifier (see
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#cancel(Account, String)}
	 */
	public static final String KEY_RESERVATION_CANCEL = "cancel";

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
