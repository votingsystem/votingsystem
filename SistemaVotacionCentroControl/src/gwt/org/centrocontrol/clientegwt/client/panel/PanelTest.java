package org.centrocontrol.clientegwt.client.panel;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.HistoryToken;
import org.centrocontrol.clientegwt.client.HtmlTemplates;
import org.centrocontrol.clientegwt.client.PuntoEntrada;
import org.centrocontrol.clientegwt.client.dialogo.DialogoCargaClienteAndroid;
import org.centrocontrol.clientegwt.client.dialogo.ErrorDialog;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.centrocontrol.clientegwt.client.util.Browser;
import org.centrocontrol.clientegwt.client.util.RequestHelper;
import org.centrocontrol.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PanelTest extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelTest");

	private static PanelTestUiBinder uiBinder = GWT
			.create(PanelTestUiBinder.class);

	interface PanelTestUiBinder extends UiBinder<Widget, PanelTest> {	}

    
    @UiField Button votarAndroidButton;
    @UiField Button enviarMensaje;

	public PanelTest() {
    	initWidget(uiBinder.createAndBindUi(this));
	}
    

	
    @UiHandler("votarAndroidButton")
    void handleVotarAndroidButton(ClickEvent e) {
    	if(Browser.isAndroid()) {
    		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
    				Operacion.SOLICITUD_COPIA_SEGURIDAD.toString(), 
    				MensajeClienteFirmaJso.SC_PROCESANDO);
    		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlSolicitudCopiaSeguridad());
    		mensajeClienteFirma.setEmailSolicitante("España continuación");
        	mensajeClienteFirma.setNombreDestinatarioFirma(
        			PuntoEntrada.INSTANCIA.servidor.getNombre());
    		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
    	} else {
    		Window.alert("Es cliente NO es android");
    	}
	}
    
    @UiHandler("enviarMensaje")
    void handleEnviarMensaje(ClickEvent e) {
    	enviarMensaje("PING from GWT");
    }
    
    @UiHandler("redirect") void handleRedirect(ClickEvent e) {
    	logger.info("redirect");
    	ServerPaths.redirect(ServerPaths.getUrlClienteAndroid());
    }
    
    @UiHandler("doget") void handleDoget(ClickEvent e) {
    	logger.info("doget");
		RequestHelper.doGet(ServerPaths.getUrlClienteAndroid(), new ServerAndroidRequestCallback());
    }
    
	public static native void enviarMensaje(String appMesage) /*-{
		$wnd.setClienteFirmaMessage(appMesage);
	}-*/;
	
    private class ServerAndroidRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	new ErrorDialog().show (Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("OK - response.getText(): " + response.getText());
            } else {
            	logger.log(Level.SEVERE, "ERROR - response.getText(): " + response.getText());
            	//new ErrorDialog().show (String.valueOf(response.getStatusCode()), response.getText());
            }
        }

    }
}