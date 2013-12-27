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
import android.util.Log;
import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeContentProvider extends ContentProvider {

    public static final String TAG = "RepresentativeContentProvider";

    private static final int DATABASE_VERSION = 1;
    private static final String DB_NAME = "voting_system_representatives.db";
    static final String TABLE_REPRESENTATIVES = "representatives";

    public static final String ID = "id";
    public static final String URL = "url";
    static final String JSON_DATA_COL = "jsonData";
    static final String IMAGE_COL = "image";
    static final String TIMESTAMP_CREATED_COL = "timestampCreated";
    static final String TIMESTAMP_UPDATED_COL = "timestampUpdated";
    public static final String DEFAULT_SORT_ORDER = ID + " DESC";

    private SQLiteDatabase database;

    private static final int ALL_REPRESENTATIVES = 1;
    private static final int SPECIFIC_REPRESENTATIVES = 2;

    private static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("votingsysten.org", "representative", ALL_REPRESENTATIVES);
        URI_MATCHER.addURI("votingsysten.org", "representative/#", SPECIFIC_REPRESENTATIVES);
    }

    // Here's the public URI used to query for representative items.
    public static final Uri CONTENT_URI = Uri.parse( "content://votingsysten.org/representative");

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

    // Convert the URI into a custom MIME type. Our UriMatcher will parse the URI to decide
    // whether the URI is for a single item or a list.
    @Override public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)){
            case ALL_REPRESENTATIVES:
                return "vnd.android.cursor.dir/representative"; // List of items.
            case SPECIFIC_REPRESENTATIVES:
                return "vnd.android.cursor.item/representative"; // Specific item.
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
        qBuilder.setTables(TABLE_REPRESENTATIVES);
        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
        if((URI_MATCHER.match(uri)) == SPECIFIC_REPRESENTATIVES){
            qBuilder.appendWhere("id=" + ContentUris.parseId(uri));
        }
        // Set sort order. If none specified, use default.
        if(TextUtils.isEmpty(sortOrder)) sortOrder = DEFAULT_SORT_ORDER;
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
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        int updateCount = database.update(TABLE_REPRESENTATIVES, values,
                whereClause, whereArgs);
        // Notify any listeners and return the updated row count.
        getContext().getContentResolver().notifyChange(uri, null);
        return updateCount;
    }

    @Override public Uri insert(Uri requestUri, ContentValues initialValues) {
        // NOTE Argument checking code omitted. Check your parameters! Check that
        // your row addition request succeeded!
        long rowId = -1;
        rowId = database.insert(TABLE_REPRESENTATIVES, null, initialValues);
        Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
        // Notify any listeners and return the URI of the new row.
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return newUri;
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        int rowCount = database.delete(TABLE_REPRESENTATIVES, ID + " = ?",
                new String[]{String.valueOf(ContentUris.parseId(uri))});
        // Notify any listeners and return the deleted row count.
        getContext().getContentResolver().notifyChange(uri, null);
        return rowCount;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        public static final String TAG = "DatabaseHelper";

        private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_REPRESENTATIVES + "(" +
                ID + " INTEGER PRIMARY KEY, " +
                URL + " TEXT," +
                JSON_DATA_COL + " TEXT, " +
                IMAGE_COL + " blob, " +
                TIMESTAMP_UPDATED_COL + " INTEGER DEFAULT 0, " +
                TIMESTAMP_CREATED_COL + " INTEGER DEFAULT 0);";

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DATABASE_VERSION);
            File dbFile = context.getDatabasePath(DB_NAME);
            Log.d(TAG + ".DatabaseHelper(...)", "dbFile.getAbsolutePath(): " + dbFile.getAbsolutePath());
        }

        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            // Don't have any upgrades yet, so if this gets called for some reason we'll
            // just drop the existing table, and recreate the database with the
            // standard method.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REPRESENTATIVES + ";");
        }


        @Override public void onCreate(SQLiteDatabase db){
            try{
                db.execSQL(DATABASE_CREATE);
                Log.d(TAG + ".onCreate()", "database created");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
