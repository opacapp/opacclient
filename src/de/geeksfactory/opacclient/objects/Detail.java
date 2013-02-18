package de.geeksfactory.opacclient.objects;

/**
 * Object representing a detail of a media item
 * 
 * @author Raphael Michel
 */
public class Detail {
	private String desc;
	private String content;

	/**
	 * Create a new detail
	 * 
	 * @param desc
	 *            Description
	 * @param content
	 *            Content
	 */
	public Detail(String desc, String content) {
		super();
		this.desc = desc;
		this.content = content;
	}

	/**
	 * Get this detail's description
	 * 
	 * @return the description
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * Set this detail's description. Description in this context means
	 * something like "Title", "Summary".
	 * 
	 * @param desc
	 *            the description
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * Get this detail's content.
	 * 
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Set this detail's content. If the description is "Title", this should
	 * contain the actual title, like "Harry Potter"
	 * 
	 * @param content
	 *            the content
	 */
	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "Detail [desc=" + desc + ", content=" + content + "]";
	}
}
