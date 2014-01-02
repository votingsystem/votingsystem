package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
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
        if(arguments != null && arguments.containsKey(ContextVS.URL_KEY)) {
            ResponseVS responseVS = HttpHelper.getData(arguments.getString(ContextVS.URL_KEY),
                    ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    UserVSResponse response = UserVSResponse.parse(responseVS.getMessage());
                    RepresentativeContentProvider.setNumTotalRepresentatives(response.getNumTotalRepresentatives());
                    List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                    for(UserVS representative : response.getUsers()) {
                        ContentValues values = new ContentValues(5);
                        values.put(RepresentativeContentProvider.ID_COL, representative.getId());
                        values.put(RepresentativeContentProvider.URL_COL, representative.getURL());
                        values.put(RepresentativeContentProvider.FULL_NAME_COL, representative.getFullName());
                        values.put(RepresentativeContentProvider.NIF_COL, representative.getNif());
                        values.put(RepresentativeContentProvider.NUM_REPRESENTATIONS_COL, representative.getNumRepresentations());
                        contentValuesList.add(values);
                    }
                    if(!contentValuesList.isEmpty()) {
                        int numRowsCreated = getContentResolver().bulkInsert(
                                RepresentativeContentProvider.CONTENT_URI, contentValuesList.toArray(
                                new ContentValues[contentValuesList.size()]));
                        Log.d(TAG + ".onHandleIntent(...)", "inserted: " + numRowsCreated +" rows");
                    } else Log.d(TAG + ".onHandleIntent(...)", "Response empty");
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
