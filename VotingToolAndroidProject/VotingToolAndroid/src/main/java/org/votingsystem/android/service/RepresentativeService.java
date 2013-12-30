package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.json.JSONException;
import org.votingsystem.android.contentprovider.RepresentativeContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSResponse;
import org.votingsystem.util.HttpHelper;
import java.text.ParseException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeService extends IntentService {

    public static final String TAG = "RepresentativeService";

    public RepresentativeService() { super(TAG); }

    private void sendMessage(Integer statusCode, String message, Long offset, Long numTotal) {
        Intent intent = new Intent(ContextVS.HTTP_DATA_INITIALIZED_ACTION_ID);
        if(statusCode != null)
            intent.putExtra(ContextVS.HTTP_RESPONSE_STATUS_KEY, statusCode.intValue());
        if(message != null)
            intent.putExtra(ContextVS.HTTP_RESPONSE_DATA_KEY, statusCode.intValue());
        if(offset != null) intent.putExtra(ContextVS.OFFSET_KEY, offset);
        if(numTotal != null) intent.putExtra(ContextVS.NUM_TOTAL_KEY, numTotal);
        LocalBroadcastManager.getInstance(RepresentativeService.this).sendBroadcast(intent);
    }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        Log.d(TAG + ".onHandleIntent(...) ", "arguments: " + arguments);
        if(arguments != null && arguments.containsKey(ContextVS.URL_KEY)) {
            ResponseVS responseVS = HttpHelper.getData(arguments.getString(ContextVS.URL_KEY),
                    ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    UserVSResponse response = UserVSResponse.parse(responseVS.getMessage());
                    for(UserVS representative : response.getUsers()) {
                        ContentValues values = new ContentValues(5);
                        values.put(RepresentativeContentProvider.ID_COL, representative.getId());
                        values.put(RepresentativeContentProvider.URL_COL, representative.getURL());
                        values.put(RepresentativeContentProvider.FULL_NAME_COL, representative.getFullName());
                        values.put(RepresentativeContentProvider.NIF_COL, representative.getNif());
                        values.put(RepresentativeContentProvider.NUM_REPRESENTATIONS_COL, representative.getNumRepresentations());
                        Uri uri = getContentResolver().insert(
                                RepresentativeContentProvider.CONTENT_URI, values);
                        Log.d(TAG + ".loadURLData(...)", "inserted representative: " + uri.toString());
                    }
                    sendMessage(responseVS.getStatusCode(), responseVS.getMessage(),response.getOffset(),
                            response.getNumTotalRepresentatives());
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else sendMessage(responseVS.getStatusCode(), responseVS.getMessage(), null, null);
        }
    }

}
