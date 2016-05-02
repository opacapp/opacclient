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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StarDatabase extends SQLiteOpenHelper {

    public static final String STAR_TABLE = "starred";
    private static final String DATABASE_CREATE = "create table " + STAR_TABLE
            + " ( id integer primary key autoincrement," + " medianr text,"
            + " bib text," + " title text," + " mediatype text" + ");";
    // CHANGE THIS
    public static final String STAR_WHERE_ID = "id = ?";
    public static final String STAR_WHERE_LIB = "bib = ?";
    public static final String STAR_WHERE_TITLE_LIB = "bib = ? AND medianr IS NULL AND title = ?";
    public static final String STAR_WHERE_NR_LIB = "bib = ? AND medianr = ?";
    public static final String[] COLUMNS = {"id AS _id", "medianr", "bib",
            "title", "mediatype"};
    private static final String DATABASE_NAME = "starred.db";
    private static final int DATABASE_VERSION = 6; // REPLACE ONUPGRADE IF YOU

    public StarDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion >= 5) {
            if (oldVersion < 6) {
                // Add column for media type
                db.execSQL("alter table " + STAR_TABLE + " add column mediatype text");
            }
        } else {
            Log.w(StarDatabase.class.getName(), "Upgrading database from version "
                    + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");

            db.execSQL(
                    "create table temp ( id integer primary key autoincrement, medianr text, " +
                            "bib text, title text );");
            db.execSQL("insert into temp select * from starred;");
            db.execSQL("drop table starred;");
            onCreate(db);
            db.execSQL("insert into starred select * from temp;");
            db.execSQL("drop table temp;");
        }
    }

}
