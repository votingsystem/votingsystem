package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class PublishEventVSFragment extends Fragment {
	
	public static final String TAG = "PublishEventVSFragment";
	

	private TypeVS formType;
    private EditorFragment editorFragment;
    private AppContextVS contextVS;
    private TextView progressMessage;
    private EditText dateFinishEditText;
    private EditText dateBeginEditText;
    private TextView optionCaption;
    private Spinner controlCenterSetSpinner;
    private EditText subjectEditText;
    private View progressContainer;
    private FrameLayout mainLayout;
    private LinearLayout optionContainer;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private View rootView;
    private String broadCastId = null;
    private List<ControlCenterVS> controlCenterList = new ArrayList<ControlCenterVS>();
    private List<String> optionList = new ArrayList<String>();
    private Calendar dateFinishCalendar = null;
    private Calendar dateBeginCalendar = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            TypeVS operationType = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            if(pin != null) launchSignAndSendService(pin);
            else {
                int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                        ResponseVS.SC_ERROR);
                String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                if(TypeVS.ITEM_REQUEST == operationType) {
                    if(optionList.contains(message)) {
                        showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                                getActivity().getString(R.string.option_repeated_msg, message));
                    } else {
                        optionList.add(message);
                        addEventOption(message);
                    }
                    return;
                }
                showProgress(false, true);
                GroupPosition selectedSubsystem = null;
                if(ResponseVS.SC_OK == responseStatusCode) {
                    caption = getString(R.string.operation_ok_msg);
                    switch(operationType) {
                        case MANIFEST_PUBLISHING:
                            message = getString(R.string.publish_manifest_OK_prefix_msg);
                            selectedSubsystem = GroupPosition.MANIFESTS;
                            break;
                        case CLAIM_PUBLISHING:
                            message = getString(R.string.publish_claim_OK_prefix_msg);
                            selectedSubsystem = GroupPosition.CLAIMS;
                            break;
                        case VOTING_PUBLISHING:
                            message = getString(R.string.publish_voting_OK_prefix_msg);
                            selectedSubsystem = GroupPosition.VOTING;
                            break;
                    }
                    final GroupPosition groupPosition = selectedSubsystem;
                    message = message + " " + getString(R.string.publish_document_OK_sufix_msg);
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(caption).
                            setMessage(message).setPositiveButton(R.string.ok_lbl,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent(getActivity().getApplicationContext(),
                                    NavigationDrawer.class);
                            intent.putExtra(NavigatorDrawerOptionsAdapter.GROUP_POSITION_KEY,
                                    groupPosition.getPosition());
                            startActivity(intent);
                        }
                    }).show();
                    //to avoid avoid dissapear on screen orientation change
                    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                } else {
                    showMessage(responseStatusCode, getString(R.string.publish_document_ERROR_msg),
                            Html.fromHtml(responseVS.getNotificationMessage()).toString());
                }
            }
        }
    };


    DatePickerDialog.OnDateSetListener dateFinishListener = new DatePickerDialog.OnDateSetListener() {

        @Override public void onDateSet(DatePicker view, int year, int monthOfYear,int dayOfMonth) {
            //Double triggering problem
            if (!view.isShown()) return;
            Calendar todayCalendar = DateUtils.xDaysElapsed(1);
            Calendar newCalendar = Calendar.getInstance();
            newCalendar.set(Calendar.YEAR, year);
            newCalendar.set(Calendar.MONTH, monthOfYear);
            newCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            if(todayCalendar.after(newCalendar)) {
                showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                        getActivity().getString(R.string.date_error_lbl));
            } else {
                if(dateBeginCalendar != null) {
                    if(dateBeginCalendar.after(newCalendar)) {
                        showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                                getActivity().getString(R.string.date_init_after_finish_error_lbl));
                        return;
                    }
                }
                dateFinishCalendar = newCalendar;
                dateFinishEditText.setText(DateUtils.getDate_Es(dateFinishCalendar.getTime()));
            }
        }
    };

    DatePickerDialog.OnDateSetListener dateBeginListener = new DatePickerDialog.OnDateSetListener() {

        @Override public void onDateSet(DatePicker view, int year, int monthOfYear,int dayOfMonth) {
            //Double triggering problem
            if (!view.isShown()) return;
            Calendar todayCalendar = DateUtils.xDaysElapsed(1);
            Calendar newCalendar = Calendar.getInstance();
            newCalendar.set(Calendar.YEAR, year);
            newCalendar.set(Calendar.MONTH, monthOfYear);
            newCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            if(todayCalendar.after(newCalendar)) {
                showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                        getActivity().getString(R.string.date_error_lbl));
            } else {
                if(dateFinishCalendar != null) {
                    if(newCalendar.after(dateFinishCalendar)) {
                        showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                                getActivity().getString(R.string.date_init_after_finish_error_lbl));
                        return;
                    }
                }
                dateBeginCalendar = newCalendar;
                dateBeginEditText.setText(DateUtils.getDate_Es(
                        dateBeginCalendar.getTime()));
            }
        }

    };

    private void launchSignAndSendService(String pin) {
        Log.d(TAG + ".launchSignAndSendService(...) ", "operation: " + formType);
        String serviceURL = contextVS.getAccessControl().getPublishServiceURL(formType);
        String signedMessageSubject = null;
        EventVS eventVS = new EventVS();
        eventVS.setSubject(subjectEditText.getText().toString());
        eventVS.setContent(editorFragment.getEditorData());
        eventVS.setDateFinish(dateFinishCalendar.getTime());
        switch(formType) {
            case CLAIM_PUBLISHING:
                signedMessageSubject = getActivity().getString(R.string.publish_claim_msg_subject);
                break;
            case VOTING_PUBLISHING:
                signedMessageSubject = getActivity().getString(R.string.publish_election_msg_subject);
                eventVS.setControlCenter(controlCenterList.get(new Long(
                        controlCenterSetSpinner.getSelectedItemId()).intValue() - 1)); //-1 -> Select Control Center msg
                eventVS.setDateBegin(dateBeginCalendar.getTime());
                break;
            case MANIFEST_PUBLISHING:
                signedMessageSubject = getActivity().getString(R.string.publish_manifest_msg_subject);
                break;
        }
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
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    SignAndSendService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, formType);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.URL_KEY, serviceURL);
            startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED);
            startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, signedMessageSubject);
            startIntent.putExtra(ContextVS.MESSAGE_KEY, eventVS.toJSON().toString());
            progressMessage.setText(R.string.publishing_document_msg);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        broadCastId = this.getClass().getSimpleName();
        formType = (TypeVS)getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        switch(formType) {
            case CLAIM_PUBLISHING:
                rootView = inflater.inflate(R.layout.publish_claim_fragment, container, false);
                optionContainer = (LinearLayout) rootView.findViewById(R.id.optionContainer);
                optionCaption = (TextView) rootView.findViewById(R.id.eventFieldsCaption);
                break;
            case MANIFEST_PUBLISHING:
                rootView = inflater.inflate(R.layout.publish_manifest_fragment, container, false);
                break;
            case VOTING_PUBLISHING:
                rootView = inflater.inflate(R.layout.publish_election_fragment, container, false);
                optionContainer = (LinearLayout) rootView.findViewById(R.id.optionContainer);
                optionCaption = (TextView) rootView.findViewById(R.id.eventFieldsCaption);
                controlCenterSetSpinner = (Spinner) rootView.findViewById(R.id.controlCenterSetSpinner);
                Set<ControlCenterVS> controlCenterSet =
                        contextVS.getAccessControl().getControlCenters();
                List<String> controlCenterNameList = new ArrayList<String>();
                controlCenterNameList.add(getActivity().getString(R.string.select_control_center_lbl));
                for(ControlCenterVS controlCenter : controlCenterSet) {
                    controlCenterNameList.add(controlCenter.getName());
                    controlCenterList.add(controlCenter);
                }
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, controlCenterNameList);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                controlCenterSetSpinner.setAdapter(dataAdapter);
                dateBeginEditText = (EditText) rootView.findViewById(R.id.dateBegin);
                dateBeginEditText.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Log.d(TAG + ".setOnClickListener(...)", "");
                        Calendar calendarToShow = null;
                        if(dateBeginCalendar == null) calendarToShow = DateUtils.xDaysElapsed(1);
                        else calendarToShow = dateBeginCalendar;
                        DatePickerDialog dialog = new DatePickerDialog(getActivity(), dateBeginListener,
                                calendarToShow.get(Calendar.YEAR), calendarToShow.get(Calendar.MONTH),
                                calendarToShow.get(Calendar.DAY_OF_MONTH));
                        dialog.setTitle(getActivity().getString(R.string.date_begin_lbl));
                        dialog.show();
                    }
                });
                dateBeginEditText.setKeyListener(null);
                break;
        }
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        subjectEditText = (EditText) rootView.findViewById(R.id.subject);
        dateFinishEditText = (EditText) rootView.findViewById(R.id.dateFinish);
        dateFinishEditText.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Log.d(TAG + ".setOnClickListener(...)", "");
                Calendar calendarToShow = null;
                if(dateFinishCalendar == null) calendarToShow = DateUtils.xDaysElapsed(1);
                else calendarToShow = dateFinishCalendar;
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), dateFinishListener,
                        calendarToShow.get(Calendar.YEAR), calendarToShow.get(Calendar.MONTH),
                        calendarToShow.get(Calendar.DAY_OF_MONTH));
                dialog.setTitle(getActivity().getString(R.string.date_finish_lbl));
                dialog.show();
            }
        });
        dateFinishEditText.setKeyListener(null);
        if(optionCaption != null) optionCaption.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addOption();
            }
        });
        progressContainer = rootView.findViewById(R.id.progressContainer);
        progressMessage = (TextView)rootView.findViewById(R.id.progressMessage);
        mainLayout.getForeground().setAlpha(0);
        // if set to true savedInstanceState will be allways null
        //setRetainInstance(true);
        setHasOptionsMenu(true);
        Log.d(TAG + ".onCreateView(...) ", "savedInstanceState: " + savedInstanceState);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        //Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        Log.d(TAG +  ".onActivityCreated(...)", "");
        super.onActivityCreated(savedInstanceState);
        editorFragment = (EditorFragment) getFragmentManager().findFragmentByTag(EditorFragment.TAG);
        if(savedInstanceState != null) {
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false)) showProgress(true, true);
            optionList = (List<String>) savedInstanceState.getSerializable(ContextVS.FORM_DATA_KEY);
            for(String optionContent:optionList) {
                addEventOption(optionContent);
            }
        }
        String screenTitle = null;
        String serverURL = contextVS.getAccessControl().getPublishServiceURL(formType);
        switch(formType) {
            case CLAIM_PUBLISHING:
                screenTitle = getString(R.string.publish_claim_caption);
                break;
            case MANIFEST_PUBLISHING:
                screenTitle = getString(R.string.publish_manifest_caption);
                break;
            case VOTING_PUBLISHING:
                screenTitle = getString(R.string.publish_voting_caption);
                break;
        }
        Log.d(TAG + ".onCreate(...) ", "formType: " + formType + " - serverURL: " + serverURL);
        getActivity().setTitle(screenTitle);
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
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        outState.putSerializable(ContextVS.FORM_DATA_KEY, (Serializable) optionList);
        //Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
        Log.d(TAG +  ".onSaveInstanceState(...)", "");
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart(...) ", "onStart");
        super.onStart();
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.editor, menu);
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.save_editor:
                if(validateForm()) PinDialogFragment.showPinScreen(getFragmentManager(),
                        broadCastId, null, false, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addOption() {
        String caption = null;
        String message = null;
        switch(formType) {
            case VOTING_PUBLISHING:
                caption = getActivity().getString(R.string.add_vote_option_lbl);
                message = getActivity().getString(R.string.add_vote_option_msg);
                break;
            case CLAIM_PUBLISHING:
                caption = getActivity().getString(R.string.add_claim_option_lbl);
                message = getActivity().getString(R.string.add_claim_option_msg);
                break;
        }
        NewFieldDialogFragment newFieldDialog = NewFieldDialogFragment.newInstance(caption,
                message, broadCastId,  TypeVS.ITEM_REQUEST);
        newFieldDialog.show(getFragmentManager(), NewFieldDialogFragment.TAG);
    }

    private boolean validateForm () {
        Log.d(TAG + ".validateForm()", "");
        if(formType == TypeVS.VOTING_PUBLISHING) {
            if(controlCenterSetSpinner.getSelectedItemId() == 0) {
                showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                        getActivity().getString(R.string.control_center_missing_error_lbl));
                return false;
            }
            if(dateBeginCalendar == null) {
                showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                        getActivity().getString(R.string.date_error_lbl));
                return false;
            }
            if(optionList.size() < ContextVS.NUM_MIN_OPTIONS) {
                showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                        getActivity().getString(R.string.num_vote_options_error_msg));
                return false;
            }
        }
        if(editorFragment.isEditorDataEmpty()) {
            showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                    getActivity().getString(R.string.editor_empty_error_lbl));
            return false;
        }
        if(dateFinishCalendar == null) {
            showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                    getActivity().getString(R.string.date_error_lbl));
            return false;
        }
        if(TextUtils.isEmpty(subjectEditText.getText())) {
            showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                    getActivity().getString(R.string.subject_error_lbl));
            return false;
        }
        return true;
    }

    @Override public void onDestroy() {
        Log.d(TAG + ".onDestroy()", "onDestroy");
        super.onDestroy();
    }

    private void showMessage(Integer statusCode, String caption, String message) {
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

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }


    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "");
        super.onStop();
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

}