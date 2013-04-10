package de.geeksfactory.opacclient.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StarDatabase extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "starred.db";
	private static final int DATABASE_VERSION = 5; // REPLACE ONUPGRADE IF YOU
													// CHANGE THIS

	public static final String STAR_TABLE = "starred";
	public static final String STAR_WHERE_ID = "id = ?";
	public static final String STAR_WHERE_LIB = "bib = ?";
	public static final String STAR_WHERE_TITLE_LIB = "bib = ? AND title = ?";
	public static final String STAR_WHERE_NR_LIB = "bib = ? AND medianr = ?";

	private static final String DATABASE_CREATE = "create table " + STAR_TABLE
			+ " ( id integer primary key autoincrement," + " medianr text,"
			+ " bib text," + " title text" + ");";
	public static final String[] COLUMNS = { "id AS _id", "medianr", "bib",
			"title" };

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(StarDatabase.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");

		db.execSQL("create table temp ( id integer primary key autoincrement, medianr text, bib text, title text );");
		db.execSQL("insert into temp select * from starred;");
		db.execSQL("drop table starred;");
		onCreate(db);
		db.execSQL("insert into starred select * from temp;");
		db.execSQL("drop table temp;");

	}

	public StarDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

}
