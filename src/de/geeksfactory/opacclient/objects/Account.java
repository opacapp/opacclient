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

	/**
	 * Get ID the account is stored with in <code>AccountDataStore</code>
	 * 
	 * @return Account ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Set ID the account is stored with in <code>AccountDataStore</code>
	 * 
	 * @param id Account ID
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Get library identifier the Account belongs to.
	 * 
	 * @return Library identifier (see {@link Library#getIdent()})
	 */
	public String getLibrary() {
		return library;
	}

	/**
	 * Set library identifier the Account belongs to.
	 * 
	 * @param library Library identifier (see {@link Library#getIdent()})
	 */
	public void setLibrary(String library) {
		this.library = library;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
