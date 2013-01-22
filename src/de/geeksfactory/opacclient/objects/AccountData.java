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
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#prolong(String)}
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
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#cancel(String)}
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
}
