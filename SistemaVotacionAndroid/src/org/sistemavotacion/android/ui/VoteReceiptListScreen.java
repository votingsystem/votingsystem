package org.sistemavotacion.android.ui;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;

import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle2.util.encoders.Base64;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.FragmentTabsPager;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.db.VoteReceiptDBHelper;
import org.sistemavotacion.android.service.SignService;
import org.sistemavotacion.android.service.ServiceListener;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class VoteReceiptListScreen extends FragmentActivity 
	implements CertPinDialogListener, ReceiptOperationsListener, 
	ServiceListener{
	
	public static final String TAG = "VoteReceiptListScreen";
	
	public enum Operation {CANCEL_VOTE};
	
	private static final String OPTIONS_DIALOG_ID = "optionsDialog";
	protected VoteReceiptDBHelper db;
	private List<VoteReceipt> voteReceiptList;
	private ReceiptListAdapter adapter;
	private Operation operation = null;
    private ProgressDialog progressDialog = null;
	private SignService signService = null;
	private VoteReceipt operationReceipt = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG + ".onCreate(...) ", " - onCreate ");
		setContentView(R.layout.vote_receipt_list);
		db = new VoteReceiptDBHelper(this);
		try {
			voteReceiptList = db.getVoteReceiptList();	
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		Log.d(TAG + ".onCreate(...) ", " - voteReceiptList.size(): " + voteReceiptList.size());
		adapter = new ReceiptListAdapter(this);
		if(voteReceiptList.size() > 0) 
			((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.GONE);
		adapter.setData(voteReceiptList);
		try {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setTitle(getString(R.string.receipt_list_screen_caption));
			getActionBar().setLogo(R.drawable.receipt_32);
		} catch(NoSuchMethodError ex) {
			setTitle(getString(R.string.receipt_list_screen_caption));
			Log.d(TAG + ".onCreate(...)", " --- android api 11 I doesn't have method 'setLogo'");
		}  
        final ListView listView = (ListView) findViewById(R.id.listView);
        OnItemClickListener clickListener = new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?>  l, View v, int position, long id) {
				VoteReceipt receipt = ((VoteReceipt) adapter.getItem(position));
				launchOptionsDialog(receipt);
			}};
		listView.setOnItemClickListener(clickListener);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
	}


	private void launchOptionsDialog(VoteReceipt receipt) {
		String caption = receipt.getVoto().getAsunto();
		String msg = getString(R.string.receipt_options_dialog_msg);
		ReceiptOptionsDialog optionsDialog = ReceiptOptionsDialog.newInstance(
				caption, msg, receipt, this);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(OPTIONS_DIALOG_ID);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    optionsDialog.show(ft, OPTIONS_DIALOG_ID);
	}
	

    private void showPinScreen(String message) {
    	CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    pinDialog.show(ft, "pinDialog");
    }
	
	private void refreshReceiptList() {
		Log.d(TAG + ".refreshReceiptList(...)", " --- refreshReceiptList");
		try {
			voteReceiptList = db.getVoteReceiptList();	
			if(voteReceiptList.size() == 0) 
				((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.VISIBLE);
			else ((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.GONE);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		dismissOptionsDialog();
		adapter.setData(voteReceiptList);
		adapter.notifyDataSetChanged();
		adapter.setNotifyOnChange (true);
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {  
		Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
		switch (item.getItemId()) {        
	    	case android.R.id.home:  
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", " - home - ");
	    		Intent intent = new Intent(this, FragmentTabsPager.class);   
	    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    		startActivity(intent);            
	    		this.finish();
	    		return true;        
	    	default:            
	    		return super.onOptionsItemSelected(item);    
		}
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.vote_receipt_list, menu);
		return true;
	}
	
    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", " - onDestroy");
    	unbindSignService();
    };
    
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
                TextView receiptState = (TextView) view.findViewById(R.id.receipt_state);
                
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
                if(voteReceipt.isCanceled()) {
                	Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - voteReceipt: " + voteReceipt.getId() 
                			+ " -position: " + position + " - isCanceled");
                	receiptState.setText(getContext().getString(R.string.canceled_lbl));
                	receiptState.setVisibility(View.VISIBLE);
                } else {
                	Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - voteReceipt: " + voteReceipt.getId() 
                			+ " -position: " + position);
                	receiptState.setVisibility(View.GONE);
                } 
                
                //Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - Usuario: " + voteReceipt.getVoto().getUsuario());
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
	
	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", " - caption: " 
				+ caption + "  - showMessage: " + message);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setMessage(message).show();
	}
    
	private void processCancelVote(char[] password) {   
        String subject = getString(R.string.cancel_vote_msg_subject); 
        String serverURL = ServerPaths.getURLAnulacionVoto(Aplicacion.CONTROL_ACCESO_URL);
        try {
			FileInputStream fis = openFileInput(KEY_STORE_FILE);
			byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
			X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);

			byte[] base64encodedvoteCertPrivateKey = Encryptor.decryptMessage(
					 operationReceipt.getEncryptedKey(), signerCert, signerPrivatekey);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
					Base64.decode(base64encodedvoteCertPrivateKey));
	        KeyFactory kf = KeyFactory.getInstance("RSA");
	        PrivateKey certPrivKey = kf.generatePrivate(keySpec);
			operationReceipt.setCertVotePrivateKey(certPrivKey);

            boolean isEncryptedResponse = true;
            
    		if(signService != null) signService.processSignature(null, operationReceipt.
	        		getVoto().getCancelVoteData(), subject, serverURL, this, 
	        		isEncryptedResponse, keyStoreBytes, password);	
        } catch(Exception ex) {
			ex.printStackTrace();
			showMessage(getString(R.string.error_lbl), 
					getString(R.string.pin_error_msg));
        }

	}
    
	ServiceConnection signServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG + ".signingServiceConnection.onServiceDisconnected()", 
					" - signingServiceConnection.onServiceDisconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG + ".signingServiceConnection.onServiceConnected()", 
					" - signingServiceConnection.onServiceConnected");
			signService = ((SignService.SignServiceBinder) service).getBinder();
		}
	};

	private void unbindSignService() {
		Log.d(TAG + ".unbindSignService()", "--- unbindSignService");
		if(signService != null)	unbindService(signServiceConnection);
	}
	
	@Override public void setPin(final String pin) {
		Log.d(TAG + ".setPin()", "--- setPin - operation: " + operation);
		if(pin != null) {
	        progressDialog = ProgressDialog.show(this, 
	        		getString(R.string.canceling_vote_caption), 
	        		getString(R.string.sending_data_lbl), true,
		            true, new DialogInterface.OnCancelListener() {
		                @Override
		                public void onCancel(DialogInterface dialog) { }
	        		});
	        processCancelVote(pin.toCharArray());   
		}
	}
	
	@Override
	public void cancelVote(VoteReceipt receipt) {
		Log.d(TAG + ".cancelVote(...)", " - cancelVote");
		operation = Operation.CANCEL_VOTE;
		operationReceipt = receipt;
		Intent signServiceIntent = new Intent(this, SignService.class);
		startService(signServiceIntent);
		bindService(signServiceIntent, signServiceConnection, BIND_AUTO_CREATE);

    	if (!Aplicacion.Estado.CON_CERTIFICADO.equals(Aplicacion.INSTANCIA.getEstado())) {
    		Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
    		showCertNotFoundDialog();
    	} else {
    		showPinScreen(getString(R.string.cancel_vote_msg));
    	} 
	}
	
	private void dismissOptionsDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		DialogFragment prev = (DialogFragment) getSupportFragmentManager().findFragmentByTag(OPTIONS_DIALOG_ID);
	    if(prev != null) {
	    	prev.getDialog().dismiss();
		    if (prev != null) {
		        ft.remove(prev);
		    }
		    ft.addToBackStack(null);
	    }
	}
	
	private void showCertNotFoundDialog() {
		CertNotFoundDialog certDialog = new CertNotFoundDialog();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(Aplicacion.CERT_NOT_FOUND_DIALOG_ID);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    certDialog.show(ft, Aplicacion.CERT_NOT_FOUND_DIALOG_ID);
	}

	@Override public void proccessResponse(Integer requestId, Respuesta respuesta) {
		Log.d(TAG + ".setSignServiceMsg()", "--- statusCode: " + 
				respuesta.getCodigoEstado() + " - msg: " + respuesta.getMensaje());
		String caption  = null;
		if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
			operationReceipt.setCancelVoteReceipt(respuesta.getSmimeMessage());
			String msg = getString(R.string.cancel_vote_result_msg, 
					this.operationReceipt.getVoto().getAsunto());
			try {
				db.updateVoteReceipt(operationReceipt);
			} catch (Exception e) {
				e.printStackTrace();
			} 
			refreshReceiptList();
			AlertDialog.Builder builder= new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.msg_lbl)).setMessage(msg).show();
		} else {
			caption = getString(R.string.error_lbl) + " " 
					+ respuesta.getCodigoEstado();
			if(Respuesta.SC_ANULACION_REPETIDA == respuesta.getCodigoEstado()) {
				Log.e(TAG + ".setSignServiceMsg(...)", " --- ANULACION_REPETIDA --- ");
				operationReceipt.setCanceled(true);
				try {
					db.updateVoteReceipt(operationReceipt);
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
			showMessage(caption, respuesta.getMensaje());
		}
		
	}

	@Override
	public void removeReceipt(VoteReceipt receipt) {
		Log.d(TAG + ".removeReceipt()", " --- receipt: " + receipt.getId());
		try {
			db.deleteVoteReceipt(receipt);
			refreshReceiptList();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
	}

}