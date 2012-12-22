package de.geeksfactory.opacclient.objects;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Bundle;

public class DetailledItem {
	private List<Detail> details = new ArrayList<Detail>();
	private List<ContentValues> copies = new ArrayList<ContentValues>();
	private List<ContentValues> baende = new ArrayList<ContentValues>();
	private String cover;
	private String title;
	private Bitmap coverBitmap;
	private boolean reservable;
	private String id;
	private Bundle volumesearch;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id){
		this.id = id;
	}

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

	public List<Detail> getDetails() {
		return details;
	}

	public List<ContentValues> getCopies() {
		return copies;
	}

	public List<ContentValues> getBaende() {
		return baende;
	}

	public void addDetail(Detail d) {
		details.add(d);
	}

	public void addCopy(ContentValues e) {
		copies.add(e);
	}

	public void addBand(ContentValues e) {
		baende.add(e);
	}

	public Bundle getVolumesearch() {
		return volumesearch;
	}

	public void setVolumesearch(Bundle volumesearch) {
		this.volumesearch = volumesearch;
	}

	public boolean isReservable() {
		return reservable;
	}

	public void setReservable(boolean reservable) {
		this.reservable = reservable;
	}
}
