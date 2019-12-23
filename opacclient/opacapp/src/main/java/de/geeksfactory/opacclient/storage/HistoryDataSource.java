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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.BoringLayout;
import android.util.Log;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.HistoryItem;

public class HistoryDataSource {

    // Database fields
    private SQLiteDatabase database;
    private String[] allColumns = HistoryDatabase.COLUMNS;

    public enum ChangeType { NOTHING, UPDATE, INSERT};

    private Activity context;
    public HistoryDataSource(Activity context) {
        this.context = context;
    }

    public void updateLenting(Account account, AccountData adata) {
        String library = account.getLibrary();

        /*   Account   yes      /   no
        History +---------------+-----------
                /    still      /   ended
           yes  /   update      /   update
                /  lastDate     /  lending
        --------+---------------+-----------
            no  /     new       /   -
                /    insert     /
        --------+---------------+-----------
        */
        List<HistoryItem> historyItems = getAllLendingItems(account.getLibrary());
        for (LentItem lentItem : adata.getLent()) {
            HistoryItem foundItem = null;
            for (HistoryItem historyItem: historyItems ) {
                if (historyItem.isSameAsLentItem(lentItem)) {
                    foundItem = historyItem;
                    break;
                }
            }
            if (foundItem != null) {
                // immer noch ausgeliehen
                // -> update lastDate = currentDate
                if (!LocalDate.now().equals(foundItem.getLastDate())) {
                    foundItem.setLastDate(LocalDate.now());
                }
                if (!lentItem.getDeadline().equals(foundItem.getDeadline())) {
                    int count = foundItem.getProlongCount();
                    count++;
                    foundItem.setProlongCount(count);
                }

                this.updateHistoryItem(foundItem);
            } else {
                // neu ausgeliehen
                // -> insert
                this.insertLentItem(library, lentItem);
            }
        }

        for (HistoryItem historyItem: historyItems ) {
            boolean isLending = false;
            for (LentItem lentItem : adata.getLent()) {
                if (historyItem.isSameAsLentItem(lentItem)) {
                    isLending = true;
                    break;
                }
            }
            if (isLending) {
                // bereits oben behandelt
            } else {
                // nicht mehr ausgeliehen
                // -> update lending = false
                historyItem.setLending(false);
                this.updateHistoryItem(historyItem);
            }
        }
    }

    public JSONArray getAllItemsAsJson(String bib) throws
            JSONException {

        String[] selA = {bib};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_LIB,
                        selA, null);

        cursor.moveToFirst();
        JSONArray items = new JSONArray();
        while (!cursor.isAfterLast()) {
            JSONObject item = cursorToJson(HistoryDatabase.COLUMNS, cursor);
            items.put(item);

            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return items;
    }

    public ChangeType insertOrUpdate(String bib, JSONObject entry) throws  JSONException {

        final String methodName = "insertOrUpdate";

        // - same id/medianr or same (title, author and type)
        // - and Zeitraum=(first bis last) überschneiden sich
        LocalDate firstDate = LocalDate.parse(entry.getString(HistoryDatabase.HIST_COL_FIRST_DATE));
        LocalDate lastDate  = LocalDate.parse(entry.getString(HistoryDatabase.HIST_COL_LAST_DATE));

        Log.d(methodName, String.format("bib: %s, json: dates %s - %s", bib, firstDate, lastDate));

        HistoryItem item = null;

        if (entry.has(HistoryDatabase.HIST_COL_MEDIA_NR)) {
            //
            String id = entry.getString(HistoryDatabase.HIST_COL_MEDIA_NR);
            Log.d(methodName, String.format("json: medianr %s", id));
            item = isHistory(bib, id, firstDate, lastDate);
        } else {
            // title, type and author
            String title = entry.getString(HistoryDatabase.HIST_COL_TITLE);
            String author = entry.getString(HistoryDatabase.HIST_COL_AUTHOR);
            String mediatype = entry.getString(HistoryDatabase.HIST_COL_MEDIA_TYPE);
            Log.d(methodName, String.format("json: title %s, author %s, mediatype %s"
                    ,title, author, mediatype));
            item = isHistory(bib, title, author, mediatype , firstDate, lastDate);
        }
        Log.d(methodName, String.format("HistoryItem: %s", item));

        ChangeType changeType = ChangeType.NOTHING;
        if (item==null) {
            // noch kein entsprechender Satz in Datenbank
            Log.d(methodName, "call insertHistoryItem(...)");
            insertHistoryItem(bib, entry);
            changeType = ChangeType.INSERT;
        } else {
            // Satz vorhanden, ev. updaten

            // firstDate, lastDate und count 'mergen'
            if (firstDate.compareTo(item.getFirstDate())<0) {
                Log.d(methodName, "firstDate changed");
                changeType = ChangeType.UPDATE;
                item.setFirstDate(firstDate);
            }
            if (lastDate.compareTo(item.getLastDate())>0) {
                Log.d(methodName, "lastDate changed");
                changeType = ChangeType.UPDATE;
                item.setLastDate(lastDate);
            }
            int prolongCount = entry.getInt(HistoryDatabase.HIST_COL_PROLONG_COUNT);
            if (prolongCount > item.getProlongCount()) {
                Log.d(methodName, "prolongCount changed");
                changeType = ChangeType.UPDATE;
                item.setProlongCount(prolongCount);
            }

            if (changeType == ChangeType.UPDATE) {
                Log.d(methodName, "call updateHistoryItem(...)");
                updateHistoryItem(item);
            } else {
                Log.d(methodName, "nothing changed");
            }
        }

        return changeType;
    }

