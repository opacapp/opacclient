package de.geeksfactory.opacclient.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AccountDatabase extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "accounts.db";
	private static final int DATABASE_VERSION = 1; // REPLACE ONUPGRADE IF YOU
													// CHANGE THIS

	private static final String DATABASE_CREATE = "create table "
			+ "accounts ( id integer primary key autoincrement," + " bib text,"
			+ " label text," + " name text," + " password text" + ");";
	public static final String[] COLUMNS = { "id", "bib", "label", "name", "password" };

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(AccountDatabase.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS accounts");
		onCreate(db);

	}

	public AccountDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

}
