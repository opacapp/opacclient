package de.geeksfactory.opacclient.objects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Object representing a supported library
 * 
 * @author Raphael Michel
 */
public class Library {
	private String ident;
	private String city;
	private String title;
	private String support;
	private String api;
	private JSONObject data;

	/**
	 * Create a Library object based on a <code>JSONObject</code>.
	 * 
	 * @param ident
	 *            identifier
	 * @param input
	 *            input data
	 * @return new Library object
	 * @throws JSONException
	 *             if parsing failed or objects were missing
	 * @see #getIdent()
	 */
	public static Library fromJSON(String ident, JSONObject input)
			throws JSONException {
		Library lib = new Library();
		lib.setIdent(ident);
		lib.setApi(input.getString("api"));
		lib.setCity(input.getString("city"));
		lib.setTitle(input.getString("title"));
		lib.setSupport(input.getString("support"));
		lib.setData(input.getJSONObject("data"));
		if (lib.getTitle().equals(""))
			lib.setTitle(null);
		if (lib.getSupport().equals(""))
			lib.setSupport(null);
		return lib;
	}

	/**
	 * Get the library's identifier (in OpacClient app this is the filename in
	 * assets/bibs/)
	 * 
	 * @return unique library identifier
	 */
	public String getIdent() {
		return ident;
	}

	/**
	 * Set the library's unique identifier.
	 * 
	 * @param ident
	 *            Identifier
	 */
	public void setIdent(String ident) {
		this.ident = ident;
	}

	/**
	 * Get the city the library is located in
	 * 
	 * @return city name
	 */
	public String getCity() {
		return city;
	}

	/**
	 * Set the city the library is located in
	 * 
	 * @param city
	 *            city name
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * Get an additional name of the library if it is not the main public
	 * library in the city it is located in
	 * 
	 * @return a title, not including the city's name
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set an additional name of the library if it is not the main public
	 * library in the city it is located in
	 * 
	 * @param title
	 *            a title, not including the city's name
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Get a human-readable string describing what features are supported in
	 * this library
	 * 
	 * @return Support string
	 */
	public String getSupport() {
		return support;
	}

	/**
	 * Set a human-readable string describing what features are supported in
	 * this library
	 * 
	 * @param support
	 *            Support string
	 */
	public void setSupport(String support) {
		this.support = support;
	}

	/**
	 * Get the name of the API implementation used for this library
	 * 
	 * @return API implementation
	 */
	public String getApi() {
		return api;
	}

	/**
	 * Set the name of the API implementation used for this library
	 * 
	 * @param api
	 *            API implementation (like "bond26")
	 */
	public void setApi(String api) {
		this.api = api;
	}

	/**
	 * Get additional data from JSON configuration
	 * 
	 * @return "data" object from JSON file
	 */
	public JSONObject getData() {
		return data;
	}

	/**
	 * Set additional data from JSON configuration
	 * 
	 * @param data
	 *            "data" object from JSON file
	 */
	public void setData(JSONObject data) {
		this.data = data;
	}
}
