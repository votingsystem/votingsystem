package org.votingsystem.android.contentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
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

import org.votingsystem.android.util.WebSocketMessage;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageContentProvider extends ContentProvider {

    public enum State {READED, NOT_READED}

    public static final String TAG = MessageContentProvider.class.getSimpleName();

    private static final int DATABASE_VERSION = 1;
    private static final String DB_NAME = "voting_system_messagevs.db";
    private static final String TABLE_NAME = "messagevs";
    public static final String AUTHORITY = "votingsystem.org.messagevs";

    public static final String ID_COL                = "_id";
    public static final String TYPE_COL              = "type";
    public static final String STATE_COL             = "state";
    public static final String JSON_COL              = "jsonCol";
    public static final String TIMESTAMP_CREATED_COL = "timestampCreated";
    public static final String TIMESTAMP_UPDATED_COL = "timestampUpdated";
    public static final String DEFAULT_SORT_ORDER = ID_COL + " DESC";

    private SQLiteDatabase database;

    private static final int ALL_ITEMS = 1;
    private static final int SPECIFIC_ITEM = 2;

    private static final String BASE_PATH = "messagevs";

    private static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH, ALL_ITEMS);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH + "/#", SPECIFIC_ITEM);
    }

    // Here's the public URI used to query for representative items.
    //public static final Uri CONTENT_URI = Uri.parse( "content://" +
    //        AUTHORITY + "/" + BASE_PATH);
    public static final Uri CONTENT_URI = Uri.parse( "content://" + AUTHORITY + "/" + BASE_PATH);

    public static Uri getMessageURI(Long messageId) {
        return Uri.parse( "content://" + AUTHORITY + "/" + BASE_PATH + "/" + messageId);
    }

    @Override public boolean onCreate() {
        // If database file isn't found this will throw a  FileNotFoundException, and we will
        // then create the database.
        //getContext().deleteDatabase(DB_NAME);
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
            case ALL_ITEMS:
                return "vnd.android.cursor.dir/messagevslist"; // List of items.
            case SPECIFIC_ITEM:
                return "vnd.android.cursor.item/messagevs"; // Specific item.
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
        if((URI_MATCHER.match(uri)) == SPECIFIC_ITEM){
            qBuilder.appendWhere(ID_COL + "=" + ContentUris.parseId(uri));
        }
        if(TextUtils.isEmpty(sortOrder)) sortOrder = DEFAULT_SORT_ORDER;
        Cursor cursor = qBuilder.query(database, projection, selection, selectionArgs,
                groupBy, having, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        values.put(MessageContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
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

    @Override public Uri insert(Uri requestUri, ContentValues values) {
        // NOTE Argument checking code omitted. Check your parameters! Check that
        // your row addition request succeeded!
        Uri newUri = null;
        if(values != null) {
            long rowId = -1;
            values.put(MessageContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
            values.put(MessageContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
            rowId = database.insert(TABLE_NAME, null, values);
            newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
        }
        // Notify any listeners and return the URI of the new row.
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return newUri;
    }

    public static Uri insert(ContentResolver contentResolver, WebSocketMessage socketMsg) {
        return contentResolver.insert(CONTENT_URI, getContentValues(socketMsg, State.NOT_READED));
    }

    public static ContentValues getContentValues(WebSocketMessage socketMsg, State state) {
        ContentValues values = new ContentValues();
        values.put(MessageContentProvider.STATE_COL, state.toString());
        values.put(MessageContentProvider.JSON_COL, socketMsg.getDecryptedJSON().toString());
        values.put(MessageContentProvider.TYPE_COL, socketMsg.getOperation().toString());
        values.put(MessageContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
        values.put(MessageContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
        return values;
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        String idColStr = String.valueOf(ContentUris.parseId(uri));
        Integer rowCount = null;
        if(selection == null) {
            rowCount = database.delete(TABLE_NAME, ID_COL + " = ?", new String[]{idColStr});
        } else  rowCount = database.delete(TABLE_NAME, selection, selectionArgs);
        // Notify any listeners and return the deleted row count.
        LOGD(TAG + ".delete", "messagevs id: " + idColStr);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowCount;
    }

    public static void deleteById(Long messageId, Context context) {
        context.getContentResolver().delete(MessageContentProvider.getMessageURI(
                messageId), null, null);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {


        private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_NAME + "(" +
                ID_COL                + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TYPE_COL              + " TEXT," +
                STATE_COL             + " TEXT," +
                JSON_COL              + " TEXT, " +
                TIMESTAMP_UPDATED_COL + " INTEGER DEFAULT 0, " +
                TIMESTAMP_CREATED_COL + " INTEGER DEFAULT 0);";

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DATABASE_VERSION);
            //File dbFile = context.getDatabasePath(DB_NAME);
            //LOGD(TAG + ".DatabaseHelper", "dbFile.getAbsolutePath(): " + dbFile.getAbsolutePath());
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
                LOGD(TAG + ".DatabaseHelper.onCreate", "Database created");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

}
