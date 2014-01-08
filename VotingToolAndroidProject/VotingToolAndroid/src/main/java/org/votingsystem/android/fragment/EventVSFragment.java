package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.InputType;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSStatisticsPagerActivity;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.model.ContextVS.MAX_SUBJECT_SIZE;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventVSFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "EventVSFragment";

    private Button signAndSendButton;
    private EventVS eventVS;
    private ContextVS contextVS;
    private Map<Integer, EditText> fieldsMap;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String broadCastId = null;


    public static EventVSFragment newInstance(String eventJSONStr) {
        EventVSFragment fragment = new EventVSFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.EVENTVS_KEY, eventJSONStr);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            if(pin != null) launchSignAndSendService(pin);
            else {
                int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                        ResponseVS.SC_ERROR);
                String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                showMessage(responseStatusCode, caption, message);
                if(ResponseVS.SC_OK != responseStatusCode) signAndSendButton.setEnabled(true);
                showProgress(false, true);
            }
        }
    };

    private void launchSignAndSendService(String pin) {
        Log.d(TAG + ".launchSignAndSendService(...) ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    SignAndSendService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, eventVS.getTypeVS());
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            if(eventVS.getTypeVS().equals(TypeVS.MANIFEST_EVENT)) {
                startIntent.putExtra(ContextVS.ITEM_ID_KEY, eventVS.getEventVSId());
            } else {
                startIntent.putExtra(ContextVS.URL_KEY,
                        contextVS.getAccessControl().getEventVSClaimCollectorURL());
                startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                        ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED);
                String messageSubject = getActivity().getString(R.string.signature_msg_subject)
                        + eventVS.getSubject();
                startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, messageSubject);
                JSONObject signatureContent = eventVS.getSignatureContentJSON();
                startIntent.putExtra(ContextVS.MESSAGE_KEY, signatureContent.toString());
            }
            showProgress(true, true);
            signAndSendButton.setEnabled(false);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
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
        View rootView = inflater.inflate(R.layout.eventvs_fragment, container, false);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = eventVS.getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);

        contentTextView.setText(Html.fromHtml(eventVS.getContent()));
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        signAndSendButton = (Button) rootView.findViewById(R.id.sign_and_send_button);
        if (!eventVS.isActive()) signAndSendButton.setEnabled(false);
        signAndSendButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG + "- signAndSendButton -", " - state: " + contextVS.getState().toString());
                if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
                    Log.d(TAG + "-signAndSendButton-", " - showCertNotFoundDialog");
                    showCertNotFoundDialog();
                    return;
                }
                if (eventVS.getTypeVS().equals(TypeVS.CLAIM_EVENT)) {
                    if(eventVS.getFieldsEventVS() != null && eventVS.getFieldsEventVS().size() > 0) {
                        showClaimFieldsDialog();
                        return;
                    }
                }
                showPinScreen(null);
            }
        });
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        TextView eventSubject = (TextView) rootView.findViewById(R.id.event_subject);
        eventSubject.setOnClickListener(this);
        if(savedInstanceState != null && savedInstanceState.getBoolean(
                ContextVS.LOADING_KEY, false)) showProgress(true, true);
        broadCastId = this.getClass().getSimpleName()+ "_" + eventVS.getId();
        return rootView;
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
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

    @Override public void onDestroy() {
        Log.d(TAG + ".onDestroy()", "");
        super.onDestroy();
    };

    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "");
        super.onStop();
    }

    public void onClickSubject(View v) {
        Log.d(TAG + ".onClickSubject(...)", "");
        if(eventVS != null && eventVS.getSubject() != null &&
                eventVS.getSubject().length() > MAX_SUBJECT_SIZE) {
            showMessage(null, getActivity().getString(R.string.subject_lbl), eventVS.getSubject());
        }
    }

    private void showCertNotFoundDialog() {
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(
                ContextVS.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) ft.remove(prev);
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(message, false, broadCastId, null);
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

    private void showClaimFieldsDialog() {
        Log.d(TAG + ".showClaimFieldsDialog(...)", "");
        if (eventVS.getFieldsEventVS() == null) {
            Log.d(TAG + ".showClaimFieldsDialog(...)", "Event without fields");
            return;
        }
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ScrollView mScrollView = (ScrollView) inflater.inflate(R.layout.claim_dinamic_form,
                (ViewGroup) getActivity().getCurrentFocus());
        LinearLayout mFormView = (LinearLayout) mScrollView.findViewById(R.id.form);
        final TextView errorMsgTextView = (TextView) mScrollView.findViewById(R.id.errorMsg);
        errorMsgTextView.setVisibility(View.GONE);
        Set<FieldEventVS> fields = eventVS.getFieldsEventVS();

        fieldsMap = new HashMap<Integer, EditText>();
        for (FieldEventVS field : fields) {
            addFormField(field.getContent(), InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                    mFormView, field.getId().intValue());
        }
        builder.setTitle(R.string.eventfields_dialog_caption).setView(mScrollView).
                setPositiveButton(getString(R.string.aceptar_button), null).
                setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) { }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();//to get positiveButton this must be called first
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View onClick) {
                Set<FieldEventVS> fields = eventVS.getFieldsEventVS();
                for (FieldEventVS field : fields) {
                    EditText editText = fieldsMap.get(field.getId().intValue());
                    String fieldValue = editText.getText().toString();
                    if ("".equals(fieldValue)) {
                        errorMsgTextView.setVisibility(View.VISIBLE);
                        return;
                    } else field.setValue(fieldValue);
                    Log.d(TAG + ".ClaimFieldsDialog", "field id: " + field.getId() +
                            " - text: " + fieldValue);
                }
                dialog.dismiss();
                showPinScreen(null);
            }
        });
        //to avoid avoid dissapear on screen orientation change
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private void addFormField(String label, int type, LinearLayout mFormView, int id) {
        Log.d(TAG + ".addFormField(...)", "field: " + label);
        TextView textView = new TextView(getActivity().getApplicationContext());
        textView.setLayoutParams(getDefaultParams(true));
        textView.setText(label);
        EditText fieldText = new EditText(getActivity().getApplicationContext());
        fieldText.setLayoutParams(getDefaultParams(false));
        fieldText.setTextColor(Color.BLACK);
        // setting an unique id is important in order to save the state
        // (content) of this view across screen configuration changes
        fieldText.setId(id);
        fieldText.setInputType(type);
        mFormView.addView(textView);
        mFormView.addView(fieldText);
        fieldsMap.put(id, fieldText);
    }

    private LinearLayout.LayoutParams getDefaultParams(boolean isLabel) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isLabel) {
            params.bottomMargin = 5;
            params.topMargin = 10;
        }
        params.leftMargin = 20;
        params.rightMargin = 20;
        return params;
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
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


    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
    }
}

