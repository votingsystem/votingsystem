package org.votingsystem.android.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSStatisticsPagerActivity;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.android.ui.CancelVoteDialog;
import org.votingsystem.android.ui.CertNotFoundDialog;
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
    private ContextVS contextVS;
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
                    CancelVoteDialog cancelVoteDialog = CancelVoteDialog.newInstance(
                            getString(R.string.msg_lbl), message, vote);
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    cancelVoteDialog.show(fragmentManager, CancelVoteDialog.TAG);
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
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
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
                showPinScreen(getString(R.string.cancel_vote_msg));
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
        values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL, ObjectUtils.serializeObject(vote));
        values.put(ReceiptContentProvider.TYPE_COL, TypeVS.VOTEVS.toString());
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
        if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
            showCertNotFoundDialog();
        } else {
            vote = new VoteVS(eventVS, optionSelected);
            String content = optionSelected.getContent().length() >
                    ContextVS.SELECTED_OPTION_MAX_LENGTH ?
                    optionSelected.getContent().substring(0, ContextVS.SELECTED_OPTION_MAX_LENGTH) +
                            "..." : optionSelected.getContent();
            showPinScreen(getString(R.string.option_selected_msg, content));
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        Log.d(TAG + ".onResume() ", "onResume");
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    private void showCertNotFoundDialog() {
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(
                ContextVS.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
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
                message, htmlMessage);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(message, false, broadCastId, null);
        pinDialog.show(getFragmentManager(), PinDialogFragment.TAG);
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

    /*public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_in));
            progressContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_out));
            progressContainer.setVisibility(View.GONE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }*/

}