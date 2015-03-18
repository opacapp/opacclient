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
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.geeksfactory.opacclient.objects.AccountData;

public class AccountDatabase extends SQLiteOpenHelper {

    public static final String[] COLUMNS = {"id", "bib", "label", "name",
            "password", "cached", "pendingFees", "validUntil", "warning"};
    public static final String[] COLUMNS_NOTIFIED = {"id", "account",
            "timestamp"};
    // CHANGE THIS
    public static final Map<String, String> COLUMNS_LENT;
    public static final Map<String, String> COLUMNS_RESERVATIONS;
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put(AccountData.KEY_LENT_AUTHOR, "author");
        aMap.put(AccountData.KEY_LENT_BARCODE, "barcode");
        aMap.put(AccountData.KEY_LENT_BRANCH, "branch");
        aMap.put(AccountData.KEY_LENT_DEADLINE, "deadline");
        aMap.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, "deadline_ts");
        aMap.put(AccountData.KEY_LENT_LENDING_BRANCH, "lending_branch");
        aMap.put(AccountData.KEY_LENT_LINK, "link");
        aMap.put(AccountData.KEY_LENT_RENEWABLE, "renewable");
        aMap.put(AccountData.KEY_LENT_DOWNLOAD, "download");
        aMap.put(AccountData.KEY_LENT_FORMAT, "format");
        aMap.put(AccountData.KEY_LENT_STATUS, "status");
        aMap.put(AccountData.KEY_LENT_TITLE, "title");
        aMap.put(AccountData.KEY_LENT_ID, "itemid");
        COLUMNS_LENT = Collections.unmodifiableMap(aMap);

        Map<String, String> bMap = new HashMap<String, String>();
        bMap.put(AccountData.KEY_RESERVATION_AUTHOR, "author");
        bMap.put(AccountData.KEY_RESERVATION_BRANCH, "branch");
        bMap.put(AccountData.KEY_RESERVATION_CANCEL, "cancel");
        bMap.put(AccountData.KEY_RESERVATION_READY, "ready");
        bMap.put(AccountData.KEY_RESERVATION_EXPIRE, "expire");
        bMap.put(AccountData.KEY_RESERVATION_TITLE, "title");
        bMap.put(AccountData.KEY_RESERVATION_BOOKING, "bookingurl");
        bMap.put(AccountData.KEY_RESERVATION_ID, "itemid");
        COLUMNS_RESERVATIONS = Collections.unmodifiableMap(bMap);
    }
    public static final String TABLENAME_ACCOUNTS = "accounts";
    public static final String TABLENAME_LENT = "accountdata_lent";
    public static final String TABLENAME_RESERVATION = "accountdata_reservations";
    public static final String TABLENAME_NOTIFIED = "notified";
    private static final String DATABASE_NAME = "accounts.db";
    private static final int DATABASE_VERSION = 20; // REPLACE ONUPGRADE IF YOU

    public AccountDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "
                + "accounts ( id integer primary key autoincrement,"
                + " bib text," + " label text," + " name text,"
                + " password text," + " cached integer," + " pendingFees text,"
                + " validUntil text," + " warning text" + ");");
        db.execSQL("create table " + "accountdata_lent ( account integer, "
                + "title text," + "barcode text," + "author text,"
                + "deadline text," + "deadline_ts integer," + "status text,"
                + "branch text," + "lending_branch text," + "link text,"
                + "itemid text," + "renewable text," + "format text,"
                + "download text" + ");");
        db.execSQL("create table "
                + "accountdata_reservations ( account integer, "
                + "title text," + "author text," + "ready text,"
                + "branch text," + "cancel text," + "expire text,"
                + "itemid text," + "bookingurl text);");
        db.execSQL("create table "
                + "notified ( id integer primary key autoincrement, "
                + "account integer, " + "timestamp integer);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Provide something here if you change the database version

        if (oldVersion < 2) {
            // App version 2.0.0-alpha to 2.0.0, adding tables for account data
            db.execSQL("create table " + "accountdata_lent ( account integer, "
                    + "title text," + "barcode text," + "author text,"
                    + "deadline text," + "deadline_ts integer,"
                    + "status text," + "branch text," + "lending_branch text,"
                    + "link text" + ");");
            db.execSQL("create table "
                    + "accountdata_reservations ( account integer, "
                    + "title text," + "author text," + "ready text,"
                    + "branch text," + "cancel text" + ");");
        }
        if (oldVersion < 3) {
            // App version 2.0.0-alpha3 to 2.0.0-alpha4, adding tables for
            // account data
            db.execSQL("alter table accounts add column cached integer");
        }
        if (oldVersion == 3) {
            // App version 2.0.0-alpha4 to 2.0.0-alpha5, adding tables for
            // account data
            db.execSQL("alter table accountdata_lent add column deadline_ts integer");
        }
        if (oldVersion < 7) {
            // App version 2.0.5-1 to 2.0.6, adding "expire" to reservations
            try {
                db.execSQL("alter table accountdata_reservations add column expire text");
            } catch (SQLiteException sqle) {
                sqle.printStackTrace();
            }
        }
        if (oldVersion < 8) {
            // App version 2.0.6 to 2.0.7
            db.execSQL("create table "
                    + "notified ( id integer primary key autoincrement, "
                    + "account integer, " + "timestamp integer);");
        }
        if (oldVersion < 9) {
            // App version 2.0.14 to 2.0.15
            db.execSQL("alter table accountdata_lent add column download text");
        }
        if (oldVersion < 11) {
            // App version 2.0.15 to 2.0.16
            db.execSQL("alter table accountdata_reservations add column bookingurl text");
        }
        if (oldVersion < 12) {
            // App version 2.0.17 to 2.0.18
            db.execSQL("alter table accounts add column pendingFees text");
        }
        if (oldVersion < 13) {
            // App version 2.0.23 to 2.0.24
            db.execSQL("alter table accounts add column validUntil text");
        }
        if (oldVersion < 15) {
            // App version 2.0.23 to 2.0.24
            db.execSQL("alter table accountdata_lent add column format text");
        }
        if (oldVersion < 16) {
            // App version 2.1.1 to 3.0.0beta
            db.execSQL("alter table accountdata_lent add column renewable text");
        }
        if (oldVersion < 17) {
            // App version 2.1.1 to 3.0.0beta
            db.execSQL("alter table accountdata_lent add column itemid text");
        }
        if (oldVersion < 18) {
            // App version 2.1.1 to 3.0.0beta
            db.execSQL("alter table accountdata_reservations add column itemid text");
        }
        if (oldVersion < 20) {
            // App version 3.0.1 to 3.0.2
            db.execSQL("alter table accounts add column warning text");
        }


    }

}
