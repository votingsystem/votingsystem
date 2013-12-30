package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.contentprovider.RepresentativeContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.EventVSResponse;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSResponse;
import org.votingsystem.util.HttpHelper;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.mail.Provider;

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

    private void sendMessage(Integer statusCode, String message, Long offset,
            String eventType, String state) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + statusCode + " - offset: " +
                offset  + " - eventType: " + eventType +
                " - state: " + state);
        Intent intent = new Intent(ContextVS.HTTP_DATA_INITIALIZED_ACTION_ID);
        if(statusCode != null)
            intent.putExtra(ContextVS.HTTP_RESPONSE_STATUS_KEY, statusCode.intValue());
        if(message != null)
            intent.putExtra(ContextVS.HTTP_RESPONSE_DATA_KEY, statusCode.intValue());
        if(offset != null) intent.putExtra(ContextVS.OFFSET_KEY, offset);
        if(state != null) intent.putExtra(ContextVS.STATE_KEY, state);
        if(eventType != null) intent.putExtra(ContextVS.EVENT_TYPE_KEY, eventType);
        LocalBroadcastManager.getInstance(EventVSService.this).sendBroadcast(intent);
    }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        Log.d(TAG + ".onHandleIntent(...) ", "arguments: " + arguments);
        if(arguments != null && arguments.containsKey(ContextVS.STATE_KEY)
                && arguments.containsKey(ContextVS.EVENT_TYPE_KEY)
                && arguments.containsKey(ContextVS.OFFSET_KEY)) {
            String eventStateStr = arguments.getString(ContextVS.STATE_KEY);
            EventVS.State eventState = EventVS.State.valueOf(eventStateStr);
            String eventTypeStr = arguments.getString(ContextVS.EVENT_TYPE_KEY);
            Long offset = arguments.getLong(ContextVS.OFFSET_KEY);
            String serviceURL = ContextVS.getInstance(this).getAccessControl().getEventVSURL(
                    eventState, EventVS.getURLPart(TypeVS.valueOf(eventTypeStr)),
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
                    for(EventVS eventVS : response.getEvents()) {
                        checkDates(eventVS);
                        ContentValues values = new ContentValues(5);
                        values.put(EventVSContentProvider.ID_COL, eventVS.getId());
                        values.put(EventVSContentProvider.URL_COL, eventVS.getURL());
                        values.put(EventVSContentProvider.JSON_DATA_COL, eventVS.toJSON().toString());
                        values.put(EventVSContentProvider.TYPE_COL, eventVS.getTypeVS().toString());
                        values.put(EventVSContentProvider.STATE_COL, eventVS.getState().toString());
                        Uri uri = getContentResolver().insert(
                                EventVSContentProvider.CONTENT_URI, values);
                        Log.d(TAG + ".loadURLData(...)", "inserted event: " + uri.toString());
                    }
                    sendMessage(responseVS.getStatusCode(), responseVS.getMessage(),
                            Long.valueOf(response.getOffset()), eventTypeStr, eventStateStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else sendMessage(responseVS.getStatusCode(), responseVS.getMessage(), null,
                    eventTypeStr, eventStateStr);
        }
    }

}
