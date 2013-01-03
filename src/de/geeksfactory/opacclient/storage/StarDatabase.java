package de.geeksfactory.opacclient.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StarDatabase extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "starred.db";
	private static final int DATABASE_VERSION = 5; // REPLACE ONUPGRADE IF YOU
													// CHANGE THIS

	private static final String DATABASE_CREATE = "create table "
			+ "starred ( id integer primary key autoincrement,"
			+ " medianr text," + " bib text," + " title text" + ");";
	public static final String[] COLUMNS = { "id", "medianr", "bib", "title" };

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
