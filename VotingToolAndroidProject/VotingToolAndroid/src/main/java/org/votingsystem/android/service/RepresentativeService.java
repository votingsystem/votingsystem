package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSResponse;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeService extends IntentService {

    public static final String TAG = "RepresentativeService";

    public RepresentativeService() { super(TAG); }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        Log.d(TAG + ".onHandleIntent(...) ", "arguments: " + arguments);
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        if(arguments != null && arguments.containsKey(ContextVS.URL_KEY)) {
            if(operation == TypeVS.ITEMS_REQUEST) {
                requestRepresentatives(arguments.getString(
                        ContextVS.URL_KEY), arguments.getString(ContextVS.CALLER_KEY));
            } else if (operation == TypeVS.ITEM_REQUEST) {
                requestRepresentative(arguments.getLong(ContextVS.ITEM_ID_KEY),
                        arguments.getString(ContextVS.CALLER_KEY));
            }
        }
    }

    private void requestRepresentatives(String serviceURL, String serviceCaller) {
        ResponseVS responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            try {
                JSONObject requestJSON = new JSONObject(responseVS.getMessage());
                UserVSResponse response = UserVSResponse.populate(requestJSON);
                UserContentProvider.setNumTotalRepresentatives(
                        response.getNumTotalRepresentatives());
                List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                for(UserVS representative : response.getUsers()) {
                    ContentValues values = new ContentValues();
                    values.put(UserContentProvider.SQL_INSERT_OR_REPLACE, true );
                    values.put(UserContentProvider.ID_COL, representative.getId());
                    values.put(UserContentProvider.URL_COL, representative.getURL());
                    values.put(UserContentProvider.FULL_NAME_COL, representative.getFullName());
                    values.put(UserContentProvider.SERIALIZED_OBJECT_COL,
                            ObjectUtils.serializeObject(representative));
                    values.put(UserContentProvider.NIF_COL, representative.getNif());
                    values.put(UserContentProvider.NUM_REPRESENTATIONS_COL,
                            representative.getNumRepresentations());
                    values.put(UserContentProvider.TIMESTAMP_CREATED_COL,
                            System.currentTimeMillis());
                    values.put(UserContentProvider.TIMESTAMP_UPDATED_COL,
                            System.currentTimeMillis());
                    contentValuesList.add(values);
                }
                if(!contentValuesList.isEmpty()) {
                    int numRowsCreated = getContentResolver().bulkInsert(
                            UserContentProvider.CONTENT_URI,contentValuesList.toArray(
                            new ContentValues[contentValuesList.size()]));
                    Log.d(TAG + ".onHandleIntent(...)", "inserted: " + numRowsCreated +" rows");
                } else { //To notify ContentProvider Listeners
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, null);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                sendMessage(ResponseVS.SC_ERROR, getString(R.string.alert_exception_caption),
                        ex.getMessage(), TypeVS.ITEMS_REQUEST, serviceCaller);

            }
        } else sendMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                responseVS.getMessage(),TypeVS.ITEMS_REQUEST, serviceCaller);
    }

    private void requestRepresentative(Long representativeId, String serviceCaller) {
        String serviceURL = ContextVS.getInstance(this).getAccessControl().
                getRepresentativeURL(representativeId);
        String imageServiceURL = ContextVS.getInstance(this).getAccessControl().
                getRepresentativeImageURL(representativeId);
        byte[] representativeImageBytes = null;
        try {
            ResponseVS responseVS = HttpHelper.getData(imageServiceURL, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                representativeImageBytes = responseVS.getMessageBytes();
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject requestJSON = new JSONObject(responseVS.getMessage());
                UserVS representative = UserVS.populate(requestJSON);
                representative.setImageBytes(representativeImageBytes);
                ContentValues values = new ContentValues();
                values.put(UserContentProvider.SQL_INSERT_OR_REPLACE, true );
                values.put(UserContentProvider.ID_COL, representative.getId());
                values.put(UserContentProvider.URL_COL, representative.getURL());
                values.put(UserContentProvider.TYPE_COL, UserVS.Type.REPRESENTATIVE.toString());
                values.put(UserContentProvider.FULL_NAME_COL, representative.getFullName());
                values.put(UserContentProvider.SERIALIZED_OBJECT_COL,
                        ObjectUtils.serializeObject(representative));
                values.put(UserContentProvider.NIF_COL, representative.getNif());
                values.put(UserContentProvider.NUM_REPRESENTATIONS_COL,
                        representative.getNumRepresentations());
                values.put(UserContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
                values.put(UserContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
                getContentResolver().insert(UserContentProvider.CONTENT_URI, null);
                sendMessage(responseVS.getStatusCode(), null, null, TypeVS.ITEMS_REQUEST, serviceCaller);
            } else sendMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                    responseVS.getMessage(), TypeVS.ITEMS_REQUEST, serviceCaller);
        } catch(Exception ex) {
            ex.printStackTrace();
            sendMessage(ResponseVS.SC_ERROR, getString(R.string.operation_error_msg),
                    ex.getMessage(), TypeVS.ITEMS_REQUEST, serviceCaller);
        }
    }

    private void sendMessage(Integer statusCode, String caption, String message, TypeVS typeVS,
             String serviceCaller) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + statusCode + " - caption: " +
                caption  + " - message: " + message + " - serviceCaller: " + serviceCaller);
        Intent intent = new Intent(serviceCaller);
        intent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
        if(statusCode != null) {
            intent.putExtra(ContextVS.RESPONSE_STATUS_KEY, statusCode.intValue());
            if(ResponseVS.SC_CONNECTION_TIMEOUT == statusCode)
                message = getString(R.string.conn_timeout_msg);
        }
        if(caption != null) intent.putExtra(ContextVS.CAPTION_KEY, caption);
        if(message != null) intent.putExtra(ContextVS.MESSAGE_KEY, message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}
