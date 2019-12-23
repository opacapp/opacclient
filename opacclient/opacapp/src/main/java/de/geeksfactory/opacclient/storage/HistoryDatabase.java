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

public class HistoryDatabase extends SQLiteOpenHelper {

    public static final String HIST_TABLE = "historyTable";

    public static final String HIST_COL_HISTORY_ID = "historyId";
    public static final String HIST_COL_MEDIA_NR = "medianr";
    public static final String HIST_COL_TITLE = "title";
    public static final String HIST_COL_AUTHOR = "author";
    public static final String HIST_COL_MEDIA_TYPE = "mediatype";
    public static final String HIST_COL_FIRST_DATE = "firstDate";
    public static final String HIST_COL_LAST_DATE = "lastDate";
    public static final String HIST_COL_PROLONG_COUNT = "prolongCount";

    public static final String HIST_WHERE_HISTORY_ID = HIST_COL_HISTORY_ID +" = ?";
    public static final String HIST_WHERE_LIB = "bib = ?";

    public static final String HIST_WHERE_LIB_LENDING = "bib = ? AND lending = 1";
    public static final String HIST_WHERE_TITLE_LIB = "bib = ? AND "
            + HIST_COL_MEDIA_NR + " IS NULL AND "
            + HIST_COL_TITLE + " = ?";
    public static final String HIST_WHERE_LIB_NR = "bib = ? AND "
            + HIST_COL_MEDIA_NR + " = ?";
    public static final String HIST_WHERE_LIB_TITLE_AUTHOR_TYPE = "bib = ? AND "
            + HIST_COL_TITLE + " = ? AND "
            + HIST_COL_AUTHOR + " = ? AND "
            + HIST_COL_MEDIA_TYPE + " = ?"
            ;
    public static final String[] COLUMNS = {HIST_COL_HISTORY_ID +" AS _id", // wg. android.widget.CursorAdapter
                // siehe https://developer.android.com/reference/android/widget/CursorAdapter.html
            HIST_COL_FIRST_DATE,
            HIST_COL_LAST_DATE,
            "lending",
            HIST_COL_MEDIA_NR,
            "bib",
            HIST_COL_TITLE,
            HIST_COL_AUTHOR,
            "format",
            "status",
            "cover",
            HIST_COL_MEDIA_TYPE,
            "homeBranch",
            "lendingBranch",
            "ebook",
            "barcode",
            "deadline",
            HIST_COL_PROLONG_COUNT
        };

    private static final String DATABASE_CREATE = "create table historyTable (\n" +
            "\t" + HIST_COL_HISTORY_ID +" integer primary key autoincrement,\n" +
            "\t" + HIST_COL_FIRST_DATE + " date,\n" +
            "\t" + HIST_COL_LAST_DATE + " date,\n" +
            "\tlending boolean,\n" +
            "\t" + HIST_COL_MEDIA_NR + " text,\n" +
            "\tbib text,\n" +
            "\t" + HIST_COL_TITLE + " text,\n" +
            "\t" + HIST_COL_AUTHOR + " text,\n" +
            "\tformat text,\n" +
            "\tstatus text,\n" +
            "\tcover text,\n" +
            "\t" + HIST_COL_MEDIA_TYPE + " text,\n" +
            "\thomeBranch text,\n" +
            "\tlendingBranch text,\n" +
            "\tebook boolean,\n" +
            "\tbarcode text,\n" +
            "\tdeadline date,\n" +
            "\t" + HIST_COL_PROLONG_COUNT + " integer\n" +
            ");";


    private static final String DATABASE_NAME = "history.db";
    private static final int DATABASE_VERSION = 1; // REPLACE ONUPGRADE IF YOU

    public HistoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.w(HistoryDatabase.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");

        final String createTemp =
            "create table tempTable (\n" +
                    "\t" + HIST_COL_HISTORY_ID +" integer primary key autoincrement,\n" +
                    "\t" + HIST_COL_FIRST_DATE + " date,\n" +
                    "\t" + HIST_COL_LAST_DATE + " date,\n" +
                    "\tlending boolean,\n" +
                    "\t" + HIST_COL_MEDIA_NR + " text,\n" +
                    "\tbib text,\n" +
                    "\t" + HIST_COL_TITLE + " text,\n" +
                    "\t" + HIST_COL_AUTHOR + " text,\n" +
                    "\tformat text,\n" +
                    "\tstatus text,\n" +
                    "\tcover text,\n" +
                    "\t" + HIST_COL_MEDIA_TYPE + " text,\n" +
                    "\thomeBranch text,\n" +
                    "\tlendingBranch text,\n" +
                    "\tebook boolean,\n" +
                    "\tbarcode text,\n" +
                    "\tdeadline date,\n" +
                    "\t" + HIST_COL_PROLONG_COUNT + " integer\n" +
                    ");";

        db.execSQL(createTemp);
        db.execSQL("insert into tempTable select * from " + HIST_TABLE + ";");
        db.execSQL("drop table " + HIST_TABLE + ";");
        onCreate(db);
        db.execSQL("insert into " + HIST_TABLE + " select * from tempTable;");
        db.execSQL("drop table tempTable;");
    }

}
