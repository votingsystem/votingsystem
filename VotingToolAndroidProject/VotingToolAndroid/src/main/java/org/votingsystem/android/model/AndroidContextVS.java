package org.votingsystem.android.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.votingsystem.android.util.EventState;
import org.votingsystem.android.util.SubSystem;
import org.votingsystem.android.util.SubSystemChangeListener;
import org.votingsystem.model.*;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AndroidContextVS extends ContextVS {
	
	public static final String TAG = "AndroidContextVS";

    public enum State {CON_CERTIFICADO, CON_CSR, SIN_CSR}


    private String accessControlURL = null;
    private State state = State.SIN_CSR;
    private List<SubSystemChangeListener> subSystemChangeListeners = new ArrayList<SubSystemChangeListener>();
    private SubSystem selectedSubsystem = SubSystem.VOTING;
    private EventState navigationDrawerEventState = EventState.OPEN;
    private EventVS eventVSSeleccionado;
    private ArrayList<EventVS> eventsSelectedList;

    private AccessControlVS accessControlVS;
    private UserVS userVS;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private OperationVS operationVS = null;

    private static AndroidContextVS INSTANCE;
    private Context context = null;

    private ExecutorService executorService;

    private AndroidContextVS(Context context) {
        this.context = context.getApplicationContext();
    }

    public static AndroidContextVS getInstance(Context context) {
        if(INSTANCE == null) {
            Log.d(TAG + ".getInstance(...)", " - instantiating singleton data");
            INSTANCE = new AndroidContextVS(context);
        }
        return INSTANCE;
    }
	
	public void setEvent(EventVS event) {
        Log.d(TAG + ".setEvent(...)", "- eventId: " + event.getId() + " - type: " + event.getTypeVS());
		this.eventVSSeleccionado = event;
	}

    public List<EventVS> getEvents() {
        return eventsSelectedList;
    }

    public void setEventList(List<EventVS> events) {
        Log.d(TAG + ".setEventList(...)", " --- setEventList - events.size(): " + events.size());
        eventsSelectedList = new ArrayList<EventVS>();
        if(events != null) {
            for(EventVS event: events) {
                ((EventVS)event).setAccessControlVS(accessControlVS);
                eventsSelectedList.add(event);
            }
        }
    }

    public int getEventIndex(EventVS event) {
        return eventsSelectedList.indexOf(event);
    }

	public EventVS getEvent() {
		return eventVSSeleccionado;
	}

    public Future<ResponseVS> submit(Callable<ResponseVS> callable) {
        Log.d(TAG + ".submit(...)", " --- submit");
        if(executorService == null) executorService = Executors.newFixedThreadPool(3);
        return executorService.submit(callable);
    }

	public OperationVS getOperationVS() {
		return operationVS;
	}

	public void setOperationVS(OperationVS operationVS) {
		if(operationVS == null) Log.d(TAG + ".setOperationVS(...)", "- removing pending operationVS");
		else Log.d(TAG + ".setOperationVS(...)", "- operationVS: " + operationVS.getTypeVS());
		this.operationVS = operationVS;
	}

    public void setAccessControlURL(String accessControlURL) {
        Log.d(TAG + ".setAccessControlURL() ", " - setAccessControlURL: " + accessControlURL);
        if(accessControlURL == null) {
            Log.d(TAG + ".actualizarEstado(...)", "----- NULL accessControlURL -----");
            return;
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String stateStr = settings.getString(
                PREFS_ESTADO + "_" + accessControlURL, State.SIN_CSR.toString());
        state = State.valueOf(stateStr);
        this.accessControlURL = accessControlURL;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setState(State state) {
        Log.d(TAG + ".setState(...)", " - state: " + state.toString()
                + " - server: " + PREFS_ESTADO + "_" + accessControlURL);
        this.state = state;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_ESTADO + "_" + accessControlURL , state.toString());
        editor.commit();
    }

    public State getState() {
        return state;
    }

    public EventState getNavigationDrawerEventState() {
        return navigationDrawerEventState;
    }

    public void setNavigationDrawerEventState(EventState eventState) {
        this.navigationDrawerEventState = eventState;
    }


    public SubSystem getSelectedSubsystem () {
        return selectedSubsystem;
    }

    public X509Certificate getCert(String serverURL) {
        Log.d(TAG + ".getCert(...)", " - getCert - serverURL: " + serverURL);
        if(serverURL == null) return null;
        return certsMap.get(serverURL);
    }

    public void putCert(String serverURL, X509Certificate cert) {
        Log.d(TAG + ".putCert(...)", " - putCert - serverURL: " + serverURL);
        certsMap.put(serverURL, cert);
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public AccessControlVS getAccessControlVS() {
        Log.d(TAG + ".getAccessControlVS() ", " - getAccessControlVS");
        return accessControlVS;
    }

    public void setAccessControlVS(AccessControlVS accessControlVS) {
        this.accessControlVS = accessControlVS;
    }

    public void addSubSystemChangeListener(SubSystemChangeListener listener) {
        subSystemChangeListeners.add(listener);
    }

    public void removeSubSystemChangeListener(SubSystemChangeListener listener) {
        subSystemChangeListeners.remove(listener);
    }
    public void setSelectedSubsystem (SubSystem selectedSubsystem) {
        Log.d(TAG + ".setSelectedSubsystem(...)", " - Subsystem: " + selectedSubsystem);
        this.selectedSubsystem = selectedSubsystem;
        for(SubSystemChangeListener listener : subSystemChangeListeners) {
            listener.onChangeSubSystem(selectedSubsystem);
        }
    }
}