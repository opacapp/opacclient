/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.objects;

import java.text.Collator;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Object representing a supported library
 * 
 * @author Raphael Michel
 */
public class Library implements Comparable<Library> {
	private String ident;
	private String city;
	private String title;
	private String displayName;
	private String support;
	private String api;
	private JSONObject data;
	private String country;
	private String state;
	private String replacedby;
	private double[] geo;
	private float geo_distance;

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
		lib.setCountry(input.getString("country"));
		lib.setState(input.getString("state"));
		lib.setData(input.getJSONObject("data"));
		
		if (input.has("displayname"))
			lib.setDisplayName(input.getString("displayname"));
		
		if (input.has("replacedby"))
			lib.setReplacedBy(input.getString("replacedby"));
		
		if (input.has("geo")) {
			double[] geo = new double[2];
			geo[0] = input.getJSONArray("geo").getDouble(0);
			geo[1] = input.getJSONArray("geo").getDouble(1);
			lib.setGeo(geo);
		}

		if (lib.getTitle().equals(""))
			lib.setTitle(null);
		if (lib.getSupport().equals(""))
			lib.setSupport(null);
		return lib;
	}

	/**
	 * Get the library's identifier (in OpacClient app this is the filename in
	 * assets/bibs/ without the .json extension)
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
	 * The official name of the library to display e.g. in the account detail
	 * view. Returns "city · title" if not set.
	 * 
	 * @return a name, including the city's name
	 */
	public String getDisplayName() {
		if (displayName != null)
			return displayName;
		
		if (getTitle() != null && !getTitle().equals("null")) {
			return getCity() + " · " + getTitle();
		} else {
			return getCity();
		}
	}

	/**
	 * The official name of the library to display e.g. in the account detail
	 * view. Optional.
	 * 
	 * @return a name, including the city's name
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country
	 *            the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state
	 *            the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * Get latitude and longitude of the library's geolocation
	 * 
	 * @return Array of latitude and longitude
	 */
	public double[] getGeo() {
		return geo;
	}

	/**
	 * Set latitude and longitude of the library's geolocation
	 * 
	 * @param geo
	 *            Array of latitude and longitude
	 */
	public void setGeo(double[] geo) {
		this.geo = geo;
	}

	public String getReplacedBy() {
		return replacedby;
	}

	public void setReplacedBy(String replacedby) {
		this.replacedby = replacedby;
	}

	/**
	 * @return Geo distance - only for temporary use.
	 */
	public float getGeo_distance() {
		return geo_distance;
	}

	/**
	 * @param geo_distance
	 *            Set the geo distance - only for temporary use.
	 */
	public void setGeo_distance(float geo_distance) {
		this.geo_distance = geo_distance;
	}

	@Override
	public int compareTo(Library arg0) {
		Collator deCollator = Collator.getInstance(Locale.GERMAN);
		deCollator.setStrength(Collator.TERTIARY);

		int g = deCollator.compare(country, arg0.getCountry());
		if (g == 0) {
			g = deCollator.compare(state, arg0.getState());
			if (g == 0) {
				g = deCollator.compare(city, arg0.getCity());
			}
		}
		return g;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ident == null) ? 0 : ident.hashCode());
		return result;
	}

	/**
	 * Evaluates, whether this object represents the same library as the given
	 * one. Only the library ident (aka. filename) is taken into consideration!
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Library other = (Library) obj;
		if (ident == null) {
			if (other.ident != null)
				return false;
		} else if (!ident.equals(other.ident))
			return false;
		return true;
	}

}
