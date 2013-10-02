package org.sistemavotacion.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.util.EventState;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.SubSystem;
import org.sistemavotacion.util.SubSystemChangeListener;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    public static final String MANIFEST_FILE_NAME         = "Manifest";
    public static final String NOMBRE_ARCHIVO_FIRMADO     = "archivoFirmado";
    public static final String CSR_FILE_NAME              = "csr";
    public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest";
    public static final String NOMBRE_ARCHIVO_BYTE_ARRAY  = "byteArray";
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
    public static final String SISTEMA_VOTACION_DIR = "SistemaVotacion";
    public static final String VOTING_HEADER_LABEL  = "votingSystemMessageType";


    public static final String CERT_NOT_FOUND_DIALOG_ID      = "certNotFoundDialog";
    public static final String PIN_DIALOG_ID                 = "pinDialog";

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

    private ActorConIP controlAcceso;
    private Usuario usuario;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private Operation operation = null;

    private static AppData INSTANCE;
    private Context context = null;

    private ExecutorService executorService;

    private AppData(Context context) {
        this.context = context.getApplicationContext();
        Properties props = new Properties();
        try {
            props.load(context.getAssets().open("VotingSystem.properties"));
            if(props != null) {
                accessControlURL = props.getProperty("CONTROL_ACCESO_URL");
                Log.d(TAG + ".onCreate()", " - accessControlURL: " + accessControlURL);
            } else Log.d(TAG + ".onCreate()", " - NULL accessControlURL");
        } catch (IOException ex) {
            Log.e(TAG + ".onCreate()", ex.getMessage(), ex);
        }
        actualizarEstado(accessControlURL);

    }

    public static AppData getInstance(Context context) {
        if(INSTANCE == null) {
            Log.d(TAG + ".getInstance(...)", " - instantiating singleton data");
            INSTANCE = new AppData(context);
        }
        return INSTANCE;
    }
	
	public void setEvent(Evento event, Context context) {
        Log.d(TAG + ".setEvent(...)", "- event type: " + event.getTipo());
		this.eventoSeleccionado = event;
        if(Tipo.EVENTO_VOTACION == event.getTipo()) {
            try {
                checkCert(eventoSeleccionado.getCentroControl(), context);
            } catch (Exception e) {
                e.printStackTrace();
                showMessage(context.getString(R.string.CONTROL_CENTER_CONECTION_ERROR_MSG), context);
            }
        }
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
        this.accessControlURL = accessControlURL;
    }


    public void actualizarEstado(String accessControlURL) {
        if(accessControlURL == null) {
            Log.d(TAG + ".actualizarEstado(...)", "----- NULL accessControlURL -----");
            return;
        }
        this.accessControlURL = accessControlURL;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String estadoStr = settings.getString(
                PREFS_ESTADO + "_" + accessControlURL, Estado.SIN_CSR.toString());
        estado = Estado.valueOf(estadoStr);
    }


    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void showMessage(String message, Context context) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
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


    public void checkConnection(Context context) {
        Log.d(TAG + ".checkConnection() ", " - accessControlURL: " + accessControlURL);
        if (controlAcceso == null || !controlAcceso.getServerURL().equals(accessControlURL)) {
            try {
                GetDataTask getDataTask = (GetDataTask) new GetDataTask(null).
                        execute(ServerPaths.getURLInfoServidor(accessControlURL));
                Respuesta respuesta = getDataTask.get();
                if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    try {
                        controlAcceso = ActorConIP.parse(respuesta.getMensaje(),
                                ActorConIP.Tipo.CONTROL_ACCESO);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showMessage(context.getString(R.string.error_lbl) + ": " +
                                ex.getMessage(), context);
                    }
                } else showMessage(context.getString(R.string.error_lbl) + ": " +
                        respuesta.getMensaje(), context);
            } catch(Exception ex) {
                ex.printStackTrace();
                showMessage(context.getString(R.string.error_lbl) + ": " + ex.getMessage(), context);
            }
        }
    }

    public void getNetworkCert(String serverURL, Context context) throws Exception {
        Log.d(TAG + ".getServerCert() ", " - getServerCert - serverURL: " + serverURL);
        String serverCertURL = ServerPaths.getURLCadenaCertificacion(serverURL);
        GetDataTask getDataTask = (GetDataTask) new GetDataTask(null).
                execute(serverCertURL);
        Respuesta respuesta = getDataTask.get();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(
                    respuesta.getMessageBytes());
            X509Certificate serverCert = certChain.iterator().next();
            certsMap.put(serverURL, serverCert);
        } else {
            Log.d(TAG + ".getServerCert() ", " - Error - server status: " +
                    respuesta.getCodigoEstado() + " - message: " + respuesta.getMensaje());
            showMessage(context.getString(R.string.get_cert_error_msg) + ": " + serverURL, context);
        }
    }

    public X509Certificate getCert(String serverURL) {
        Log.d(TAG + ".getCert(...)", " - getCert - serverURL: " + serverURL);
        if(serverURL == null) return null;
        return certsMap.get(serverURL);
    }

    public void checkCert(ActorConIP actorConIP, Context context) throws Exception {
        Log.d(TAG + ".checkCert(...)", " - checkCert - serverURL: "
                + actorConIP.getServerURL());
        if(actorConIP.getCertificado() == null) {
            if(actorConIP.getServerURL() == null) {
                throw new Exception(" - Missing serverURL param - ");
            }
            X509Certificate actorCert = certsMap.get(actorConIP.getServerURL().trim());
            if(actorCert == null) {
                getNetworkCert(actorConIP.getServerURL(),context);
            } else {
                actorConIP.setCertificado(actorCert);
            }
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public ActorConIP getControlAcceso() {
        return controlAcceso;
    }

    public void setControlAcceso(ActorConIP controlAcceso) {
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