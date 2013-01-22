package de.geeksfactory.opacclient.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;

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
		values.put("bib", acc.getLibrary());
		values.put("label", acc.getLabel());
		values.put("name", acc.getName());
		values.put("password", acc.getPassword());
		return database.insert("accounts", null, values);
	}

	public void update(Account acc) {
		ContentValues values = new ContentValues();
		values.put("bib", acc.getLibrary());
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

	public List<Account> getAllAccounts(String bib) {
		List<Account> accs = new ArrayList<Account>();
		String[] selA = { bib };
		Cursor cursor = database.query("accounts", allColumns, "bib = ?", selA,
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

	public List<Account> getAccountsWithPassword() {
		List<Account> accs = new ArrayList<Account>();

		Cursor cursor = database.query("accounts", allColumns,
				"name is not null AND name != '' AND password is not null",
				null, null, null, null);

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
		acc.setLibrary(cursor.getString(1));
		acc.setLabel(cursor.getString(2));
		acc.setName(cursor.getString(3));
		acc.setPassword(cursor.getString(4));
		return acc;
	}

	public void remove(Account acc) {
		String[] selA = { "" + acc.getId() };
		database.delete("accounts", "id=?", selA);
	}

	public AccountData getCachedAccountData(Account account) {
		AccountData adata = new AccountData();

		List<ContentValues> lent = new ArrayList<ContentValues>();
		String[] selectionArgs = { "" + account.getId() };
		Cursor cursor = database.query(AccountDatabase.TABLENAME_LENT,
				(String[]) AccountDatabase.COLUMNS_LENT.values().toArray(),
				"account = ?", selectionArgs, null, null, null);
		cursor.moveToFirst();

		if (!cursor.isAfterLast()) {
			ContentValues entry = new ContentValues();
			for (Object o : AccountDatabase.COLUMNS_LENT.entrySet()) {
				Map.Entry<String, String> field = (Map.Entry<String, String>) o;
				entry.put(field.getKey(), cursor.getString(cursor
						.getColumnIndex(field.getValue())));
			}
			lent.add(entry);
			cursor.moveToNext();
		}
		cursor.close();
		adata.setLent(lent);

		List<ContentValues> res = new ArrayList<ContentValues>();
		cursor = database.query(AccountDatabase.TABLENAME_RESERVATION,
				(String[]) AccountDatabase.COLUMNS_RESERVATIONS.values()
						.toArray(), "account = ?", selectionArgs, null, null,
				null);
		cursor.moveToFirst();

		if (!cursor.isAfterLast()) {
			ContentValues entry = new ContentValues();
			for (Object o : AccountDatabase.COLUMNS_RESERVATIONS.entrySet()) {
				Map.Entry<String, String> field = (Map.Entry<String, String>) o;
				entry.put(field.getKey(), cursor.getString(cursor
						.getColumnIndex(field.getValue())));
			}
			res.add(entry);
			cursor.moveToNext();
		}
		cursor.close();
		adata.setReservations(res);

		return adata;
	}

	public void storeCachedAccountData(Account account, AccountData adata) {
		database.delete(AccountDatabase.TABLENAME_LENT, "account = ?",
				new String[] { "" + account.getId() });
		for (ContentValues entry : adata.getLent()) {
			ContentValues insertmapping = new ContentValues();
			for (Entry<String, Object> inner : entry.valueSet()) {
				insertmapping.put(
						AccountDatabase.COLUMNS_LENT.get(inner.getKey()),
						(String) inner.getValue());
			}
			database.insert(AccountDatabase.TABLENAME_LENT, null, insertmapping);
		}
		database.delete(AccountDatabase.TABLENAME_RESERVATION, "account = ?",
				new String[] { "" + account.getId() });
		for (ContentValues entry : adata.getReservations()) {
			ContentValues insertmapping = new ContentValues();
			for (Entry<String, Object> inner : entry.valueSet()) {
				insertmapping.put(AccountDatabase.COLUMNS_RESERVATIONS
						.get(inner.getKey()), (String) inner.getValue());
			}
			database.insert(AccountDatabase.TABLENAME_RESERVATION, null,
					insertmapping);
		}
	}
}
