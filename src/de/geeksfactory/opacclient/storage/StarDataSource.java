package de.geeksfactory.opacclient.storage;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import de.geeksfactory.opacclient.objects.Starred;

public class StarDataSource {
	// Database fields
	private SQLiteDatabase database;
	private StarDatabase dbHelper;
	private String[] allColumns = StarDatabase.COLUMNS;

	public StarDataSource(Context context) {
		dbHelper = new StarDatabase(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public void star(String nr, String title, String bib) {
		ContentValues values = new ContentValues();
		values.put("medianr", nr);
		values.put("title", title);
		values.put("bib", bib);
		database.insert("starred", null, values);
	}

	public List<Starred> getAllItems(String bib) {
		List<Starred> items = new ArrayList<Starred>();
		String[] selA = { bib };
		Cursor cursor = database.query("starred", allColumns, "bib = ?", selA,
				null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Starred item = cursorToItem(cursor);
			items.add(item);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return items;
	}

	public Starred getItem(String bib, String id) {
		String[] selA = { bib, id };
		Cursor cursor = database.query("starred", allColumns,
				"bib = ? AND medianr = ?", selA, null, null, null);
		Starred item = null;

		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			item = cursorToItem(cursor);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return item;
	}

	public boolean isStarred(String bib, String id) {
		if (id == null)
			return false;
		String[] selA = { bib, id };
		Cursor cursor = database.query("starred", allColumns,
				"bib = ? AND medianr = ?", selA, null, null, null);
		int c = cursor.getCount();
		cursor.close();
		return (c > 0);
	}

	private Starred cursorToItem(Cursor cursor) {
		Starred item = new Starred();
		item.setId(cursor.getInt(0));
		item.setMNr(cursor.getString(1));
		item.setTitle(cursor.getString(3));
		return item;
	}

	public void remove(Starred item) {
		String[] selA = { "" + item.getId() };
		database.delete("starred", "id=?", selA);
	}

}
