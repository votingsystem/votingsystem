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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.android.ui.CancelVoteDialog;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
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
                        launchVoteCancellation(pin);
                        break;
                }
            } else {
                int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                        ResponseVS.SC_ERROR);
                String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                TypeVS resultOperation = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
                if(resultOperation == TypeVS.CANCEL_VOTE){
                    if(ResponseVS.SC_OK == responseStatusCode) {
                        ContentValues values = new ContentValues();
                        try {
                            values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                                    ObjectUtils.serializeObject(vote));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        values.put(ReceiptContentProvider.TYPE_COL, TypeVS.VOTEVS_CANCELLED.toString());
                        values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
                        values.put(ReceiptContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
                        values.put(ReceiptContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
                        Uri uriToUpdate = ReceiptContentProvider.getreceiptURI(vote.getId());
                        getActivity().getContentResolver().update(uriToUpdate, values, null, null);
                        CancelVoteDialog cancelVoteDialog = CancelVoteDialog.newInstance(
                                caption, message, vote);
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        cancelVoteDialog.show(fragmentManager, CancelVoteDialog.TAG);
                    }
                }
            }
        }
    };

    private void launchVoteCancellation(String pin) {
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                VoteService.class);
        startIntent.putExtra(ContextVS.PIN_KEY, pin);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CANCEL_VOTE);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.VOTE_KEY, vote);
        showProgress(true, true);
        getActivity().startService(startIntent);
    }

    private ContextVS contextVS;
    private TypeVS receiptType;
    private VoteVS vote;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private SMIMEMessageWrapper selectedReceipt;
    private String broadCastId = null;


    public static Fragment newInstance(int cursorPosition) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        broadCastId = this.getClass().getSimpleName() + "_" + cursorPosition;
        Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                ReceiptContentProvider.CONTENT_URI, null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                ReceiptContentProvider.SERIALIZED_OBJECT_COL));
        String typeStr = cursor.getString(cursor.getColumnIndex(ReceiptContentProvider.TYPE_COL));
        receiptType = TypeVS.valueOf(typeStr);
        ReceiptContainer receipt = null;
        View rootView = inflater.inflate(R.layout.receipt_fragment, container, false);
        LinearLayout receiptDataContainer = (LinearLayout) rootView.
                findViewById(R.id.receipt_data_container);
        try {
            receipt = (ReceiptContainer) ObjectUtils.
                    deSerializeObject(serializedReceiptContainer);
            switch(receiptType) {
                case VOTEVS:
                    vote = (VoteVS) receipt;
                    initVoteReceiptScreen(vote, receiptType, inflater, receiptDataContainer);
                    break;
                default:
                    Log.d(TAG + ".onCreateView(...)", "unknown receipt type: " + receipt.getType());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        TextView receiptSubject = (TextView)rootView.findViewById(R.id.receipt_subject);
        receiptSubject.setText(receipt.getSubject());
        Log.d(TAG + ".onCreateView(...)", "receiptSubject: " + receipt.getSubject());
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        return rootView;
    }

    private void initVoteReceiptScreen (VoteVS vote, TypeVS type, LayoutInflater inflater,
            LinearLayout receiptDataContainer) {
        Log.d(TAG + ".initVoteReceiptScreen(...)", "type: " + type);
        if(TypeVS.VOTEVS == type) selectedReceipt = vote.getVoteReceipt();
        else if(TypeVS.CANCEL_VOTE == type) selectedReceipt = vote.getCancelVoteReceipt();
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Log.d(TAG + ".onCreateOptionsMenu(...) ", "");
        switch(receiptType) {
            case VOTEVS:
                menuInflater.inflate(R.menu.receipt_vote, menu);
                if(vote.getEventVS().getDateFinish().after(new Date(System.currentTimeMillis()))) {
                    menu.removeItem(R.id.cancel_vote);
                }
                break;
            default: Log.d(TAG + ".onCreateOptionsMenu(...) ", "unprocessed type: " + receiptType);
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

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
		switch (item.getItemId()) {
            case R.id.show_signers_info:
                try {
                    SignersInfoDialogFragment newFragment = SignersInfoDialogFragment.newInstance(
                            selectedReceipt.getBytes());
                    newFragment.show(getFragmentManager(), SignersInfoDialogFragment.TAG);
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case R.id.show_timestamp_info:
                TimeStampInfoDialogFragment newFragment = TimeStampInfoDialogFragment.newInstance(
                        selectedReceipt.getSigner().getTimeStampToken(),
                        getActivity().getApplicationContext());
                newFragment.show(getFragmentManager(), TimeStampInfoDialogFragment.TAG);
                break;
            case R.id.share_receipt:
                try {
                    Intent sendIntent = new Intent();
                    String receiptStr = new String(selectedReceipt.getBytes());
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, receiptStr);
                    sendIntent.setType(ContentTypeVS.TEXT.getName());
                    startActivity(sendIntent);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            case R.id.check_receipt:
                return true;
            case R.id.delete_receipt:
                return true;
            case R.id.cancel_vote:
                AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.cancel_vote_lbl));
                builder.setMessage(Html.fromHtml(getString(R.string.cancel_vote_from_receipt_msg,
                        vote.getSubject())));
                builder.setPositiveButton(getString(R.string.ok_button),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        showPinScreen(getString(R.string.cancel_vote_msg), TypeVS.CANCEL_VOTE);
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
                return true;
		}
        return super.onOptionsItemSelected(item);
	}

    private void showMessage(Integer statusCode,String caption,String message, String htmlMessage) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message, htmlMessage);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    private void showPinScreen(String message, TypeVS typeVS) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(message, false, broadCastId, typeVS);
        pinDialog.show(getFragmentManager(), PinDialogFragment.TAG);
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

}