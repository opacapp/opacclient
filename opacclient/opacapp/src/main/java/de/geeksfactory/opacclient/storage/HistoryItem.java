package de.geeksfactory.opacclient.storage;

import org.joda.time.LocalDate;

import java.io.Serializable;

import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.LentItem;

public class HistoryItem extends AccountItem implements Serializable {

    private int historyId;          // Unique Id in HistoryDatabase

    private LocalDate firstDate;    // firstDate the Iten was in Accout
    private LocalDate lastDate;     // lastDate Item was seen in Account
    private boolean lending;        // Is currently lent?

    private String bib;
    private String homeBranch;
    private String lendingBranch;
    private boolean ebook;
    private String barcode;

    private LocalDate deadline;
    private int prolongCount = 0;

    public void setProlongCount(int count) {
        prolongCount = count;
    }

    public int getProlongCount() {
        return prolongCount;
    }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

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
     * @return Return firstDate for a history item. Should be set.
     */
    public LocalDate getFirstDate() {
        return firstDate;
    }

    /**
     * Set return firstDate for a history item
     */
    public void setFirstDate(LocalDate firstDate) {
        this.firstDate = firstDate;
    }

    public boolean isLending() {
        return lending;
    }

    public void setLending(boolean lending) {
        this.lending = lending;
    }

    /**
     * @return Return lastDate for a history item. Should be set.
     */
    public LocalDate getLastDate() {
        return lastDate;
    }

    /**
     * Set return lastDate for a history item
     */
    public void setLastDate(LocalDate lastDate) {
        this.lastDate = lastDate;
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
            case "homebranch":
                setHomeBranch(value);
                break;
            case "lendingbranch":
                setLendingBranch(value);
                break;
            default:
                super.set(key, value);
                break;
        }
    }

    public String getBib() {
        return bib;
    }
    public void setBib(String bib) {
        this.bib = bib;
    }

    public int getHistoryId() {
        return historyId;
    }
    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public boolean isSameAsLentItem(LentItem lentItem) {

        // Id/MediaNr
        if (getId() == null) {
            if (lentItem.getId() != null) {
                return false;
            }
        } else {
            if (!getId().equals(lentItem.getId())) {
                return false;
            }
            // Id/MediaNr are equal
            // return true; ??
        }

        if (getMediaType() == null) {
            if (lentItem.getMediaType() != null) {
                return false;
            }
        } else {
            if (!this.getMediaType().equals(lentItem.getMediaType())) {
                return false;
            }
        }

        if (getTitle() == null) {
            if (lentItem.getTitle() != null) {
                return false;
            }
        } else {
            if (!this.getTitle().equals(lentItem.getTitle())) {
                return false;
            }
        }
        if (getAuthor() == null) {
            if (lentItem.getAuthor() != null) {
                return false;
            }
        } else {
            if (!this.getAuthor().equals(lentItem.getAuthor())) {
                return false;
            }
        }

        // TODO: Prüfen ob weitere Werte übereinstimmen sollten

        return true;
    }

    @Override
    public String toString() {
        return "HistoryItem{" +
                "account=" + account +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", format='" + format + '\'' +
                ", mediaType=" + mediaType +
                ", firstDate='" + firstDate + '\'' +
                ", lastDate='" + lastDate + '\'' +
                ", lending='" + lending + '\'' +
                ", id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", historyId=" + historyId +
                ", cover='" + cover + '\'' +
                ", barcode='" + barcode + '\'' +
                ", homeBranch='" + homeBranch + '\'' +
                ", lendingBranch='" + lendingBranch + '\'' +
                ", ebook=" + ebook + '\'' +
                ", deadline=" + deadline + '\'' +
                ", prolongCount=" + prolongCount +
                '}';
    }
}
