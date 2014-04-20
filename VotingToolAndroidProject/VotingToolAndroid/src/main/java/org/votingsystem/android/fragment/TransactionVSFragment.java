package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TransactionVSFragment extends Fragment {

    public static final String TAG = TransactionVSFragment.class.getSimpleName();

    private TransactionVS selectedTransactionVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private Menu menu;
    private TextView transactionvsSubject;
    private TextView transactionvs_content;
    private TextView to_user;
    private TextView from_user;
    private TextView receipt;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private SMIMEMessageWrapper messageSMIME;
    private String broadCastId = null;
    private AppContextVS contextVS;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras(): " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) == null) {
            switch(responseVS.getTypeVS()) {
                case RECEIPT:
                    byte[] receiptBytes = (byte[]) responseVS.getData();
                    selectedTransactionVS.setMessageSMIMEBytes(receiptBytes);
                    TransactionVSContentProvider.updateTransaction(contextVS, selectedTransactionVS);
                break;
            }
        }
        }
    };

    public static Fragment newInstance(int cursorPosition) {
        TransactionVSFragment fragment = new TransactionVSFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        //String selection = TransactionVSContentProvider.WEEK_LAPSE_COL + "= ? ";
        Cursor cursor = getActivity().getContentResolver().query(TransactionVSContentProvider.CONTENT_URI,
                null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        Long transactionId = cursor.getLong(cursor.getColumnIndex(
                TransactionVSContentProvider.ID_COL));
        selectedTransactionVS = (TransactionVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                cursor.getColumnIndex(TransactionVSContentProvider.SERIALIZED_OBJECT_COL)));
        selectedTransactionVS.setLocalId(transactionId);
        /*String weekLapseStr = cursor.getString(cursor.getColumnIndex(
                TransactionVSContentProvider.WEEK_LAPSE_COL));
        String currencyStr = cursor.getString(cursor.getColumnIndex(
                TransactionVSContentProvider.CURRENCY_COL));
        Log.d(TAG + ".onCreateView(...)", "weekLapse: " + weekLapseStr + " - currency:" + currencyStr);*/
        broadCastId = TransactionVSFragment.class.getSimpleName() + "_" + cursorPosition;
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.transactionvs_fragment, container, false);
        to_user = (TextView)rootView.findViewById(R.id.to_user);
        from_user = (TextView)rootView.findViewById(R.id.from_user);
        receipt = (TextView)rootView.findViewById(R.id.receipt);
        transactionvs_content = (TextView)rootView.findViewById(R.id.transactionvs_content);
        transactionvs_content.setMovementMethod(LinkMovementMethod.getInstance());
        transactionvsSubject = (TextView)rootView.findViewById(R.id.transactionvs_subject);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) {
            selectedTransactionVS = (TransactionVS) savedInstanceState.getSerializable(
                    ContextVS.MESSAGE_KEY);
        }
        initTransactionVSScreen(selectedTransactionVS);
    }

    private void initTransactionVSScreen (final TransactionVS transactionvs) {
        Log.d(TAG + ".initTransactionVSScreen(...)", "transactionvsId: " + transactionvs.getId());
        if(transactionvs.getFromUserVS() != null) {
            from_user.setText(Html.fromHtml(getString(R.string.transactionvs_from_user_lbl,
                    transactionvs.getFromUserVS().getNif(),
                    transactionvs.getFromUserVS().getFullName())));
            from_user.setVisibility(View.VISIBLE);
        }
        if(transactionvs.getToUserVS() != null) {
            to_user.setText(Html.fromHtml(getString(R.string.transactionvs_to_user_lbl,
                    transactionvs.getToUserVS().getNif(),
                    transactionvs.getToUserVS().getFullName())));
            to_user.setVisibility(View.VISIBLE);
        }
        String transactionHtml = getString(R.string.transactionvs_formatted,
                DateUtils.getLongDate_Es(transactionvs.getDateCreated()),
                transactionvs.getAmount().toPlainString(), transactionvs.getCurrencyVS().toString());
        messageSMIME = transactionvs.getMessageSMIME();
        transactionvsSubject.setText(getString(R.string.smime_subject_msg, selectedTransactionVS.getSubject()));
        transactionvs_content.setText(Html.fromHtml(transactionHtml));
        receipt.setText(Html.fromHtml(getString(R.string.transactionvs_receipt_url, transactionvs.getMessageSMIMEURL())));
        from_user.setVisibility(View.GONE);
        transactionvsSubject.setVisibility(View.GONE);
        receipt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.TRANSACTION_KEY, transactionvs);
                intent.putExtra(ContextVS.FRAGMENT_KEY, ReceiptFragment.class.getName());
                startActivity(intent);
            }
        });
    }

    private void setActionBar() {
        if(selectedTransactionVS == null) return;
        switch(selectedTransactionVS.getType()) {
            case VICKET_CANCELLATION:
                break;
            case VICKET_REQUEST:
                //menu.removeItem(R.id.cancel_vote);
                break;
            case VICKET_SEND:
                //menu.removeItem(R.id.cancel_vote);
                break;
            default: Log.d(TAG + ".onCreateOptionsMenu(...) ", "unprocessed type: " +
                    selectedTransactionVS.getType());
        }
        if(getActivity() instanceof ActionBarActivity) {
            ((ActionBarActivity)getActivity()).setTitle(getString(R.string.transactionv_lbl));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(
                    selectedTransactionVS.getDescription(getActivity().getApplicationContext()));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setLogo(
                    selectedTransactionVS.getIconId(getActivity().getApplicationContext()));
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedTransactionVS != null) outState.putSerializable(ContextVS.MESSAGE_KEY, selectedTransactionVS);
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
        Log.d(TAG + ".onCreateOptionsMenu(...) ", " selected transactionvs type:");
        menuInflater.inflate(R.menu.transactionvs_fragment, menu);
        this.menu = menu;
        setActionBar();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        AlertDialog dialog = null;
        switch (item.getItemId()) {
            case R.id.show_signers_info:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMessage(Integer statusCode, String caption,String message) {
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


}