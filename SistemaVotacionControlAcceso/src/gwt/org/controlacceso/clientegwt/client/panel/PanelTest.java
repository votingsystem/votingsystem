package org.controlacceso.clientegwt.client.panel;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.HtmlTemplates;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
    	enviarMensaje("Hoooooooola desde GWT");
    }
    
    
	public static native void enviarMensaje(String appMesage) /*-{
		$wnd.setAppMessage(appMesage);
	}-*/;
}