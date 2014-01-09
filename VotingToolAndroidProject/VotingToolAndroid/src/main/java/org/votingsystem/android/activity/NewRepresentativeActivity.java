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

package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EditorFragment;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.UserCertRequestFormFragment;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class NewRepresentativeActivity extends ActionBarActivity {
	
	public static final String TAG = "NewRepresentativeActivity";


    private static final int SELECT_PICTURE = 1;

    private TypeVS formType;
    private EditorFragment editorFragment;
    private ContextVS contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String broadCastId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            TypeVS operationType = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            if(pin != null) launchSignAndSendService(pin);
            else {

            }
        }
    };

    private void launchSignAndSendService(String pin) {
        Log.d(TAG + ".launchSignAndSendService(...) ", "operation: " + formType);
        String serviceURL = contextVS.getAccessControl().getPublishServiceURL(formType);
        String signedMessageSubject = null;
        String contentToSign = null;
        try {
            Intent startIntent = new Intent(getApplicationContext(), SignAndSendService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, formType);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.URL_KEY, serviceURL);
            startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED);
            startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, signedMessageSubject);
            startIntent.putExtra(ContextVS.MESSAGE_KEY, contentToSign);
            progressMessage.setText(R.string.publishing_document_msg);
            showProgress(true, true);
            startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getApplicationContext());
        broadCastId = this.getClass().getSimpleName();
        Log.d(TAG + ".onCreate(...)", "contextVS.getState(): " + contextVS.getState() +
                " - savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.new_representative_fragment);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mainLayout = (FrameLayout)findViewById(R.id.mainLayout);
        progressContainer =findViewById(R.id.progressContainer);
        progressMessage = (TextView)findViewById(R.id.progressMessage);
        mainLayout.getForeground().setAlpha(0);
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.save_editor:
                if(validateForm()) {
                    showPinScreen(null);
                }
                return true;
            case R.id.add_option:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                //To select multiple images
                //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent,"Seeeelect Picture"), SELECT_PICTURE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG + ".onCreateOptionsMenu(...)", "");
        getMenuInflater().inflate(R.menu.editor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean validateForm () {
        Log.d(TAG + ".validateForm()", "");
        if(editorFragment.isEditorDataEmpty()) {
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.editor_empty_error_lbl));
            return false;
        }
        return true;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG + ".onActivityResult(...)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        if(data.getData() != null) {
            Uri selectedImageUri = data.getData();
            String imagePath = selectedImageUri.getPath();
            File imageFile = new File(selectedImageUri.getPath());
            Log.d(TAG + ".onActivityResult(...)", "imagePath: " + imagePath);
        }
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, false, broadCastId, null);
        pinDialog.show(getSupportFragmentManager(), PinDialogFragment.TAG);
    }

    private void showCertNotFoundDialog() {
        Log.d(TAG + ".showCertNotFoundDialog(...)", "showCertNotFoundDialog");
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(
                ContextVS.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) ft.remove(prev);
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getApplicationContext(), android.R.anim.fade_in));
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
                    getApplicationContext(), android.R.anim.fade_out));
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

    @Override public void onResume() {
    	super.onResume();
    	Log.d(TAG + ".onResume() ", "onResume");
    }

    @Override protected void onStop() {
        super.onStop();
    	Log.d(TAG + ".onStop()", "onStop");
    };

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", "onDestroy");
    };

}