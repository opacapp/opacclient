package de.geeksfactory.opacclient.objects;

import java.util.List;

import android.content.ContentValues;

public class AccountData {
	private List<ContentValues> lent;
	private List<ContentValues> reservations;

	public List<ContentValues> getLent() {
		return lent;
	}

	public void setLent(List<ContentValues> lent) {
		this.lent = lent;
	}

	public List<ContentValues> getReservations() {
		return reservations;
	}

	public void setReservations(List<ContentValues> reservations) {
		this.reservations = reservations;
	}
}
