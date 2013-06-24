/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sistemavotacion.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Consulta;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.util.SubSystem;
import org.sistemavotacion.util.SubSystemChangeListener;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class Aplicacion extends FragmentActivity {
	
	public static final String TAG = "Aplicacion";
	
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
    public static String CONTROL_ACCESO_URL = null;
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
    
    private SubSystem selectedSubsystem = SubSystem.VOTING;
    private List<SubSystemChangeListener> subSystemChangeListeners = new ArrayList<SubSystemChangeListener>();
	private Evento eventoSeleccionado;
	private static ActorConIP controlAcceso;
	private static Usuario usuario;
	private static Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private static Estado estado = Estado.SIN_CSR;
    public static Aplicacion INSTANCIA; 
    private Operation operation = null;
    private SharedPreferences settings;
	
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG + ".onCreate(...)", " - onCreate - ");
    	super.onCreate(savedInstanceState);  
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", " - Intent.ACTION_SEARCH - query: "+ query);
            return;
        }
        if(getIntent().getStringExtra(SERVER_URL_EXTRA_PROP_NAME) != null) {
        	CONTROL_ACCESO_URL = getIntent().getStringExtra(SERVER_URL_EXTRA_PROP_NAME);
        }
    	settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    	INSTANCIA = this;
    	Properties props = new Properties();
        try {
            props.load(getAssets().open("VotingSystem.properties"));
            if(props != null) {
            	String controlAccesoURL = props.getProperty("CONTROL_ACCESO_URL");
            	if(controlAccesoURL != null) {
            		CONTROL_ACCESO_URL = controlAccesoURL;
            	} else Log.d(TAG + ".onCreate()", " - CONTROL_ACCESO_URL in VotingSystem.properties is NULL");
            	Log.d(TAG + ".onCreate()", " - controlAccesoURL: " + controlAccesoURL);
            }
        } catch (IOException ex) {
        	Log.e(TAG + ".onCreate()", ex.getMessage(), ex);
        }
    	String estadoAplicacion = settings.getString(
    			PREFS_ESTADO + "_" + CONTROL_ACCESO_URL, Estado.SIN_CSR.toString());
    	Log.d(TAG + ".onCreate()", " - checking state: " + PREFS_ESTADO + "_" + CONTROL_ACCESO_URL);
    	estado = Estado.valueOf(estadoAplicacion);
        if(Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        	//getIntent().getCategories().contains(Intent.CATEGORY_BROWSABLE);
            final Uri data = getIntent().getData();
            CONTROL_ACCESO_URL = data.getQueryParameter("serverURL");
            estadoAplicacion = settings.getString(
        			PREFS_ESTADO + "_" + CONTROL_ACCESO_URL, Estado.SIN_CSR.toString());
        	estado = Estado.valueOf(estadoAplicacion);
            String eventoId = data.getQueryParameter("eventoId");
            String browserToken = data.getQueryParameter("browserToken");
            String encodedMsg = data.getQueryParameter("msg");
            String msg = StringUtils.decodeString(encodedMsg);
            Log.d(TAG + ".onCreate() - ", " - launched by browser - host: " + 
            		data.getHost() + 
            		" - path: " + data.getPath() + 
            		" - userInfo: " + data.getUserInfo() + 
            		" - serverURL: " + CONTROL_ACCESO_URL + " - eventoId: " + eventoId +
            		" - browserToken: " + browserToken + 
            		" - msg: " + msg);
            if(msg != null) {
        		try {
        			operation = Operation.parse(msg);
        		} catch (Exception ex) {
        			Log.e(TAG + ".onCreate(...)", ex.getMessage(), ex);
        		}
            } else {
            	Log.d(TAG + ".onCreate(...)", "- msg null");
            	operation = new Operation();
            }
            if(browserToken != null)
            	operation.setTipo(browserToken.trim());
            if(operation.getEvento() != null) {
            	try {
                	GetDataTask getDataTask = (GetDataTask)new GetDataTask(null).execute(
                			operation.getEvento().getURL());
                	Respuesta respuesta = getDataTask.get();
                	Log.d(TAG + ".onCreate(...)", " - getDataTask - statusCode: " + respuesta.getCodigoEstado());
                	if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                		Consulta consulta = Consulta.parse(respuesta.getMensaje());
						if(consulta.getEventos() != null && consulta.getEventos().size() > 0) {
							eventoSeleccionado = consulta.getEventos().iterator().next();
							eventoSeleccionado.setOpcionSeleccionada(operation.
									getEvento().getOpcionSeleccionada());
							operation.setEvento(eventoSeleccionado);	
						}
						processOperation(operation);
                	} else showMessage(getString(R.string.error_lbl), respuesta.getMensaje());
            	} catch(Exception ex) {
            		ex.printStackTrace();
            		showMessage(getString(R.string.error_lbl), ex.getMessage());
            	}
            } else {
            	Log.d(TAG + ".onCreate(...)", " - operation: " + operation.getTipo());
            	if(msg != null) {
            		checkConnection();
            		Intent intent = new Intent(this, WebActivity.class);
     			    intent.putExtra(WebActivity.OPERATION_KEY, msg);
     			    startActivity(intent);
            	}
            } 
            return;
        }
    	setActivityState();
    }

    @Override public void onResume() {
    	super.onResume();
    	Log.d(TAG + ".onResume() ", " - onResume");
    	//setActivityState();
    }
    
    public void checkConnection() {
    	Log.d(TAG + ".checkConnection() ", " - CONTROL_ACCESO_URL: " + CONTROL_ACCESO_URL);
    	if (controlAcceso == null || !controlAcceso.getServerURL().
    			equals(CONTROL_ACCESO_URL)) {
    		try {
        		GetDataTask getDataTask = (GetDataTask) new GetDataTask(null).
        				execute(ServerPaths.getURLInfoServidor(CONTROL_ACCESO_URL));
        		Respuesta respuesta = getDataTask.get();
    			if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
    				try {
    					controlAcceso = ActorConIP.parse(respuesta.getMensaje(), 
    							ActorConIP.Tipo.CONTROL_ACCESO);
    				} catch (Exception ex) {
    					ex.printStackTrace();
    				    showMessage(getString(R.string.error_lbl), ex.getMessage());
    				}
    			} else showMessage(getString(R.string.error_lbl), respuesta.getMensaje());
    		} catch(Exception ex) {
    			ex.printStackTrace();
    			showMessage(getString(R.string.error_lbl), ex.getMessage());
    		}
    	}
    }
    
    public void getNetworkCert(String serverURL) throws Exception {
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
		} else Log.d(TAG + ".getServerCert() ", " - Error - server status: " + 
				respuesta.getCodigoEstado() + " - message: " + respuesta.getMensaje());
    }
    
    public X509Certificate getCert(String serverURL) {
		Log.d(TAG + ".getCert(...)", " - getCert - serverURL: " + serverURL);
		if(serverURL == null) return null;
		return certsMap.get(serverURL);
	}
    
    public void checkCert(ActorConIP actorConIP) throws Exception {
    	Log.d(TAG + ".checkCert(...)", " - checkCert - serverURL: " 
    			+ actorConIP.getServerURL());
        if(actorConIP.getCertificado() == null) {
        	if(actorConIP.getServerURL() == null) {
        		throw new Exception(" - Missing serverURL param - ");
        	}
        	X509Certificate actorCert = getCert(
        			actorConIP.getServerURL());
        	if(actorCert == null) {
        		getNetworkCert(actorConIP.getServerURL());
			} else actorConIP.setCertificado(actorCert);
        }
    }
	
	public static void putCert(String serverURL, X509Certificate cert) {
		Log.d(TAG + ".putCert(...)", " - putCert - serverURL: " + serverURL);
		certsMap.put(serverURL, cert);
	}

    private void setActivityState() {
    	checkConnection();
    	Log.d(TAG + ".setActivityState()", " - estado: " + estado);
    	Intent intent = null;	
    	switch (estado) {
	    	case SIN_CSR:
	    		String idAplicacion = settings.getString(PREFS_ID_APLICACION, null);
	    		if (idAplicacion == null || "".equals(idAplicacion)) {
	    			Log.d(TAG + ".setActivityState() ", " - guardando ID aplicación");
	    			idAplicacion = UUID.randomUUID().toString();
	    			SharedPreferences.Editor editor = settings.edit();
	    			editor.putString(PREFS_ID_APLICACION, idAplicacion);
			        editor.commit();
	    		}
	            setContentView(R.layout.init_screen);
	            Button cancelarButton = (Button) findViewById(R.id.cancelar_button);
	            cancelarButton.setOnClickListener(new OnClickListener() {
	                public void onClick(View v) { 
	                	//finish(); 
	                	Intent intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
	                	startActivity(intent);
	                }
	            });
	            
	            Button solicitarButton = (Button) findViewById(R.id.solicitar_button);
	            solicitarButton.setOnClickListener(new OnClickListener() {
	                public void onClick(View v) {
	                	Intent intent = new Intent(getApplicationContext(), UserCertRequestForm.class);
	                	startActivity(intent);
	                }
	            });
	    		break;
	    	case CON_CSR:
	    		intent = new Intent(getApplicationContext(), UserCertResponseForm.class);
	    		break;
	    	case CON_CERTIFICADO:
	    		intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
	    		break;
    	}
    	if(intent != null) {
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(intent);	
    	}
    }
    
    private void processOperation(Operation operation) {
    	Log.d(TAG + ".processOperation(...)", "- operation: " + 
    			operation.getTipo() + " - estado: " + estado);
    	Intent intent = null;
    	setEventoSeleccionado(operation.getEvento());
    	if(Estado.CON_CERTIFICADO == estado) {
    		switch(operation.getTipo()) {
		        case VOTAR:
		        	Log.d(TAG + ".setActivityState(...)", " - intent voting ---- ");
		        	intent = new Intent(this, VotingEventScreen.class);
		        	break;
		        case FIRMAR_MANIFIESTO:
		        case FIRMAR_RECLAMACION:
		        	intent = new Intent(this, EventScreen.class);
		        	break;
		        default: 
		        	Log.e(TAG + ".processOperation(...)", "- unknown operation");;
	        }
    		checkConnection();
    		startActivity(intent);
    	} else {
    		AlertDialog.Builder builder= new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.cert_not_found_caption)).
    			setMessage(R.string.cert_not_found_msg).show();
    		setActivityState();
    	}
    }
    
	private void showMessage(final String caption, final String message) {
		Log.d(TAG + ".showMessage(...)", " - caption: " + caption + 
				" - message: " + message);
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	Toast.makeText(Aplicacion.this, message, Toast.LENGTH_LONG).show();
    	    }
    	});   
	}

    @Override protected void onStop() {
        super.onStop();
    	Log.d(TAG + ".onStop()", " - onStop");
    };

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", " - onDestroy");
    };
	
	public Evento getEventoSeleccionado() {
		return eventoSeleccionado;
	}

	public void setEventoSeleccionado(Evento eventoSeleccionado) {
    	try {
			checkCert(eventoSeleccionado.getCentroControl());
		} catch (Exception e) {
			e.printStackTrace();
			showMessage(getString(R.string.error_lbl), 
					getString(R.string.CONTROL_CENTER_CONECTION_ERROR_MSG));
		}
		this.eventoSeleccionado = eventoSeleccionado;
	}
	
	public boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) return true;
	    return false;
	}

	public static Usuario getUsuario() {
		return usuario;
	}


	public static void setUsuario(Usuario usuario) {
		Aplicacion.usuario = usuario;
	}

	public static ActorConIP getControlAcceso() {
		return controlAcceso;
	}

	public static void setControlAcceso(ActorConIP controlAcceso) {
		Aplicacion.controlAcceso = controlAcceso;
	}

	public void addSubSystemChangeListener(SubSystemChangeListener listener) {
		subSystemChangeListeners.add(listener);
	}
	
	public void removeSubSystemChangeListener(SubSystemChangeListener listener) {
		subSystemChangeListeners.remove(listener);
	}
	
	public SubSystem getSelectedSubsystem () {
		return selectedSubsystem;
	}
	
	public String getSelectedSubsystemDesc () {
		return getSubsystemDesc(selectedSubsystem);
	}
	
	public String getSubsystemDesc (SubSystem subsystem) {
		if(subsystem == null) subsystem = SubSystem.VOTING;
		switch(subsystem) {
			case CLAIMS:
				return getString(R.string.claims_drop_down_lbl);
			case VOTING:
				return getString(R.string.voting_drop_down_lbl);
			case MANIFESTS:
				return getString(R.string.manifiests_drop_down_lbl);
			default: 
				return getString(R.string.unknown_drop_down_lbl);
		}
	}
	
	public String setSelectedSubsystem (SubSystem selectedSubsystem) {
		if(selectedSubsystem == this.selectedSubsystem) 
			return getSelectedSubsystemDesc();
		Log.d(TAG + ".setSelectedSubsystem(...)", " - Subsystem: " + selectedSubsystem);
		this.selectedSubsystem = selectedSubsystem;
    	Intent intent = null;
    	intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
		if(intent != null) {
	    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	startActivity(intent);	
		}
		for(SubSystemChangeListener listener : subSystemChangeListeners) {
			listener.onChangeSubSystem(selectedSubsystem);
		}
		return getSelectedSubsystemDesc();
	}

	public static FileOutputStream openFileOutputStream(String filename) {
		Log.d(TAG + ".openFileOutputStream(...)", " - filename: " + filename);
		FileOutputStream fout = null;
		try {
			fout = INSTANCIA.openFileOutput(filename, Context.MODE_PRIVATE);
		} catch(Exception ex) {
			Log.e(TAG + ".openFileOutputStream(...)", ex.getMessage(), ex);
		}
		return fout;
	}
	
	public static File getFile(String filename) {
		File file = null;
		try {
			//file = new File(INSTANCIA.getExternalFilesDir(null), filename);
			file = new File(INSTANCIA.
					getApplicationContext().getFilesDir(), filename);
			Log.d(TAG + ".getFile(...)", " - file.getAbsolutePath(): " 
					+ file.getAbsolutePath());
		} catch(Exception ex) {
			Log.e(TAG + ".getFile(...)", ex.getMessage(), ex);
		}
		return file;
	}
    
	/*public static File guardar (String nif, String controlAcceso, String idEvento, 
	 		MimeMessage body) throws FileNotFoundException {
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File (sdCard.getAbsolutePath() + File.separator + 
				SISTEMA_VOTACION_DIR + File.separator + controlAcceso + 
				File.separator + nif + File.separator + idEvento);
		dir.mkdirs();
		File resultado = new File(dir, "filename");
        body.writeTo(new FileOutputStream(resultado));
        return resultado;
	}*/
	
	public Estado getEstado() {
		return estado;
	}
	
    @Override public boolean onSearchRequested() {
    	Log.d(TAG + ".onSearchRequested(...)", " - onSearchRequested - ");
		return false;
	}
    
    public static String getAppString(int resId, Object... formatArgs) {
    	return INSTANCIA.getString(resId, formatArgs);
    }
    
	public static void setEstado(Estado estado) {
		Log.d(TAG + ".setEstado(...)", " - estado: " + estado.toString() 
				+ " - server: " + PREFS_ESTADO + "_" + CONTROL_ACCESO_URL);
		Aplicacion.estado = estado;
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(INSTANCIA.getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_ESTADO + "_" + CONTROL_ACCESO_URL , estado.toString());
        editor.commit();
	}

}