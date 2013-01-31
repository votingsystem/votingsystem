package org.sistemavotacion.android.ui;

import java.util.ArrayList;
import java.util.List;

import org.sistemavotacion.android.FragmentTabsPager;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.VotingEventScreen;
import org.sistemavotacion.android.db.VoteReceiptDBHelper;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.util.DateUtils;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class VoteReceiptListScreen extends ListActivity {
	
	public static final String TAG = "VoteReceiptListScreen";
	
	protected VoteReceiptDBHelper db;
	List<VoteReceipt> voteReceiptList;
	ReceiptListAdapter adapter;
	//ReceiptListAdapter mAdapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vote_receipt_list);
		db = new VoteReceiptDBHelper(this);
		try {
			voteReceiptList = db.getVoteReceiptList();	
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		Log.d(TAG + ".onCreate(...) ", " - voteReceiptList.size(): " + voteReceiptList.size());
		adapter = new ReceiptListAdapter(this);
		adapter.setData(voteReceiptList);
		try {
			getActionBar().setTitle(getString(R.string.receipt_list_screen_caption));
			getActionBar().setLogo(R.drawable.receipt_48);
		} catch(NoSuchMethodError ex) {
			Log.d(TAG + ".onCreate(...)", " --- android api 11 I doesn't have method 'setLogo'");
		}  
		getActionBar().setHomeButtonEnabled(true);
		setListAdapter(adapter);
		//ListView listView = (ListView) findViewById(R.id.list);
		//listView.setOnClickListener(this);
	}
	
	@Override public void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG +  ".onListItemClick", "Item clicked: " + id);
		VoteReceipt receipt = ((VoteReceipt) getListAdapter().getItem(position));
		String caption = receipt.getVoto().getAsunto();
		String msg = getString(R.string.receipt_options_dialog_msg);
		ReceiptOptionsDialog optionsDialog = ReceiptOptionsDialog.newInstance(caption, msg, receipt);
	    FragmentTransaction ft = getFragmentManager().beginTransaction();
	    Fragment prev = getFragmentManager().findFragmentByTag("optionsDialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    optionsDialog.show(ft, "optionsDialog");
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {  
		Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
		switch (item.getItemId()) {        
	    	case android.R.id.home:  
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", " - home - ");
	    		Intent intent = new Intent(this, FragmentTabsPager.class);   
	    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    		startActivity(intent);            
	    		return true;        
	    	default:            
	    		return super.onOptionsItemSelected(item);    
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.vote_receipt_list, menu);
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
			Log.d(TAG + "VoteReceiptLisAdapter", "voteReceipt: " +  String.valueOf(voteReceipt.getId()));
			return convertView;
		}

	}

    public static class ReceiptListAdapter extends ArrayAdapter<VoteReceipt> {
    	
        private final LayoutInflater mInflater;

        public ReceiptListAdapter(Context context) {
            super(context, R.layout.row_evento);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<VoteReceipt> data) {
            clear();
            if (data != null) {
                for (VoteReceipt receipt : data) {
                    add(receipt);
                }
            }
        }

        /**
         * Populate new items in the list.
         */
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.row_receipt, parent, false);
            } else {
                view = convertView;
            }
            VoteReceipt voteReceipt = getItem(position);
            if (voteReceipt != null) {
            	LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
            	linearLayout.setBackgroundColor(Color.WHITE);
                TextView subject = (TextView) view.findViewById(R.id.event_subject);
                TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
                TextView author = (TextView) view.findViewById(R.id.event_author);
                
                subject.setText(voteReceipt.getVoto().getAsunto());
                String dateInfoStr = null;
                ImageView imgView = (ImageView)view.findViewById(R.id.event_state_icon);
                if(DateUtils.getTodayDate().after(voteReceipt.getVoto().getFechaFin())) {
                	imgView.setImageResource(R.drawable.closed);
                	dateInfoStr = "<b>" + getContext().getString(R.string.closed_upper_lbl) + "</b> - " + 
                			"<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " + 
                			DateUtils.getShortSpanishStringFromDate(
                					voteReceipt.getVoto().getFechaInicio()) + " - " + 
        					"<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " + 
                			DateUtils.getShortSpanishStringFromDate(voteReceipt.getVoto().getFechaFin());
                } else {
                	imgView.setImageResource(R.drawable.open);
                	dateInfoStr = "<b>" + getContext().getString(R.string.remain_lbl, 
            				DateUtils.getElpasedTimeHoursFromNow(voteReceipt.getVoto().getFechaFin()))  +"</b>";
                }
                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);

                
  
                
                
                Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - fecha fin: " + 
                		voteReceipt.getVoto().getFechaFin());
                
                /*switch(evento.getEstadoEnumValue()) {
	                case ACTIVO:
	                	imgView.setImageResource(R.drawable.open);
	                	dateInfoStr = "<b>" + getContext().getString(R.string.remain_lbl, 
                				DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin()))  +"</b>";
	                	break;
	                case PENDIENTE_COMIENZO:
	                	imgView.setImageResource(R.drawable.pending);
	                	dateInfoStr = "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " + 
	                			DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " + 
            					"<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " + 
	                			DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
	                	break;
	                case FINALIZADO:
	                	imgView.setImageResource(R.drawable.closed);
	                	dateInfoStr = "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " + 
	                			DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " + 
            					"<b>" + Aplicacion.INSTANCIA.
		                		getContext().getString(R.string.fin_lbl) + "</b>: " + 
	                			DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
	                	break;
                }
                */
                Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - Usuario: " +
                		voteReceipt.getVoto().getUsuario());
                if(voteReceipt.getVoto().getUsuario() != null && !"".equals(
                		voteReceipt.getVoto().getUsuario().getNombreCompleto())) {
	                String authorStr =  "<b>" + getContext().getString(R.string.author_lbl) + "</b>: " + 
	                		voteReceipt.getVoto().getUsuario().getNombreCompleto();
	                author.setText(Html.fromHtml(authorStr));
                } else author.setVisibility(View.GONE);
            }
            return view;
        }
    }
	
	
}