    private static JSONObject cursorToJson(String[] columns, Cursor cursor) throws
            JSONException {
        JSONObject jsonItem = new JSONObject();
        int i=0;
        for (String col : columns) {
            switch (col) {
                case "lending":
                case "ebook":
                    // boolean wie int
                case "prolongCount":
                    // Integer
                    jsonItem.put(col, Integer.toString(cursor.getInt(i++)));
                    break;
                case "historyId AS _id":
                    col = "historyId";
                case HistoryDatabase.HIST_COL_FIRST_DATE:
                case HistoryDatabase.HIST_COL_LAST_DATE:
                case "deadline":
                    // date wie String
                default:
                    // String
                    jsonItem.put(col, cursor.getString(i++));
            }
        }

        return jsonItem;
    }

    public static HistoryItem cursorToItem(Cursor cursor) {
        HistoryItem item = new HistoryItem();
        int i=0;
        item.setHistoryId(cursor.getInt(i++));
        String ds = cursor.getString(i++);
        if ( ds!=null) {
            item.setFirstDate(LocalDate.parse(ds));
        }
        ds = cursor.getString(i++);
        if ( ds!=null) {
            item.setLastDate(LocalDate.parse(ds));
        }
        item.setLending(cursor.getInt(i++) > 0);
        item.setMNr(cursor.getString(i++));
        item.setBib(cursor.getString(i++));
        item.setTitle(cursor.getString(i++));
        item.setAuthor(cursor.getString(i++));
        item.setFormat(cursor.getString(i++));
        item.setStatus(cursor.getString(i++));
        item.setCover(cursor.getString(i++));
        String mds = cursor.getString(i++);
        if (mds!=null) {
            try {
                SearchResult.MediaType mediaType = SearchResult.MediaType.valueOf(mds);
                item.setMediaType(mediaType);
            } catch (IllegalArgumentException e) {
                // TODO log invalid MediaType
            }
        }
        item.setHomeBranch(cursor.getString(i++));
        item.setLendingBranch(cursor.getString(i++));
        boolean ebook = cursor.getInt(i++) > 0;
        item.setEbook(ebook);
        item.setBarcode(cursor.getString(i++));

        ds = cursor.getString(i++);
        if ( ds!=null) {
            item.setDeadline(LocalDate.parse(ds));
        }
        int count = cursor.getInt(i++);
        item.setProlongCount(count);

        return item;
    }

    private void addAccountItemValues(ContentValues values, AccountItem item ) {
        putOrNull(values,"medianr", item.getId());
        putOrNull(values,"title", item.getTitle());
        putOrNull(values,"author", item.getAuthor());
        putOrNull(values,"format", item.getFormat());
        putOrNull(values,"status", item.getStatus());
        putOrNull(values,"cover", item.getCover());
        SearchResult.MediaType mediaType = item.getMediaType();
        putOrNull(values,"mediatype", mediaType != null ? mediaType.toString() : null);
    }

