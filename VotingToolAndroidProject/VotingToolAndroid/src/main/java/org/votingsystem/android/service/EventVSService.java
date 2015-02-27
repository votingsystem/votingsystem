package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSSearchResultActivity;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.EventVSResponse;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSService extends IntentService {

    public static final String TAG = EventVSService.class.getSimpleName();

    public EventVSService() { super(TAG); }

    private AppContextVS contextVS = null;

    public void checkDates(EventVS eventVS) {
        Date todayDate = Calendar.getInstance().getTime();
        final String checkURL = contextVS.getAccessControl().
                getCheckDatesServiceURL(eventVS.getId());
        Runnable runnable = null;
        if(eventVS.getState() == EventVS.State.ACTIVE) {
            if(todayDate.after(eventVS.getDateFinish())){
                runnable = new Runnable() {
                    public void run() {HttpHelper.getData(checkURL, null);}
                };
            }
        } else if(eventVS.getState() == EventVS.State.PENDING) {
            if(todayDate.after(eventVS.getDateBegin())){
                runnable = new Runnable() {
                    public void run() {HttpHelper.getData(checkURL, null);}
                };
            }
        }
        if(runnable != null) new Thread(runnable).start();
    }

    @Override protected void onHandleIntent(Intent intent) {
        LOGD(TAG + ".onHandleIntent", "onHandleIntent ");
        ResponseVS responseVS = null;
        final Bundle arguments = intent.getExtras();
        contextVS = (AppContextVS) getApplicationContext();
        if(arguments != null && arguments.containsKey(ContextVS.STATE_KEY)
                && arguments.containsKey(ContextVS.OFFSET_KEY)) {
            String queryStr = arguments.getString(ContextVS.QUERY_KEY);
            EventVS.State eventState = (EventVS.State) arguments.getSerializable(ContextVS.STATE_KEY);
            Long offset = arguments.getLong(ContextVS.OFFSET_KEY);
            if(contextVS.getAccessControl() == null) {
                LOGD(TAG, "AccessControl not initialized");
                return;
            }
            if(queryStr != null) {
                processSearch(queryStr, eventState);
                return;
            }
            String serviceURL = contextVS.getAccessControl().getEventVSURL(eventState,
                    ContextVS.EVENTS_PAGE_SIZE, offset);
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    EventVSResponse response = EventVSResponse.parse(responseVS.getMessage(),
                            TypeVS.VOTING_EVENT);
                    switch (eventState) {
                        case ACTIVE:
                            EventVSContentProvider.setNumTotalElectionsActive(
                                    Long.valueOf(response.getNumEventsVSElectionInSystem()));
                            break;
                        case PENDING:
                            EventVSContentProvider.setNumTotalElectionsPending(
                                    Long.valueOf(response.getNumEventsVSElectionInSystem()));
                            break;
                        case TERMINATED:
                            EventVSContentProvider.setNumTotalElectionsTerminated(
                                    Long.valueOf(response.getNumEventsVSElectionInSystem()));
                            break;
                    }
                    List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                    for(EventVS eventVS : response.getEvents()) {
                        EventVS.State eventVSState = eventVS.getState();
                        if(eventVSState == EventVS.State.CANCELLED) eventVSState =
                                EventVS.State.TERMINATED;
                        checkDates(eventVS);
                        ContentValues values = new ContentValues(5);
                        values.put(EventVSContentProvider.SQL_INSERT_OR_REPLACE, true );
                        values.put(EventVSContentProvider.ID_COL, eventVS.getId());
                        values.put(EventVSContentProvider.URL_COL, eventVS.getURL());
                        values.put(EventVSContentProvider.JSON_DATA_COL, eventVS.toJSON().toString());
                        values.put(EventVSContentProvider.TYPE_COL, eventVS.getTypeVS().toString());
                        values.put(EventVSContentProvider.STATE_COL, eventVSState.toString());
                        contentValuesList.add(values);
                    }
                    if(!contentValuesList.isEmpty()) {
                        int numRowsCreated = getContentResolver().bulkInsert(
                                EventVSContentProvider.CONTENT_URI, contentValuesList.toArray(
                                new ContentValues[contentValuesList.size()]));
                        LOGD(TAG + ".onHandleIntent", "inserted: " + numRowsCreated + " rows" +
                            " - eventState: " + eventState);
                    } else { //To notify ContentProvider Listeners
                        getContentResolver().insert(EventVSContentProvider.CONTENT_URI, null);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseVS = ResponseVS.EXCEPTION(ex, this);
                }
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
            contextVS.broadcastResponse(responseVS);
        } else LOGD(TAG + ".onHandleIntent", "missing params");
    }

    private void processSearch(String queryStr, EventVS.State eventState) {
        String serviceURL = contextVS.getAccessControl().getSearchServiceURL(null, null, queryStr,
                EventVS.Type.ELECTION, eventState);
        ResponseVS responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
        Intent intent = new Intent(this, EventVSSearchResultActivity.class);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        intent.putExtra(ContextVS.EVENT_STATE_KEY, eventState);
        intent.putExtra(ContextVS.QUERY_KEY, queryStr);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
