package org.votingsystem.android.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
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

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.RepresentativeDelegationActivity;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeFragment extends Fragment {

	public static final String TAG = RepresentativeFragment.class.getSimpleName();

    private static final int REPRESENTATIVE_DELEGATION   = 1;

    private AppContextVS contextVS = null;
    private View rootView;
    private String broadCastId = null;
    private Button selectButton;
    private Long representativeId;
    private UserVS representative;
    private ImageView representative_image;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = (ResponseVS) intent.getSerializableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) == null) {
            if(TypeVS.ITEM_REQUEST == responseVS.getTypeVS()) {
                printRepresentativeData((UserVS) intent.getSerializableExtra(ContextVS.USER_KEY));
                setProgressDialogVisible(false);
            }
        }
        }
    };

    public static Fragment newInstance(Long representativeId) {
        RepresentativeFragment fragment = new RepresentativeFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.CURSOR_POSITION_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        representativeId =  getArguments().getLong(ContextVS.CURSOR_POSITION_KEY);
        Cursor cursor = getActivity().getContentResolver().query(UserContentProvider.
                getUserVSURI(representativeId), null, null, null, null);
        cursor.moveToFirst();
        final UserVS representative = (UserVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
        rootView = inflater.inflate(R.layout.representative, container, false);
        representative_image = (ImageView) rootView.findViewById(R.id.representative_image);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
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
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        if(representative.getDescription() != null) printRepresentativeData(representative);
        else {
            setProgressDialogVisible(true);
            Intent startIntent = new Intent(getActivity(), RepresentativeService.class);
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
        String message = null;
        if(data != null) message = data.getStringExtra(ContextVS.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            MessageDialogFragment.showDialog(ResponseVS.SC_OK, getString(R.string.operation_ok_msg),
                    message, getFragmentManager());
        } else if(message != null) MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                    getString(R.string.operation_error_msg), message, getFragmentManager());
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    private void printRepresentativeData(UserVS representative) {
        this.representative = representative;
        if(representative.getImageBytes() != null)
            UIUtils.setImage(representative_image, representative.getImageBytes(), getActivity());
        else new ImageDownloaderTask(representativeId).execute();
        if(representative.getDescription() != null) {
            String representativeDescription = "<html style='background-color:#eeeeee;'>" +
                    representative.getDescription() + "</html>";
            ((WebView)rootView.findViewById(R.id.representative_description)).loadData(
                    representativeDescription, "text/html; charset=UTF-8", "UTF-8");
        }
        selectButton.setVisibility(View.VISIBLE);
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

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class ImageDownloaderTask extends AsyncTask<String, Void, ResponseVS> {

        Long representativeId;

        public ImageDownloaderTask(Long representativeId) {
            this.representativeId = representativeId;
        }

        @Override protected void onPreExecute() { }

        @Override protected ResponseVS doInBackground(String... urls) {
            return  HttpHelper.getData(contextVS.getAccessControl().
                    getRepresentativeImageURL(representativeId), null);
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            LOGD(TAG + "ImageDownloaderTask.onPostExecute() ", "statusCode: " +
                    responseVS.getStatusCode());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                UIUtils.setImage(representative_image, responseVS.getMessageBytes(), getActivity());
                representative.setImageBytes(responseVS.getMessageBytes());
                getActivity().getContentResolver().insert(UserContentProvider.CONTENT_URI,
                        UserContentProvider.getContentValues(representative));
            } else MessageDialogFragment.showDialog(responseVS, getFragmentManager());
        }

    }

}