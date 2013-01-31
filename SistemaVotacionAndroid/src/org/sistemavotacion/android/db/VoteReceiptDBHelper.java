package org.sistemavotacion.android.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

import org.json.JSONException;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.StringUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class VoteReceiptDBHelper extends SQLiteOpenHelper {

	public static final String TAG = "VoteReceiptDBHelper";
	
	private static final int DATABASE_VERSION = 2;
	private static final String DB_NAME               = "voting_system.db";
	static final String TABLE_NAME                    = "vote_receipt";
	static final String ID_COL                        = "id";
	static final String KEY_COL                       = "key";
	static final String SMIME_COL                     = "smime";
	static final String CANCEL_VOTE_RECEIPT_SMIME_COL = "cancelVoteRecepitSmime";
	static final String JSON_DATA_COL                 = "jsonData";
	static final String TIMESTAMP_CREATED_COL         = "timestampCreated";
	static final String TIMESTAMP_UPDATED_COL         = "timestampUpdated";

	private static final String[] ID_DETAIL_COL_PROJECTION = { SMIME_COL, JSON_DATA_COL };
	
	public VoteReceiptDBHelper(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
		File dbFile = context.getDatabasePath(DB_NAME);
		Log.d(TAG + ".onCreate(...)" , "dbFile.getAbsolutePath(): " + dbFile.getAbsolutePath());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( "	+ 
				ID_COL                        + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				KEY_COL                       + " TEXT NOT NULL, " + 
				SMIME_COL                     + " blob, " + 
				TIMESTAMP_CREATED_COL         + " INTEGER, " +
				TIMESTAMP_UPDATED_COL         + " INTEGER, " +
				JSON_DATA_COL                 + " blob, " +
				CANCEL_VOTE_RECEIPT_SMIME_COL + " blob);";
		Log.d(TAG + ".onCreate(...)", " - onCreate");
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
		Log.d(TAG + ".onUpgrade(...)", " - onUpgrade - oldV: " + oldV + 
				" - newV: " + newV);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		// Create tables again
		onCreate(db);
	}
	  
	public void addVoteReceipt(VoteReceipt voteReceipt) throws 
		JSONException, IOException, MessagingException {
		Log.d(TAG + ".addVoteReceipt(...)", " - addVoteReceipt");
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		String receiptKey = StringUtils.getCadenaNormalizada(
				voteReceipt.getEventoURL());
		
		Log.d(TAG + ".addVoteReceipt(...)", "=====================");
		Cursor cursor = db.query(TABLE_NAME, ID_DETAIL_COL_PROJECTION,  KEY_COL + "=?",
                new String[] { receiptKey }, null,  null,  TIMESTAMP_CREATED_COL + " DESC", "1");
		Log.d(TAG + ".addVoteReceipt(...)", " - cursor.getCount(): " + cursor.getCount());
		Log.d(TAG + ".addVoteReceipt(...)", "=====================");
		
		if(voteReceipt.getSmimeMessage() != null) {
	        values.put(SMIME_COL, voteReceipt.getSmimeMessage().getBytes());
		}
		if(voteReceipt.getCancelVoteReceipt() != null) {
			values.put(CANCEL_VOTE_RECEIPT_SMIME_COL, 
					voteReceipt.getCancelVoteReceipt().getBytes());
		}
		byte[] jsonDataBytes = null;
		String jsonDataStr = voteReceipt.toJSONString();
		Log.d(TAG + ".addVoteReceipt(...)", "===== jsonDataStr: " + jsonDataStr);
		if(jsonDataStr != null) jsonDataBytes = jsonDataStr.getBytes();
		values.put(KEY_COL, receiptKey);
		values.put(JSON_DATA_COL, jsonDataBytes);
	    values.put(TIMESTAMP_CREATED_COL, System.currentTimeMillis());
	    values.put(TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
		db.insert(TABLE_NAME, null, values);
		db.close();
	}
	
	public void deleteVoteReceipt(VoteReceipt voteReceipt) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		
		int count = db.delete(TABLE_NAME, ID_COL + "=" + voteReceipt.getId(), null);
	    //int count = db.delete(TABLE_NAME, ID_COL + "=" + voteReceipt.getId(), new String[] {});
		
    	Log.d(TAG + ".deleteVoteReceipt(...)", " - voteReceipt.id: " + voteReceipt.getId() + 
    			" - count: " + count);
        //db.delete("repos", "orgId=?", new String[] { Integer.toString(org.getId()) });
        
        //db.delete(DB_TABLE_NOTES, where, whereArgs);
	}

	public List<VoteReceipt> getVoteReceiptList() {
		Log.d(TAG + ".getVoteReceiptList(...)", " - getVoteReceiptList");
		List<VoteReceipt> voteReceiptList = new ArrayList<VoteReceipt>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_NAME;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		Log.d(TAG + ".getVoteReceiptList(...)", " - cursor.getColumnCount(): " 
				+ cursor.getColumnCount());
		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(0);
				String receiptKey = cursor.getString(1);
				byte[] smimeMessageBytes = cursor.getBlob(2);
				Long timestampCreated = cursor.getLong(3);
				Long timestampUPdated = cursor.getLong(4);
				byte[] jsonDataBytes = cursor.getBlob(5);
				byte[] cancelVoteSmimeMessageBytes = cursor.getBlob(6);
				String jsonDataStr =  new String(jsonDataBytes);
				Log.d(TAG + ".getVoteReceiptList(...)", " - reading receipt: " + id + 
						" -receiptKey: " + receiptKey + " - timestampCreated: " + timestampCreated + 
						" - timestampUPdated: " + timestampUPdated + 
						" - jsonDataStr: " + jsonDataStr);
				try {
					VoteReceipt voteReceipt = VoteReceipt.parse(jsonDataStr); 
					if(timestampCreated != null) 
						voteReceipt.setDateCreated(new Date(timestampCreated));
					if(timestampUPdated != null) 
						voteReceipt.setDateUpdated(new Date(timestampUPdated));
					if(smimeMessageBytes != null)
						voteReceipt.setSmimeMessage(new SMIMEMessageWrapper(null,
								new ByteArrayInputStream(smimeMessageBytes), null));
					if(cancelVoteSmimeMessageBytes != null)
						voteReceipt.setCancelVoteReceipt(new SMIMEMessageWrapper(null,
								new ByteArrayInputStream(cancelVoteSmimeMessageBytes), null));
					voteReceiptList.add(voteReceipt);
				}catch(Exception ex) {
					Log.e(TAG + ".getVoteReceiptList(...) ", ex.getMessage(), ex);
				}
			} while (cursor.moveToNext());
		}
		return voteReceiptList;
	}

	public void updateVoteReceipt(VoteReceipt voteReceipt) 
			throws IOException, MessagingException, JSONException {
		Log.d(TAG + ".updateVoteReceipt(...)", " - updateVoteReceipt");
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		String receiptKey = StringUtils.getCadenaNormalizada(
				voteReceipt.getEventoURL());
		if(voteReceipt.getSmimeMessage() != null) 
	        values.put(SMIME_COL, voteReceipt.getSmimeMessage().getBytes());
		if(voteReceipt.getCancelVoteReceipt() != null)
			values.put(CANCEL_VOTE_RECEIPT_SMIME_COL, 
					voteReceipt.getCancelVoteReceipt().getBytes());
		values.put(KEY_COL, receiptKey);
		values.put(JSON_DATA_COL, voteReceipt.toJSONString());
	    values.put(TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
		db.update(TABLE_NAME, values, ID_COL + " = ?",new String[] {String.valueOf(voteReceipt.getId())});
		db.close();
	}

}