package org.votingsystem.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ObjectUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeFragment extends Fragment {

	public static final String TAG = "RepresentativeFragment";

    private View rootView;
    private String broadCastId = null;
    private ContextVS contextVS;
    private Button selectButton;
    private View progressContainer;
    private FrameLayout mainLayout;
    private Long representativeId;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                "intent.getExtras(): " + intent.getExtras());
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(pin != null) launchSignAndSendService(pin);
        else {
            if(TypeVS.ITEM_REQUEST == typeVS) {
                Cursor cursor = getActivity().getApplicationContext().getContentResolver().
                        query(UserContentProvider.getRepresentativeURI(representativeId),
                                null, null, null, null);
                cursor.moveToFirst();
                UserVS representative = (UserVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                        cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
                printRepresentativeData(representative);
                showProgress(false, true);
            }
        }
        }
    };

    private void launchSignAndSendService(String pin) {
        Log.d(TAG + ".launchUserCertRequestService() ", "pin: " + pin);
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    SignAndSendService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            //startIntent.putExtra(ContextVS.TYPEVS_KEY, eventVS.getTypeVS());
            startIntent.putExtra(ContextVS.CALLER_KEY, this.getClass().getName());
            /*if(eventVS.getTypeVS().equals(TypeVS.MANIFEST_EVENT)) {
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
                signatureContent.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE);
                startIntent.putExtra(ContextVS.MESSAGE_KEY, signatureContent.toString());
            }*/
            showProgress(true, true);
            //signAndSendButton.setEnabled(false);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }


    public static Fragment newInstance(Long representativeId) {
        RepresentativeFragment fragment = new RepresentativeFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.ITEM_ID_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        representativeId =  getArguments().getLong(ContextVS.ITEM_ID_KEY);
        Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                UserContentProvider.getRepresentativeURI(representativeId),
                null, null, null, null);
        cursor.moveToFirst();
        UserVS representative = (UserVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
        rootView = inflater.inflate(R.layout.representative, container, false);
        selectButton = (Button) rootView.findViewById(R.id.select_representative_button);
        selectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        null, false, null);
            }
        });
        selectButton.setVisibility(View.GONE);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        broadCastId = this.getClass().getSimpleName()+ "_" + representativeId;
        if(representative.getDescription() != null) {
            printRepresentativeData(representative);
        } else {
            showProgress(true, true);
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    RepresentativeService.class);
            startIntent.putExtra(ContextVS.ITEM_ID_KEY, representativeId);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.ITEM_REQUEST);
            getActivity().startService(startIntent);
        }
        return rootView;
    }

    private void printRepresentativeData(UserVS representative) {
        if(representative.getImageBytes() != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(representative.getImageBytes(), 0,
                    representative.getImageBytes().length);
            ImageView image = (ImageView) rootView.findViewById(R.id.representative_image);
            image.setImageBitmap(bmp);
        }
        if(representative.getDescription() != null) {
            ((WebView)rootView.findViewById(R.id.representative_description)).loadData(
                    representative.getDescription(), null, null);
        }
        selectButton.setVisibility(View.VISIBLE);
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.representative, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.send_message:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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