package de.geeksfactory.opacclient.objects;

import org.json.JSONException;
import org.json.JSONObject;

public class Library {
	private String ident;
	private String city;
	private String title;
	private String support;
	private String api;
	private JSONObject data;

	public static Library fromJSON(String ident, JSONObject input)
			throws JSONException {
		Library lib = new Library();
		lib.setIdent(ident);
		lib.setApi(input.getString("api"));
		lib.setCity(input.getString("city"));
		lib.setTitle(input.getString("title"));
		lib.setSupport(input.getString("support"));
		lib.setData(input.getJSONObject("data"));
		if(lib.getTitle().equals("")) lib.setTitle(null);
		if(lib.getSupport().equals("")) lib.setSupport(null);
		return lib;
	}

	public String getIdent() {
		return ident;
	}

	public void setIdent(String ident) {
		this.ident = ident;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSupport() {
		return support;
	}

	public void setSupport(String support) {
		this.support = support;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public JSONObject getData() {
		return data;
	}

	public void setData(JSONObject data) {
		this.data = data;
	}
}