    private ContentValues createContentValues(HistoryItem historyItem) {
        ContentValues values = new ContentValues();
        addAccountItemValues(values, historyItem);

        putOrNull(values,HistoryDatabase.HIST_COL_FIRST_DATE, historyItem.getFirstDate());
        putOrNull(values,HistoryDatabase.HIST_COL_LAST_DATE, historyItem.getLastDate());
        putOrNull(values,"lending", historyItem.isLending());
        putOrNull(values,"bib", historyItem.getBib());
        putOrNull(values,"homeBranch", historyItem.getHomeBranch());
        putOrNull(values,"lendingBranch", historyItem.getLendingBranch());
        putOrNull(values,"ebook", historyItem.isEbook());
        putOrNull(values,"barcode", historyItem.getBarcode());
        putOrNull(values,"deadline", historyItem.getDeadline());
        values.put("prolongCount", historyItem.getProlongCount());

        return values;
    }

    private void updateHistoryItem(HistoryItem historyItem ) {
        ContentValues values = createContentValues(historyItem);
        String where = "historyId = ?";
        context.getContentResolver()
               .update(((OpacClient) context.getApplication()).getHistoryProviderHistoryUri()
                       , values, where, new String[]{Integer.toString(historyItem.getHistoryId())
                       } );
    }

    public void insertHistoryItem(HistoryItem historyItem ) {
        ContentValues values = createContentValues(historyItem);
        context.getContentResolver()
               .insert(((OpacClient) context.getApplication()).getHistoryProviderHistoryUri(),
               values);
    }

