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
import android.database.Cursor;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.Starred;

public class StarDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarDataSource.class.getName());

    private final Activity context;

    public StarDataSource(Activity context) {
        this.context = context;
    }

    public static Starred cursorToItem(Cursor cursor) {
        Starred item = new Starred();
        mapCursoToItem(cursor, item);
        return item;
    }

    private static void mapCursoToItem(Cursor cursor, Starred item) {
        item.setId(cursor.getInt(0));
        item.setMNr(cursor.getString(1));
        // columnIndex 2 = bib; no matching field, interessiert nicht
        item.setTitle(cursor.getString(3));
        try {
            item.setMediaType(
                    cursor.getString(4) != null ?
                            SearchResult.MediaType.valueOf(cursor.getString(4)) :
                            null);
        } catch (IllegalArgumentException e) {
            // Do not crash on invalid media types stored in the database
        }
    }

    public long star(String nr, String title, String bib, SearchResult.MediaType mediaType, List<Copy> copies) {

        // star
        int starId = star(nr, title, bib, mediaType);

        insertBranches(bib, starId, copies);
        return starId;
    }

    public void insertBranches(String bib, int starId, List<Copy> copies) {

        LOGGER.info("bib = {}, starId = {}, copies.size = {}" , bib, starId, copies.size());

        // Copies aggregieren zu Branches
        Map<String, StarBranchItem> branches = copiesToBranches(copies);

        LOGGER.info("branches.keySet().size = {}" , branches.keySet().size());

        // alle Beziehungen von starred Media zu Branches (zunächst) löschen,
        // da alle nachher unten neu angelegt / insertet werden
        LOGGER.info("removeStarBranch(starId = {})" , starId);
        removeStarBranch(starId);

        final long now = new Date().getTime();

        // jetzt über alle Branches zu starId ...
        for (String branch: branches.keySet()) {

            // LOGGER.info("branch = {}" , branch);

            // Prüfen, ob es Branch schon in StarDatabase gibt
            long branchId = getBranchId(bib, branch);
            LOGGER.info("branch = {}, branchId = {}" , branch, branchId);
            if (branchId == 0) {
                // Branch noch nicht vorhanden, anlegen
                branchId = insertBranch(bib, branch);
            }

            StarBranchItem item = branches.get(branch);
            // Starred-Branch-Relation neu anlegen
            LOGGER.info("insertStarBranch( starId = {}, branchId = {})" , starId, branchId);
            long statusTime = ((item.getStatus() !=null) || (item.getReturnDate()!=0)) ? now : 0;
            LOGGER.info("item.status = {}, .returnDate = {}, statusTime = {}"
                    , item.getStatus(), item.getReturnDate(), statusTime);
            insertStarBranch(starId, branchId, item.getStatus(), statusTime, item.getReturnDate());
        }
    }

    private Map<String, StarBranchItem> copiesToBranches(List<Copy> copies) {
        HashMap<String, StarBranchItem> map = new HashMap<>();
        if ((copies == null) || (copies.isEmpty())) {
            // leere Map zurückgeben
            return map;
        }

        for (Copy copy : copies) {
            String branch = copy.getBranch();
            if (branch == null) {
                // Copy hat keine Branch
                continue;
            }
            StarBranchItem item = map.get(branch);
            if (item == null) {
                // branch kommt noch nicht vor,
                // anlegen und in Map ablegen
                item = new StarBranchItem();

                item.setStatus(copy.getStatus());
                if (copy.getReturnDate() == null) {
                    item.setReturnDate(0);
                } else {
                    item.setReturnDate(copy.getReturnDate().toDate().getTime());
                }
                map.put(branch, item);
            } else {
                // es gab schon eine Copy in diesem Brach
                // dann Abgleich
                if (copy.getReturnDate() == null) {
                    // Annahme: kein ReturnDate: ev. Ausleihbar. Status übernehmen
                    // Muss aber nicht stimmen. Z. B. "zur Zeit vermisst"
                    // Beispiel: Irma la Douce [DVD] in Stuttgart-Vaihingen:
                    // erste Copy "Ausleihbar", zweite Copy "zur Zeit vermisst"

                    if ((item.getStatus() == null) || !item.isAusleihbar()) {
                        // status noch nicht gesetzt  oder
                        // status bereits gesetzt, aber nicht ausleihbar,
                        // dannn übernehmen
                        item.setStatus(copy.getStatus());
                    }
                    // sonst belassen
                } else {
                    // vergleichen, wir suchen das nächste ReturnDate aller Copies = Minimum
                    long returnDate = copy.getReturnDate().toDate().getTime();
                    if (returnDate < item.getReturnDate()) {
                        item.setReturnDate(returnDate);
                    }
                }
            }
        }
        return map;
    }

    public int star(String nr, String title, String bib, SearchResult.MediaType mediaType) {
        ContentValues values = new ContentValues();
        values.put("medianr", nr);
        values.put("title", title);
        values.put("bib", bib);
        values.put("mediatype", mediaType != null ? mediaType.toString() : null);
        Uri uri = context.getContentResolver()
               .insert(((OpacClient) context.getApplication()).getStarProviderStarUri(), values);
        return getId(uri);
    }

    public List<Starred> getAllItems(String bib) {
        List<Starred> items = new ArrayList<>();
        String[] selA = {bib};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getStarProviderStarUri(),
                        StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_LIB,
                        selA, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Starred item = cursorToItem(cursor);
            items.add(item);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return items;
    }

    public List<StarBranchItem> getStarredInBranch(String bib, int branchId) {

        String selection;
        String[] selArgs;
        if ( branchId == 0) {
            selArgs = new String[] {bib};
            selection = StarDatabase.STAR_WHERE_LIB;
        } else if ( branchId == -1) {
            selArgs = new String[]{bib};
            selection = StarDatabase.STAR_WHERE_LIB_BRANCH_IS_NULL;
        } else {
            selArgs = new String[] {bib, Integer.toString(branchId)};
            selection = StarDatabase.STAR_WHERE_LIB_BRANCH;
        }

        final String[] projection = {"id AS _id", "medianr", "bib", "title", "mediatype"
                , "id_branch", "status", "statusTime", "returnDate"};

        Cursor cursor = context.getContentResolver().query(
            StarContentProvider.STAR_JOIN_STAR_BRANCH_URI,
            projection, selection, selArgs, null);

        List<StarBranchItem> items = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            StarBranchItem item = cursorToStarBranchItem(cursor);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        return items;
    }

    public static StarBranchItem cursorToStarBranchItem(Cursor cursor) {
        StarBranchItem item = new StarBranchItem();
        mapCursoToItem(cursor, item);
        item.setBranchId(cursor.getLong(5));
        item.setStatus(cursor.getString(6));
        item.setStatusTime(cursor.getLong(7));
        item.setReturnDate(cursor.getLong(8));
        return item;
    }

    public Starred getItemByTitle(String bib, String title) {
        String[] selA = {bib, title};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getStarProviderStarUri(),
                        StarDatabase.COLUMNS,
                        StarDatabase.STAR_WHERE_TITLE_LIB, selA, null);
        Starred item = null;

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToItem(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public Starred getItem(String bib, String id) {
        String[] selA = {bib, id};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getStarProviderStarUri(),
                        StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_NR_LIB,
                        selA, null);
        Starred item = null;

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToItem(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public int getCountItemWithValidMnr(String bib) {

        final String sel = "bib = ? AND medianr IS NOT NULL";
        String[] selArg = {bib};
        String[] proj = { "count(*)" };

        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getStarProviderStarUri(),
                        proj, sel, selArg, null);

        int count = 0;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public Starred getItem(long id) {
        String[] selA = {String.valueOf(id)};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getStarProviderStarUri(),
                        StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_ID, selA,
                        null);
        Starred item = null;

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToItem(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public boolean isStarred(String bib, String id) {
        if (id == null) {
            return false;
        }
        String[] selA = {bib, id};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getStarProviderStarUri(),
                        StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_NR_LIB,
                        selA, null);
        int c = cursor.getCount();
        cursor.close();
        return (c > 0);
    }

    public boolean isStarredTitle(String bib, String title) {
        if (title == null) {
            return false;
        }
        String[] selA = {bib, title};
        Cursor cursor = context
                .getContentResolver()
                .query(((OpacClient) context.getApplication())
                                .getStarProviderStarUri(),
                        StarDatabase.COLUMNS,
                        StarDatabase.STAR_WHERE_TITLE_LIB, selA, null);
        int c = cursor.getCount();
        cursor.close();
        return (c > 0);
    }

    public void remove(Starred item) {
        String[] selA = {"" + item.getId()};
        context.getContentResolver()
               .delete(((OpacClient) context.getApplication())
                               .getStarProviderStarUri(),
                       StarDatabase.STAR_WHERE_ID, selA);
    }

    public void removeAll(String bib) {
        String[] selA = {bib};
        context.getContentResolver()
               .delete(((OpacClient) context.getApplication())
                               .getStarProviderStarUri(),
                       StarDatabase.STAR_WHERE_LIB, selA);
    }

    public void renameLibraries(Map<String, String> map) {
        for (Entry<String, String> entry : map.entrySet()) {
            ContentValues cv = new ContentValues();
            cv.put("bib", entry.getValue());

            context.getContentResolver()
                   .update(((OpacClient) context.getApplication())
                                   .getStarProviderStarUri(),
                           cv, StarDatabase.STAR_WHERE_LIB,
                           new String[]{entry.getKey()});
        }
    }

    /**
     * setzt Column Filtertimestamp zur branchId
     *
     * @param id branchId
     * @param time filtertimestamp to set
     */
    public void updateBranchFiltertimestamp(int id, long time) {
        ContentValues cv = new ContentValues();
        cv.put("filtertimestamp", time);
        context.getContentResolver()
               .update(StarContentProvider.BRANCH_URI,
                       cv, StarDatabase.BRANCH_WHERE_ID,
                       new String[]{Integer.toString(id)});
    }

    /**
     * ermittelt alle Branches (Namen) zu einer starId
     *
     * @param starId unique id for starred item
     * @return list of branches, where copies for starred item exist
     */
    public List<String> getBranches(int starId) {
        String[] proj = {"name, count(*) as count"};
        String[] selA = { Long.toString(starId)};
        Cursor cursor = context
                .getContentResolver()
                .query(StarContentProvider.STAR_BRANCH_JOIN_BRANCH_URI, proj,
                        "id_star = ?", selA, null);

        List<String> list = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(0));
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        return list;
    }


    private int getBranchId(String bib, String name) {
        if (name == null) {
            return 0;
        }
        String[] selC = {"id"};
        String[] selA = {bib, name};
        Cursor cursor = context
                .getContentResolver()
                .query(StarContentProvider.BRANCH_URI,
                        selC,
                        StarDatabase.BRANCH_WHERE_LIB_NAME, selA, null);

        int id = 0;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            id = cursor.getInt(0);
        }
        // Make sure to close the cursor
        cursor.close();

        return id;
    }

    /**
     * Reads the (one or none) Branch for a branchId from the branch-table
     *
     * @param bib library (TODO neccessary?)
     * @param branchId unique key branch.id > 0
     *
     * @return Branch if found, else null
     */
    public Branch getBranch(String bib, int branchId) {
        if (branchId <= 0) {
            return null;
        }

        String[] selC = { "id, name, filtertimestamp, count(*) as count, MIN(statusTime)" };
        String[] selA = { bib, Integer.toString(branchId)};
        Cursor cursor = context.getContentResolver()
                  .query(StarContentProvider.STAR_BRANCH_JOIN_BRANCH_URI,
                   selC,
                   StarDatabase.STAR_WHERE_LIB_ID, selA, null);

        Branch branch = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            branch = cursorToBranch(cursor);
        }
        // Make sure to close the cursor
        cursor.close();

        return branch;
    }

    /**
     * Reads all branches of a library, to which starred media items exist
     *
     * @param bib name of libray
     * @return List of branches
     */
    public List<Branch> getStarredBranches(String bib) {
        String[] proj = {"id, name, filtertimestamp, count(*) as count, MIN(statusTime)"};
        String[] selA = { bib };
        Cursor cursor = context
                .getContentResolver()
                .query(StarContentProvider.STAR_BRANCH_JOIN_BRANCH_URI, proj,
                        "bib = ?", selA, "filtertimestamp DESC");

        List<Branch> list = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Branch item = cursorToBranch(cursor);
            if (item.getCount() > 0) {
                list.add(item);
            }
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        return list;
    }

    public int getCountStarredWithoutBranch(String bib) {
        String[] proj = {"starred.id"};
        String[] selA = {bib};
        Cursor cursor = context
                .getContentResolver()
                .query(StarContentProvider.STAR_JOIN_STAR_BRANCH_URI, proj,
                        StarDatabase.STAR_WHERE_LIB_BRANCH_IS_NULL, selA, null);

        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    private static Branch cursorToBranch(Cursor cursor) {
        Branch item = new Branch();
        item.setId(cursor.getInt(0));
        item.setName(cursor.getString(1));
        item.setFiltertimestamp(cursor.getInt(2));
        item.setCount(cursor.getInt(3));
        item.setMinStatusTime(cursor.getLong(4));
        return item;
    }

    private long insertBranch(String bib, String name) {
        ContentValues values = new ContentValues();
        values.put("bib", bib);
        values.put("name", name);
        Uri uri = context.getContentResolver().insert(StarContentProvider.BRANCH_URI, values);
        return getId(uri);
    }


    private long insertStarBranch(int starId, long branchId, String status, long statusTime, long returnDate) {
        ContentValues values = new ContentValues();
        values.put("id_star", starId);
        values.put("id_branch", branchId);
        values.put("status", status);
        if (returnDate != 0) {
            values.put("returnDate", returnDate);
        }
        if (statusTime != 0) {
            values.put("statusTime", statusTime);
        }
        Uri uri = context.getContentResolver().insert(StarContentProvider.STAR_BRANCH_URI, values);
        return getId(uri);
    }

    private int removeStarBranch(int starId) {
        String where = "id_star = ?";
        String[] selection = {Integer.toString(starId)};
        return context.getContentResolver().delete(StarContentProvider.STAR_BRANCH_URI, where, selection);
    }

    private int getId(Uri uri) {
        return Integer.parseInt(uri.getLastPathSegment());
    }
}
