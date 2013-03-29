package de.geeksfactory.opacclient.storage;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import de.geeksfactory.opacclient.objects.AccountData;

public class AccountDatabase extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "accounts.db";
	private static final int DATABASE_VERSION = 7; // REPLACE ONUPGRADE IF YOU
													// CHANGE THIS

	public static final String[] COLUMNS = { "id", "bib", "label", "name",
			"password", "cached" };

	public static final Map<String, String> COLUMNS_LENT;
	public static final Map<String, String> COLUMNS_RESERVATIONS;

	static {
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put(AccountData.KEY_LENT_AUTHOR, "author");
		aMap.put(AccountData.KEY_LENT_BARCODE, "barcode");
		aMap.put(AccountData.KEY_LENT_BRANCH, "branch");
		aMap.put(AccountData.KEY_LENT_DEADLINE, "deadline");
		aMap.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, "deadline_ts");
		aMap.put(AccountData.KEY_LENT_LENDING_BRANCH, "lending_branch");
		aMap.put(AccountData.KEY_LENT_LINK, "link");
		aMap.put(AccountData.KEY_LENT_STATUS, "status");
		aMap.put(AccountData.KEY_LENT_TITLE, "title");
		COLUMNS_LENT = Collections.unmodifiableMap(aMap);

		Map<String, String> bMap = new HashMap<String, String>();
		bMap.put(AccountData.KEY_RESERVATION_AUTHOR, "author");
		bMap.put(AccountData.KEY_RESERVATION_BRANCH, "branch");
		bMap.put(AccountData.KEY_RESERVATION_CANCEL, "cancel");
		bMap.put(AccountData.KEY_RESERVATION_READY, "ready");
		bMap.put(AccountData.KEY_RESERVATION_EXPIRE, "expire");
		bMap.put(AccountData.KEY_RESERVATION_TITLE, "title");
		COLUMNS_RESERVATIONS = Collections.unmodifiableMap(bMap);
	}

	public static final String TABLENAME_ACCOUNTS = "accounts";
	public static final String TABLENAME_LENT = "accountdata_lent";
	public static final String TABLENAME_RESERVATION = "accountdata_reservations";

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table "
				+ "accounts ( id integer primary key autoincrement,"
				+ " bib text," + " label text," + " name text,"
				+ " password text," + " cached integer" + ");");
		db.execSQL("create table " + "accountdata_lent ( account integer, "
				+ "title text," + "barcode text," + "author text,"
				+ "deadline text," + "deadline_ts integer," + "status text,"
				+ "branch text," + "lending_branch text," + "link text" + ");");
		db.execSQL("create table "
				+ "accountdata_reservations ( account integer, "
				+ "title text," + "author text," + "ready text,"
				+ "branch text," + "cancel text" + ", expire text);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Provide something here if you change the database version

		if (oldVersion < 2) {
			// App version 2.0.0-alpha to 2.0.0, adding tables for account data
			db.execSQL("create table " + "accountdata_lent ( account integer, "
					+ "title text," + "barcode text," + "author text,"
					+ "deadline text," + "deadline_ts integer,"
					+ "status text," + "branch text," + "lending_branch text,"
					+ "link text" + ");");
			db.execSQL("create table "
					+ "accountdata_reservations ( account integer, "
					+ "title text," + "author text," + "ready text,"
					+ "branch text," + "cancel text" + ");");
		}
		if (oldVersion < 3) {
			// App version 2.0.0-alpha3 to 2.0.0-alpha4, adding tables for
			// account data
			db.execSQL("alter table accounts add column cached integer");
		}
		if (oldVersion == 3) {
			// App version 2.0.0-alpha4 to 2.0.0-alpha5, adding tables for
			// account data
			db.execSQL("alter table accountdata_lent add column deadline_ts integer");
		}
		if (oldVersion < 7) {
			// App version 2.0.5-1 to 2.0.6, adding "expire" to reservations
			try {
				db.execSQL("alter table accountdata_reservations add column expire text");
			} catch (SQLiteException sqle) {
				sqle.printStackTrace();
			}
		}

	}

	public AccountDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

}
