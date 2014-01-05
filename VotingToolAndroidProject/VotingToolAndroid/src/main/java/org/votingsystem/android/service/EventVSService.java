package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.EventVSResponse;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.HttpHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventVSService extends IntentService {

    public static final String TAG = "EventVSService";

    public EventVSService() { super(TAG); }

    public void checkDates(EventVS eventVS) {
        Date todayDate = Calendar.getInstance().getTime();
        final String checkURL = ContextVS.getInstance(this).getAccessControl().
                getCheckDatesServiceURL(eventVS.getId());
        Runnable runnable = null;
        if(eventVS.getState() == EventVS.State.ACTIVE) {
            if(todayDate.after(eventVS.getDateFinish())){
                runnable = new Runnable() {
                    public void run() {HttpHelper.getData(checkURL, null);}
                };
            }
        } else if(eventVS.getState() == EventVS.State.AWAITING) {
            if(todayDate.after(eventVS.getDateBegin())){
                runnable = new Runnable() {
                    public void run() {HttpHelper.getData(checkURL, null);}
                };
            }
        }
        if(runnable != null) new Thread(runnable).start();
    }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        if(arguments != null && arguments.containsKey(ContextVS.STATE_KEY)
                && arguments.containsKey(ContextVS.EVENT_TYPE_KEY)
                && arguments.containsKey(ContextVS.OFFSET_KEY)) {
            String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
            EventVS.State eventState = (EventVS.State) arguments.getSerializable(ContextVS.STATE_KEY);
            TypeVS eventType = (TypeVS)arguments.getSerializable(ContextVS.EVENT_TYPE_KEY);
            Long offset = arguments.getLong(ContextVS.OFFSET_KEY);
            String serviceURL = ContextVS.getInstance(this).getAccessControl().getEventVSURL(
                    eventState, EventVS.getURLPart(eventType),
                    ContextVS.EVENTS_PAGE_SIZE, offset);
            ResponseVS responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    EventVSResponse response = EventVSResponse.parse(responseVS.getMessage());
                    if(response.getNumEventsVSClaimInSystem() != null) {
                        switch (eventState) {
                            case ACTIVE:
                                EventVSContentProvider.setNumTotalClaimsActive(
                                        Long.valueOf(response.getNumEventsVSClaimInSystem()));
                                break;
                            case AWAITING:
                                EventVSContentProvider.setNumTotalClaimsPending(
                                        Long.valueOf(response.getNumEventsVSClaimInSystem()));
                                break;
                            case TERMINATED:
                                EventVSContentProvider.setNumTotalClaimsTerminated(
                                        Long.valueOf(response.getNumEventsVSClaimInSystem()));
                                break;
                        }
                    } else if(response.getNumEventsVSElectionInSystem() != null) {
                        switch (eventState) {
                            case ACTIVE:
                                EventVSContentProvider.setNumTotalElectionsActive(
                                        Long.valueOf(response.getNumEventsVSElectionInSystem()));
                                break;
                            case AWAITING:
                                EventVSContentProvider.setNumTotalElectionsPending(
                                        Long.valueOf(response.getNumEventsVSElectionInSystem()));
                                break;
                            case TERMINATED:
                                EventVSContentProvider.setNumTotalElectionsTerminated(
                                        Long.valueOf(response.getNumEventsVSElectionInSystem()));
                                break;
                        }
                    } else if(response.getNumEventsVSManifestInSystem() != null) {
                        switch (eventState) {
                            case ACTIVE:
                                EventVSContentProvider.setNumTotalManifestsActive(
                                        Long.valueOf(response.getNumEventsVSManifestInSystem()));
                                break;
                            case AWAITING:
                                EventVSContentProvider.setNumTotalManifestsPending(
                                        Long.valueOf(response.getNumEventsVSManifestInSystem()));
                                break;
                            case TERMINATED:
                                EventVSContentProvider.setNumTotalManifestsTerminated(
                                        Long.valueOf(response.getNumEventsVSManifestInSystem()));
                                break;
                        }
                    }
                    List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                    for(EventVS eventVS : response.getEvents()) {
                        EventVS.State eventVSState = eventVS.getState();
                        if(eventVSState == EventVS.State.CANCELLED) eventVSState =
                                EventVS.State.TERMINATED;
                        checkDates(eventVS);
                        ContentValues values = new ContentValues(5);
                        values.put(EventVSContentProvider.ID_COL, eventVS.getId());
                        values.put(EventVSContentProvider.URL_COL, eventVS.getURL());
                        values.put(EventVSContentProvider.JSON_DATA_COL, eventVS.toJSON().toString());
                        values.put(EventVSContentProvider.TYPE_COL, eventVS.getTypeVS().toString());
                        values.put(EventVSContentProvider.STATE_COL, eventVSState.toString());
                        values.put(EventVSContentProvider.TIMESTAMP_CREATED_COL,
                                System.currentTimeMillis());
                        values.put(EventVSContentProvider.TIMESTAMP_UPDATED_COL,
                                System.currentTimeMillis());
                        contentValuesList.add(values);
                    }
                    if(!contentValuesList.isEmpty()) {
                        int numRowsCreated = getContentResolver().bulkInsert(
                                EventVSContentProvider.CONTENT_URI, contentValuesList.toArray(
                                new ContentValues[contentValuesList.size()]));
                        Log.d(TAG + ".onHandleIntent(...)", "inserted: " + numRowsCreated + " rows" +
                            " - eventType: " + eventType + " - eventState: " + eventState);
                    } else { //To notify ContentProvider Listeners
                        getContentResolver().insert(EventVSContentProvider.CONTENT_URI, null);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sendMessage(ResponseVS.SC_ERROR, getString(R.string.alert_exception_caption),
                            ex.getMessage(), serviceCaller);

                }
            } else sendMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                    responseVS.getMessage(), serviceCaller);
        }
    }


    private void sendMessage(Integer statusCode, String caption, String message,
            String serviceCaller) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + statusCode + " - serviceCaller: " +
                serviceCaller  + " - caption: " + caption  + " - message: " + message );
        Intent intent = new Intent(serviceCaller);
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
