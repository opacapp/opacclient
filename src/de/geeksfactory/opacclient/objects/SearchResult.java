package de.geeksfactory.opacclient.objects;

/**
 * Object representing a search result
 * 
 * @author Raphael Michel
 */
public class SearchResult {
	private MediaType type;
	private int nr;
	private String id;
	private String innerhtml;

	/**
	 * Supported media types.
	 * 
	 * @since 2.0.3
	 */
	public enum MediaType {
		NONE, BOOK, CD, CD_SOFTWARE, CD_MUSIC, DVD, MOVIE, AUDIOBOOK, PACKAGE,
		GAME_CONSOLE, EBOOK, SCORE_MUSIC, PACKAGE_BOOKS, UNKNOWN, NEWSPAPER,
		BOARDGAME, SCHOOL_VERSION, MAP, BLURAY
	}

	/**
	 * Create a new SearchResult object
	 * 
	 * @param type
	 *            media type (like "BOOK")
	 * @param nr
	 *            Position in result list
	 * @param innerhtml
	 *            HTML to display
	 */
	public SearchResult(MediaType type, int nr, String innerhtml) {
		this.type = type;
		this.nr = nr;
		this.innerhtml = innerhtml;
	}

	/**
	 * Create an empty object
	 */
	public SearchResult() {
		this.type = MediaType.NONE;
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
	 * Get this item's media type.
	 * 
	 * @return Media type or <code>null</code> if unknown
	 */
	public MediaType getType() {
		return type;
	}

	/**
	 * Set this item's media type.
	 * 
	 * @param type
	 *            Media type
	 */
	public void setType(MediaType type) {
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
	 * Get HTML describing the item to the user in a result list.
	 * 
	 * @return position
	 */
	public String getInnerhtml() {
		return innerhtml;
	}

	/**
	 * Set HTML describing the item to the user in a result list. Only "simple"
	 * HTML like <b>, <i>, etc. can be used.
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
