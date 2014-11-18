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
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;
import java.util.Date;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptFragment extends Fragment {

    public static final String TAG = ReceiptFragment.class.getSimpleName();


    private AppContextVS contextVS;
    private ReceiptContainer selectedReceipt;
    private ReceiptContainer selectedReceiptChild;
    private TransactionVS transaction;
    private Menu menu;
    private TextView receiptSubject;
    private WebView receipt_content;
    private SMIMEMessage selectedReceiptSMIME;
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
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
                setProgressDialogVisible(false, null, null);
                MessageDialogFragment.showDialog(responseVS, getFragmentManager());
            }
        }
    };

    private void launchVoteCancellation(VoteVS vote) {
        Intent startIntent = new Intent(getActivity(), VoteService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CANCEL_VOTE);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.VOTE_KEY, vote);
        setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                getString(R.string.loading_info_msg));
        getActivity().startService(startIntent);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        broadCastId = ReceiptFragment.class.getSimpleName() + "_" + cursorPosition;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.receipt_fragment, container, false);
        LinearLayout receiptDataContainer = (LinearLayout) rootView.
                findViewById(R.id.receipt_data_container);
        receipt_content = (WebView)rootView.findViewById(R.id.receipt_content);
        receiptSubject = (TextView)rootView.findViewById(R.id.receipt_subject);
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
                    ReceiptFetcher getDataTask = new ReceiptFetcher();
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
                    ReceiptFetcher getDataTask = new ReceiptFetcher();
                    getDataTask.execute(receiptURL);
                }
            } else {
                int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
                Cursor cursor = getActivity().getContentResolver().query(
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
        LOGD(TAG + ".initReceiptScreen", "type: " + receiptContainer.getTypeVS() +
                " - messageId: " + receiptContainer.getMessageId());
        try {
            String contentFormatted = "";
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
                case VOTEVS:
                    VoteVS voteVS = (VoteVS)receiptContainer;
                    contentFormatted = getString(R.string.votevs_info_formatted,
                            CMSUtils.getTimeStampDateStr(selectedReceiptSMIME.getSigner().getTimeStampToken()),
                            voteVS.getEventVS().getSubject(), voteVS.getOptionSelected().getContent(),
                            receiptContainer.getReceipt().getSignedContent());

                    break;
                default:
                    contentFormatted = receiptContainer.getReceipt().getSignedContent();

            }
            receiptSubject.setText(receiptSubjectStr);
            //Html.fromHtml(contentFormatted)
            receipt_content.loadData(contentFormatted, "text/html; charset=UTF-8", null);
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
            default: LOGD(TAG + ".onCreateOptionsMenu", "unprocessed type: " +
                    receiptContainer.getTypeVS());
        }
        if(receiptContainer.getLocalId() < 0) {
            menu.removeItem(R.id.delete_receipt);
        } else menu.removeItem(R.id.save_receipt);
        if(getActivity() instanceof FragmentActivity) {
            ((ActionBarActivity)getActivity()).setTitle(getString(R.string.receipt_lbl));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(
                    receiptContainer.getTypeDescription(getActivity()));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setLogo(UIUtils.getEmptyLogo(getActivity()));
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedReceipt != null) outState.putSerializable(ContextVS.RECEIPT_KEY,selectedReceipt);
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ModalProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ModalProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected receipt type:" +
                selectedReceipt.getTypeVS());
        menuInflater.inflate(R.menu.receipt_fragment, menu);
        this.menu = menu;
        setActionBar(selectedReceipt);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        switch (item.getItemId()) {
            case android.R.id.home:
                if(selectedReceiptChild != null) {
                    initReceiptScreen(selectedReceipt);
                    selectedReceiptChild = null;
                    return true;
                } else return false;
            case R.id.show_signers_info:
                UIUtils.showSignersInfoDialog(selectedReceiptSMIME.getSigners(),
                        getFragmentManager(), getActivity());
                break;
            case R.id.show_timestamp_info:
                UIUtils.showTimeStampInfoDialog(selectedReceiptSMIME.getSigner().getTimeStampToken(),
                        getFragmentManager(), getActivity());
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
            case R.id.signature_content:
                MessageDialogFragment.showDialog(ResponseVS.SC_OK, getString(R.string.signature_content),
                        selectedReceiptSMIME.getSignedContent(), getFragmentManager());
                break;
            case R.id.check_receipt:
                if(selectedReceipt instanceof VoteVS) {
                    new VoteVSChecker().execute(((VoteVS)selectedReceipt).getHashCertVSBase64());
                }
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

    private class VoteVSChecker extends AsyncTask<String, Void, ResponseVS> {

        @Override protected void onPostExecute(ResponseVS responseVS) {
            super.onPostExecute(responseVS);
            setProgressDialogVisible(false, null, null);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) MessageDialogFragment.showDialog(
                    responseVS, getFragmentManager());
            else {
                try {
                    VoteVS.State voteState =  VoteVS.State.valueOf(responseVS.getMessageJSON().
                            getString("state"));
                    String voteValue = responseVS.getMessageJSON().getString("value");
                    MessageDialogFragment.showDialog(ResponseVS.SC_OK,
                            MsgUtils.getVoteVSStateMsg(voteState, getActivity()),
                            getString(R.string.votvs_value_msg, voteValue), getFragmentManager());
                } catch (Exception ex) {ex.printStackTrace();}
            }
        }

        @Override protected void onPreExecute() {
            super.onPreExecute();
            setProgressDialogVisible(true, getString(R.string.wait_msg),
                    getString(R.string.checking_vote_state_lbl));
        }

        @Override protected ResponseVS doInBackground(String... params) {
            ResponseVS responseVS = null;
            try {
                String hashHex = CMSUtils.getBase64ToHexStr(params[0]);
                responseVS = HttpHelper.getData(contextVS.getAccessControl().
                        getVoteVSCheckServiceURL(hashHex), ContentTypeVS.JSON);
            } catch(Exception ex) {
                responseVS = ResponseVS.getExceptionResponse(ex, getActivity());
            } finally {return responseVS;}
        }
    }

    public class ReceiptFetcher extends AsyncTask<String, String, ResponseVS> {

        public ReceiptFetcher() { }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.checking_vote_state_lbl),
                    getString(R.string.wait_msg));
        }

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
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.exception_lbl),
                            ex.getMessage(), getFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                        responseVS.getMessage(), getFragmentManager());
            }
            setProgressDialogVisible(false, null, null);
        }
    }


}