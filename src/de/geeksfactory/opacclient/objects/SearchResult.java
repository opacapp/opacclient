package de.geeksfactory.opacclient.objects;

/**
 * Object representing a search result
 * 
 * @author Raphael Michel
 */
public class SearchResult {
	private String type;
	private int nr;
	private String id;
	private String innerhtml;

	/**
	 * Create a new SearchResult object
	 * 
	 * @param type
	 *            media type (like "book")
	 * @param nr
	 *            Position in result list
	 * @param innerhtml
	 *            HTML to display
	 */
	public SearchResult(String type, int nr, String innerhtml) {
		this.type = type;
		this.nr = nr;
		this.innerhtml = innerhtml;
	}

	/**
	 * Create an empty object
	 */
	public SearchResult() {
		this.type = "";
		this.nr = 0;
		this.innerhtml = "";
	}

	/**
	 * Get the unique identifier of this object
	 * 
	 * @return ID or <code>null</code> if unknown
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the unique identifier of this object
	 * 
	 * @param id
	 *            unique identifier
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get this item's media type. There is a mapping to drawables in
	 * de.geeksfactory.opacclient.frontend.ResultsAdapter
	 * 
	 * @return Media type or <code>null</code> if unknown
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set this item's media type.
	 * 
	 * @param type
	 *            Media type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get this item's position in result list
	 * 
	 * @return position
	 */
	public int getNr() {
		return nr;
	}

	/**
	 * Set this item's position in result list
	 * 
	 * @param nr
	 *            position
	 */
	public void setNr(int nr) {
		this.nr = nr;
	}

	/**
	 * Get HTML describing the item to the user in a result list
	 * 
	 * @return position
	 */
	public String getInnerhtml() {
		return innerhtml;
	}

	/**
	 * Set HTML describing the item to the user in a result list
	 * 
	 * @param innerhtml
	 *            simple HTML code
	 */
	public void setInnerhtml(String innerhtml) {
		this.innerhtml = innerhtml;
	}

	@Override
	public String toString() {
		return "SearchResult [id= " + id + ", type=" + type + ", nr=" + nr
				+ ", innerhtml=" + innerhtml + "]";
	}

}
