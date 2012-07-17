package de.geeksfactory.opacclient;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

public class DetailledItem {
	private List<String[]> details = new ArrayList<String[]>();
	private List<String[]> copies = new ArrayList<String[]>();
	private List<String[]> baende = new ArrayList<String[]>();
	private String cover;
	private String title;
	private Bitmap coverBitmap;
	private boolean reservable;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Bitmap getCoverBitmap() {
		return coverBitmap;
	}

	public void setCoverBitmap(Bitmap coverBitmap) {
		this.coverBitmap = coverBitmap;
	}

	public DetailledItem() {
		super();
	}

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}

	@Override
	public String toString() {
		return "DetailledItem [details=" + details + ", copies=" + copies
				+ ", cover=" + cover + ", title = " + title + ", reservable = "
				+ reservable + "]";
	}

	public List<String[]> getDetails() {
		return details;
	}

	public List<String[]> getCopies() {
		return copies;
	}

	public List<String[]> getBaende() {
		return baende;
	}

	public void addDetail(String[] e) {
		details.add(e);
	}

	public void addCopy(String[] e) {
		copies.add(e);
	}

	public void addBand(String[] e) {
		baende.add(e);
	}

	public boolean isReservable() {
		return reservable;
	}

	public void setReservable(boolean reservable) {
		this.reservable = reservable;
	}
}
