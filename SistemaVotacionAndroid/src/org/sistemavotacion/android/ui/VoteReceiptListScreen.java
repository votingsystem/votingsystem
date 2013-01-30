package org.sistemavotacion.android.ui;

import java.util.ArrayList;
import java.util.List;

import org.sistemavotacion.android.R;
import org.sistemavotacion.android.R.id;
import org.sistemavotacion.android.R.layout;
import org.sistemavotacion.android.db.VoteReceiptDBHelper;
import org.sistemavotacion.modelo.VoteReceipt;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class VoteReceiptListScreen extends Activity {
	
	public static final String TAG = "VoteReceiptListScreen";
	
	protected VoteReceiptDBHelper db;
	List<VoteReceipt> voteReceiptList;
	VoteReceiptLisAdapter adapt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vote_receipt_list);
		db = new VoteReceiptDBHelper(this);
		try {
			voteReceiptList = db.getVoteReceiptList();	
		} catch(Exception ex) {
			Log.e(TAG + ".onCreate(...) ", ex.getMessage(), ex);
		}
		Log.d(TAG + ".onCreate(...) ", " - voteReceiptList.size(): " + voteReceiptList.size());
		adapt = new VoteReceiptLisAdapter(this, R.layout.receipt_list_inner_view, voteReceiptList);
		ListView listReceipts = (ListView) findViewById(R.id.listView1);
		listReceipts.setAdapter(adapt);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_vote_receipt_list, menu);
		return true;
	}

	private class VoteReceiptLisAdapter extends ArrayAdapter<VoteReceipt> {

		Context context;
		List<VoteReceipt> voteReceiptList = new ArrayList<VoteReceipt>();
		int layoutResourceId;

		public VoteReceiptLisAdapter(Context context, int layoutResourceId,
				List<VoteReceipt> receipts) {
			super(context, layoutResourceId, receipts);
			this.layoutResourceId = layoutResourceId;
			this.voteReceiptList = receipts;
			this.context = context;
		}

		/**
		 * This method will DEFINe what the view inside the list view will
		 * finally look like Here we are going to code that the checkbox state
		 * is the status of task and check box text is the task name
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			VoteReceipt voteReceipt = voteReceiptList.get(position);
			Log.d(TAG + "VoteReceiptLisAdapter","voteReceipt: " +  String.valueOf(voteReceipt.getId()));
			return convertView;
		}

	}

}