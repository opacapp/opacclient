package de.geeksfactory.opacclient.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.database.Cursor;
import de.geeksfactory.opacclient.frontend.OpacActivity;
import de.geeksfactory.opacclient.objects.Starred;

public class StarDataSource {

	private OpacActivity context;

	public StarDataSource(OpacActivity context) {
		this.context = context;
	}

	public void star(String nr, String title, String bib) {
		ContentValues values = new ContentValues();
		values.put("medianr", nr);
		values.put("title", title);
		values.put("bib", bib);
		context.getContentResolver().insert(
				context.getOpacApplication().getStarProviderStarUri(), values);
	}

	public List<Starred> getAllItems(String bib) {
		List<Starred> items = new ArrayList<Starred>();
		String[] selA = { bib };
		Cursor cursor = context.getContentResolver().query(
				context.getOpacApplication().getStarProviderStarUri(),
				StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_LIB, selA, null);

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

	public Starred getItemByTitle(String bib, String title) {
		String[] selA = { bib, title };
		Cursor cursor = context.getContentResolver().query(
				context.getOpacApplication().getStarProviderStarUri(),
				StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_TITLE_LIB, selA,
				null);
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

	public Starred getItem(String bib, String id) {
		String[] selA = { bib, id };
		Cursor cursor = context.getContentResolver().query(
				context.getOpacApplication().getStarProviderStarUri(),
				StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_NR_LIB, selA,
				null);
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

	public Starred getItem(long id) {
		String[] selA = { String.valueOf(id) };
		Cursor cursor = context.getContentResolver().query(
				context.getOpacApplication().getStarProviderStarUri(),
				StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_ID, selA, null);
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
		Cursor cursor = context.getContentResolver().query(
				context.getOpacApplication().getStarProviderStarUri(),
				StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_NR_LIB, selA,
				null);
		int c = cursor.getCount();
		cursor.close();
		return (c > 0);
	}

	public boolean isStarredTitle(String bib, String title) {
		if (title == null)
			return false;
		String[] selA = { bib, title };
		Cursor cursor = context.getContentResolver().query(
				context.getOpacApplication().getStarProviderStarUri(),
				StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_TITLE_LIB, selA,
				null);
		int c = cursor.getCount();
		cursor.close();
		return (c > 0);
	}

	public static Starred cursorToItem(Cursor cursor) {
		Starred item = new Starred();
		item.setId(cursor.getInt(0));
		item.setMNr(cursor.getString(1));
		item.setTitle(cursor.getString(3));
		return item;
	}

	public void remove(Starred item) {
		String[] selA = { "" + item.getId() };
		context.getContentResolver().delete(
				context.getOpacApplication().getStarProviderStarUri(),
				StarDatabase.STAR_WHERE_ID, selA);
	}

	public void renameLibraries(Map<String, String> map) {
		for (Entry<String, String> entry : map.entrySet()) {
			ContentValues cv = new ContentValues();
			cv.put("bib", entry.getValue());

			context.getContentResolver().update(
					context.getOpacApplication().getStarProviderStarUri(), cv,
					StarDatabase.STAR_WHERE_LIB,
					new String[] { entry.getKey() });
		}
	}

}
