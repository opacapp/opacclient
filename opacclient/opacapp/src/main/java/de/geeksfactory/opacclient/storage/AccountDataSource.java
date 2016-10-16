/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.reminder.Alarm;

public class AccountDataSource {
    // Database fields
    private SQLiteDatabase database;
    private String[] allColumns = AccountDatabase.COLUMNS;

    public AccountDataSource(Context context) {
        AccountDatabase dbHelper = AccountDatabase.getInstance(context);
        database = dbHelper.getWritableDatabase();
        // we do not need to close the database, as only one instance is created
        // see e.g. http://stackoverflow
        // .com/questions/4547461/closing-the-database-in-a-contentprovider/12715032#12715032
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
        values.put("passwordValid", acc.isPasswordKnownValid() ? 1 : 0);
        database.update("accounts", values, "id = ?", new String[]{acc.getId() + ""});
    }

    public long addAccount(String bib, String label, String name, String password) {
        ContentValues values = new ContentValues();
        values.put("bib", bib);
        values.put("label", label);
        values.put("name", name);
        values.put("password", password);
        return database.insert("accounts", null, values);
    }

    public List<Account> getAllAccounts() {
        List<Account> accs = new ArrayList<>();
        Cursor cursor = database.query("accounts", allColumns, null, null, null, null, null);

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
        List<Account> accs = new ArrayList<>();
        String[] selA = {bib};
        Cursor cursor = database.query("accounts", allColumns, "bib = ?", selA, null, null, null);

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
        List<Account> accs = new ArrayList<>();

        Cursor cursor = database.query("accounts", allColumns,
                "name is not null AND name != '' AND password is not null", null, null, null, null);

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
        String[] selA = {"" + id};
        Cursor cursor = database.query("accounts", allColumns, "id = ?", selA, null, null, null);
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
        acc.setPasswordKnownValid(cursor.getLong(9) > 0);
        return acc;
    }

    public void remove(Account acc) {
        deleteAccountData(acc);
        String[] selA = {"" + acc.getId()};
        database.delete("accounts", "id=?", selA);
    }

    public int getExpiring(Account account, int tolerance) {
        String[] selA = {String.valueOf(account.getId())};
        Cursor cursor = database.query(AccountDatabase.TABLENAME_LENT, new String[]{"COUNT(*)"},
                "account = ? AND date(deadline) < date('now','-" + tolerance + " days')", selA,
                null, null, null);
        cursor.moveToFirst();
        int result = cursor.getInt(0);
        cursor.close();
        return result;
    }

