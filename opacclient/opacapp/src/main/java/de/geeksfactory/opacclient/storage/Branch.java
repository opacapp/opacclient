package de.geeksfactory.opacclient.storage;

/**
 * Branch-data for a stared media-item
 */
public class Branch {

    /**
     * unique db-row-id
     */
    private int id;

    /**
     * Branch name
     */
    private String name;

    /**
     * count media-items starred in this brach
     */
    private int count;

    /**
     * minimal (oldest) statusTime off all starred items in this branch
     */
    private long minStatusTime;

    /**
     * most recent timestamp this branch was used for filtering.
     * Used for LRU-sortorder of filter menu items
     */
    private int filtertimestamp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getFiltertimestamp() {
        return filtertimestamp;
    }

    public void setFiltertimestamp(int filtertimestamp) {
        this.filtertimestamp = filtertimestamp;
    }

    public long getMinStatusTime() {
        return minStatusTime;
    }

    public void setMinStatusTime(long minStatusTime) {
        this.minStatusTime = minStatusTime;
    }
}
