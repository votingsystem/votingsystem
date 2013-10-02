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

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import org.sistemavotacion.modelo.Consulta;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.util.StringUtils;

import java.util.UUID;

import static org.sistemavotacion.android.AppData.PREFS_ID_APLICACION;
import static org.sistemavotacion.android.AppData.SERVER_URL_EXTRA_PROP_NAME;

public class MainActivity extends FragmentActivity {
	
	public static final String TAG = "MainActivity";

    private AppData appData;

    @Override protected void onCreate(Bundle savedInstanceState) {
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        Log.i(TAG + ".onCreate(...)", " - onCreate - isTablet: " + isTablet);
    	super.onCreate(savedInstanceState);  
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", " - Intent.ACTION_SEARCH - query: "+ query);
            return;
        }
        appData = AppData.getInstance(getBaseContext());
        if(getIntent().getStringExtra(SERVER_URL_EXTRA_PROP_NAME) != null) {
            appData.setAccessControlURL(getIntent().getStringExtra(SERVER_URL_EXTRA_PROP_NAME));
        }

        if(Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        	//getIntent().getCategories().contains(Intent.CATEGORY_BROWSABLE);
            final Uri data = getIntent().getData();
            appData.actualizarEstado(data.getQueryParameter("serverURL"));
            String eventoId = data.getQueryParameter("eventoId");
            String browserToken = data.getQueryParameter("browserToken");
            String encodedMsg = data.getQueryParameter("msg");
            String msg = StringUtils.decodeString(encodedMsg);
            Log.d(TAG + ".onCreate() - ", " - launched by browser - host: " + 
            		data.getHost() + 
            		" - path: " + data.getPath() + 
            		" - userInfo: " + data.getUserInfo() + 
            		" - serverURL: " + appData.getAccessControlURL() + " - eventoId: " + eventoId +
            		" - browserToken: " + browserToken + 
            		" - msg: " + msg);
            Operation operation = null;
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
							Evento eventoSeleccionado = consulta.getEventos().iterator().next();
							eventoSeleccionado.setOpcionSeleccionada(operation.
									getEvento().getOpcionSeleccionada());
							operation.setEvento(eventoSeleccionado);
                            appData.setEvent(eventoSeleccionado, getBaseContext());
						}
						processOperation(operation, appData.getEstado());
                	} else showMessage(getString(R.string.error_lbl), respuesta.getMensaje());
            	} catch(Exception ex) {
            		ex.printStackTrace();
            		showMessage(getString(R.string.error_lbl), ex.getMessage());
            	}
            } else {
            	Log.d(TAG + ".onCreate(...)", " - operation: " + operation.getTipo());
            	if(msg != null) {
                    appData.checkConnection(getBaseContext());
            		Intent intent = new Intent(this, WebActivity.class);
     			    intent.putExtra(WebActivity.OPERATION_KEY, msg);
     			    startActivity(intent);
            	}
            } 
            return;
        }
    	setActivityState(appData.getEstado());
    }

    @Override public void onResume() {
    	super.onResume();
    	Log.d(TAG + ".onResume() ", " - onResume");
    	//setActivityState();
    }

    private void setActivityState(AppData.Estado estado) {
        appData.checkConnection(getBaseContext());
    	Log.d(TAG + ".setActivityState()", " - estado: " + estado);
    	Intent intent = null;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        appData.setEstado(estado);
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
	                	Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
	                	startActivity(intent);
	                }
	            });
	            
	            Button solicitarButton = (Button) findViewById(R.id.solicitar_button);
	            solicitarButton.setOnClickListener(new OnClickListener() {
	                public void onClick(View v) {
	                	Intent intent = new Intent(getBaseContext(), UserCertRequestForm.class);
	                	startActivity(intent);
	                }
	            });
	    		break;
	    	case CON_CSR:
	    		intent = new Intent(getBaseContext(), UserCertResponseForm.class);
	    		break;
	    	case CON_CERTIFICADO:
	    		intent = new Intent(getBaseContext(), NavigationDrawer.class);
	    		break;
    	}
    	if(intent != null) {
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(intent);	
    	}
    }
    
    private void processOperation(Operation operation, AppData.Estado estado) {
    	Log.d(TAG + ".processOperation(...)", "- operation: " + 
    			operation.getTipo() + " - estado: " + estado);
    	Intent intent = null;
        appData.setEvent(operation.getEvento(), getBaseContext());
    	if(AppData.Estado.CON_CERTIFICADO == estado) {
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
            appData.checkConnection(getBaseContext());
    		startActivity(intent);
    	} else {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.cert_not_found_caption)).
    			setMessage(R.string.cert_not_found_msg).show();
    		setActivityState(estado);
    	}
    }
    
	private void showMessage(final String caption, final String message) {
		Log.d(TAG + ".showMessage(...)", " - caption: " + caption + 
				" - message: " + message);
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
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


}