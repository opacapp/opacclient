package de.geeksfactory.opacclient.storage;

import org.joda.time.LocalDate;

import java.util.Date;

import de.geeksfactory.opacclient.objects.Starred;

public class StarBranchItem extends Starred {
    private long branchId;
    private String status;
    private long statusTime;
    private long returnDate;

    public long getBranchId() {
        return branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(long statusTime) {
        this.statusTime = statusTime;
    }

    public Date getStatusDate() {
        if (statusTime == 0) {
            return null;
        }
        return new Date(statusTime);
    }

    public long getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(long returnDate) {
        this.returnDate = returnDate;
    }

    @Override
    public String toString() {
        return "StarBranch [id=" + getId()
                + (getTitle() == null ? "" : ", title=" + getTitle())
                + (getMediaType() == null ? "" : ", mediaType=" + getMediaType().toString())
                + ", branchId=" + branchId
                + (returnDate>0  ? "" : "returnDate = " + returnDate)
                + (getMNr() == null ?  "" : ", mnr=" + getMNr())
                + "]";
    }

    public Boolean isAusleihbar() {
        if (status == null) {
            return null;
        }

        return Boolean.valueOf(
                // siehe Adis.java line 514 ff
                status.matches(".*ist verf.+gbar") ||
                status.contains("is available") ||
                status.equalsIgnoreCase("verfügbar") ||
                status.equalsIgnoreCase("Ausleihbar") ||
                status.contains("ist ausleihbar")
        );
    }
}
