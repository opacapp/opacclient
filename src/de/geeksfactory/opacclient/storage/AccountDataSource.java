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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	public boolean notificationIsSent(long account, long timestamp) {
		String[] selA = { "" + account, "" + timestamp };
		Cursor cursor = database.query(AccountDatabase.TABLENAME_NOTIFIED,
				AccountDatabase.COLUMNS_NOTIFIED,
				"account = ? AND timestamp = ?", selA, null, null, null);
		boolean found = cursor.getCount() > 0;
		cursor.close();
		return found;
	}

	public long notificationSave(long account, long timestamp) {
		if (notificationIsSent(account, timestamp))
			return 0;
		ContentValues values = new ContentValues();
		values.put("account", account);
		values.put("timestamp", timestamp);
		return database
				.insert(AccountDatabase.TABLENAME_NOTIFIED, null, values);
	}

	public void notificationClearCache(boolean all) {
		if (all) {
			database.delete(AccountDatabase.TABLENAME_NOTIFIED, null, null);
		} else {
			String[] selA = { ""
					+ (System.currentTimeMillis() - (1000 * 3600 * 24 * 30)) };
			database.delete(AccountDatabase.TABLENAME_NOTIFIED,
					"timestamp < ?", selA);
		}
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
		acc.setCached(cursor.getLong(5));
		return acc;
	}

	public void remove(Account acc) {
		String[] selA = { "" + acc.getId() };
		database.delete("accounts", "id=?", selA);
	}

	public int getExpiring(Account account, long tolerance) {
		String[] selA = { "" + account.getId() };
		Cursor cursor = database.query(AccountDatabase.TABLENAME_LENT,
				new String[] { "COUNT(*)" }, "account = ? AND deadline_ts - "
						+ System.currentTimeMillis() + " <= " + tolerance,
				selA, null, null, null);
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		return result;
	}

	public AccountData getCachedAccountData(Account account) {
		AccountData adata = new AccountData(account.getId());

		List<Map<String, String>> lent = new ArrayList<Map<String, String>>();
		String[] selectionArgs = { "" + account.getId() };
		Cursor cursor = database.query(AccountDatabase.TABLENAME_LENT,
				AccountDatabase.COLUMNS_LENT.values().toArray(new String[] {}),
				"account = ?", selectionArgs, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Map<String, String> entry = new HashMap<String, String>();
			for (Object o : AccountDatabase.COLUMNS_LENT.entrySet()) {
				Map.Entry<String, String> field = (Map.Entry<String, String>) o;
				String value = cursor.getString(cursor.getColumnIndex(field
						.getValue()));
				if (value != null) {
					if (!value.equals("")) {
						entry.put(field.getKey(), value);
					}
				}
			}
			lent.add(entry);
			cursor.moveToNext();
		}
		cursor.close();
		adata.setLent(lent);

		List<Map<String, String>> res = new ArrayList<Map<String, String>>();
		cursor = database.query(AccountDatabase.TABLENAME_RESERVATION,
				AccountDatabase.COLUMNS_RESERVATIONS.values()
						.toArray(new String[] {}), "account = ?",
				selectionArgs, null, null, null);
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			Map<String, String> entry = new HashMap<String, String>();
			for (Object o : AccountDatabase.COLUMNS_RESERVATIONS.entrySet()) {
				Map.Entry<String, String> field = (Map.Entry<String, String>) o;
				String value = cursor.getString(cursor.getColumnIndex(field
						.getValue()));
				if (value != null) {
					if (!value.equals("")) {
						entry.put(field.getKey(), value);
					}
				}
			}
			res.add(entry);
			cursor.moveToNext();
		}
		cursor.close();
		adata.setReservations(res);

		String[] selA = { "" + account.getId() };
		cursor = database.query("accounts", new String[] { "pendingFees",
				"validUntil", "warning" }, "id = ?", selA, null, null, null);
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			adata.setPendingFees(cursor.getString(0));
			adata.setValidUntil(cursor.getString(1));
			adata.setWarning(cursor.getString(2));
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();

		return adata;
	}

	public void invalidateCachedData() {
		database.delete(AccountDatabase.TABLENAME_LENT, null, null);
		database.delete(AccountDatabase.TABLENAME_RESERVATION, null, null);
		ContentValues update = new ContentValues();
		update.put("cached", 0);
		String pf = null;
		update.put("pendingFees", pf);
		update.put("validUntil", pf);
		update.put("warning", pf);
		database.update(AccountDatabase.TABLENAME_ACCOUNTS, update, null, null);
	}

	public void invalidateCachedAccountData(Account account) {
		database.delete(AccountDatabase.TABLENAME_LENT, "account = ?",
				new String[] { "" + account.getId() });
		database.delete(AccountDatabase.TABLENAME_RESERVATION, "account = ?",
				new String[] { "" + account.getId() });
		ContentValues update = new ContentValues();
		update.put("cached", 0);
		String pf = null;
		update.put("pendingFees", pf);
		update.put("validUntil", pf);
		update.put("warning", pf);
		database.update(AccountDatabase.TABLENAME_ACCOUNTS, update, "id = ?",
				new String[] { "" + account.getId() });
	}

	public long getCachedAccountDataTime(Account account) {
		return getAccount(account.getId()).getCached();
	}

	public void storeCachedAccountData(Account account, AccountData adata) {
		if (adata == null)
			return;

		long time = System.currentTimeMillis();
		ContentValues update = new ContentValues();
		update.put("cached", time);
		update.put("pendingFees", adata.getPendingFees());
		update.put("validUntil", adata.getValidUntil());
		update.put("warning", adata.getWarning());
		database.update(AccountDatabase.TABLENAME_ACCOUNTS, update, "id = ?",
				new String[] { "" + account.getId() });

		database.delete(AccountDatabase.TABLENAME_LENT, "account = ?",
				new String[] { "" + account.getId() });
		for (Map<String, String> entry : adata.getLent()) {
			ContentValues insertmapping = new ContentValues();
			for (Entry<String, String> inner : entry.entrySet()) {
				insertmapping.put(
						AccountDatabase.COLUMNS_LENT.get(inner.getKey()),
						inner.getValue());
			}
			insertmapping.put("account", account.getId());
			database.insert(AccountDatabase.TABLENAME_LENT, null, insertmapping);
		}

		database.delete(AccountDatabase.TABLENAME_RESERVATION, "account = ?",
				new String[] { "" + account.getId() });
		for (Map<String, String> entry : adata.getReservations()) {
			ContentValues insertmapping = new ContentValues();
			for (Entry<String, String> inner : entry.entrySet()) {
				insertmapping.put(
						AccountDatabase.COLUMNS_RESERVATIONS.get(inner.getKey()),
						inner.getValue());
			}
			insertmapping.put("account", account.getId());
			database.insert(AccountDatabase.TABLENAME_RESERVATION, null,
					insertmapping);
		}
	}

	public List<Account> getAccountsWithPassword(String ident) {
		List<Account> accs = new ArrayList<Account>();

		Cursor cursor = database
				.query("accounts",
						allColumns,
						"name is not null AND name != '' AND password is not null AND bib = ?",
						new String[] { ident }, null, null, null);

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
}
