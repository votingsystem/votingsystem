/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.votingsystem.android.contentprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;

import static org.votingsystem.util.LogUtils.LOGD;

// Content Provider for RSS feed information. Each row describes a single
// RSS feed. See the public static constants at the end of this class
// to learn what each record contains.
public class RssContentProvider extends ContentProvider {

    public static final String TAG = RssContentProvider.class.getSimpleName();

    private static final int DATABASE_VERSION = 1;
    private static final String DB_NAME = "voting_system_rss.db";
    static final String TABLE_NAME = "rss";

    private static final int ALL_ITEMS = 1;
    private static final int SPECIFIC_ITEM = 2;

    public static final String ID_COL = "_id";
    public static final String URL_COL = "url";
    public static final String TITLE_COL = "title";
    public static final String HAS_BEEN_READ_COL = "hasbeenread";
    public static final String CONTENT_COL = "rawcontent";
    public static final String LAST_UPDATED_COL = "lastupdated";
    public static final String DEFAULT_SORT_ORDER = TITLE_COL + " DESC";

    private SQLiteDatabase database;

    private static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("votingsysten.org", "rssitem", ALL_ITEMS);
        URI_MATCHER.addURI("votingsysten.org", "rssitem/#", SPECIFIC_ITEM);
    }

    public static final String AUTHORITY = "votingsysten.org/rssitem";
    // Here's the public URI used to query for RSS items.
    public static final Uri CONTENT_URI = Uri.parse( "content://" + AUTHORITY);

    @Override public boolean onCreate() {
        // First we need to open the database. If this is our first time,
        // the attempt to retrieve a database will throw
        // FileNotFoundException, and we will then create the database.
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
        try{
            database = databaseHelper.getWritableDatabase();
        } catch (Exception ex) {
            return false;
        }
        if(database == null) return false;
        else return true;
    }

    // Convert the URI into a custom MIME type.
    // Our UriMatcher will parse the URI to decide whether the
    // URI is for a single item or a list.
    @Override public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)){
            case ALL_ITEMS:
                return "vnd.android.cursor.dir/rssitem"; // List of items.
            case SPECIFIC_ITEM:
                return "vnd.android.cursor.item/rssitem"; // Specific item.
            default:
                return null;
        }
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // We won't bother checking the validity of params here, but you should!
        String groupBy = null;
        String having = null;
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        qBuilder.setTables(TABLE_NAME);
        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
        if((URI_MATCHER.match(uri)) == SPECIFIC_ITEM){
            qBuilder.appendWhere("id=" + ContentUris.parseId(uri));
        }
        // Set sort order. If none specified, use default.
        if(TextUtils.isEmpty(sortOrder)) sortOrder =DEFAULT_SORT_ORDER;
        Cursor c = qBuilder.query(database,
                projection,
                selection,
                selectionArgs,
                groupBy,
                having,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        int updateCount = 0;
        switch (URI_MATCHER.match(uri)){
            case ALL_ITEMS:
                updateCount = database.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            case SPECIFIC_ITEM:
                updateCount = database.update(TABLE_NAME, values, ID_COL +
                        " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        // Notify any listeners and return the updated row count.
        getContext().getContentResolver().notifyChange(uri, null);
        return updateCount;
    }

    @Override public Uri insert(Uri requestUri, ContentValues initialValues) {
        // NOTE Argument checking code omitted. Check your parameters! Check that
        // your row addition request succeeded!
        Uri newUri = null;
        if(initialValues != null) {
            long rowId = -1;
            rowId = database.insert(TABLE_NAME, null, initialValues);
            newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
        }
        // Notify any listeners and return the URI of the new row.
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return newUri;
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        int rowCount = database.delete(TABLE_NAME, ID_COL + " = ?",
                new String[]{String.valueOf(ContentUris.parseId(uri))});
        // Notify any listeners and return the deleted row count.
        getContext().getContentResolver().notifyChange(uri, null);
        return rowCount;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        public static final String TAG = "DatabaseHelper";

        private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_NAME + "(" +
                ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                URL_COL + " TEXT," +
                TITLE_COL + " TEXT," +
                HAS_BEEN_READ_COL + " BOOLEAN DEFAULT 0," +
                CONTENT_COL + " TEXT," +
                LAST_UPDATED_COL + " INTEGER DEFAULT 0);";

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DATABASE_VERSION);
            File dbFile = context.getDatabasePath(DB_NAME);
            LOGD(TAG + ".DatabaseHelper", "dbFile.getAbsolutePath(): " + dbFile.getAbsolutePath());
        }

        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            // Don't have any upgrades yet, so if this gets called for some reason we'll
            // just drop the existing table, and recreate the database with the
            // standard method.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + ";");
        }


        @Override public void onCreate(SQLiteDatabase db){
            try{
                db.execSQL(DATABASE_CREATE);
                LOGD(TAG + ".onCreate()", "database created");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
