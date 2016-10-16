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

public class AccountDatabase extends SQLiteOpenHelper {

    private static AccountDatabase instance;
    public static final String[] COLUMNS = {"id", "bib", "label", "name",
            "password", "cached", "pendingFees", "validUntil", "warning", "passwordValid"};
    public static final String[] COLUMNS_ALARMS = {"id", "deadline", "media", "alarm",
            "notified", "finished"};
    // CHANGE THIS
    public static final String[] COLUMNS_LENT = {"id", "account", "title", "author", "format",
            "itemid", "status", "barcode", "deadline", "homebranch", "lending_branch",
            "prolong_data", "renewable", "download_data", "ebook", "mediatype", "cover"};
    public static final String[] COLUMNS_RESERVATIONS = {"id", "account", "title", "author",
            "format", "itemid", "status", "ready", "expiration", "branch", "cancel_data",
            "booking_data", "mediatype", "cover"};
    public static final String TABLENAME_ACCOUNTS = "accounts";
    public static final String TABLENAME_LENT = "accountdata_lent";
    public static final String TABLENAME_RESERVATION = "accountdata_reservations";
    public static final String TABLENAME_ALARMS = "alarms";
    private static final String DATABASE_NAME = "accounts.db";
    private static final int DATABASE_VERSION = 27; // REPLACE ONUPGRADE IF YOU

    private AccountDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized AccountDatabase getInstance(Context context) {
        if (instance == null) instance = new AccountDatabase(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "
                + "accounts ( id integer primary key autoincrement,"
                + " bib text," + " label text," + " name text,"
                + " password text," + " cached integer," + " pendingFees text,"
                + " validUntil text," + " warning text," + " passwordValid integer" + ");");
        db.execSQL(
                "create table " + "accountdata_lent (" + "id integer primary key autoincrement," +
                        "account integer," + "title text," + "author text," + "format text," +
                        "itemid text," + "status text," + "barcode text," + "deadline text," +
                        "homebranch text," + "lending_branch text," + "prolong_data text," +
                        "renewable integer," + "download_data text," + "ebook integer," +
                        "mediatype text," + "cover text" + ");");
        db.execSQL("create table " + "accountdata_reservations (" +
                "id integer primary key autoincrement," + "account integer," + "title text," +
                "author text," + "format text," + "itemid text," + "status text," + "ready text," +
                "expiration text," + "branch text," + "cancel_data text," + "booking_data text," +
                "mediatype text," + "cover text" + ");");
        db.execSQL("create table " + "alarms (" + "id integer primary key autoincrement," +
                "deadline text," + "media text," + "alarm text," + "notified integer," +
                "finished integer" + ");");
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
        if (oldVersion < 21) {
            // App version 4.1.11 to 4.2.0
            // KEY_RESERVATION_FORMAT existed before but was missing in the DB
            db.execSQL("alter table accountdata_reservations add column format text");
        }
        if (oldVersion < 22) {
            // App version 4.2.0 to 4.2.1
            // We added KEY_RESERVATION_FORMAT to onUpgrade but didn't in onCreate,
            // so we need to fix this by adding it again if it does not exist
            try {
                db.execSQL("alter table accountdata_reservations add column format text");
            } catch (Exception e) {
                // it already exists, do nothing
            }
        }
        if (oldVersion < 23) {
            // Upgrade to new Notifications implementation using the "alarms" table
            db.execSQL("drop table notified");
            db.execSQL("create table " + "alarms (" + " id integer primary key autoincrement," +
                    " deadline_ts integer," +
                    " media text," + " alarm_ts integer," + " notified integer," + " finished " +
                    "integer" + ");");
        }
        if (oldVersion < 24) {
            // Upgrade to new AccountItem implementaion
            db.execSQL("drop table accountdata_lent");
            db.execSQL("drop table accountdata_reservations");
            db.execSQL("create table " + "accountdata_lent (" +
                    "id integer primary key autoincrement," + "account integer," + "title text," +
                    "author text," + "format text," + "itemid text," + "status text," +
                    "barcode text," + "deadline text," + "homebranch text," +
                    "lending_branch text," + "prolong_data text," + "renewable integer," +
                    "download_data text," + "ebook integer" + ");");
            db.execSQL("create table " + "accountdata_reservations (" +
                    "id integer primary key autoincrement," + "account integer," + "title text," +
                    "author text," + "format text," + "itemid text," + "status text," +
                    "ready text," + "expiration text," + "branch text," + "cancel_data text," +
                    "booking_data text" + ");");

            db.execSQL("drop table alarms");
            db.execSQL("create table " + "alarms (" + "id integer primary key autoincrement," +
                    "deadline text," + "media text," + "alarm text," + "notified integer," +
                    "finished integer" + ");");
        }
        if (oldVersion < 26) {
            // App version 4.4.x to 4.5.0
            // We incremented by one, because I am stupid.
            try {
                db.execSQL("alter table accounts add column passwordValid integer");
            } catch (Exception e) {
                // it already exists, do nothing
            }
        }
        if (oldVersion < 27) {
            db.execSQL("alter table accountdata_lent add column mediatype text");
            db.execSQL("alter table accountdata_lent add column cover text");
            db.execSQL("alter table accountdata_reservations add column mediatype text");
            db.execSQL("alter table accountdata_reservations add column cover text");
        }
    }

}
