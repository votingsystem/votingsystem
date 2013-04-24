package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class PopupReciboVoto {
	
    private static Logger logger = Logger.getLogger("PopupReciboVoto");
	
    private static PopupReciboVotoUiBinder uiBinder = GWT.create(PopupReciboVotoUiBinder.class);
    
    
    interface PopupReciboVotoUiBinder extends UiBinder<Widget, PopupReciboVoto> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField PushButton guardarVotoButton;
    
    EventoSistemaVotacionJso voto;
    
	public PopupReciboVoto(EventoSistemaVotacionJso voto) {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
        this.voto = voto;
	}

    @UiHandler("guardarVotoButton")
    void handleGuardarVotoButton(ClickEvent e) {
    	popupPanel.hide();
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create();
		mensajeClienteFirma.setArgs(voto.getHashCertificadoVotoBase64());
		mensajeClienteFirma.setOperacionEnumValue(Operacion.GUARDAR_RECIBO_VOTO);
		if(!Browser.isAndroid()) PanelVotacion.INSTANCIA.setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
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
    
}
