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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class ReceiptFragment extends Fragment {

    public static final String TAG = "ReceiptFragment";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            TypeVS typeVS = (TypeVS)intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            if(pin != null) {
                switch(typeVS) {
                    case CANCEL_VOTE:
                        launchVoteCancellation(pin, (VoteVS)selectedReceipt);
                        break;
                }
            } else {
                int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                        ResponseVS.SC_ERROR);
                String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                TypeVS resultOperation = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
                if(resultOperation == TypeVS.CANCEL_VOTE){
                    if(ResponseVS.SC_OK == responseStatusCode) { }
                    getActivity().onBackPressed();
                }
                showProgress(false, true);
                showMessage(responseStatusCode, caption, message);
            }
        }
    };

    private void launchVoteCancellation(String pin, VoteVS vote) {
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                VoteService.class);
        startIntent.putExtra(ContextVS.PIN_KEY, pin);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CANCEL_VOTE);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.VOTE_KEY, vote);
        showProgress(true, true);
        getActivity().startService(startIntent);
    }

    private AppContextVS contextVS;
    private ReceiptContainer selectedReceipt;
    private View progressContainer;
    private FrameLayout mainLayout;
    private Menu menu;
    private TextView receiptSubject;
    private TextView receipt_content;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
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
        broadCastId = this.getClass().getSimpleName() + "_" + cursorPosition;
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.receipt_fragment, container, false);
        LinearLayout receiptDataContainer = (LinearLayout) rootView.
                findViewById(R.id.receipt_data_container);
        receipt_content = (TextView)rootView.findViewById(R.id.receipt_content);
        receipt_content.setMovementMethod(LinkMovementMethod.getInstance());
        receiptSubject = (TextView)rootView.findViewById(R.id.receipt_subject);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TypeVS type = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        String receiptURL = getArguments().getString(ContextVS.URL_KEY);
        if(savedInstanceState != null) {
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

    private void initReceiptScreen (ReceiptContainer receipt) {
        Log.d(TAG + ".initReceiptScreen(...)", "type: " + receipt.getType() + " - messageId: " +
            receipt.getMessageId());
        try {
            selectedReceiptSMIME = receipt.getReceipt();
            receiptSubject.setText(getString(R.string.smime_subject_msg, selectedReceipt.getSubject()));
            receipt_content.setText(Html.fromHtml(getReceiptContentFormatted(selectedReceipt)));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setActionBar() {
        if(selectedReceipt == null) return;
        switch(selectedReceipt.getType()) {
            case VOTEVS:
                if(((VoteVS)selectedReceipt).getEventVS().getDateFinish().before(
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
            case REPRESENTATIVE_SELECTION:
            case ANONYMOUS_REPRESENTATIVE_REQUEST:
                menu.removeItem(R.id.cancel_vote);
                menu.removeItem(R.id.check_receipt);
                break;
            default: Log.d(TAG + ".onCreateOptionsMenu(...) ", "unprocessed type: " +
                    selectedReceipt.getType());
        }
        if(selectedReceipt.getLocalId() < 0) {
            menu.removeItem(R.id.delete_receipt);
        } else menu.removeItem(R.id.save_receipt);
        if(getActivity() instanceof ActionBarActivity) {
            ((ActionBarActivity)getActivity()).setTitle(getString(R.string.receipt_lbl));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(
                    selectedReceipt.getTypeDescription(getActivity()));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setLogo(R.drawable.receipt_32);
        }
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart(...) ", "");
        super.onStart();
    }


    @Override public void onDestroy() {
        Log.d(TAG + ".onDestroy()", "");
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedReceipt != null) outState.putSerializable(ContextVS.RECEIPT_KEY, selectedReceipt);
    }

    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "");
        super.onStop();
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
                selectedReceipt.getType());
        menuInflater.inflate(R.menu.receipt_fragment, menu);
        this.menu = menu;
        setActionBar();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        AlertDialog dialog = null;
        switch (item.getItemId()) {
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
                        getActivity().getApplicationContext());
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
                values.put(ReceiptContentProvider.TYPE_COL, selectedReceipt.getType().toString());
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

    private void showMessage(Integer statusCode,String caption,String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

    public class ReceiptDownloader extends AsyncTask<String, String, ResponseVS> {

        public ReceiptDownloader() { }

        @Override protected void onPreExecute() {showProgress(true, true); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String receiptURL = urls[0];
            return HttpHelper.getData(receiptURL, null);
        }

        @Override  protected void onProgressUpdate(String... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    selectedReceipt.setReceiptBytes(responseVS.getMessageBytes());
                    initReceiptScreen(selectedReceipt);
                    setActionBar();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage(ResponseVS.SC_ERROR, getString(R.string.exception_lbl),
                            ex.getMessage());
                }
            } else {
                showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                        responseVS.getMessage());
            }
            showProgress(false, true);
        }
    }


    public String getReceiptContentFormatted(ReceiptContainer selectedReceipt) {
        String result = null;
        try {
            switch(selectedReceipt.getType()) {
                case REPRESENTATIVE_SELECTION:
                case ANONYMOUS_REPRESENTATIVE_REQUEST:
                    JSONObject dataJSON = new JSONObject(selectedReceipt.getReceipt().getSignedContent());
                    result = getString(R.string.anonymous_representative_request_formatted,
                            dataJSON.getString("weeksOperationActive"),
                            dataJSON.getString("dateFrom"),
                            dataJSON.getString("dateTo"),
                            dataJSON.getString("accessControlURL"));
                    break;
                default: return selectedReceipt.getReceipt().getSignedContent();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

}