    public AccountData getCachedAccountData(Account account) {
        AccountData adata = new AccountData(account.getId());

        List<LentItem> lent = new ArrayList<>();
        String[] selectionArgs = {"" + account.getId()};
        Cursor cursor = database.query(AccountDatabase.TABLENAME_LENT, AccountDatabase.COLUMNS_LENT,
                "account = ?", selectionArgs, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LentItem entry = cursorToLentItem(cursor);
            lent.add(entry);
            cursor.moveToNext();
        }
        cursor.close();
        adata.setLent(lent);

        List<ReservedItem> res = new ArrayList<>();
        cursor = database.query(AccountDatabase.TABLENAME_RESERVATION,
                AccountDatabase.COLUMNS_RESERVATIONS, "account = ?", selectionArgs, null, null,
                null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            ReservedItem entry = cursorToReservedItem(cursor);
            res.add(entry);
            cursor.moveToNext();
        }
        cursor.close();
        adata.setReservations(res);

        String[] selA = {"" + account.getId()};
        cursor = database
                .query("accounts", new String[]{"pendingFees", "validUntil", "warning"}, "id = ?",
                        selA, null, null, null);
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

    private LentItem cursorToLentItem(Cursor cursor) {
        LentItem item = new LentItem();
        setAccountItemAttributes(cursor, item);
        item.setBarcode(cursor.getString(7));
        item.setDeadline(cursor.getString(8));
        item.setHomeBranch(cursor.getString(9));
        item.setLendingBranch(cursor.getString(10));
        item.setProlongData(cursor.getString(11));
        item.setRenewable(cursor.getInt(12) == 1);
        item.setDownloadData(cursor.getString(13));
        item.setEbook(cursor.getInt(14) == 1);
        return item;
    }

    private ReservedItem cursorToReservedItem(Cursor cursor) {
        ReservedItem item = new ReservedItem();
        setAccountItemAttributes(cursor, item);
        item.setReadyDate(cursor.getString(7));
        item.setExpirationDate(cursor.getString(8));
        item.setBranch(cursor.getString(9));
        item.setCancelData(cursor.getString(10));
        item.setBookingData(cursor.getString(11));
        return item;
    }

    private void setAccountItemAttributes(Cursor cursor, AccountItem item) {
        item.setDbId(cursor.getLong(0));
        item.setAccount(cursor.getLong(1));
        item.setTitle(cursor.getString(2));
        item.setAuthor(cursor.getString(3));
        item.setFormat(cursor.getString(4));
        item.setId(cursor.getString(5));
        item.setStatus(cursor.getString(6));
        String mediatype = cursor.getString(cursor.getColumnIndex("mediatype"));
        item.setMediaType(mediatype != null ? SearchResult.MediaType.valueOf(mediatype) : null);
        item.setCover(cursor.getString(cursor.getColumnIndex("cover")));
    }

    private ContentValues lentItemToContentValues(LentItem item, long accountId) {
        ContentValues cv = new ContentValues();
        setAccountItemAttributes(item, cv, accountId);
        putOrNull(cv, "barcode", item.getBarcode());
        putOrNull(cv, "deadline", item.getDeadline());
        putOrNull(cv, "homebranch", item.getHomeBranch());
        putOrNull(cv, "lending_branch", item.getLendingBranch());
        putOrNull(cv, "prolong_data", item.getProlongData());
        cv.put("renewable", item.isRenewable() ? 1 : 0);
        putOrNull(cv, "download_data", item.getDownloadData());
        cv.put("ebook", item.isEbook() ? 1 : 0);
        return cv;
    }

    private ContentValues reservedItemToContentValues(ReservedItem item, long accountId) {
        ContentValues cv = new ContentValues();
        setAccountItemAttributes(item, cv, accountId);
        putOrNull(cv, "ready", item.getReadyDate());
        putOrNull(cv, "expiration", item.getExpirationDate());
        putOrNull(cv, "branch", item.getBranch());
        putOrNull(cv, "cancel_data", item.getCancelData());
        putOrNull(cv, "booking_data", item.getBookingData());
        return cv;
    }

    private void setAccountItemAttributes(AccountItem item, ContentValues cv, long accountId) {
        if (item.getDbId() != null) cv.put("id", item.getDbId());
        cv.put("account", accountId);
        putOrNull(cv, "title", item.getTitle());
        putOrNull(cv, "author", item.getAuthor());
        putOrNull(cv, "format", item.getFormat());
        putOrNull(cv, "itemid", item.getId());
        putOrNull(cv, "status", item.getStatus());
        putOrNull(cv, "cover", item.getCover());
        putOrNull(cv, "mediatype",
                item.getMediaType() != null ? item.getMediaType().toString() : null);
    }

    private void putOrNull(ContentValues cv, String key, LocalDate value) {
        if (value != null) {
            cv.put(key, value.toString());
        } else {
            cv.putNull(key);
        }
    }

    private void putOrNull(ContentValues cv, String key, String value) {
        if (value != null) {
            cv.put(key, value);
        } else {
            cv.putNull(key);
        }
    }

    public void invalidateCachedData() {
        database.delete(AccountDatabase.TABLENAME_LENT, null, null);
        database.delete(AccountDatabase.TABLENAME_RESERVATION, null, null);
        database.delete(AccountDatabase.TABLENAME_ALARMS, null, null);
        ContentValues update = new ContentValues();
        update.put("cached", 0);
        update.put("pendingFees", (String) null);
        update.put("validUntil", (String) null);
        update.put("warning", (String) null);
        database.update(AccountDatabase.TABLENAME_ACCOUNTS, update, null, null);
    }

    public void deleteAccountData(Account account) {
        database.delete(AccountDatabase.TABLENAME_LENT, "account = ?",
                new String[]{"" + account.getId()});
        database.delete(AccountDatabase.TABLENAME_RESERVATION, "account = ?",
                new String[]{"" + account.getId()});

    }

    public void invalidateCachedAccountData(Account account) {
        ContentValues update = new ContentValues();
        update.put("cached", 0);
        database.update(AccountDatabase.TABLENAME_ACCOUNTS, update, "id = ?",
                new String[]{"" + account.getId()});
    }

    public long getCachedAccountDataTime(Account account) {
        return getAccount(account.getId()).getCached();
    }

    public void storeCachedAccountData(Account account, AccountData adata) {
        if (adata == null) {
            return;
        }

        long time = System.currentTimeMillis();
        ContentValues update = new ContentValues();
        update.put("cached", time);
        update.put("pendingFees", adata.getPendingFees());
        update.put("validUntil", adata.getValidUntil());
        update.put("warning", adata.getWarning());
        database.update(AccountDatabase.TABLENAME_ACCOUNTS, update, "id = ?",
                new String[]{"" + account.getId()});

        database.delete(AccountDatabase.TABLENAME_LENT, "account = ?",
                new String[]{"" + account.getId()});
        for (LentItem entry : adata.getLent()) {
            ContentValues insertmapping = lentItemToContentValues(entry, account.getId());
            database.insert(AccountDatabase.TABLENAME_LENT, null, insertmapping);
        }

        database.delete(AccountDatabase.TABLENAME_RESERVATION, "account = ?",
                new String[]{"" + account.getId()});
        for (ReservedItem entry : adata.getReservations()) {
            ContentValues insertmapping = reservedItemToContentValues(entry, account.getId());
            database.insert(AccountDatabase.TABLENAME_RESERVATION, null, insertmapping);
        }
    }

    public List<LentItem> getAllLentItems() {
        List<LentItem> items = new ArrayList<>();
        Cursor cursor = database
                .query(AccountDatabase.TABLENAME_LENT, AccountDatabase.COLUMNS_LENT, null, null,
                        null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LentItem item = cursorToLentItem(cursor);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();
        return items;
    }

    public LentItem getLentItem(long id) {
        String[] selA = {"" + id};
        Cursor cursor = database
                .query(AccountDatabase.TABLENAME_LENT, AccountDatabase.COLUMNS_LENT, "id = ?", selA,
                        null, null, null);
        LentItem item = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToLentItem(cursor);
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public List<LentItem> getLentItems(long[] ids) {
        List<LentItem> items = new ArrayList<>();
        Cursor cursor = database
                .query(AccountDatabase.TABLENAME_LENT, AccountDatabase.COLUMNS_LENT, "id IN(" +
                        joinLongs(ids, ",") + ")", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LentItem item = cursorToLentItem(cursor);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();
        return items;
    }

    public List<Account> getAccountsWithPassword(String ident) {
        List<Account> accs = new ArrayList<>();

        Cursor cursor = database.query("accounts", allColumns,
                "name is not null AND name != '' AND password is not null AND bib = ?",
                new String[]{ident}, null, null, null);

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

    public long addAlarm(LocalDate deadline, long[] media, DateTime alarmTime) {
        for (long mid : media) {
            if (getLentItem(mid) == null) {
                throw new DataIntegrityException(
                        "Cannot add alarm with deadline " + deadline.toString() +
                                " that has dependency on the non-existing media item " + mid);
            }
        }

        ContentValues values = new ContentValues();
        values.put("deadline", deadline.toString());
        values.put("media", joinLongs(media, ","));
        values.put("alarm", alarmTime.toString());
        values.put("notified", 0);
        values.put("finished", 0);
        return database.insert(AccountDatabase.TABLENAME_ALARMS, null, values);
    }

    public void updateAlarm(Alarm alarm) {
        for (long mid : alarm.media) {
            if (getLentItem(mid) == null) {
                throw new DataIntegrityException(
                        "Cannot update alarm with deadline " + alarm.deadline.toString() +
                                " that has dependency on the non-existing media item " + mid);
            }
        }

        ContentValues values = new ContentValues();
        values.put("deadline", alarm.deadline.toString());
        values.put("media", joinLongs(alarm.media, ","));
        values.put("alarm", alarm.notificationTime.toString());
        values.put("notified", alarm.notified ? 1 : 0);
        values.put("finished", alarm.finished ? 1 : 0);
        database.update(AccountDatabase.TABLENAME_ALARMS, values, "id = ?",
                new String[]{alarm.id + ""});
    }

    public void resetNotifiedOnAllAlarams() {
        ContentValues values = new ContentValues();
        values.put("notified", 0);
        database.update(AccountDatabase.TABLENAME_ALARMS, values, "finished = 0 AND notified = 1", null);
    }

    public Alarm getAlarmByDeadline(LocalDate deadline) {
        String[] selA = {deadline.toString()};
        Cursor cursor = database
                .query(AccountDatabase.TABLENAME_ALARMS, AccountDatabase.COLUMNS_ALARMS,
                        "deadline = ?", selA, null, null, null);
        Alarm item = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToAlarm(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public Alarm getAlarm(long id) {
        String[] selA = {"" + id};
        Cursor cursor = database
                .query(AccountDatabase.TABLENAME_ALARMS, AccountDatabase.COLUMNS_ALARMS, "id = ?",
                        selA, null, null, null);
        Alarm item = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToAlarm(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public List<Alarm> getAllAlarms() {
        List<Alarm> alarms = new ArrayList<>();
        Cursor cursor = database
                .query(AccountDatabase.TABLENAME_ALARMS, AccountDatabase.COLUMNS_ALARMS, null, null,
                        null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Alarm alarm = cursorToAlarm(cursor);
            alarms.add(alarm);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return alarms;
    }

    public void clearAlarms() {
        database.delete(AccountDatabase.TABLENAME_ALARMS, null, null);
    }

    private Alarm cursorToAlarm(Cursor cursor) {
        Alarm alarm = new Alarm();
        alarm.id = cursor.getLong(0);
        alarm.deadline = new LocalDate(cursor.getString(1));
        alarm.media = splitLongs(cursor.getString(2), ",");
        alarm.notificationTime = new DateTime(cursor.getString(3));
        alarm.notified = cursor.getInt(4) == 1;
        alarm.finished = cursor.getInt(5) == 1;
        return alarm;
    }

    public void removeAlarm(Alarm alarm) {
        String[] selA = {"" + alarm.id};
        database.delete(AccountDatabase.TABLENAME_ALARMS, "id=?", selA);
    }

    private String joinLongs(long[] longs, String separator) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (long l : longs) {
            if (first) {
                first = false;
            } else {
                sb.append(separator);
            }
            sb.append(l);
        }
        return sb.toString();
    }

    private long[] splitLongs(String string, String separator) {
        String[] strings = string.split(separator);
        long[] longs = new long[strings.length];
        for (int i = 0; i < strings.length; i++) {
            longs[i] = Long.valueOf(strings[i]);
        }
        return longs;
    }
}
