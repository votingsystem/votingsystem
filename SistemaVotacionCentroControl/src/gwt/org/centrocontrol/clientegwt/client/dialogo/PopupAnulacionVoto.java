package org.centrocontrol.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.PuntoEntrada;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.centrocontrol.clientegwt.client.panel.PanelVotacion;
import org.centrocontrol.clientegwt.client.util.Browser;
import org.centrocontrol.clientegwt.client.util.ServerPaths;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class PopupAnulacionVoto implements ConfirmacionListener {
	
    private static Logger logger = Logger.getLogger("PopupAnulacionVoto");
	
    private static PopupAnulacionVotoUiBinder uiBinder = GWT.create(PopupAnulacionVotoUiBinder.class);
    
    
    interface PopupAnulacionVotoUiBinder extends UiBinder<Widget, PopupAnulacionVoto> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField PushButton anularVotoButton;
    
    EventoSistemaVotacionJso voto;
    
	public PopupAnulacionVoto(EventoSistemaVotacionJso voto) {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
        this.voto = voto;
	}

    @UiHandler("anularVotoButton")
    void handleAnularVotoButton(ClickEvent e) {    	popupPanel.hide();
    	DialogoConfirmacion dialogoOpcion = new DialogoConfirmacion(null, this);
    	dialogoOpcion.show(Constantes.INSTANCIA.anularVotoLabel(), 
    			Constantes.INSTANCIA.textoConfirmAnularVoto(
				voto.getOpcionSeleccionada().getContenido()));
    }
	

    public void setPopupPosition(int clientX, int clientY) {
    	popupPanel.setPopupPosition(clientX, clientY);
    }
    
	public void show() {
    	popupPanel.show();
    }
    
	public void hide() {
		popupPanel.hide();
	}

	@Override
	public void confirmed(Integer id, Object param) {
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create();
		mensajeClienteFirma.setArgs(voto.getHashCertificadoVotoBase64());
		mensajeClienteFirma.setOperacionEnumValue(Operacion.ANULAR_VOTO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getURLAnulacionVoto(
				voto.getControlAcceso().getServerURL()));
		mensajeClienteFirma.setNombreDestinatarioFirma(PuntoEntrada.INSTANCIA.servidor.getNombre());
		if(!Browser.isAndroid()) PanelVotacion.INSTANCIA.setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
		
	}
}
