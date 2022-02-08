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
    private static final String STAR_TABLE_CREATE = "create table " + STAR_TABLE
            + " ( id integer primary key autoincrement," + " medianr text,"
            + " bib text," + " title text," + " mediatype text"
            + " );";

    /* table for branches, which are used by starred-media-items */
    public static final String BRANCH_TABLE = "branch";
    private static final String BRANCH_TABLE_CREATE = "create table " + BRANCH_TABLE
            + " ( id integer primary key autoincrement,"
            + " bib text,"
            + " name text,"
            + " filtertimestamp integer"
            + " );";

    /* table for relation between starred-media-items and branches */
    public static final String STAR_BRANCH_TABLE = "starred_branch";
    private static final String STAR_BRANCH_TABLE_CREATE = "create table " + STAR_BRANCH_TABLE
            + " ( id_star integer,"
            + " id_branch integer,"
            + " status text,"
            + " statusTime integer,"
            + " returnDate integer,"
            + " PRIMARY KEY (id_star, id_branch),"
            + "  FOREIGN KEY ( id_star ) REFERENCES " + STAR_TABLE + "( id ) ON DELETE CASCADE,"
            + "  FOREIGN KEY ( id_branch ) REFERENCES " + BRANCH_TABLE + "( id ) ON DELETE CASCADE"
            + ");";

    // CHANGE THIS
    public static final String STAR_WHERE_ID = "id = ?";
    public static final String STAR_WHERE_LIB = "bib = ?";
    public static final String STAR_WHERE_LIB_ID = "bib = ? and id = ?";
    public static final String STAR_WHERE_LIB_BRANCH = "bib = ? and id_branch = ?";
    public static final String STAR_WHERE_LIB_BRANCH_IS_NULL = "bib = ? and id_branch is null";
    public static final String STAR_WHERE_TITLE_LIB = "bib = ? AND medianr IS NULL AND title = ?";
    public static final String STAR_WHERE_NR_LIB = "bib = ? AND medianr = ?";
    public static final String[] COLUMNS = {"id AS _id", "medianr", "bib",
            "title", "mediatype"};

    public static final String BRANCH_WHERE_LIB_NAME = "bib = ? and name = ?";
    public static final String BRANCH_WHERE_ID = "id = ?";

    private static final String DATABASE_NAME = "starred.db";
    private static final int DATABASE_VERSION = 7; // REPLACE ONUPGRADE IF YOU

    public StarDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(STAR_TABLE_CREATE);
        db.execSQL(BRANCH_TABLE_CREATE);
        db.execSQL(STAR_BRANCH_TABLE_CREATE);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion >= 5) {
            if (oldVersion < 6) {
                // Add column for media type
                db.execSQL("alter table " + STAR_TABLE + " add column mediatype text");
            }
            if (oldVersion <7) {
                db.execSQL(BRANCH_TABLE_CREATE);
                db.execSQL(STAR_BRANCH_TABLE_CREATE);
            }
        } else {
            // oldVersion < 5
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
