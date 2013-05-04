package org.controlacceso.clientegwt.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeAplicacion;
import org.controlacceso.clientegwt.client.firmas.PanelPublicacionManifiesto;
import org.controlacceso.clientegwt.client.modelo.ActorConIPJso;
import org.controlacceso.clientegwt.client.reclamaciones.PanelPublicacionReclamacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.votaciones.PanelPublicacionVotacion;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PuntoEntradaEditor implements EntryPoint {

    private static Logger logger = Logger.getLogger("PuntoEntradaEditor");
    
	
    private static PuntoEntradaEditorBinder uiBinder = GWT.create(PuntoEntradaEditorBinder.class);
    
    interface PuntoEntradaEditorBinder extends UiBinder<VerticalPanel, PuntoEntradaEditor> { }
    
    @UiField VerticalPanel panelEditor;
    public ActorConIPJso servidor;
    public static PuntoEntradaEditor INSTANCIA;
    private boolean androidClientLoaded = false;
	
    public void onModuleLoad() {
    	INSTANCIA = this;
    	logger.log(Level.INFO, "Browser.getUserAgent(): " + Browser.getUserAgent());
    	panelEditor = uiBinder.createAndBindUi(this);
    	RequestHelper.doGet(ServerPaths.getInfoServidorPath(), 
    			new ServerRequestInfoCallback());
    	RootPanel.get("ui").add(panelEditor);
    	//RootLayoutPanel.get().add(appPanel);

        initSetClienteFirmaMessageJS(this);
        History.fireCurrentHistoryState();
        logger.info("queryString: " +  Window.Location.getQueryString());
        String editor = Window.Location.getParameter("editor");
        String androidClientLoadedStr = Window.Location.getParameter("androidClientLoaded");
        if(androidClientLoadedStr != null) {
        	androidClientLoaded = Boolean.valueOf(androidClientLoadedStr);
        }
        logger.info("androidClientLoaded: " +  androidClientLoaded);
        if (editor != null) {
        	if("manifest".equals(editor)) {
    		    PanelPublicacionManifiesto panelPublicacionManifiesto 
    		    	= new PanelPublicacionManifiesto();
    		    panelEditor.add(panelPublicacionManifiesto);
        	} else if("vote".equals(editor)) {
    		    PanelPublicacionVotacion panelPublicacionVotacion 
			    	= new PanelPublicacionVotacion();
			    panelEditor.add(panelPublicacionVotacion);
        	} else if("claim".equals(editor)) {
        		PanelPublicacionReclamacion panelPubReclamacion 
			    	= new PanelPublicacionReclamacion();
			    panelEditor.add(panelPubReclamacion);
        	} else {
        		logger.log(Level.SEVERE, "No se ha solicitado ningún editor");
        	}
        }
    }

    public boolean getAndroidClientLoaded() {
    	return androidClientLoaded;
    }

    private native void initSetClienteFirmaMessageJS (PuntoEntradaEditor pe) /*-{
	    $wnd.setClienteFirmaMessage = function (messageClienteFirma) {
	        pe.@org.controlacceso.clientegwt.client.PuntoEntradaEditor::setClienteFirmaMessage(Ljava/lang/String;)(messageClienteFirma);
	    };
 	}-*/;


    public String setClienteFirmaMessage(String message) {
    	logger.log(Level.INFO, "setClienteFirmaMessage - message: " + message);
    	String resultado = "Respuesta de la aplicación gwt";
    	return resultado;
    }
    
    private void showErrorDialog (String caption, String message) {
    	ResultDialog resultDialog = new ResultDialog();
		resultDialog.show(caption, message,Boolean.FALSE);  
    }
	
    private class ServerRequestInfoCallback implements RequestCallback {

        @Override
    	public void onError(Request request, Throwable exception) {
        	logger.log(Level.SEVERE, exception.getMessage(), exception);
        	showErrorDialog(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());
    	}

        @Override
    	public void onResponseReceived(Request request, Response response) {
			logger.log(Level.INFO, "StatusCode: " + response.getStatusCode() + 
					", Response Text: " + response.getText());
			if (response.getStatusCode() == Response.SC_OK) {
                servidor = ActorConIPJso.create(response.getText());
                EventoGWTMensajeAplicacion mensaje = new EventoGWTMensajeAplicacion(
                		servidor, HistoryToken.OBTENIDA_INFO_SERVIDOR);
                BusEventos.fireEvent(mensaje);

    		} else if (response.getStatusCode() == Response.SC_UNAUTHORIZED) {}
    		else {
            	if(response.getStatusCode() == 0) {//Magic Number!!! -> network problem
            		showErrorDialog (Constantes.INSTANCIA.errorLbl() , 
            				Constantes.INSTANCIA.networkERROR());
            	} else showErrorDialog (String.valueOf(
            			response.getStatusCode()), response.getText());
    		}
    	}

    }
}
