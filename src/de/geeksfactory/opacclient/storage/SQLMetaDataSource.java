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
	public boolean hasMeta(String library) {
		String[] selA = { library };
		Cursor cursor = database.query("meta", allColumns, "bib = ?", selA,
				null, null, null);
		int num = cursor.getCount();
		cursor.close();
		return num > 0;
	}

	@Override
	public boolean hasMeta(String library, String type) {
		String[] selA = { library, type };
		Cursor cursor = database.query("meta", allColumns,
				"bib = ? AND type = ?", selA, null, null, null);
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
