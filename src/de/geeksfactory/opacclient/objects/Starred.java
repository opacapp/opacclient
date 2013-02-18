package de.geeksfactory.opacclient.objects;

/**
 * Object representing a bookmarked item. Not part of the API you are interested
 * in if you want to implement a library system.
 * 
 * @author Raphael Michel
 */
public class Starred {
	private int id;
	private String mnr;
	private String title;

	@Override
	public String toString() {
		return "Starred [id=" + id + ", mnr=" + mnr + ", title=" + title + "]";
	}

	/**
	 * Get this item's ID in bookmark database
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set this item's ID in bookmark database
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Get this item's unique identifier
	 */
	public String getMNr() {
		return mnr;
	}

	/**
	 * Set this item's unique identifier
	 */
	public void setMNr(String mnr) {
		this.mnr = mnr;
	}

	/**
	 * Get this item's title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set this item's title
	 */
	public void setTitle(String title) {
		this.title = title;
	}
}
