package de.geeksfactory.opacclient.storage;

public class SearchResult {
	private String type;
	private int nr;
	private String id;
	private String innerhtml;

	public SearchResult(String type, int nr, String innerhtml) {
		super();
		this.type = type;
		this.nr = nr;
		this.innerhtml = innerhtml;
	}

	public SearchResult() {
		super();
		this.type = "";
		this.nr = 0;
		this.innerhtml = "";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getNr() {
		return nr;
	}

	public void setNr(int nr) {
		this.nr = nr;
	}

	public String getInnerhtml() {
		return innerhtml;
	}

	public void setInnerhtml(String innerhtml) {
		this.innerhtml = innerhtml;
	}

	@Override
	public String toString() {
		return "SearchResult [id= " + id + ", type=" + type + ", nr=" + nr
				+ ", innerhtml=" + innerhtml + "]";
	}

}
