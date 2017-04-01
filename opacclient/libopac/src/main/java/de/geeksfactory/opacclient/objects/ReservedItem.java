package de.geeksfactory.opacclient.objects;

import org.joda.time.LocalDate;

import java.io.Serializable;

public class ReservedItem extends AccountItem implements Serializable {
    private LocalDate readyDate;
    private LocalDate expirationDate;
    private String branch;
    private String cancelData;
    private String bookingData;

    /**
     * @return Expected date for an ordered item to arrive. Optional.
     */
    public LocalDate getReadyDate() {
        return readyDate;
    }

    /**
     * Set expected date for an ordered item to arrive in ISO-8601 format (yyyy-MM-dd). Optional.
     */
    public void setReadyDate(String readyDate) {
        if (readyDate != null) {
            this.readyDate = new LocalDate(readyDate);
        } else {
            this.readyDate = null;
        }
    }

    /**
     * Set expected date for an ordered item to arrive. Optional.
     */
    public void setReadyDate(LocalDate readyDate) {
        this.readyDate = readyDate;
    }

    /**
     * @return Date of expiration. Optional.
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Set date of expiration in ISO-8601 format (yyyy-MM-dd). Optional.
     */
    public void setExpirationDate(String expirationDate) {
        if (expirationDate != null) {
            this.expirationDate = new LocalDate(expirationDate);
        } else {
            this.expirationDate = null;
        }
    }

    /**
     * Set date of expiration. Optional.
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * @return Library branch an item is ordered to. Optional, but should be set if your library has
     * multiple branches.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Set library branch an item is ordered to. Optional, but should be set if your library has
     * multiple branches.
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * @return Internal identifier which will be supplied to your {@link
     * de.geeksfactory.opacclient.apis.OpacApi#cancel(String, Account, int, String)} implementation
     * when the user wants to cancel the order. Cancel button won't be displayed if this is not
     * set.
     */
    public String getCancelData() {
        return cancelData;
    }

    /**
     * Set internal identifier which will be supplied to your {@link de.geeksfactory.opacclient
     * .apis.OpacApi#cancel(String,
     * Account, int, String)} implementation when the user wants to cancel the order. Cancel button
     * won't be displayed if this is not set.
     */
    public void setCancelData(String cancelData) {
        this.cancelData = cancelData;
    }

    /**
     * @return Internal identifier which will be supplied to your {@link
     * de.geeksfactory.opacclient.apis.EbookServiceApi#booking(DetailedItem, Account, int, String)}
     * implementation when the user wants to book the order. Booking button won't be displayed if
     * this is not set.
     */
    public String getBookingData() {
        return bookingData;
    }

    /**
     * Set internal identifier which will be supplied to your {@link de.geeksfactory.opacclient
     * .apis.EbookServiceApi#booking(DetailedItem,
     * Account, int, String)} implementation when the user wants to book the order. Booking button
     * won't be displayed if this is not set.
     */
    public void setBookingData(String bookingData) {
        this.bookingData = bookingData;
    }

    @Override
    public void set(String key, String value) {
        if ("".equals(value)) {
            value = null;
        }
        switch (key) {
            case "availability":
                setReadyDate(new LocalDate(value));
                break;
            case "expirationdate":
                setExpirationDate(new LocalDate(value));
                break;
            case "branch":
                setBranch(value);
                break;
            case "cancelurl":
                setCancelData(value);
                break;
            case "bookingurl":
                setBookingData(value);
                break;
            default:
                super.set(key, value);
                break;
        }
    }

    @Override
    public String toString() {
        return "ReservedItem{" +
                "account=" + account +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", format='" + format + '\'' +
                ", mediaType=" + mediaType +
                ", id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", dbId=" + dbId +
                ", cover='" + cover + '\'' +
                ", readyDate=" + readyDate +
                ", expirationDate=" + expirationDate +
                ", branch='" + branch + '\'' +
                ", cancelData='" + cancelData + '\'' +
                ", bookingData='" + bookingData + '\'' +
                '}';
    }
}
