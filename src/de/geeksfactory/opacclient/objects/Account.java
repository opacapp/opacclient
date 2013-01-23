package de.geeksfactory.opacclient.objects;

/**
 * Object representing a library account
 * 
 * @author Raphael Michel
 */
public class Account {
	private long id;
	private String library;
	private String label;
	private String name;
	private String password;
	private long cached;

	/**
	 * Get ID this account is stored with in <code>AccountDataStore</code>
	 * 
	 * @return Account ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Set ID this account is stored with in <code>AccountDataStore</code>
	 * 
	 * @param id
	 *            Account ID
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Get library identifier this Account belongs to.
	 * 
	 * @return Library identifier (see {@link Library#getIdent()})
	 */
	public String getLibrary() {
		return library;
	}

	/**
	 * Set library identifier this Account belongs to.
	 * 
	 * @param library
	 *            Library identifier (see {@link Library#getIdent()})
	 */
	public void setLibrary(String library) {
		this.library = library;
	}

	/**
	 * Get user-configured Account label.
	 * 
	 * @return Label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set user-configured Account label.
	 * 
	 * @param label
	 *            Label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Get user name / identification
	 * 
	 * @return User name or ID
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set user name / identification
	 * 
	 * @param name
	 *            User name or ID
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set user password
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Get user password
	 * 
	 * @param password
	 *            Password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Set date of last caching
	 * 
	 * @return Timestamp in milliseconds
	 */
	public long getCached() {
		return cached;
	}

	/**
	 * Set date of last caching
	 * 
	 * @param cached
	 *            Timestamp in milliseconds (use
	 *            <code>System.currentTimeMillis</code>)
	 */
	public void setCached(long cached) {
		this.cached = cached;
	}
}
