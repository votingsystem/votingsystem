package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class DialogoAnulacionSolicitudAcceso implements EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("DialogoAnulacionSolicitudAcceso");
	
    private static DialogoAnulacionSolicitudAccesoUiBinder uiBinder = GWT.create(DialogoAnulacionSolicitudAccesoUiBinder.class);
    
    
    interface DialogoAnulacionSolicitudAccesoUiBinder extends UiBinder<Widget, DialogoAnulacionSolicitudAcceso> {}

    @UiField PushButton anularSolicitudButton;
    @UiField PushButton cancelButton;
    @UiField HTML cancelAccessRequestMsg;
    @UiField HTML accessRequestCanceledMsg;
    
    
    @UiField DialogBox dialogBox;
    EventoSistemaVotacionJso voto;

    
	public DialogoAnulacionSolicitudAcceso(EventoSistemaVotacionJso voto) {
        uiBinder.createAndBindUi(this);
        this.voto = voto;
        accessRequestCanceledMsg.setVisible(false);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
	}

    @UiHandler("anularSolicitudButton")
    void handleAnularSolicitudButton(ClickEvent e) {
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create();
		mensajeClienteFirma.setArgs(voto.getHashCertificadoVotoBase64());
		mensajeClienteFirma.setOperacionEnumValue(Operacion.ANULAR_SOLICITUD_ACCESO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getURLAnulacionVoto());
		mensajeClienteFirma.setNombreDestinatarioFirma(PuntoEntrada.INSTANCIA.servidor.getNombre());
		if(!Browser.isAndroid()) PanelVotacion.INSTANCIA.setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
		anularSolicitudButton.setVisible(false);
    }
    
    @UiHandler("cancelButton")
    void handleCancelButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    public void hide() {
    	dialogBox.hide();
    }
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    }

	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case ANULAR_SOLICITUD_ACCESO:
				PanelVotacion.INSTANCIA.setWidgetsStateFirmando(false);
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
			        accessRequestCanceledMsg.setVisible(true);
			        cancelAccessRequestMsg.setVisible(false);
			        cancelButton.setVisible(true);
				} else {
					anularSolicitudButton.setVisible(true);
				}
				break;
			default: break;
		}
		
	}


}
