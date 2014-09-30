package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptFragment extends Fragment {

    public static final String TAG = ReceiptFragment.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            TypeVS typeVS = (TypeVS)intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
                switch(typeVS) {
                    case CANCEL_VOTE:
                        launchVoteCancellation((VoteVS)selectedReceipt);
                        break;
                }
            } else {
                if(responseVS.getTypeVS() == TypeVS.CANCEL_VOTE){
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) { }
                    getActivity().onBackPressed();
                }
                ((ActivityVS)getActivity()).refreshingStateChanged(false);
                ((ActivityVS)getActivity()).showMessage(responseVS.getStatusCode(),
                        responseVS.getCaption(), responseVS.getNotificationMessage());
            }
        }
    };

    private void launchVoteCancellation(VoteVS vote) {
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                VoteService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CANCEL_VOTE);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.VOTE_KEY, vote);
        ((ActivityVS)getActivity()).refreshingStateChanged(true);
        getActivity().startService(startIntent);
    }

    private AppContextVS contextVS;
    private ReceiptContainer selectedReceipt;
    private ReceiptContainer selectedReceiptChild;
    private TransactionVS transaction;
    private Menu menu;
    private TextView receiptSubject;
    private TextView receipt_content;
    private TextView receipt;
    private SMIMEMessageWrapper selectedReceiptSMIME;
    private String broadCastId = null;


    public static Fragment newInstance(int cursorPosition) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(String receiptURL, TypeVS type) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(ContextVS.TYPEVS_KEY, type);
        args.putString(ContextVS.URL_KEY, receiptURL);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        broadCastId = ReceiptFragment.class.getSimpleName() + "_" + cursorPosition;
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.receipt_fragment, container, false);
        LinearLayout receiptDataContainer = (LinearLayout) rootView.
                findViewById(R.id.receipt_data_container);
        receipt_content = (TextView)rootView.findViewById(R.id.receipt_content);
        receipt_content.setMovementMethod(LinkMovementMethod.getInstance());
        receiptSubject = (TextView)rootView.findViewById(R.id.receipt_subject);
        receipt = (TextView)rootView.findViewById(R.id.receipt);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TypeVS type = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        String receiptURL = getArguments().getString(ContextVS.URL_KEY);
        selectedReceipt = (ReceiptContainer) getArguments().getSerializable(ContextVS.RECEIPT_KEY);
        transaction = (TransactionVS) getArguments().getSerializable(ContextVS.TRANSACTION_KEY);
        if(transaction != null) {
            selectedReceipt = new ReceiptContainer(transaction.getTypeVS(),
                    transaction.getMessageSMIMEURL());
            try {
                selectedReceipt.setReceiptBytes(transaction.getMessageSMIMEBytes());
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        if(selectedReceipt != null) {
            try {
                if(selectedReceipt.getReceipt() == null) {
                    ReceiptDownloader getDataTask = new ReceiptDownloader();
                    getDataTask.execute(selectedReceipt.getURL());
                } else initReceiptScreen(selectedReceipt);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        } else if(savedInstanceState != null) {
            selectedReceipt = (ReceiptContainer) savedInstanceState.getSerializable(
                    ContextVS.RECEIPT_KEY);
            initReceiptScreen(selectedReceipt);
        } else {
            if(receiptURL != null) {
                selectedReceipt = new ReceiptContainer(type, receiptURL);
                String selection = ReceiptContentProvider.URL_COL + "=? ";
                String[] selectionArgs = new String[]{receiptURL};
                Cursor cursor = getActivity().getContentResolver().query(
                        ReceiptContentProvider.CONTENT_URI, null, selection, selectionArgs, null);
                if(cursor.getCount() > 0 ) {
                    cursor.moveToFirst();
                    byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                            ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                    Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
                    try {
                        selectedReceipt = (ReceiptContainer) ObjectUtils.
                                deSerializeObject(serializedReceiptContainer);
                        selectedReceipt.setLocalId(receiptId);
                        initReceiptScreen(selectedReceipt);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    ReceiptDownloader getDataTask = new ReceiptDownloader();
                    getDataTask.execute(receiptURL);
                }
            } else {
                int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
                Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                        ReceiptContentProvider.CONTENT_URI, null, null, null, null);
                cursor.moveToPosition(cursorPosition);
                byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                        ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
                try {
                    selectedReceipt = (ReceiptContainer) ObjectUtils.
                            deSerializeObject(serializedReceiptContainer);
                    selectedReceipt.setLocalId(receiptId);
                    initReceiptScreen(selectedReceipt);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initReceiptScreen (ReceiptContainer receiptContainer) {
        Log.d(TAG + ".initReceiptScreen(...)", "type: " + receiptContainer.getTypeVS() +
                " - messageId: " + receiptContainer.getMessageId());
        try {
            String contentFormatted = null;
            String bas64EncodedSMIMEChild = null;
            JSONObject dataJSON = null;
            BigDecimal totalAmount = null;
            String currency = null;
            selectedReceiptSMIME = receiptContainer.getReceipt();
            String receiptSubjectStr = selectedReceiptSMIME == null? null :
                    selectedReceiptSMIME.getSubject();
            switch(receiptContainer.getTypeVS()) {
                case REPRESENTATIVE_SELECTION:
                case ANONYMOUS_REPRESENTATIVE_REQUEST:
                    dataJSON = new JSONObject(receiptContainer.getReceipt().getSignedContent());
                    contentFormatted = getString(R.string.anonymous_representative_request_formatted,
                            dataJSON.getString("weeksOperationActive"),
                            dataJSON.getString("dateFrom"),
                            dataJSON.getString("dateTo"),
                            dataJSON.getString("accessControlURL"));
                    break;
                case VICKET_REQUEST:
                    dataJSON = new JSONObject(receiptContainer.getReceipt().getSignedContent());
                    totalAmount = new BigDecimal(dataJSON.getString("totalAmount"));
                    currency = dataJSON.getString("currency");
                    String serverURL = dataJSON.getString("serverURL");
                    JSONArray arrayVickets = dataJSON.getJSONArray("vickets");
                    String vicketValueStr = arrayVickets.getJSONObject(0).getString("vicketValue");
                    Integer numVickets = arrayVickets.getJSONObject(0).getInt("numVickets");
                    contentFormatted = getString(R.string.vicket_request_formatted,
                            totalAmount.toPlainString(), currency, numVickets, vicketValueStr, serverURL);
                    break;
                default: contentFormatted = receiptContainer.getReceipt().getSignedContent();
            }
            receiptSubject.setText(getString(R.string.smime_subject_msg, receiptSubjectStr));
            receipt_content.setText(Html.fromHtml(contentFormatted));
            /*if(receiptContainer.getTypeVS() == TypeVS.USER_ALLOCATION_INPUT) {
                receipt.setText(Html.fromHtml(getString(R.string.user_allocation_receipt_url, "")));
                receipt.setVisibility(View.VISIBLE);
                final byte[] selectedReceiptChildBytes = Base64.decode(bas64EncodedSMIMEChild);
                receipt.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        selectedReceiptChild = new ReceiptContainer(
                                TypeVS.USER_ALLOCATION_INPUT_RECEIPT, null);
                        try {
                            selectedReceiptChild.setReceiptBytes(selectedReceiptChildBytes);
                            initReceiptScreen(selectedReceiptChild);
                            setActionBar(selectedReceiptChild);
                        } catch(Exception ex) {ex.printStackTrace();}
                    }
                });
            } else receipt.setVisibility(View.GONE);*/
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }


    private void setActionBar(ReceiptContainer receiptContainer) {
        if(receiptContainer == null) return;
        switch(receiptContainer.getTypeVS()) {
            case VOTEVS:
                if(((VoteVS)receiptContainer).getEventVS().getDateFinish().before(
                        new Date(System.currentTimeMillis()))) {
                    menu.removeItem(R.id.cancel_vote);
                }
                break;
            case CANCEL_VOTE:
            case VOTEVS_CANCELLED:
                MenuItem checkReceiptMenuItem = menu.findItem(R.id.check_receipt);
                checkReceiptMenuItem.setTitle(R.string.check_vote_Cancellation_lbl);
                menu.removeItem(R.id.cancel_vote);
                break;
            case VICKET_REQUEST:
            case REPRESENTATIVE_SELECTION:
            case ANONYMOUS_REPRESENTATIVE_REQUEST:
                menu.removeItem(R.id.cancel_vote);
                menu.removeItem(R.id.check_receipt);
                break;
            default: Log.d(TAG + ".onCreateOptionsMenu(...) ", "unprocessed type: " +
                    receiptContainer.getTypeVS());
        }
        if(receiptContainer.getLocalId() < 0) {
            menu.removeItem(R.id.delete_receipt);
        } else menu.removeItem(R.id.save_receipt);
        if(getActivity() instanceof ActionBarActivity) {
            ((ActionBarActivity)getActivity()).setTitle(getString(R.string.receipt_lbl));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(
                    receiptContainer.getTypeDescription(getActivity()));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setLogo(R.drawable.receipt_32);
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedReceipt != null) outState.putSerializable(ContextVS.RECEIPT_KEY,selectedReceipt);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "onResume");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Log.d(TAG + ".onCreateOptionsMenu(...) ", " selected receipt type:" +
                selectedReceipt.getTypeVS());
        menuInflater.inflate(R.menu.receipt_fragment, menu);
        this.menu = menu;
        setActionBar(selectedReceipt);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        AlertDialog dialog = null;
        switch (item.getItemId()) {
            case android.R.id.home:
                if(selectedReceiptChild != null) {
                    initReceiptScreen(selectedReceipt);
                    selectedReceiptChild = null;
                    return true;
                } else return false;
            case R.id.show_signers_info:
                try {
                    SignersInfoDialogFragment newFragment = SignersInfoDialogFragment.newInstance(
                            selectedReceiptSMIME.getBytes());
                    newFragment.show(getFragmentManager(), SignersInfoDialogFragment.TAG);
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case R.id.show_timestamp_info:
                TimeStampInfoDialogFragment newFragment = TimeStampInfoDialogFragment.newInstance(
                        selectedReceiptSMIME.getSigner().getTimeStampToken(),
                        (AppContextVS) getActivity().getApplicationContext());
                newFragment.show(getFragmentManager(), TimeStampInfoDialogFragment.TAG);
                break;
            case R.id.share_receipt:
                try {
                    Intent sendIntent = new Intent();
                    String receiptStr = new String(selectedReceiptSMIME.getBytes());
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, receiptStr);
                    sendIntent.setType(ContentTypeVS.TEXT.getName());
                    startActivity(sendIntent);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            case R.id.save_receipt:
                ContentValues values = new ContentValues();
                values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                        ObjectUtils.serializeObject(selectedReceipt));
                values.put(ReceiptContentProvider.TYPE_COL, selectedReceipt.getTypeVS().toString());
                values.put(ReceiptContentProvider.URL_COL, selectedReceipt.getMessageId());
                values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
                Uri uri = getActivity().getContentResolver().insert(
                        ReceiptContentProvider.CONTENT_URI, values);
                menu.removeItem(R.id.save_receipt);
                break;
            case R.id.check_receipt:
                return true;
            case R.id.delete_receipt:
                dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.delete_receipt_lbl)).
                        setMessage(Html.fromHtml(getString(R.string.delete_receipt_msg,
                                selectedReceipt.getSubject()))).setPositiveButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                getActivity().getContentResolver().delete(ReceiptContentProvider.
                                        getReceiptURI(selectedReceipt.getLocalId()), null, null);
                                getActivity().onBackPressed();
                            }
                        }).setNegativeButton(getString(R.string.cancel_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();
                //to avoid avoid dissapear on screen orientation change
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                return true;
            case R.id.cancel_vote:
                dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.cancel_vote_lbl)).
                        setMessage(Html.fromHtml(getString(R.string.cancel_vote_from_receipt_msg,
                                ((VoteVS) selectedReceipt).getSubject()))).setPositiveButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                                        getString(R.string.cancel_vote_msg), false, TypeVS.CANCEL_VOTE);
                            }
                        }).setNegativeButton(getString(R.string.cancel_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();
                //to avoid avoid dissapear on screen orientation change
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class ReceiptDownloader extends AsyncTask<String, String, ResponseVS> {

        public ReceiptDownloader() { }

        @Override protected void onPreExecute() {
            ((ActivityVS)getActivity()).refreshingStateChanged(true); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String receiptURL = urls[0];
            return HttpHelper.getData(receiptURL, null);
        }

        @Override  protected void onProgressUpdate(String... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    selectedReceipt.setReceiptBytes(responseVS.getMessageBytes());
                    if(transaction != null) {
                        transaction.setMessageSMIMEBytes(responseVS.getMessageBytes());
                        TransactionVSContentProvider.updateTransaction(contextVS, transaction);
                    }
                    initReceiptScreen(selectedReceipt);
                    setActionBar(selectedReceipt);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ((ActivityVS)getActivity()).showMessage(ResponseVS.SC_ERROR, getString(R.string.exception_lbl),
                            ex.getMessage());
                }
            } else {
                ((ActivityVS)getActivity()).showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                        responseVS.getMessage());
            }
            ((ActivityVS)getActivity()).refreshingStateChanged(false);
        }
    }


}