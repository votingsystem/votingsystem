package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.RepresentativeDelegationActivity;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeFragment extends Fragment {

	public static final String TAG = RepresentativeFragment.class.getSimpleName();

    private static final int REPRESENTATIVE_DELEGATION   = 1;

    private ModalProgressDialogFragment progressDialog;
    private View rootView;
    private String broadCastId = null;
    private AppContextVS contextVS;
    private Button selectButton;
    private Long representativeId;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver",
                "extras:" + intent.getExtras());
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) == null) {
            if(TypeVS.ITEM_REQUEST == typeVS) {
                Cursor cursor = getActivity().getApplicationContext().getContentResolver().
                        query(UserContentProvider.getRepresentativeURI(representativeId),
                                null, null, null, null);
                cursor.moveToFirst();
                UserVS representative = (UserVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                        cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
                printRepresentativeData(representative);
                setProgressDialogVisible(false);
            }
        }
        }
    };


    public static Fragment newInstance(Long representativeId) {
        RepresentativeFragment fragment = new RepresentativeFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.USER_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        representativeId =  getArguments().getLong(ContextVS.USER_KEY);
        Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                UserContentProvider.getRepresentativeURI(representativeId),
                null, null, null, null);
        cursor.moveToFirst();
        final UserVS representative = (UserVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
        rootView = inflater.inflate(R.layout.representative, container, false);
        selectButton = (Button) rootView.findViewById(R.id.select_representative_button);
        selectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), RepresentativeDelegationActivity.class);
                intent.putExtra(ContextVS.USER_KEY, representative);
                startActivityForResult(intent, REPRESENTATIVE_DELEGATION);
            }
        });
        selectButton.setVisibility(View.GONE);
        setHasOptionsMenu(true);
        broadCastId = RepresentativeFragment.class.getSimpleName() + "_" + representativeId;
        if(representative.getDescription() != null) {
            printRepresentativeData(representative);
        } else {
            setProgressDialogVisible(true);
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    RepresentativeService.class);
            startIntent.putExtra(ContextVS.ITEM_ID_KEY, representativeId);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.ITEM_REQUEST);
            getActivity().startService(startIntent);
        }
        return rootView;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        int statusCode = -1;
        String caption = null;
        String message = null;
        if(data != null) message = data.getStringExtra(ContextVS.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            statusCode = ResponseVS.SC_OK;
            caption = getString(R.string.operation_ok_msg);
            showMessage(statusCode, caption, message);
        } else if(message != null) {
            statusCode = ResponseVS.SC_ERROR;
            caption = getString(R.string.operation_error_msg);
            showMessage(statusCode, caption, message);
        }
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            progressDialog = ModalProgressDialogFragment.showDialog(
                    getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg),
                    getFragmentManager());
        } else if(progressDialog != null) progressDialog.dismiss();
    }

    private void printRepresentativeData(UserVS representative) {
        if(representative.getImageBytes() != null) {
            final Bitmap bmp = BitmapFactory.decodeByteArray(representative.getImageBytes(), 0,
                    representative.getImageBytes().length);
            ImageView image = (ImageView) rootView.findViewById(R.id.representative_image);
            image.setImageBitmap(bmp);
            image.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ImageView imageView = new ImageView(getActivity());
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    imageView.setImageBitmap(bmp);
                    new AlertDialog.Builder(getActivity()).setView(imageView).show();
                }
            });
        }
        if(representative.getDescription() != null) {
            String representativeDescription =
                    "<html style='background-color:#eeeeee;margin: 5px 10px 10px 10px;'>" +
                    representative.getDescription() + "</html>";
            ((WebView)rootView.findViewById(R.id.representative_description)).loadData(
                    representativeDescription, "text/html", "utf-8");
        }
        selectButton.setVisibility(View.VISIBLE);
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.representative, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
    }

}