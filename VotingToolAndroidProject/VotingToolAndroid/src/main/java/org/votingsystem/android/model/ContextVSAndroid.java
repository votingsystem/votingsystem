package org.votingsystem.android.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.android.util.EventState;
import org.votingsystem.android.util.SubSystem;
import org.votingsystem.android.util.SubSystemChangeListener;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.votingsystem.model.ContextVS;

public class ContextVSAndroid extends ContextVS {
	
	public static final String TAG = "ContextVSAndroid";

    public enum Estado {CON_CERTIFICADO, CON_CSR, SIN_CSR}


    private String accessControlURL = null;
    private Estado estado = Estado.SIN_CSR;
    private List<SubSystemChangeListener> subSystemChangeListeners = new ArrayList<SubSystemChangeListener>();
    private SubSystem selectedSubsystem = SubSystem.VOTING;
    private EventState navigationDrawerEventState = EventState.OPEN;
    private EventVSAndroid eventVSAndroidSeleccionado;
    private ArrayList<EventVS> eventsSelectedList;

    private AccessControl accessControl;
    private UserVS userVSBase;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private OperationVSAndroid operationVSAndroid = null;

    private static ContextVSAndroid INSTANCE;
    private Context context = null;

    private ExecutorService executorService;

    private ContextVSAndroid(Context context) {
        this.context = context.getApplicationContext();
    }

    public static ContextVSAndroid getInstance(Context context) {
        if(INSTANCE == null) {
            Log.d(TAG + ".getInstance(...)", " - instantiating singleton data");
            INSTANCE = new ContextVSAndroid(context);
        }
        return INSTANCE;
    }
	
	public void setEvent(EventVSAndroid event) {
        Log.d(TAG + ".setEvent(...)", "- eventId: " + event.getId() + " - type: " + event.getTypeVS());
		this.eventVSAndroidSeleccionado = event;
	}

    public List<EventVS> getEvents() {
        return eventsSelectedList;
    }

    public void setEventList(List<EventVS> events) {
        Log.d(TAG + ".setEventList(...)", " --- setEventList - events.size(): " + events.size());
        eventsSelectedList = new ArrayList<EventVS>();
        if(events != null) {
            for(EventVS event: events) {
                ((EventVSAndroid)event).setAccessControl(accessControl);
                eventsSelectedList.add(event);
            }
        }
    }

    public int getEventIndex(EventVS event) {
        return eventsSelectedList.indexOf(event);
    }

	public EventVSAndroid getEvent() {
		return eventVSAndroidSeleccionado;
	}

    public Future<ResponseVS> submit(Callable<ResponseVS> callable) {
        Log.d(TAG + ".submit(...)", " --- submit");
        if(executorService == null) executorService = Executors.newFixedThreadPool(3);
        return executorService.submit(callable);
    }

	public OperationVSAndroid getOperationVSAndroid() {
		return operationVSAndroid;
	}

	public void setOperationVSAndroid(OperationVSAndroid operationVSAndroid) {
		if(operationVSAndroid == null) Log.d(TAG + ".setOperationVSAndroid(...)", "- removing pending operationVSAndroid");
		else Log.d(TAG + ".setOperationVSAndroid(...)", "- operationVSAndroid: " + operationVSAndroid.getTipo());
		this.operationVSAndroid = operationVSAndroid;
	}

    public void setAccessControlURL(String accessControlURL) {
        Log.d(TAG + ".setAccessControlURL() ", " - setAccessControlURL: " + accessControlURL);
        if(accessControlURL == null) {
            Log.d(TAG + ".actualizarEstado(...)", "----- NULL accessControlURL -----");
            return;
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String estadoStr = settings.getString(
                PREFS_ESTADO + "_" + accessControlURL, Estado.SIN_CSR.toString());
        estado = Estado.valueOf(estadoStr);
        this.accessControlURL = accessControlURL;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setEstado(Estado estado) {
        Log.d(TAG + ".setEstado(...)", " - estado: " + estado.toString()
                + " - server: " + PREFS_ESTADO + "_" + accessControlURL);
        this.estado = estado;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_ESTADO + "_" + accessControlURL , estado.toString());
        editor.commit();
    }

    public Estado getEstado() {
        return estado;
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

    public UserVS getUserVSBase() {
        return userVSBase;
    }

    public void setUserVSBase(UserVS userVSBase) {
        this.userVSBase = userVSBase;
    }

    public AccessControl getAccessControl() {
        Log.d(TAG + ".getAccessControl() ", " - getAccessControl");
        return accessControl;
    }

    public void setAccessControl(AccessControl accessControl) {
        this.accessControl = accessControl;
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