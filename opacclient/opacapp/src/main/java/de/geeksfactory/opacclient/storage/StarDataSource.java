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
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.Starred;
import de.geeksfactory.opacclient.objects.Tag;

public class StarDataSource {

    private SQLiteDatabase database;
    private Activity context;

    public StarDataSource(Activity context) {
        this.context = context;
        StarDatabase dbHelper = StarDatabase.getInstance(context);
        database = dbHelper.getWritableDatabase();
    }

    public static Starred cursorToItem(Cursor cursor) {
        Starred item = new Starred();
        item.setId(cursor.getInt(0));
        item.setMNr(cursor.getString(1));
        item.setTitle(cursor.getString(3));
        try {
            item.setMediaType(
                    cursor.getString(4) != null ?
                            SearchResult.MediaType.valueOf(cursor.getString(4)) :
                            null);
        } catch (IllegalArgumentException e) {
            // Do not crash on invalid media types stored in the database
        }
        return item;
    }

    public void star(String nr, String title, String bib, SearchResult.MediaType mediaType) {
        ContentValues values = new ContentValues();
        values.put("medianr", nr);
        values.put("title", title);
        values.put("bib", bib);
        values.put("mediatype", mediaType != null ? mediaType.toString() : null);
        context.getContentResolver()
               .insert(((OpacClient) context.getApplication()).getStarProviderStarUri(), values);
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

    public static Tag cursorToTag(Cursor cursor) {
        Tag tag = new Tag();
        tag.setId(cursor.getInt(0));
        tag.setTagName(cursor.getString(1));
        return tag;
    }

    public boolean hasTagNameInTagTable(String tagName) {
        if (tagName == null) {
            return false;
        }
        String[] selA = {tagName};
        Cursor cursor = database.query(StarDatabase.TAGS_TABLE, null, "tag = ?",
                selA, null, null, null);
        int c = cursor.getCount();
        cursor.close();
        return (c > 0);
    }

    public boolean hasTagIdInStarTagTable(long tagId) {
        String[] selA = {"" + tagId};
        Cursor cursor = database.query(StarDatabase.STAR_TAGS_TABLE, null, "tag = ?",
                selA, null, null, null);
        int c = cursor.getCount();
        cursor.close();
        return (c > 0);
    }

    /**
     * Add given tag to the tags database and the starred-tag database
     */
    public long addTag(Starred item, String tagName) {
        ContentValues values = new ContentValues();
        values.put("tag", tagName);
        if (!hasTagNameInTagTable(tagName)) {
            database.insert(StarDatabase.TAGS_TABLE, null, values);
        }

        values = new ContentValues();
        values.put("tag", getTagByTagName(tagName).getId());
        values.put("item", item.getId());
        try {
            return database.insert(StarDatabase.STAR_TAGS_TABLE, null, values);
        } catch (SQLiteConstraintException e) {
            return -1;
        }

    }

    /**
     * Remove given tag from the starred-tag database and the tags database
     */
    public void removeTag(Tag tag) {
        String[] selA = {"" + tag.getId()};
        database.delete(StarDatabase.STAR_TAGS_TABLE, "tag=?", selA);
        if (!hasTagIdInStarTagTable(tag.getId())) {
            selA = new String[]{"" + tag.getTagName()};
            database.delete(StarDatabase.TAGS_TABLE, "tag=?", selA);
        }
    }

    public Tag getTagByTagName(String tagName) {
        String[] selA = {tagName};
        Cursor cursor = database.query(StarDatabase.TAGS_TABLE, StarDatabase.TAGS_COLUMNS, "tag = ?",
                selA, null, null, null);
        Tag item = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToTag(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public Tag getTagById(long id) {
        String[] selA = {String.valueOf(id)};
        Cursor cursor = database.query(StarDatabase.TAGS_TABLE, StarDatabase.TAGS_COLUMNS, "id = ?",
                selA, null, null, null);
        Tag item = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            item = cursorToTag(cursor);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return item;
    }

    public static int cursorToStarAndTagId(Cursor cursor) {
        return(cursor.getInt(0));
    }


    /**
     * Get all tags
     */
    public List<Tag> getAllTagsInDatabase() {
        List<Tag> tags = new ArrayList<>();
        Cursor cursor = database.query(StarDatabase.TAGS_TABLE, StarDatabase.TAGS_COLUMNS, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int tagId = cursorToStarAndTagId(cursor);
            tags.add(getTagById(tagId));
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return tags;
    }

    /**
     * Get all tags that belong to this Starred item
     */
    public List<Tag> getAllTags(Starred item) {
        List<Tag> tags = new ArrayList<>();
        String[] selA = {Integer.toString(item.getId())};
        Cursor cursor = database.query(StarDatabase.STAR_TAGS_TABLE, StarDatabase.STAR_TAGS_COLUMNS, "item = ?", selA, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int tagId = cursorToStarAndTagId(cursor);
            tags.add(getTagById(tagId));
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return tags;
    }

    /**
     * Get all tag names that belong to this Starred item
     */
    public List<String> getAllTagNames(Starred item) {
        List<String> tags = new ArrayList<>();
        String[] selA = {Integer.toString(item.getId())};
        Cursor cursor = database.query(StarDatabase.STAR_TAGS_TABLE, StarDatabase.STAR_TAGS_COLUMNS, "item = ?", selA, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int tagId = cursorToStarAndTagId(cursor);
            tags.add(getTagById(tagId).getTagName());
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return tags;
    }

    /**
     * Get all tags that do not belong to this Starred item
     */
    public List<String> getAllTagNamesExceptThisItem(Starred item) {
        List<String> listOfTagNames = new ArrayList<>();
        Cursor cursor = database.rawQuery("select * from " + StarDatabase.STAR_TAGS_TABLE, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (cursor.getInt(1) != item.getId()) {
                int tagId = cursorToStarAndTagId(cursor);
                listOfTagNames.add(getTagById(tagId).getTagName());
            }
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return listOfTagNames;
    }
}
