package de.geeksfactory.opacclient.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MetaDatabase extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "meta.db";
	private static final int DATABASE_VERSION = 1; // REPLACE ONUPGRADE IF YOU
													// CHANGE THIS

	private static final String DATABASE_CREATE = "create table "
			+ "meta ( id integer primary key autoincrement," + " type text,"
			+ " bib text," + " key text," + " value text" + ");";
	public static final String[] COLUMNS = { "id", "type", "bib", "key",
			"value" };

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MetaDatabase.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS meta");
		onCreate(db);

	}

	public MetaDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

}
