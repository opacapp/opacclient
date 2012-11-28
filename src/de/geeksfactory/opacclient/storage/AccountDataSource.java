package de.geeksfactory.opacclient.storage;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Starred;

public class AccountDataSource {
	// Database fields
	private SQLiteDatabase database;
	private AccountDatabase dbHelper;
	private String[] allColumns = AccountDatabase.COLUMNS;

	public AccountDataSource(Context context) {
		dbHelper = new AccountDatabase(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public long addAccount(Account acc) {
		ContentValues values = new ContentValues();
		values.put("bib", acc.getBib());
		values.put("label", acc.getLabel());
		values.put("name", acc.getName());
		values.put("password", acc.getPassword());
		return database.insert("accounts", null, values);
	}

	public void update(Account acc) {
		ContentValues values = new ContentValues();
		values.put("bib", acc.getBib());
		values.put("label", acc.getLabel());
		values.put("name", acc.getName());
		values.put("password", acc.getPassword());
		database.update("accounts", values, "id = ?",
				new String[] { acc.getId() + "" });
	}

	public long addAccount(String bib, String label, String name,
			String password) {
		ContentValues values = new ContentValues();
		values.put("bib", bib);
		values.put("label", label);
		values.put("name", name);
		values.put("password", password);
		return database.insert("accounts", null, values);
	}

	public List<Account> getAllAccounts() {
		List<Account> accs = new ArrayList<Account>();
		Cursor cursor = database.query("accounts", allColumns, null, null,
				null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Account acc = cursorToAccount(cursor);
			accs.add(acc);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return accs;
	}

	public Account getAccount(long id) {
		String[] selA = { "" + id };
		Cursor cursor = database.query("accounts", allColumns, "id = ?", selA,
				null, null, null);
		Account acc = null;

		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			acc = cursorToAccount(cursor);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return acc;
	}

	private Account cursorToAccount(Cursor cursor) {
		Account acc = new Account();
		acc.setId(cursor.getLong(0));
		acc.setBib(cursor.getString(1));
		acc.setLabel(cursor.getString(2));
		acc.setName(cursor.getString(3));
		acc.setPassword(cursor.getString(4));
		return acc;
	}

	public void remove(Starred item) {
		String[] selA = { "" + item.getId() };
		database.delete("accounts", "id=?", selA);
	}

}
