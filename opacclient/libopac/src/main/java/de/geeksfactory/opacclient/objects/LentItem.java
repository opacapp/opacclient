package de.geeksfactory.opacclient.objects;

import org.joda.time.LocalDate;

import java.io.Serializable;

public class LentItem extends AccountItem implements Serializable {
    private String barcode;
    private LocalDate deadline;
    private String homeBranch;
    private String lendingBranch;
    private String prolongData;
    private boolean renewable = true;
    private String downloadData;
    private boolean ebook;

    /**
     * @return Barcode/unique identifier of a lent item. Should be set.
     */
    public String getBarcode() {
        return barcode;
    }

    /**
     * Set barcode/unique identifier of a lent item. Should be set.
     */
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    /**
     * @return Return date for a lent item. Should be set.
     */
    public LocalDate getDeadline() {
        return deadline;
    }

    /**
     * Set return date for a lent item in ISO-8601 format (yyyy-MM-dd). Should be set.
     */
    public void setDeadline(String deadline) {
        if (deadline != null) {
            this.deadline = new LocalDate(deadline);
        } else {
            this.deadline = null;
        }
    }

    /**
     * Set return date for a lent item. Should be set.
     */
    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    /**
     * @return Library branch the item belongs to. Optional.
     */
    public String getHomeBranch() {
        return homeBranch;
    }

    /**
     * Set library branch the item belongs to. Optional.
     */
    public void setHomeBranch(String homeBranch) {
        this.homeBranch = homeBranch;
    }

    /**
     * @return Library branch the item was lent from. Optional.
     */
    public String getLendingBranch() {
        return lendingBranch;
    }

    /**
     * Set library branch the item was lent from. Optional.
     */
    public void setLendingBranch(String lendingBranch) {
        this.lendingBranch = lendingBranch;
    }

    /**
     * @return Internal identifier which will be supplied to your {@link
     * de.geeksfactory.opacclient.apis.OpacApi#prolong(String, Account, int, String)} implementation
     * for prolonging. Button for prolonging will only be displayed if this is set.
     */
    public String getProlongData() {
        return prolongData;
    }

    /**
     * Set internal identifier which will be supplied to your {@link de.geeksfactory.opacclient
     * .apis.OpacApi#prolong(String,
     * Account, int, String)} implementation for prolonging. Button for prolonging will only be
     * displayed if this is set.
     */
    public void setProlongData(String prolongData) {
        this.prolongData = prolongData;
    }

    /**
     * @return whether this item is renewable. Optional, defaults to true.
     */
    public boolean isRenewable() {
        return renewable;
    }

    /**
     * Set whether this item is renewable. Optional, defaults to true.
     */
    public void setRenewable(boolean renewable) {
        this.renewable = renewable;
    }

    /**
     * @return Internal identifier which will be supplied to your {@link
     * de.geeksfactory.opacclient.apis.EbookServiceApi#downloadItem(Account, String, int, String)} implementation
     * for download. Button for download will only be displayed if this is set.
     */
    public String getDownloadData() {
        return downloadData;
    }

    /**
     * Set internal identifier which will be supplied to your
     * {@link de.geeksfactory.opacclient.apis.EbookServiceApi#downloadItem(Account, String, int, String)}
     * implementation for download. Button for download will only be displayed if this is
     * set.
     */
    public void setDownloadData(String downloadData) {
        this.downloadData = downloadData;
    }

    /**
     * @return Whether this item is an eBook. Optional, defaults to false.
     */
    public boolean isEbook() {
        return ebook;
    }

    /**
     * Set whether this item is an eBook. Optional, defaults to false.
     */
    public void setEbook(boolean ebook) {
        this.ebook = ebook;
    }

    @Override
    public void set(String key, String value) {
        if ("".equals(value)) {
            value = null;
        }
        switch (key) {
            case "barcode":
                setBarcode(value);
                break;
            case "returndate":
                setDeadline(new LocalDate(value));
                break;
            case "homebranch":
                setHomeBranch(value);
                break;
            case "lendingbranch":
                setLendingBranch(value);
                break;
            case "prolongurl":
                setProlongData(value);
                break;
            case "renewable":
                setRenewable("Y".equals(value));
                break;
            case "download":
                setDownloadData(value);
                break;
            default:
                super.set(key, value);
                break;
        }
    }

    @Override
    public String toString() {
        return "LentItem{" +
                "account=" + account +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", format='" + format + '\'' +
                ", mediaType=" + mediaType +
                ", id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", dbId=" + dbId +
                ", cover='" + cover + '\'' +
                ", barcode='" + barcode + '\'' +
                ", deadline=" + deadline +
                ", homeBranch='" + homeBranch + '\'' +
                ", lendingBranch='" + lendingBranch + '\'' +
                ", prolongData='" + prolongData + '\'' +
                ", renewable=" + renewable +
                ", downloadData='" + downloadData + '\'' +
                ", ebook=" + ebook +
                '}';
    }
}
