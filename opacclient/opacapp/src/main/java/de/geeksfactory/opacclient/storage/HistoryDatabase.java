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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class HistoryDatabase extends SQLiteOpenHelper {

    public static final String HIST_TABLE = "historyTable";

    public static final String HIST_COL_HISTORY_ID = "historyId";
    public static final String HIST_COL_MEDIA_NR = "medianr";
    public static final String HIST_COL_BIB = "bib";
    public static final String HIST_COL_TITLE = "title";
    public static final String HIST_COL_AUTHOR = "author";
    public static final String HIST_COL_FORMAT = "format";
    public static final String HIST_COL_COVER = "cover";
    public static final String HIST_COL_MEDIA_TYPE = "mediatype";
    public static final String HIST_COL_FIRST_DATE = "firstDate";
    public static final String HIST_COL_LAST_DATE = "lastDate";
    public static final String HIST_COL_PROLONG_COUNT = "prolongCount";
    public static final String HIST_COL_DEADLINE = "deadline";
    public static final String HIST_COL_LENDING = "lending";

    public static final String HIST_WHERE_HISTORY_ID = HIST_COL_HISTORY_ID + " = ?";
    public static final String HIST_WHERE_LIB = "bib = ?";

    public static final String HIST_WHERE_LIB_LENDING = "bib = ? AND lending = 1";
    public static final String HIST_WHERE_TITLE_LIB = "bib = ? AND "
            + HIST_COL_MEDIA_NR + " IS NULL AND "
            + HIST_COL_TITLE + " = ?";
    public static final String HIST_WHERE_LIB_MEDIA_NR = "bib = ? AND "
            + HIST_COL_MEDIA_NR + " = ?";
    public static final String HIST_WHERE_LIB_TITLE_AUTHOR_TYPE = "bib = ? AND "
            + HIST_COL_TITLE + " = ? AND "
            + HIST_COL_AUTHOR + " = ? AND "
            + HIST_COL_MEDIA_TYPE + " = ?";
    public static final String[] COLUMNS =
            {HIST_COL_HISTORY_ID + " AS _id", // wg. android.widget.CursorAdapter
                    // siehe https://developer.android.com/reference/android/widget/CursorAdapter
                    // .html
                    HIST_COL_FIRST_DATE,
                    HIST_COL_LAST_DATE,
                    HIST_COL_LENDING,
                    HIST_COL_MEDIA_NR,
                    HIST_COL_BIB,
                    HIST_COL_TITLE,
                    HIST_COL_AUTHOR,
                    HIST_COL_FORMAT,
                    HIST_COL_COVER,
                    HIST_COL_MEDIA_TYPE,
                    "homeBranch",
                    "lendingBranch",
                    "ebook",
                    "barcode",
                    HIST_COL_DEADLINE,
                    HIST_COL_PROLONG_COUNT
            };

    private static final String DATABASE_CREATE = "create table historyTable (\n" +
            "\t" + HIST_COL_HISTORY_ID + " integer primary key autoincrement,\n" +
            "\t" + HIST_COL_FIRST_DATE + " date,\n" +
            "\t" + HIST_COL_LAST_DATE + " date,\n" +
            "\t" + HIST_COL_LENDING + " boolean,\n" +
            "\t" + HIST_COL_MEDIA_NR + " text,\n" +
            "\t" + HIST_COL_BIB + " text,\n" +
            "\t" + HIST_COL_TITLE + " text,\n" +
            "\t" + HIST_COL_AUTHOR + " text,\n" +
            "\t" + HIST_COL_FORMAT + " text,\n" +
            "\t" + HIST_COL_COVER + " text,\n" +
            "\t" + HIST_COL_MEDIA_TYPE + " text,\n" +
            "\thomeBranch text,\n" +
            "\tlendingBranch text,\n" +
            "\tebook boolean,\n" +
            "\tbarcode text,\n" +
            "\t" + HIST_COL_DEADLINE + " date,\n" +
            "\t" + HIST_COL_PROLONG_COUNT + " integer\n" +
            ");";


    private static final String DATABASE_NAME = "history.db";
//  private static final int DATABASE_VERSION = 1; // initial
    private static final int DATABASE_VERSION = 2; // Column status removed

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

        // 1. rename historyTable to oldTable
        db.execSQL("alter table " + HIST_TABLE + " rename to oldTable ;");

        // 2. historyTable neu anlegen
        onCreate(db);

        // insert/select der relevanten Spalten vorbereiten
        StringBuffer sb = new StringBuffer("insert into " + HIST_TABLE + " select ");
        sb.append(HIST_COL_HISTORY_ID);
        sb.append(", ");
        // i = 1, damit "as _id" nicht verwendet wird
        for (int i = 1; i < COLUMNS.length; i++) {
            sb.append(COLUMNS[i]);
            sb.append(", ");
        }
        // letztes Komma entfernen
        sb.setLength(sb.length()-2);
        sb.append(" from oldTable ;");
        final String insertHistory = sb.toString();
        Log.i(HistoryDatabase.class.getName(), "insert history: " + insertHistory);

        // 3. Daten von oldTable nach (neuem) historyTable kopieren
        db.execSQL(insertHistory);

        // 4. drop oldTable
        db.execSQL("drop table oldTable;");
    }
}
