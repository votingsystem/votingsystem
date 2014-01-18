package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSStatisticsPagerActivity;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.model.ContextVS.MAX_SUBJECT_SIZE;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VotingEventFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "VotingEventFragment";

    private TypeVS operation = TypeVS.VOTEVS;
    private EventVS eventVS;
    private VoteVS vote;
    private List<Button> optionButtonList;
    private Button saveReceiptButton;
    private Button cancelVoteButton;
    private AppContextVS contextVS;
    private View rootView;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String broadCastId = null;

    private ProgressDialog progressDialog = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "intentExtras:" + intent.getExtras());
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        if(pin != null) launchVoteService(pin);
        else {
            int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                    ResponseVS.SC_ERROR);
            String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
            String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
            vote = (VoteVS) intent.getSerializableExtra(ContextVS.VOTE_KEY);
            TypeVS resultOperation = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            if(resultOperation == TypeVS.VOTEVS) {
                if(ResponseVS.SC_OK == responseStatusCode) {
                    message = getString(R.string.vote_ok_msg, eventVS.getSubject(),
                            vote.getOptionSelected().getContent());
                    showMessage(responseStatusCode, getString(R.string.operation_ok_msg),
                            message, null);
                    setReceiptScreen(vote);
                } else {
                    if(ResponseVS.SC_ERROR_REQUEST_REPEATED != responseStatusCode){
                        setOptionButtonsEnabled(true);
                        showMessage(responseStatusCode, caption, message, null);
                    } else showMessage(responseStatusCode, caption, null, message);
                }
            } else if(resultOperation == TypeVS.CANCEL_VOTE){
                if(ResponseVS.SC_OK == responseStatusCode) {
                    setEventScreen(eventVS);
                    AlertDialog dialog  = new AlertDialog.Builder(getActivity()).setTitle(
                            getString(R.string.msg_lbl)).setMessage(Html.fromHtml(message)).
                            setPositiveButton(R.string.save_receipt_lbl,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            saveCancelReceipt(vote);
                                        }
                                    }).setNegativeButton(R.string.cancel_lbl, null).show();
                    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                } else {
                    cancelVoteButton.setEnabled(true);
                    showMessage(responseStatusCode, caption, message, null);
                }
            }
        }
        }
    };

    private void launchVoteService(String pin) {
        Log.d(TAG + ".launchVoteService(...) ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VoteService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, operation);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.VOTE_KEY, vote);
            if(operation == TypeVS.CANCEL_VOTE) cancelVoteButton.setEnabled(false);
            else setOptionButtonsEnabled(false);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static VotingEventFragment newInstance(String eventJSONStr) {
        VotingEventFragment fragment = new VotingEventFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.EVENTVS_KEY, eventJSONStr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        try {
            if(getArguments().getString(ContextVS.EVENTVS_KEY) != null) {
                eventVS = EventVS.parse(new JSONObject(getArguments().getString(
                        ContextVS.EVENTVS_KEY)));
                eventVS.setAccessControlVS(contextVS.getAccessControl());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        rootView = inflater.inflate(R.layout.voting_event_fragment, container, false);
        saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        cancelVoteButton = (Button) rootView.findViewById(R.id.cancel_vote_button);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        Button cancelVoteButton = (Button) rootView.findViewById(R.id.cancel_vote_button);
        cancelVoteButton.setOnClickListener(this);
        Button saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        saveReceiptButton.setOnClickListener(this);
        TextView eventSubject = (TextView) rootView.findViewById(R.id.event_subject);
        eventSubject.setOnClickListener(this);
        broadCastId = this.getClass().getSimpleName()+ "_" + eventVS.getId();
        return rootView;
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel_vote_button:
                operation = TypeVS.CANCEL_VOTE;
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.cancel_vote_msg), false, null);
                break;
            case R.id.save_receipt_button:
                saveVote();
                break;
            case R.id.event_subject:
                onClickSubject(view);
                break;
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.eventvs, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.eventInfo:
                Intent intent = new Intent(getActivity().getApplicationContext(),
                        EventVSStatisticsPagerActivity.class);
                intent.putExtra(ContextVS.ITEM_ID_KEY, eventVS.getId());
                intent.putExtra(ContextVS.TYPEVS_KEY, eventVS.getTypeVS());
                intent.putExtra(ContextVS.EVENT_STATE_KEY, eventVS.getState());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setReceiptScreen(final VoteVS vote) {
        Log.d(TAG + ".setReceiptScreen(...)", "");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.VISIBLE);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = vote.getEventVS().getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(vote.getEventVS().getContent()) + "\n");
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventVS> fieldsEventVS = vote.getEventVS().getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(optionButtonList == null) {
            optionButtonList = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 15, 15);
            for (final FieldEventVS option:fieldsEventVS) {
                Button optionButton = new Button(getActivity().getApplicationContext());
                optionButton.setText(option.getContent());
                optionButtonList.add(optionButton);
                optionButton.setEnabled(false);
                linearLayout.addView(optionButton, paramsButton);
            }
        } else setOptionButtonsEnabled(false);
    }


    public void saveVote() {
        Log.d(TAG + ".saveVote(...)", "");
        ContentValues values = new ContentValues();
        vote.setType(TypeVS.VOTEVS);
        values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL, ObjectUtils.serializeObject(vote));
        values.put(ReceiptContentProvider.TYPE_COL, vote.getType().toString());
        values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
        values.put(ReceiptContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
        values.put(ReceiptContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
        Uri uri = getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI, values);
        Log.d(TAG + ".saveVote(...)", "uri: " + uri.toString());
        saveReceiptButton.setEnabled(false);
    }

    private void setEventScreen(final EventVS event) {
        Log.d(TAG + ".setEventScreen(...)", " - setEventScreen");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.GONE);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        String subject = event.getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(event.getContent()));
        //contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventVS> fieldsEventVS = event.getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(optionButtonList != null) linearLayout.removeAllViews();
        optionButtonList = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final FieldEventVS option:fieldsEventVS) {
            Button optionButton = new Button(getActivity().getApplicationContext());
            optionButton.setText(option.getContent());
            optionButton.setOnClickListener(new Button.OnClickListener() {
                FieldEventVS optionSelected = option;
                public void onClick(View v) {
                    Log.d(TAG + "- optionButton - optionId: " +
                            optionSelected.getId(), "state: " +
                            contextVS.getState().toString());
                    processSelectedOption(optionSelected);
                }
            });
            optionButtonList.add(optionButton);
            if (!event.isActive()) optionButton.setEnabled(false);
            linearLayout.addView(optionButton, paramsButton);

        }
    }

    private void processSelectedOption(FieldEventVS optionSelected) {
        Log.d(TAG + ".processSelectedOption", "processSelectedOption");
        operation = TypeVS.VOTEVS;
        vote = new VoteVS(eventVS, optionSelected);
        String pinMsgPart = optionSelected.getContent().length() >
                ContextVS.SELECTED_OPTION_MAX_LENGTH ? optionSelected.getContent().substring(0,
                ContextVS.SELECTED_OPTION_MAX_LENGTH) + "..." : optionSelected.getContent();
        PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                getString(R.string.option_selected_msg, pinMsgPart), false, null);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
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

    public void onClickSubject(View v) {
        Log.d(TAG + ".onClickSubject(...)", "");
        if(eventVS != null && eventVS.getSubject() != null &&
                eventVS.getSubject().length() > MAX_SUBJECT_SIZE) {
            showMessage(null, getActivity().getString(R.string.subject_lbl),
                    eventVS.getSubject(), null);
        }
    }

    private void setOptionButtonsEnabled(boolean areEnabled) {
        if(optionButtonList == null) return;
        for(Button button:optionButtonList) {
            button.setEnabled(areEnabled);
        }
    }

    private void showMessage(Integer statusCode, String caption, String message,
                 String htmlMessage) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message + " - htmlMessage: " + htmlMessage);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        Log.d(TAG + ".onDestroy()", " - onDestroy");
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        outState.putSerializable(ContextVS.VOTE_KEY, vote);
    }

    @Override public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if(bundle != null) {
            if(bundle.getBoolean(ContextVS.LOADING_KEY, false)) showProgress(true, true);
            vote = (VoteVS) bundle.getSerializable(ContextVS.VOTE_KEY);
        }
        if(vote != null && vote.getVoteReceipt() != null) setReceiptScreen(vote);
        else setEventScreen(eventVS);
    }

    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "");
        super.onStop();
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if(progressVisible.get()) {
            if (progressDialog == null) progressDialog = new ProgressDialog(getActivity());
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(getString(R.string.send_vote_caption));
            progressDialog.setMessage(getString(R.string.subject_lbl) + ": " + eventVS.getSubject());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        } else if (progressDialog != null) progressDialog.dismiss();
    }

    public void saveCancelReceipt(VoteVS vote) {
        Log.d(TAG + ".saveCancelReceipt(...)", "saveCancelReceipt");
        ContentValues values = new ContentValues();
        values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL, ObjectUtils.serializeObject(vote));
        values.put(ReceiptContentProvider.TYPE_COL, TypeVS.CANCEL_VOTE.toString());
        values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
        values.put(ReceiptContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
        values.put(ReceiptContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
        getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI, values);
        showMessage(null, getString(R.string.msg_lbl),
                getString(R.string.saved_cancel_vote_recepit_msg), null);
    }

}