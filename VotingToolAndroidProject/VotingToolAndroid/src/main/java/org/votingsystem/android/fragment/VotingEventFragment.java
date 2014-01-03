/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.votingsystem.android.fragment;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSStatisticsPagerActivity;
import org.votingsystem.android.contentprovider.VoteReceiptDBHelper;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.android.ui.CancelVoteDialog;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.VotingResultDialog;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VoteVS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.model.ContextVS.MAX_SUBJECT_SIZE;

public class VotingEventFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "VotingEventFragment";

    private VoteService.Operation operation = VoteService.Operation.VOTE;
    private EventVS eventVS;
    private List<Button> optionButtonList;
    private Button saveReceiptButton;
    private Button cancelVoteButton;
    private ContextVS contextVS;
    private View rootView;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String broadCastId = null;


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
            VoteService.Operation resultOperation = (VoteService.Operation) intent.
                    getSerializableExtra(ContextVS.OPERATION_KEY);
            if(resultOperation == VoteService.Operation.VOTE) {
                if(ResponseVS.SC_OK == responseStatusCode) {
                    VoteVS resultReceipt = contextVS.getVoteReceipt(eventVS.getId());
                    VotingResultDialog votingResultDialog = VotingResultDialog.newInstance(
                            getString(R.string.operation_ok_msg), resultReceipt);
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    votingResultDialog.show(fragmentManager, VotingResultDialog.TAG);
                    setReceiptScreen(contextVS.getVoteReceipt(eventVS.getId()));
                } else {
                    showMessage(responseStatusCode, caption, message);
                    if(ResponseVS.SC_ERROR_REQUEST_REPEATED != responseStatusCode){
                        setOptionButtonsEnabled(true);
                    }
                }
            } else if(resultOperation == VoteService.Operation.CANCEL_VOTE){
                if(ResponseVS.SC_OK == responseStatusCode) {
                    setEventScreen(eventVS);
                    CancelVoteDialog cancelVoteDialog = CancelVoteDialog.newInstance(
                            getString(R.string.msg_lbl), message,
                            contextVS.getVoteReceipt(eventVS.getId()));
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    cancelVoteDialog.show(fragmentManager, CancelVoteDialog.TAG);
                } else {
                    cancelVoteButton.setEnabled(true);
                    showMessage(responseStatusCode, caption, message);
                }
            }
        }
        }
    };

    private void launchVoteService(String pin) {
        Log.d(TAG + ".launchVoteService() ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VoteService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.OPERATION_KEY, operation);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.EVENTVS_KEY, eventVS);
            if(operation == VoteService.Operation.CANCEL_VOTE) {
                VoteVS receipt = contextVS.getVoteReceipt(eventVS.getId());
                startIntent.putExtra(ContextVS.MESSAGE_KEY, receipt.getVote().getCancelVoteData());
                cancelVoteButton.setEnabled(false);
            } else setOptionButtonsEnabled(false);
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
        Log.d(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        try {
            if(getArguments().getString(ContextVS.EVENTVS_KEY) != null) {
                eventVS = EventVS.parse(new JSONObject(getArguments().getString(
                        ContextVS.EVENTVS_KEY)));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        rootView = inflater.inflate(R.layout.voting_event_fragment, container, false);
        saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        cancelVoteButton = (Button) rootView.findViewById(R.id.cancel_vote_button);
        setEventScreen(eventVS);
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
        if(savedInstanceState != null && savedInstanceState.getBoolean(
                ContextVS.LOADING_KEY, false)) showProgress(true, true);
        broadCastId = this.getClass().getSimpleName()+ "_" + eventVS.getId();
        return rootView;
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel_vote_button:
                cancelVote(view);
                break;
            case R.id.save_receipt_button:
                saveVote(view);
                break;
            case R.id.event_subject:
                onClickSubject(view);
                break;
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.event, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.eventInfo:
                Intent intent = new Intent(getActivity().getApplicationContext(),
                        EventVSStatisticsPagerActivity.class);
                intent.putExtra(ContextVS.ITEM_ID_KEY, eventVS.getId());
                intent.putExtra(ContextVS.EVENT_TYPE_KEY, eventVS.getTypeVS());
                intent.putExtra(ContextVS.EVENT_STATE_KEY, eventVS.getState());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setReceiptScreen(final VoteVS receipt) {
        Log.d(TAG + ".setReceiptScreen(...)", " - setReceiptScreen");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.VISIBLE);
        ((ActionBarActivity)getActivity().getApplicationContext()).getSupportActionBar().
                setTitle(getString(R.string.already_voted_lbl, receipt.getVote().
                getOptionSelected().getContent()));
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = receipt.getVote().getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(receipt.getVote().getContent()) + "\n");
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventVS> fieldsEventVS = receipt.getVote().getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(optionButtonList == null) {
            optionButtonList = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 15, 15);
            for (final FieldEventVS opcion:fieldsEventVS) {
                Button optionButton = new Button(getActivity().getApplicationContext());
                optionButton.setText(opcion.getContent());
                optionButtonList.add(optionButton);
                optionButton.setEnabled(false);
                linearLayout.addView(optionButton, paramsButton);
            }
        } else setOptionButtonsEnabled(false);
    }

    public void cancelVote(View v) {
        Log.d(TAG + ".cancelVote(...)", "");
        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(contextVS.getVoteReceipt(eventVS.getId()).getNotificationId());
        operation = VoteService.Operation.CANCEL_VOTE;
        showPinScreen(getString(R.string.cancel_vote_msg));
    }

    public void saveVote(View v) {
        Log.d(TAG + ".saveVote(...)", "");
        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        VoteVS receipt = contextVS.getVoteReceipt(eventVS.getId());
        notificationManager.cancel(receipt.getNotificationId());
		/*Log.d(TAG + ".guardarReciboButton ", " - Files dir path: " +
		getActivity().getApplicationContext().getFilesDir().getAbsolutePath());
		String receiptFileName = StringUtils.getNormalized(reciboVoto.getEventURL()) ;

		File file = new File(getActivity().getApplicationContext().getFilesDir(), receiptFileName);
		Log.d(TAG + ".guardarReciboButton ", " - Files path: " + file.getAbsolutePath());
		try {
			reciboVoto.writoToFile(file);
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.operation_ok_msg)).
				setMessage(getString(R.string.receipt_file_saved_msg)).show();
		} catch(Exception ex) {
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.error_lbl)).
				setMessage(ex.getMessage()).show();
		}*/
        VoteReceiptDBHelper db = new VoteReceiptDBHelper(getActivity().getApplicationContext());
        try {
            receipt.setId(db.insertVoteReceipt(receipt));
            saveReceiptButton.setEnabled(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        if(event.getOptionSelected() != null) {
            Log.d(TAG + ".setEventScreen", "Selected option: " +event.getOptionSelected().getContent() );
            processSelectedOption(event.getOptionSelected());
        } else Log.d(TAG + ".setEventScreen", "Selected null option");
    }

    private void processSelectedOption(FieldEventVS optionSelected) {
        Log.d(TAG + ".processSelectedOption", "processSelectedOption");
        operation = VoteService.Operation.VOTE;
        eventVS.setOptionSelected(optionSelected);
        if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
            showCertNotFoundDialog();
        } else {
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
            showMessage(null, getActivity().getString(R.string.subject_lbl), eventVS.getSubject());
        }
    }

    private void setOptionButtonsEnabled(boolean areEnabled) {
        if(optionButtonList == null) return;
        for(Button button:optionButtonList) {
            button.setEnabled(areEnabled);
        }
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + "caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(message, false, broadCastId);
        pinDialog.show(getFragmentManager(), PinDialogFragment.TAG);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", " - onDestroy");
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
    }

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", "");
    }

    public void showProgress(boolean showProgress, boolean animate) {
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
    }

}