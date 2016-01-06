/**
 * Copyright (C) 2016 by Johan von Forstner under the MIT license:
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.objects;

import org.joda.time.LocalDate;

/**
 * Represents a copy of a medium ({@link DetailledItem}) available in a library.
 */
public class Copy {
    private String barcode;
    private String location;
    private String department;
    private String branch;
    private String status;
    private LocalDate returnDate;
    private String reservations;
    private String shelfmark;
    private String resInfo;
    private String url;

    /**
     * @return The barcode of a copy. Optional.
     */
    public String getBarcode() {
        return barcode;
    }

    /**
     * @param barcode The barcode of a copy. Optional.
     */
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    /**
     * @return The location (like "third floor") of a copy. Optional.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location The location (like "third floor") of a copy. Optional.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return The department (like "music library") of a copy. Optional.
     */
    public String getDepartment() {
        return department;
    }

    /**
     * @param department The department (like "music library") of a copy. Optional.
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * @return The branch a copy is in. Should be set, if your library has more than one branch.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @param branch The branch a copy is in. Should be set, if your library has more than one
     *               branch.
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * @return Current status of a copy ("lent", "free", ...). Should be set.
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status Current status of a copy ("lent", "free", ...). Should be set.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return Expected date of return if a copy is lent out. Optional.
     */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    /**
     * @param returndate Expected date of return if a copy is lent out. Optional.
     */
    public void setReturnDate(LocalDate returndate) {
        this.returnDate = returndate;
    }

    /**
     * @return Number of pending reservations if a copy is currently lent out. Optional.
     */
    public String getReservations() {
        return reservations;
    }

    /**
     * @param reservations Number of pending reservations if a copy is currently lent out.
     *                     Optional.
     */
    public void setReservations(String reservations) {
        this.reservations = reservations;
    }

    /**
     * @return Identification in the libraries' shelf system. Optional.
     */
    public String getShelfmark() {
        return shelfmark;
    }

    /**
     * @param shelfmark Identification in the libraries' shelf system. Optional.
     */
    public void setShelfmark(String shelfmark) {
        this.shelfmark = shelfmark;
    }

    /**
     * @return Reservation information for copy-based reservations. Intended for use in your {@link
     * de.geeksfactory.opacclient.apis.OpacApi#reservation(DetailledItem, Account, int, String)}
     * implementation.
     */
    public String getResInfo() {
        return resInfo;
    }

    /**
     * @param resInfo Reservation information for copy-based reservations. Intended for use in your
     *                {@link de.geeksfactory.opacclient.apis.OpacApi#reservation (DetailledItem,
     *                Account, int, String)} implementation.
     */
    public void setResInfo(String resInfo) {
        this.resInfo = resInfo;
    }

    /**
     * @return URL to an online copy
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url URL to an online copy
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Set property using the following keys: barcode, location, department, branch
     *
     * @param key   one of the keys mentioned above
     * @param value the value to set. Dates must be in ISO-8601 format (yyyy-MM-dd).
     */
    public void set(String key, String value) {
        switch (key) {
            case "barcode":
                setBarcode(value);
                break;
            case "location":
                setLocation(value);
                break;
            case "department":
                setDepartment(value);
                break;
            case "branch":
                setBranch(value);
                break;
            case "status":
                setStatus(value);
                break;
            case "returndate":
                setReturnDate(new LocalDate(value));
                break;
            case "reservations":
                setBranch(value);
                break;
            case "signature":
                setBranch(value);
                break;
            case "resinfo":
                setResInfo(value);
                break;
            case "url":
                setUrl(value);
        }
    }
}
