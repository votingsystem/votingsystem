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

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketContentProvider extends ContentProvider {

    public static final String TAG = "TicketContentProvider";

    //from http://www.buzzingandroid.com/2013/01/sqlite-insert-or-replace-through-contentprovider/
    public static final String SQL_INSERT_OR_REPLACE = "__sql_insert_or_replace__";

    private static final int DATABASE_VERSION = 1;
    private static final String DB_NAME       = "voting_system_ticket.db";
    private static final String TABLE_NAME    = "ticket";
    public static final String AUTHORITY      = "votingsystem.org.ticket";

    public static final String ID_COL                = "_id";
    public static final String TYPE_COL              = "type";
    public static final String URL_COL               = "url";
    public static final String STATE_COL             = "state";
    public static final String AMOUNT_COL            = "amount";
    public static final String CURRENCY_COL          = "currency";
    public static final String WEEK_LAPSE_COL        = "weekLapse";
    public static final String SERIALIZED_OBJECT_COL = "serializedObject";
    public static final String TIMESTAMP_CREATED_COL = "timestampCreated";
    public static final String TIMESTAMP_UPDATED_COL = "timestampUpdated";
    public static final String DEFAULT_SORT_ORDER = ID_COL + " DESC";

    private SQLiteDatabase database;

    private static final int ALL_ITEMS = 1;
    private static final int SPECIFIC_ITEM = 2;

    private static final String BASE_PATH = "ticket";

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

    public static Uri getTicketURI(Long receipt) {
        return Uri.parse( "content://" + AUTHORITY + "/" + BASE_PATH + "/" + receipt);
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
                return "vnd.android.cursor.dir/tickets"; // List of items.
            case SPECIFIC_ITEM:
                return "vnd.android.cursor.item/ticket"; // Specific item.
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
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        values.put(TicketContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
        int updateCount = database.update(TABLE_NAME, values,
                whereClause, whereArgs);
        // Notify any listeners and return the updated row count.
        getContext().getContentResolver().notifyChange(uri, null);
        return updateCount;
    }

    //byte[] base64encodedvoteCertPrivateKey = Encryptor.decryptMessage(
    //        vote.getEncryptedKey(), signerCert, signerPrivatekey);
    //PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
    //        Base64.decode(base64encodedvoteCertPrivateKey));
    //KeyFactory kf = KeyFactory.getInstance("RSA");
    //PrivateKey certPrivKey = kf.generatePrivate(keySpec);
    //vote.setCertVotePrivateKey(certPrivKey);

    @Override public Uri insert(Uri requestUri, ContentValues values) {
        // NOTE Argument checking code omitted. Check your parameters! Check that
        // your row addition request succeeded!
        Uri newUri = null;
        if(values != null) {
            long rowId = -1;
            boolean replace = false;
            values.put(TicketContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
            values.put(TicketContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
            if (values.containsKey(SQL_INSERT_OR_REPLACE)) {
                replace = values.getAsBoolean(SQL_INSERT_OR_REPLACE);
                // Clone the values object, so we don't modify the original.
                // This is not strictly necessary, but depends on your needs
                //values = new ContentValues( values );
                // Remove the key, so we don't pass that on to db.insert() or db.replace()
                values.remove( SQL_INSERT_OR_REPLACE );
            }
            if ( replace ) {
                rowId = database.replace(TABLE_NAME, null, values);
            } else {
                rowId = database.insert(TABLE_NAME, null, values);
            }
            newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
        }
        // Notify any listeners and return the URI of the new row.
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return newUri;
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        // NOTE Argument checking code omitted. Check your parameters!
        String idColStr = String.valueOf(ContentUris.parseId(uri));
        int rowCount = database.delete(TABLE_NAME, ID_COL + " = ?", new String[]{idColStr});
        // Notify any listeners and return the deleted row count.
        Log.d(TAG + ".delete(...)", "receipt id: " + idColStr);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowCount;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {


        private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_NAME + "(" +
                ID_COL                + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TYPE_COL              + " TEXT," +
                URL_COL               + " TEXT," +
                STATE_COL             + " TEXT," +
                AMOUNT_COL            + " TEXT," +
                CURRENCY_COL          + " TEXT," +
                WEEK_LAPSE_COL        + " TEXT," +
                SERIALIZED_OBJECT_COL + " blob, " +
                TIMESTAMP_UPDATED_COL + " INTEGER DEFAULT 0, " +
                TIMESTAMP_CREATED_COL + " INTEGER DEFAULT 0);";

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DATABASE_VERSION);
            //File dbFile = context.getDatabasePath(DB_NAME);
            //Log.d(TAG + ".DatabaseHelper(...)", "dbFile.getAbsolutePath(): " + dbFile.getAbsolutePath());
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
                Log.d(TAG + ".DatabaseHelper.onCreate(...)", "Database created");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

}
