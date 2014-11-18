package org.votingsystem.android.fragment;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSPublishFragment extends Fragment {
	
	public static final String TAG = EventVSPublishFragment.class.getSimpleName();


    private EditorFragment editorFragment;
    private AppContextVS contextVS;
    private EditText dateElectionText;
    private TextView optionCaption;
    private EditText subjectEditText;
    private LinearLayout optionContainer;
    private View rootView;
    private String broadCastId = EventVSPublishFragment.class.getSimpleName();
    private List<String> optionList = new ArrayList<String>();
    private Calendar dateElectionCalendar = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            TypeVS operationType = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) launchSignAndSendService();
            else {
                setProgressDialogVisible(false);
                String fragment = intent.getStringExtra(ContextVS.FRAGMENT_KEY);
                if(MessageDialogFragment.class.getSimpleName().equals(fragment)) {
                    getActivity().onBackPressed();
                    return;
                }
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                if(TypeVS.ITEM_REQUEST == operationType) {
                    if(optionList.contains(message)) {
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getActivity().
                                getString(R.string.error_lbl), getString(
                                R.string.option_repeated_msg, message), getFragmentManager());
                    } else {
                        optionList.add(message);
                        addEventOption(message);
                    }
                    return;
                }
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    MessageDialogFragment.showDialog(ResponseVS.SC_OK,
                            responseVS.getCaption(), responseVS.getNotificationMessage(),
                            broadCastId, TypeVS.VOTING_PUBLISHING, getFragmentManager());
                    /*message = message + " " + getString(R.string.publish_document_OK_sufix_msg);
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(caption).
                            setMessage(message).setPositiveButton(R.string.ok_lbl,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent(getActivity().getApplicationContext(),
                                    EventVSMainActivity.class);
                            startActivity(intent);
                        }
                    }).show();
                    //to avoid avoid dissapear on screen orientation change
                    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);*/
                } else {
                    MessageDialogFragment.showDialog(responseVS.getStatusCode(), getString(
                            R.string.publish_document_ERROR_msg), Html.fromHtml(
                            responseVS.getNotificationMessage()).toString(), getFragmentManager());
                }
            }
        }
    };

    DatePickerDialog.OnDateSetListener dateElectionListener = new DatePickerDialog.OnDateSetListener() {

        @Override public void onDateSet(DatePicker view, int year, int monthOfYear,int dayOfMonth) {
            //Double triggering problem
            if (!view.isShown()) return;
            Calendar todayCalendar = DateUtils.addDays(1);
            Calendar electionCalendar = DateUtils.getEventVSElectionDateBeginCalendar(
                    year, monthOfYear, dayOfMonth);
            if(todayCalendar.after(electionCalendar)) {
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(
                        R.string.error_lbl), getString(R.string.date_error_lbl),
                        getFragmentManager());
            } else {
                dateElectionCalendar = electionCalendar;
                dateElectionText.setText(DateUtils.getDayWeekDateStr(
                        dateElectionCalendar.getTime()));
            }
        }

    };

    private void launchSignAndSendService() {
        LOGD(TAG + ".launchSignAndSendService", "launchSignAndSendService");
        String serviceURL = contextVS.getAccessControl().getPublishServiceURL();
        String signedMessageSubject = null;
        EventVS eventVS = new EventVS();
        eventVS.setSubject(subjectEditText.getText().toString());
        eventVS.setContent(editorFragment.getEditorData());
        signedMessageSubject = getString(R.string.publish_election_msg_subject);
        eventVS.setDateBegin(dateElectionCalendar.getTime());
        eventVS.setDateFinish(DateUtils.addDays(dateElectionCalendar.getTime(), 1));
        if(!optionList.isEmpty()) {
            Set<FieldEventVS> voteOptionSet = new HashSet<FieldEventVS>();
            for(String optionContent:optionList) {
                FieldEventVS optionField = new FieldEventVS();
                optionField.setContent(optionContent);
                voteOptionSet.add(optionField);
            }
            eventVS.setFieldsEventVS(voteOptionSet);
        }
        try {
            Intent startIntent = new Intent(getActivity(), SignAndSendService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VOTING_PUBLISHING);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.URL_KEY, serviceURL);
            startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                    ContentTypeVS.JSON_SIGNED);
            startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, signedMessageSubject);
            JSONObject documentToSign = eventVS.toJSON();
            documentToSign.put("UUID", UUID.randomUUID().toString());
            startIntent.putExtra(ContextVS.MESSAGE_KEY, documentToSign.toString());
            Toast.makeText(getActivity(), getString(
                    R.string.publishing_document_msg), Toast.LENGTH_SHORT).show();
            setProgressDialogVisible(true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        broadCastId = EventVSPublishFragment.class.getSimpleName();
        rootView = inflater.inflate(R.layout.eventvs_election_publish, container, false);
        optionContainer = (LinearLayout) rootView.findViewById(R.id.optionContainer);
        optionCaption = (TextView) rootView.findViewById(R.id.eventFieldsCaption);
        List<String> controlCenterNameList = new ArrayList<String>();
        controlCenterNameList.add(getString(R.string.select_control_center_lbl));
        controlCenterNameList.add(contextVS.getAccessControl().getControlCenter().getName());
        dateElectionText = (EditText) rootView.findViewById(R.id.date_election);
        dateElectionText.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                LOGD(TAG + ".dateElectionText", "setOnClickListener");
                if(dateElectionCalendar == null) dateElectionCalendar = DateUtils.addDays(1);
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), dateElectionListener,
                        dateElectionCalendar.get(Calendar.YEAR), dateElectionCalendar.get(Calendar.MONTH),
                        dateElectionCalendar.get(Calendar.DAY_OF_MONTH));
                dialog.setTitle(getString(R.string.date_begin_lbl));
                dialog.show();
            }
        });
        dateElectionText.setKeyListener(null);
        subjectEditText = (EditText) rootView.findViewById(R.id.subject);
        if(optionCaption != null) optionCaption.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addOption();
            }
        });
        // if set to true savedInstanceState will be allways null
        //setRetainInstance(true);
        setHasOptionsMenu(true);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        LOGD(TAG +  ".onActivityCreated", "");
        super.onActivityCreated(savedInstanceState);
        editorFragment = (EditorFragment) getFragmentManager().findFragmentByTag(EditorFragment.TAG);
        if(savedInstanceState != null) {
            optionList = (List<String>) savedInstanceState.getSerializable(ContextVS.FORM_DATA_KEY);
            for(String optionContent:optionList) {
                addEventOption(optionContent);
            }
        }
        getActivity().setTitle(getString(R.string.publish_voting_caption));
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ModalProgressDialogFragment.showDialog(
                    getString(R.string.publishing_document_msg),
                    getString(R.string.publish_election_msg_subject),
                    getFragmentManager());
        } else ModalProgressDialogFragment.hide(getFragmentManager());
    }

    private void addEventOption(final String optionContent) {
        final LinearLayout newOptionView = (LinearLayout) getActivity().getLayoutInflater().
                inflate(R.layout.new_eventvs_field, null);
        Button remove_option_button = (Button) newOptionView.findViewById(
                R.id.remove_option_button);
        TextView fieldContentTextView = (TextView) newOptionView.findViewById(
                R.id.option_content);
        newOptionView.setVisibility(View.VISIBLE);
        fieldContentTextView.setText(optionContent);
        remove_option_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                optionList.remove(optionContent);
                optionContainer.removeView(newOptionView);
            }
        });
        optionContainer.addView(newOptionView);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.FORM_DATA_KEY, (Serializable) optionList);
        LOGD(TAG +  ".onSaveInstanceState", "");
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.text_editor, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.save_editor:
                if(validateForm()) PinDialogFragment.showPinScreen(getFragmentManager(),
                        broadCastId, getString(R.string.ping_to_sign_msg), false, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addOption() {
        NewFieldDialogFragment newFieldDialog = NewFieldDialogFragment.newInstance(
                getString(R.string.add_vote_option_lbl),
                getString(R.string.add_vote_option_msg), broadCastId,  TypeVS.ITEM_REQUEST);
        newFieldDialog.show(getFragmentManager(), NewFieldDialogFragment.TAG);
    }

    private boolean validateForm () {
        if(dateElectionCalendar == null) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                    getString(R.string.error_lbl),
                    getString(R.string.date_error_lbl), getFragmentManager());
            return false;
        }
        if(optionList.size() < ContextVS.NUM_MIN_OPTIONS) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.num_vote_options_error_msg), getFragmentManager());
            return false;
        }
        if(editorFragment.isEditorDataEmpty()) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.editor_empty_error_lbl), getFragmentManager());
            return false;
        }
        if(TextUtils.isEmpty(subjectEditText.getText())) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.subject_error_lbl), getFragmentManager());
            return false;
        }
        return true;
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

}