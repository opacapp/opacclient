package de.geeksfactory.opacclient.storage;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class SQLMetaDataSource implements MetaDataSource {
	// Database fields
	private SQLiteDatabase database;
	private MetaDatabase dbHelper;
	private String[] allColumns = MetaDatabase.COLUMNS;

	public SQLMetaDataSource(Context context) {
		dbHelper = new MetaDatabase(context);
	}

	@Override
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	@Override
	public void close() {
		dbHelper.close();
	}

	@Override
	public long addMeta(String type, String bib, String key, String value) {
		ContentValues values = new ContentValues();
		values.put("type", type);
		values.put("bib", bib);
		values.put("key", key);
		values.put("value", value);
		return database.insert("meta", null, values);
	}

	@Override
	public List<ContentValues> getMeta(String bib, String type) {
		List<ContentValues> meta = new ArrayList<ContentValues>();
		String[] selA = { bib, type };
		Cursor cursor = database.query("meta", allColumns,
				"bib = ? AND type = ?", selA, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			ContentValues m = cursorToMeta(cursor);
			meta.add(m);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return meta;
	}

	@Override
	public boolean hasMeta(String bib) {
		String[] selA = { bib };
		Cursor cursor = database.query("meta", allColumns, "bib = ?", selA,
				null, null, null);
		int num = cursor.getCount();
		cursor.close();
		return num > 0;
	}

	private ContentValues cursorToMeta(Cursor cursor) {
		ContentValues meta = new ContentValues();
		meta.put("id", cursor.getLong(0));
		meta.put("type", cursor.getString(1));
		meta.put("bib", cursor.getString(2));
		meta.put("key", cursor.getString(3));
		meta.put("value", cursor.getString(4));
		return meta;
	}

	@Override
	public void clearMeta(String bib) {
		String[] selA = { bib };
		database.delete("meta", "bib=?", selA);
	}

	@Override
	public void clearMeta() {
		database.delete("meta", null, null);
	}

}
