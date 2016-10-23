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

import java.util.List;

/**
 * Object representing details of a library account
 *
 * @author Raphael Michel
 */
public class AccountData {
    private List<LentItem> lent;
    private List<ReservedItem> reservations;
    private long account;
    private String pendingFees;
    private String validUntil;
    private String warning;

    /**
     * Create a new AccountData object
     *
     * @param account Account ID associated with this dataset.
     * @since 2.0.0
     */
    public AccountData(long account) {
        super();
        this.account = account;
    }

    /**
     * Get a list of items borrowed by this user.
     *
     * @return List of lent items
     * @see #setLent(List)
     */
    public List<LentItem> getLent() {
        return lent;
    }

    /**
     * Set the list of items borrowed by this user.
     *
     * @param lent List of items, see above
     */
    public void setLent(List<LentItem> lent) {
        this.lent = lent;
    }

    /**
     * Get the list of ordered/reserved items.
     *
     * @return List of reservations
     * @see #setReservations(List)
     */
    public List<ReservedItem> getReservations() {
        return reservations;
    }

    /**
     * Set the list of ordered/reserved items.
     *
     * @param reservations List of reservations, see above
     */
    public void setReservations(List<ReservedItem> reservations) {
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
     * @param account An ID
     * @since 2.0.0
     */
    public void setAccount(long account) {
        this.account = account;
    }

    /**
     * @return pending fees of the user's library account
     * @since 2.0.18
     */
    public String getPendingFees() {
        return pendingFees;
    }

    /**
     * Set the user's library account's pending fees.
     *
     * @param pendingFees human-readable String
     * @since 2.0.18
     */
    public void setPendingFees(String pendingFees) {
        this.pendingFees = pendingFees;
    }

    /**
     * Expiration date of a library account as a human-readable text.
     *
     * @return A date
     * @since 2.0.24
     */
    public String getValidUntil() {
        return validUntil;
    }

    /**
     * @param validUntil the Expiration date of a library account
     */
    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }

    /**
     * Get the warning. Warnings are to be displayed above the data in a yellow
     * bar.
     *
     * @return warning as a human-readable string or null, if there is no
     * warning.
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Set a warning which is shown to the user above the account data in a
     * yellow bar.
     *
     * @param warning The warning message
     */
    public void setWarning(String warning) {
        this.warning = warning;
    }

    @Override
    public String toString() {
        return "AccountData{" +
                "lent=" + lent +
                ", reservations=" + reservations +
                ", account=" + account +
                ", pendingFees='" + pendingFees + '\'' +
                ", validUntil='" + validUntil + '\'' +
                ", warning='" + warning + '\'' +
                '}';
    }
}
