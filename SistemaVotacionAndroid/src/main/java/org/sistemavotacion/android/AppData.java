package org.sistemavotacion.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.sistemavotacion.modelo.ControlAcceso;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.util.EventState;
import org.sistemavotacion.util.SubSystem;
import org.sistemavotacion.util.SubSystemChangeListener;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AppData {
	
	public static final String TAG = "AppData";

    public enum Estado {CON_CERTIFICADO, CON_CSR, SIN_CSR}


    public static final String PREFS_ESTADO               = "estado";
    public static final String PREFS_ID_SOLICTUD_CSR      = "idSolicitudCSR";
    public static final String PREFS_ID_APLICACION        = "idAplicacion";
    public static final String EVENT_KEY                  = "eventKey";
    public static final String NOMBRE_ARCHIVO_FIRMADO     = "archivoFirmado";
    public static final String CSR_FILE_NAME              = "csr";
    public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest";
    public static final String SIGNED_PART_EXTENSION      = ".p7m";
    public static final String DEFAULT_SIGNED_FILE_NAME   = "smimeMessage.p7m";
    public static final String PROVIDER                   = BouncyCastleProvider.PROVIDER_NAME;
    public static final String SERVER_URL_EXTRA_PROP_NAME = "serverURL";
    public static final int KEY_SIZE = 1024;
    public static final int EVENTS_PAGE_SIZE = 30;
    public static final int MAX_SUBJECT_SIZE = 60;
    public static final int SELECTED_OPTION_MAX_LENGTH       = 60;
    //TODO por el bug en froyo de -> JcaDigestCalculatorProviderBuilder
    public static final String SIG_HASH = "SHA256";
    public static final String SIG_NAME = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    //public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String VOTE_SIGN_MECHANISM = "SHA256WithRSA";
    public static final String ALIAS_CERT_USUARIO = "CertificadoUsuario";
    public static final String KEY_STORE_FILE = "keyStoreFile.p12";

    public static final String TIMESTAMP_USU_HASH = TSPAlgorithms.SHA256;
    public static final String TIMESTAMP_VOTE_HASH = TSPAlgorithms.SHA256;

    public static final String ASUNTO_MENSAJE_FIRMA_DOCUMENTO = "[Firma]-";
    public static final String VOTING_HEADER_LABEL  = "votingSystemMessageType";


    public static final String CERT_NOT_FOUND_DIALOG_ID      = "certNotFoundDialog";

    public static final String PDF_CONTENT_TYPE    = "application/pdf";
    public static final String SIGNED_CONTENT_TYPE = "application/x-pkcs7-signature";
    public static final String X509_CONTENT_TYPE = "application/x-x509-ca-cert";
    public static final String ENCRYPTED_CONTENT_TYPE = "application/x-pkcs7-mime";
    public static final String SIGNED_AND_ENCRYPTED_CONTENT_TYPE =
            SIGNED_CONTENT_TYPE + "," + ENCRYPTED_CONTENT_TYPE;
    public static final String PDF_SIGNED_AND_ENCRYPTED_CONTENT_TYPE =
            PDF_CONTENT_TYPE + "," +  SIGNED_CONTENT_TYPE + ";" + ENCRYPTED_CONTENT_TYPE;
    public static final String PDF_SIGNED_CONTENT_TYPE =
            PDF_CONTENT_TYPE + "," + SIGNED_CONTENT_TYPE;
    public static final String PDF_ENCRYPTED_CONTENT_TYPE =
            PDF_CONTENT_TYPE + "," + ENCRYPTED_CONTENT_TYPE;


    private String accessControlURL = null;
    private Estado estado = Estado.SIN_CSR;
    private List<SubSystemChangeListener> subSystemChangeListeners = new ArrayList<SubSystemChangeListener>();
    private SubSystem selectedSubsystem = SubSystem.VOTING;
    private EventState navigationDrawerEventState = EventState.OPEN;
    private Evento eventoSeleccionado;
    private ArrayList<Evento> eventsSelectedList;

    private ControlAcceso controlAcceso;
    private Usuario usuario;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private Operation operation = null;

    private static AppData INSTANCE;
    private Context context = null;

    private ExecutorService executorService;

    private AppData(Context context) {
        this.context = context.getApplicationContext();
    }

    public static AppData getInstance(Context context) {
        if(INSTANCE == null) {
            Log.d(TAG + ".getInstance(...)", " - instantiating singleton data");
            INSTANCE = new AppData(context);
        }
        return INSTANCE;
    }
	
	public void setEvent(Evento event) {
        Log.d(TAG + ".setEvent(...)", "- eventId: " + event.getId() + " - type: " + event.getTipo());
		this.eventoSeleccionado = event;
	}

    public List<Evento> getEvents() {
        return eventsSelectedList;
    }

    public void setEventList(List<Evento> events) {
        Log.d(TAG + ".setEventList(...)", " --- setEventList - events.size(): " + events.size());
        eventsSelectedList = new ArrayList<Evento>();
        if(events != null) {
            for(Evento event: events) {
                event.setControlAcceso(controlAcceso);
                eventsSelectedList.add(event);
            }
        }
    }

    public int getEventIndex(Evento event) {
        return eventsSelectedList.indexOf(event);
    }

	public Evento getEvent() {
		return eventoSeleccionado;
	}

    public Future<Respuesta> submit(Callable<Respuesta> callable) {
        Log.d(TAG + ".submit(...)", " --- submit");
        if(executorService == null) executorService = Executors.newFixedThreadPool(3);
        return executorService.submit(callable);
    }

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		if(operation == null) Log.d(TAG + ".setOperation(...)", "- removing pending operation");
		else Log.d(TAG + ".setOperation(...)", "- operation: " + operation.getTipo());
		this.operation = operation;
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public ControlAcceso getControlAcceso() {
        Log.d(TAG + ".getControlAcceso() ", " - getControlAcceso");
        return controlAcceso;
    }

    public void setControlAcceso(ControlAcceso controlAcceso) {
        this.controlAcceso = controlAcceso;
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