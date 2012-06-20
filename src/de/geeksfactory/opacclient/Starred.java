package de.geeksfactory.opacclient;

public class Starred {
	private int id;
	private String mnr;
	private String title;
	
	public Starred() {
		super();
	}
	
	@Override
	public String toString() {
		return "Starred [id=" + id + ", mnr=" + mnr + ", title=" + title + "]";
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getMNr() {
		return mnr;
	}
	
	
	public void setMNr(String mnr) {
		this.mnr = mnr;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
}