    public void insertHistoryItem(String bib, JSONObject item ) throws JSONException {
        ContentValues values = new ContentValues();
        values.put("bib", bib);

        Iterator<String> keys = item.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            switch (key) {
                case "lending":
                case "ebook":
                    // boolean
                    boolean b = (1 == item.getInt(key));
                    putOrNull(values, key, b );
                    break;
                case "prolongCount":
                    // Integer
                    try {
                        int i = item.getInt(key);
                        values.put(key, i);
                    } catch (JSONException e) {
                        values.putNull(key);
                    }
                    break;
                case HistoryDatabase.HIST_COL_HISTORY_ID:
                case "historyId AS _id":
                    // egal was für eine historyId der Eintrag (in einer anderen HistoryDB)
                    // hatte, hier wird die historyId neu vergeben (auch wg Duplicat Key)

                    // putOrNull(values,"historyId", item.getString(key) );
                    // key wird neu vergeben.
                    break;
                case HistoryDatabase.HIST_COL_FIRST_DATE:
                case HistoryDatabase.HIST_COL_LAST_DATE:
                case "deadline":
                    // date wird als String inserted
                default:
                    // String
                    putOrNull(values,key, item.getString(key) );
            }
        }
        context.getContentResolver()
               .insert(((OpacClient) context.getApplication()).getHistoryProviderHistoryUri(),
               values);
    }

    private void insertLentItem(String bib, LentItem lentItem ) {
        ContentValues values = new ContentValues();
        addAccountItemValues(values, lentItem);

        putOrNull(values, HistoryDatabase.HIST_COL_FIRST_DATE, LocalDate.now());
        putOrNull(values, HistoryDatabase.HIST_COL_LAST_DATE, LocalDate.now());
        putOrNull(values, "lending", true);

        putOrNull(values, "bib", bib);
        putOrNull(values, "homeBranch", lentItem.getHomeBranch());
        putOrNull(values, "lendingBranch", lentItem.getLendingBranch());
        putOrNull(values, "ebook", lentItem.isEbook());
        putOrNull(values, "barcode", lentItem.getBarcode());
        putOrNull(values, "deadline", lentItem.getDeadline());

        context.getContentResolver()
               .insert(((OpacClient) context.getApplication()).getHistoryProviderHistoryUri(),
               values);
    }

    private void putOrNull(ContentValues cv, String key, String value) {
        if (value != null) {
            cv.put(key, value);
        } else {
            cv.putNull(key);
        }
    }
    private void putOrNull(ContentValues cv, String key, LocalDate value) {
        if (value != null) {
            cv.put(key, value.toString());
        } else {
            cv.putNull(key);
        }
    }
    private void putOrNull(ContentValues cv, String key, boolean value) {
        cv.put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    public List<HistoryItem> getAllItems(String bib) {
        List<HistoryItem> items = new ArrayList<>();
        String[] selA = {bib};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_LIB,
                        selA, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            HistoryItem item = cursorToItem(cursor);
            items.add(item);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return items;
    }

    public void sort(String bib, String sortOrder) {
        String[] selA = {bib};
        context.getContentResolver()
               .query(((OpacClient) context.getApplication())
                               .getHistoryProviderHistoryUri(),
                       HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_LIB,
                       selA, sortOrder);
    }

    public List<HistoryItem> getAllLendingItems(String bib) {
        List<HistoryItem> items = new ArrayList<>();
        String[] selA = {bib};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_LIB_LENDING,
                        selA, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            HistoryItem item = cursorToItem(cursor);
            items.add(item);
            cursor.moveToNext();
        }

        // Make sure to close the cursor
        cursor.close();
        return items;
    }

    public HistoryItem getItemByTitle(String bib, String title) {
        String[] selA = {bib, title};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS,
                        HistoryDatabase.HIST_WHERE_TITLE_LIB, selA, null);
        HistoryItem item = null;

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToItem(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public HistoryItem getItem(String bib, String id) {
        String[] selA = {bib, id};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_LIB_NR,
                        selA, null);
        HistoryItem item = null;

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToItem(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public HistoryItem getItem(long id) {
        String[] selA = {String.valueOf(id)};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_HISTORY_ID, selA,
                        null);
        HistoryItem item = null;

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToItem(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public HistoryItem isHistory(String bib, String id, LocalDate firstDate, LocalDate lastDate) {
        if (id == null) {
            return null;
        }

        String[] selA = {bib, id};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_LIB_NR,
                        selA, null);
        cursor.moveToFirst();
        HistoryItem item = foo(cursor, firstDate, lastDate);
        cursor.close();

        return item;
    }

    public HistoryItem isHistory(String bib, String title, String author, String mediatype
            , LocalDate firstDate, LocalDate lastDate) {

        String[] selA = {bib, title, author, mediatype};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS, HistoryDatabase.HIST_WHERE_LIB_TITLE_AUTHOR_TYPE,
                        selA, null);
        cursor.moveToFirst();
        HistoryItem item = foo(cursor, firstDate, lastDate);
        cursor.close();
        return item;
    }

    private HistoryItem foo(Cursor cursor, LocalDate firstDate, LocalDate lastDate) {
        while (!cursor.isAfterLast()) {
            HistoryItem item = cursorToItem(cursor);

            if (firstDate.compareTo(item.getLastDate())>0) {
                // firstDate > item.lastDate: Zeitraum überschneidet sich nicht
            } else if (item.getFirstDate().compareTo(lastDate)>0) {
                // item.firstDate > lastDate: Zeitraum überschneidet sich nicht
                item = null;
            } else {
                // Zeitraum überschneidet sich
                return item;
            }

            cursor.moveToNext();
        }

        return null;
    }

    public boolean isHistoryTitle(String bib, String title) {
        if (title == null) {
            return false;
        }
        String[] selA = {bib, title};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getHistoryProviderHistoryUri(),
                        HistoryDatabase.COLUMNS,
                        HistoryDatabase.HIST_WHERE_TITLE_LIB, selA, null);
        int c = cursor.getCount();
        cursor.close();
        return (c > 0);
    }

    public void remove(HistoryItem item) {
        String[] selA = {"" + item.getHistoryId()};
        context.getContentResolver()
               .delete(((OpacClient) context.getApplication())
                               .getHistoryProviderHistoryUri(),
                       HistoryDatabase.HIST_WHERE_HISTORY_ID, selA);
    }

    public void renameLibraries(Map<String, String> map) {
        for (Entry<String, String> entry : map.entrySet()) {
            ContentValues cv = new ContentValues();
            cv.put("bib", entry.getValue());

            context.getContentResolver()
                   .update(((OpacClient) context.getApplication())
                                   .getHistoryProviderHistoryUri(),
                           cv, HistoryDatabase.HIST_WHERE_LIB,
                           new String[]{entry.getKey()});
        }
    }
}
