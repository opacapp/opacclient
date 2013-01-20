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
	 * Get lent items
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
	 * Each <code>ContentValues</code> can contain:
	 * <ul>
	 * <li>"barcode" – Object's identification</li>
	 * <li>"titel" – Title</li>
	 * <li>"verfasser" – Author</li>
	 * <li>"frist" – Return date</li>
	 * <li>"zst" – Branch</li>
	 * <li>"ast" – Branch of lending</li>
	 * <li>"link" – Identifier (see
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#prolong(String)}</li>
	 * </ul>
	 * 
	 * Sorry for the German keys, this will get fixed someday (old will continue
	 * to work or I'll port every code all by myself ;-))
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
	 * @return List of reservations
	 * @see #setReservations(List)
	 */
	public List<ContentValues> getReservations() {
		return reservations;
	}

	/**
	 * Set ordered/reserved items
	 * 
	 * Each <code>ContentValues</code> can contain:
	 * <ul>
	 * <li>"titel" – Title</li>
	 * <li>"verfasser" – Author</li>
	 * <li>"bereit" – Expected date</li>
	 * <li>"zst" – Branch</li>
	 * <li>"cancel" – Identifier (see
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#cancel(String)}</li>
	 * </ul>
	 * 
	 * Sorry for the German keys, this will get fixed someday (old will continue
	 * to work or I'll port every code all by myself ;-))
	 * 
	 * @param reservations
	 *            List of reservations, see above
	 */
	public void setReservations(List<ContentValues> reservations) {
		this.reservations = reservations;
	}
}
