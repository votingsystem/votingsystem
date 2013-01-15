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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.sistemavotacion.json.DeJSONAObjeto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.SubSystem;
import org.sistemavotacion.util.SubSystemChangeListener;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

public class Aplicacion extends SherlockActivity {
	
	public static final String TAG = "Aplicacion";
	
	public enum Estado {CON_CERTIFICADO, CON_CSR, SIN_CSR}
	

	//public static final int THEME = R.style.Theme_Sherlock_Light_DarkActionBar;
	public static final int THEME = R.style.Theme_Sherlock;
	
	public static final String PREFS_ESTADO              = "estado";
	public static final String NEW_CERT_KEY              = "NEW_CERT";
	public static final String PREFS_ID_SOLICTUD_CSR     = "idSolicitudCSR";
	public static final String PREFS_ID_APLICACION       = "idAplicacion";
	public static final String MANIFEST_FILE_NAME        = "Manifest";
    public static final String NOMBRE_ARCHIVO_FIRMADO    = "archivoFirmado";
    public static final String NOMBRE_ARCHIVO_CSR        = "csr";
    public static final String NOMBRE_ARCHIVO_BYTE_ARRAY = "byteArray";
    public static final String SIGNED_PART_EXTENSION     = ".p7m";
    public static final String DEFAULT_SIGNED_FILE_NAME  = "smimeMessage";
    public static final String SIGN_PROVIDER             = "BC";
    public static final int KEY_SIZE = 1024;
    public static final int EVENTS_PAGE_SIZE = 30;
    public static final int MAX_SUBJECT_SIZE = 60;
    //TODO por el bug en froyo de -> JcaDigestCalculatorProviderBuilder
    public static final String SIG_HASH = "SHA256";
    public static final String SIG_NAME = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final String VOTE_SIGN_MECHANISM = "SHA256withRSA";
    public static final String PROVIDER = "BC";
    public static final String ALIAS_CERT_USUARIO = "CertificadoUsuario";
    public static final String KEY_STORE_FILE = "keyStoreFile.p12";
    
    public static final String TIMESTAMP_USU_HASH = TSPAlgorithms.SHA256;
    public static final String TIMESTAMP_VOTE_HASH = TSPAlgorithms.SHA256;
    
    public static final String ASUNTO_MENSAJE_FIRMA_DOCUMENTO = "[Firma]-";    
    public static String CONTROL_ACCESO_URL = "http://192.168.1.4:8080/SistemaVotacionControlAcceso";
    public static final String SISTEMA_VOTACION_DIR = "SistemaVotacion";
    
    private SubSystem selectedSubsystem = SubSystem.VOTING;
    private List<SubSystemChangeListener> subSystemChangeListeners = new ArrayList<SubSystemChangeListener>();
	private Evento eventoSeleccionado;
	private static ActorConIP controlAcceso;
	private static Usuario usuario;
    private static Estado estado = Estado.SIN_CSR;
    public static Aplicacion INSTANCIA; 
    private SharedPreferences settings;
    
    DataListener<String> dataServerListener = new DataListener<String>() {

		@Override public void updateData(int codigoEstado, String data) {
			Log.d(TAG + ".dataServerListener.updateData() ", "data: " + data);
			try {
				ActorConIP controlAcceso = DeJSONAObjeto.obtenerActorConIP(
						data, ActorConIP.Tipo.CONTROL_ACCESO);
				setControlAcceso(controlAcceso);
			} catch (Exception e) {
				Log.e(TAG + ".dataServerListener.updateData() ", e.getMessage(), e);
				e.printStackTrace();
			}
			
		}

		@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".dataServerListener.manejarExcepcion() ", "exceptionMsg: " + exceptionMsg);	
      	  	Toast.makeText(Aplicacion.this, exceptionMsg, Toast.LENGTH_LONG).show();
		}
	};
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG + ".onCreate(...)", " - onCreate - ");
    	setTheme(Aplicacion.THEME);
    	super.onCreate(savedInstanceState);  
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", " - Intent.ACTION_SEARCH - query: "+ query);
            return;
        }
    	INSTANCIA = this;
        Properties props = new Properties();
        try {
            props.load(getAssets().open("VotingSystem.properties"));
            if(props != null) {
            	String controlAccesoURL = props.getProperty("CONTROL_ACCESO_URL");
            	if(controlAccesoURL != null) CONTROL_ACCESO_URL = controlAccesoURL;
            	Log.d(TAG + ".onCreate()", " - controlAccesoURL: " + controlAccesoURL);
            }
        } catch (IOException ex) {
        	Log.e(TAG + ".onCreate()", ex.getMessage(), ex);
        }
    	settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	setActivityState();
    }
    
    @Override public void onResume() {
    	super.onResume();
    	Log.d(TAG + ".onResume() ", " - onResume");
    	//setActivityState();
    }

    private void setActivityState() {
    	String estadoAplicacion = settings.getString(PREFS_ESTADO, Estado.SIN_CSR.toString());
    	new GetDataTask(dataServerListener).execute(ServerPaths.getURLInfoServidor(CONTROL_ACCESO_URL));
    	estado = Estado.valueOf(estadoAplicacion);
    	Log.d(TAG + ".setActivityState()", " - estadoAplicacion: " + estadoAplicacion);
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
		switch(subsystem) {
			case CLAIMS:
				return getApplicationContext().getString(R.string.claims_drop_down_lbl);
			case VOTING:
				return getApplicationContext().getString(R.string.voting_drop_down_lbl);
			case MANIFESTS:
				return getApplicationContext().getString(R.string.manifiests_drop_down_lbl);
			default: 
				return getApplicationContext().getString(R.string.unknown_drop_down_lbl);
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
	
	public String getResourceString(int resourceId, Object formatArgs) {
		return getString(resourceId, formatArgs);
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
    	return INSTANCIA.getApplicationContext().getString(resId, formatArgs);
    }
    
	public static void setEstado(Estado estado) {
		Log.d(TAG + ".setEstado(...)", " - estado: " + estado.toString());
		Aplicacion.estado = estado;
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(INSTANCIA.getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_ESTADO, estado.toString());
        editor.commit();
	}

}