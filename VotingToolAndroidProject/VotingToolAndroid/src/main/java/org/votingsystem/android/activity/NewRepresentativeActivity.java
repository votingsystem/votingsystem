package org.votingsystem.android.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.fragment.EditorFragment;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.NewFieldDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class NewRepresentativeActivity extends ActionBarActivity {
	
	public static final String TAG = NewRepresentativeActivity.class.getSimpleName();

    private static final int SELECT_PICTURE   = 1;
    private static final int CONFIRM_PICTURE  = 2;

    private TypeVS operationType;
    private EditorFragment editorFragment;
    private AppContextVS contextVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private TextView imageCaption;
    private UserVS representative;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String broadCastId = null;
    private String representativeImageName = null;
    private String editorContent = null;
    private Uri representativeImageUri = null;
    private Menu menu;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            TypeVS broadcastType = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                    ResponseVS.SC_ERROR);
            String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
            String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) launchSignAndSendService();
            else {
                if(TypeVS.NIF_REQUEST == broadcastType) {
                    if(ResponseVS.SC_OK == responseStatusCode) {
                        String representativeNif = NifUtils.validate(message);
                        if(representativeNif == null) {
                            new AlertDialog.Builder(
                                    NewRepresentativeActivity.this).setTitle(getString(R.string.error_lbl)).
                                    setMessage(getString(R.string.nif_error)).setPositiveButton(getString(R.string.ok_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            showNifDialog();
                                        }
                                    }).setCancelable(false).show();
                        } else loadRepresentativeData(representativeNif);
                    } else {
                        NewRepresentativeActivity.this.onBackPressed();
                    }
                } else if(TypeVS.NEW_REPRESENTATIVE == broadcastType) {
                    showProgress(false, true);
                    showMessage(responseStatusCode, caption, message);
                    if(ResponseVS.SC_OK != responseStatusCode) {
                        editorFragment.setEditable(true);
                        if(menu != null) getMenuInflater().inflate(R.menu.editor, menu);
                    } else {
                        imageCaption.setOnClickListener(null);
                    }
                } else if(TypeVS.ITEM_REQUEST == broadcastType) {
                    if(ResponseVS.SC_OK == responseStatusCode) {
                        Uri representativeURI = intent.getParcelableExtra(ContextVS.URI_KEY);
                        Cursor cursor = NewRepresentativeActivity.this.getApplicationContext().
                                getContentResolver().query(representativeURI,
                                null, null, null, null);
                        cursor.moveToFirst();
                        UserVS representative = (UserVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                                cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
                        printRepresentativeData(representative);
                    }
                    showProgress(false, true);
                }
            }
        }
    };

    private void launchSignAndSendService() {
        Log.d(TAG + ".launchSignAndSendService(...) ", "");
        editorFragment.setEditable(false);
        String serviceURL = contextVS.getAccessControl().getRepresentativeServiceURL();
        String signedMessageSubject = null;
        try {
            Intent startIntent = new Intent(getApplicationContext(), RepresentativeService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.NEW_REPRESENTATIVE);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.URL_KEY, serviceURL);
            startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                    ContentTypeVS.JSON_SIGNED);
            startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, signedMessageSubject);
            startIntent.putExtra(ContextVS.MESSAGE_KEY, editorContent);
            startIntent.putExtra(ContextVS.URI_KEY, representativeImageUri);
            showProgress(true, true);
            startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        broadCastId = ((Object)this).getClass().getSimpleName();
        operationType = (TypeVS) getIntent().getSerializableExtra(ContextVS.TYPEVS_KEY);
        Log.d(TAG + ".onCreate(...)", "operationType: " + operationType +
                " - savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.new_representative);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        editorFragment = (EditorFragment) getSupportFragmentManager().findFragmentByTag(
                EditorFragment.TAG);
        mainLayout = (FrameLayout)findViewById(R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        imageCaption = (TextView) findViewById(R.id.representative_image_caption);
        imageCaption.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openFileChooser();
            }
        });
        mainLayout.getForeground().setAlpha(0);
        if(operationType != null && TypeVS.REPRESENTATIVE == operationType) {
            File representativeDataFile = new File(getApplicationContext().getFilesDir(),
                    ContextVS.REPRESENTATIVE_DATA_FILE_NAME);
            if(!representativeDataFile.exists()) showNifDialog();
            else {
                try {
                    byte[] serializedRepresentative = FileUtils.getBytesFromFile(
                            representativeDataFile);
                    UserVS representativeData = (UserVS) ObjectUtils.deSerializeObject(
                            serializedRepresentative);
                    printRepresentativeData(representativeData);
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            getSupportActionBar().setTitle(getString(R.string.edit_representative_lbl));
        }
        if(savedInstanceState != null) {
            representativeImageUri = (Uri) savedInstanceState.getParcelable(ContextVS.URI_KEY);
            representativeImageName = savedInstanceState.getString(ContextVS.ICON_KEY);
            if(representativeImageUri != null) {
                setRepresentativeImage(representativeImageUri, representativeImageName);
            }
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false)) showProgress(true, true);
        }
    }

    private void showNifDialog() {
        String caption = getString(R.string.edit_representative_lbl);
        String message = getString(R.string.representative_nif_lbl);
        NewFieldDialogFragment newFieldDialog = NewFieldDialogFragment.newInstance(caption,
                message, broadCastId,  TypeVS.NIF_REQUEST);
        newFieldDialog.show(getSupportFragmentManager(), NewFieldDialogFragment.TAG);
    }

    private void printRepresentativeData(UserVS representativeData) {
        this.representative = representativeData;
        editorFragment.setEditorData(representativeData.getDescription());
        setRepresentativeImage(representativeData.getImageBytes(), null);
    }

    private void loadRepresentativeData(String representativeNif) {
        TextView progressMessage = (TextView) findViewById(R.id.progressContainer);
        progressMessage.setText(getString(R.string.loading_data_msg));
        showProgress(true, true);
        Intent startIntent = new Intent(this, RepresentativeService.class);
        startIntent.putExtra(ContextVS.NIF_KEY, representativeNif);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.NIF_REQUEST);
        startService(startIntent);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.save_editor:
                if(validateForm()) {
                    editorContent = editorFragment.getEditorData();
                    menu.removeGroup(R.id.general_items);
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                            null, false, null);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); -> To select multiple images
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.select_img_lbl)), SELECT_PICTURE);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG + ".onCreateOptionsMenu(...)", "");
        getMenuInflater().inflate(R.menu.editor, menu);
        this.menu = menu;
        if(operationType == TypeVS.REPRESENTATIVE && representative == null)
            this.menu.setGroupVisible(R.id.general_items, false);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean validateForm () {
        Log.d(TAG + ".validateForm()", "");
        if(editorFragment == null || editorFragment.isEditorDataEmpty()) {
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.editor_empty_error_lbl));
            return false;
        }
        if(representativeImageUri == null && operationType != TypeVS.REPRESENTATIVE) {
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.missing_representative_img_error_msg));
            return false;
        }
        return true;
    }

    //https://developer.android.com/guide/topics/providers/document-provider.html
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG + ".onActivityResult(...)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode); //Activity.RESULT_OK;
        if(SELECT_PICTURE == requestCode) {
            if(data != null && data.getData() != null) {
                try {
                    Intent intent = new Intent(this, ConfirmImageActivity.class);
                    intent.putExtra(ContextVS.URI_KEY, data.getData());
                    startActivityForResult(intent, CONFIRM_PICTURE);
                    //setRepresentativeImage(representativeImageUri, representativeImageName);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else if(CONFIRM_PICTURE == requestCode) {
            if(Activity.RESULT_OK == resultCode) {
                representativeImageUri = data.getData();
                Cursor cursor = getContentResolver().query(
                        representativeImageUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    representativeImageName = cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
                File representativeDataFile = new File(getApplicationContext().getFilesDir(),
                        ContextVS.REPRESENTATIVE_DATA_FILE_NAME);
                try {
                    byte[] serializedRepresentative = FileUtils.getBytesFromFile(
                            representativeDataFile);
                    UserVS representativeData = (UserVS) ObjectUtils.deSerializeObject(
                            serializedRepresentative);
                    setRepresentativeImage(representativeData.getImageBytes(), representativeImageName);
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void setRepresentativeImage(byte[] imageBytes, String imageName) {
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ImageView image = (ImageView)findViewById(R.id.representative_image);
            image.setImageBitmap(bitmap);
            TextView imagePathTextView = (TextView) findViewById(R.id.representative_image_path);
            ((TextView) findViewById(R.id.representative_image_caption)).setText(getString(
                    R.string.representative_image_lbl));
            imagePathTextView.setText(imageName);
            LinearLayout imageContainer = (LinearLayout) findViewById(R.id.imageContainer);
            imageContainer.setVisibility(View.VISIBLE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setRepresentativeImage(Uri imageUri, String imageName) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(imageUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            ImageView image = (ImageView)findViewById(R.id.representative_image);
            image.setImageBitmap(bitmap);
            TextView imagePathTextView = (TextView) findViewById(R.id.representative_image_path);
            ((TextView) findViewById(R.id.representative_image_caption)).setText(getString(
                    R.string.representative_image_lbl));
            imagePathTextView.setText(imageName);
            LinearLayout imageContainer = (LinearLayout) findViewById(R.id.imageContainer);
            imageContainer.setVisibility(View.VISIBLE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ContextVS.URI_KEY, representativeImageUri);
        outState.putSerializable(ContextVS.ICON_KEY, representativeImageName);
        outState.putSerializable(ContextVS.LOADING_KEY, progressVisible.get());
        Log.d(TAG + ".onSaveInstanceState(...)", "outState: " + outState);
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
            if(TypeVS.REPRESENTATIVE == operationType) ((TextView)findViewById(R.id.progressMessage)).
                    setText(getString(R.string.updating_representative_data_msg));
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
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override protected void onStop() {
        super.onStop();
    	Log.d(TAG + ".onStop()", "onStop");
    };

}