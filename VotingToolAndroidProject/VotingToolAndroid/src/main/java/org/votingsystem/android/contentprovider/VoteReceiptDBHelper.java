package org.votingsystem.android.contentprovider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONException;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VoteReceiptDBHelper extends SQLiteOpenHelper {

	public static final String TAG = "VoteReceiptDBHelper";
	
	private static final int DATABASE_VERSION = 1;
	private static final String DB_NAME               = "voting_system_vote_receipt.db";
	static final String TABLE_NAME                    = "vote_receipt";
	static final String ID_COL                        = "_id";
	static final String KEY_COL                       = "key";
	static final String SMIME_COL                     = "voteReceipt";
	static final String CANCEL_VOTE_RECEIPT_SMIME_COL = "cancelVoteReceipt";
	static final String SERIALIZED_OBJECT_COL         = "serializedObject";
	static final String ENCRYPTED_KEY_COL             = "encryptedKey";
	static final String TIMESTAMP_CREATED_COL         = "timestampCreated";
	static final String TIMESTAMP_UPDATED_COL         = "timestampUpdated";

	private static final String[] ID_DETAIL_COL_PROJECTION = { SMIME_COL, SERIALIZED_OBJECT_COL };
	
	public VoteReceiptDBHelper(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
		File dbFile = context.getDatabasePath(DB_NAME);
		Log.d(TAG + ".onCreate(...)" , "dbFile.getAbsolutePath(): " + dbFile.getAbsolutePath());
	}

	@Override public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( "	+
				ID_COL                        + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				KEY_COL                       + " TEXT NOT NULL, " + 
				SMIME_COL                     + " blob, " + 
				TIMESTAMP_CREATED_COL         + " INTEGER, " +
				TIMESTAMP_UPDATED_COL         + " INTEGER, " +
				SERIALIZED_OBJECT_COL         + " blob, " +
				CANCEL_VOTE_RECEIPT_SMIME_COL + " blob, " +
				ENCRYPTED_KEY_COL             + " blob);";
		Log.d(TAG + ".onCreate(...)", "");
		db.execSQL(sql);
	}

	@Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
		Log.d(TAG + ".onUpgrade(...)", "oldV: " + oldV + " - newV: " + newV);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);// Create tables again
	}
	  
	//returns the receipt id in the database
	public Long insertVoteReceipt(VoteVS voteVS) throws
		JSONException, IOException, MessagingException {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		String receiptKey = StringUtils.getNormalized(voteVS.getEventURL());

		if(voteVS.getVoteReceipt() != null) {
	        values.put(SMIME_COL, voteVS.getVoteReceipt().getBytes());
		}
		if(voteVS.getCancelVoteReceipt() != null) {
			values.put(CANCEL_VOTE_RECEIPT_SMIME_COL, 
					voteVS.getCancelVoteReceipt().getBytes());
		}
		if(voteVS.getEncryptedKey()!= null) {
			values.put(ENCRYPTED_KEY_COL, voteVS.getEncryptedKey());
		}
		values.put(KEY_COL, receiptKey);
		values.put(SERIALIZED_OBJECT_COL, StringUtils.serializeObjectToString(voteVS).getBytes());
	    values.put(TIMESTAMP_CREATED_COL, System.currentTimeMillis());
	    values.put(TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
	    //From SQLite.org: If a table contains a column of type 
	    //INTEGER PRIMARY KEY, then that column becomes an alias for the ROWID
		long rowId = db.insert(TABLE_NAME, null, values);
		db.close();
		Log.d(TAG + ".insertVoteReceipt(...)", " - inserted receipt: " + rowId);
		return new Long(rowId);
	}
	
	public void deleteVoteReceipt(VoteVS voteVS) {
		SQLiteDatabase db = this.getWritableDatabase();	
		
		int count = db.delete(TABLE_NAME, ID_COL + "=" + voteVS.getId(), null);
	    //int count = db.delete(TABLE_NAME, ID_COL + "=" + voteVS.getId(), new String[] {});
		
    	Log.d(TAG + ".deleteVoteReceipt(...)", " - voteVS.id: " + voteVS.getId() +
    			" - count: " + count);
        //db.delete("repos", "orgId=?", new String[] { Integer.toString(org.getId()) });
        
        //db.delete(DB_TABLE_NOTES, where, whereArgs);
		db.close();
	}

	public List<VoteVS> getVoteReceiptList() {
		Log.d(TAG + ".getVoteReceiptList(...)", " - getVoteReceiptList");
		List<VoteVS> voteVSList = new ArrayList<VoteVS>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_NAME;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		Log.d(TAG + ".getVoteReceiptList(...)", " - cursor.getCount(): " 
				+ cursor.getCount());
		if (cursor.moveToFirst()) {
			do {
				Long id = cursor.getLong(cursor.getColumnIndex(ID_COL));
				String receiptKey = cursor.getString(cursor.getColumnIndex(KEY_COL));
				byte[] smimeMessageBytes = cursor.getBlob(cursor.getColumnIndex(SMIME_COL));
				Long timestampCreated = cursor.getLong(cursor.getColumnIndex(TIMESTAMP_CREATED_COL));
				Long timestampUpdated = cursor.getLong(cursor.getColumnIndex(TIMESTAMP_UPDATED_COL));
				byte[] serializedVote = cursor.getBlob(cursor.getColumnIndex(SERIALIZED_OBJECT_COL));
				byte[] cancelVoteSmimeMessageBytes = cursor.getBlob(
                        cursor.getColumnIndex(CANCEL_VOTE_RECEIPT_SMIME_COL));
				byte[] encryptedKeyBytes = cursor.getBlob(cursor.getColumnIndex(ENCRYPTED_KEY_COL));
				Log.d(TAG + ".getVoteReceiptList(...)", " - reading receipt: " + id);
				/*Log.d(TAG + ".getVoteReceiptList(...)", " - reading receipt: " + id + 
						" -receiptKey: " + receiptKey + " - timestampCreated: " + timestampCreated + 
						" - timestampUpdated: " + timestampUpdated +
						" - jsonDataStr: " + jsonDataStr);*/
				try {
					VoteVS voteVS = (VoteVS) StringUtils.deSerializedObjectFromString(
                            new String(serializedVote));
					voteVS.setId(id);
					if(timestampCreated != null) 
						voteVS.setDateCreated(new Date(timestampCreated));
					if(timestampUpdated != null)
						voteVS.setDateUpdated(new Date(timestampUpdated));
					if(smimeMessageBytes != null)
						voteVS.setVoteReceipt(new SMIMEMessageWrapper(null,
                                new ByteArrayInputStream(smimeMessageBytes), null));
					if(cancelVoteSmimeMessageBytes != null) {
						SMIMEMessageWrapper smimeMessageWrapper = new SMIMEMessageWrapper(null,
								new ByteArrayInputStream(cancelVoteSmimeMessageBytes), null);
						voteVS.setCancelVoteReceipt(smimeMessageWrapper);
					}
					if(encryptedKeyBytes != null) {
						voteVS.setEncryptedKey(encryptedKeyBytes);
					}
					voteVSList.add(voteVS);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			} while (cursor.moveToNext());
		}
		close(cursor, db);
		return voteVSList;
	}

	public void updateVoteReceipt(VoteVS voteVS)
			throws IOException, MessagingException, JSONException {
		Log.d(TAG + ".updateVoteReceipt(...)", " - updateVoteReceipt");
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		if(voteVS.getVoteReceipt() != null)
	        values.put(SMIME_COL, voteVS.getVoteReceipt().getBytes());
		if(voteVS.getCancelVoteReceipt() != null)
			values.put(CANCEL_VOTE_RECEIPT_SMIME_COL, 
					voteVS.getCancelVoteReceipt().getBytes());
		values.put(SERIALIZED_OBJECT_COL, StringUtils.serializeObjectToString(voteVS).getBytes());
	    values.put(TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
		db.update(TABLE_NAME, values, ID_COL + " = ?",new String[] {String.valueOf(voteVS.getId())});
		db.close();
	}

  private static void close(Cursor cursor, SQLiteDatabase database) {
	    if (cursor != null) cursor.close();
	    if (database != null) database.close();
  }

}