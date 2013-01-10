package org.centrocontrol.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class PopupAdministrarDocumento {
	
    private static Logger logger = Logger.getLogger("PopupAdministrarDocumento");
	
    private static PopupAdministrarDocumentoUiBinder uiBinder = GWT.create(PopupAdministrarDocumentoUiBinder.class);
    
    
    interface PopupAdministrarDocumentoUiBinder extends UiBinder<Widget, PopupAdministrarDocumento> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField PushButton cancelarEventoButton;
    @UiField HTML mensajeHTML;
    EventoSistemaVotacionJso evento;
    
	public PopupAdministrarDocumento(EventoSistemaVotacionJso evento) {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
        this.evento = evento;
        String mensaje = null;
        switch(evento.getTipoEnumValue()) {
	        case EVENTO_FIRMA:
	        	mensaje = Constantes.INSTANCIA.mensajeCancelarEvento(
	        			Constantes.INSTANCIA.firmaLabelCancelarMsg());
	        	break;
	        case EVENTO_RECLAMACION:
	        	mensaje = Constantes.INSTANCIA.mensajeCancelarEvento(
	        			Constantes.INSTANCIA.reclamacionLabelCancelarMsg());
	        	break;
	        case EVENTO_VOTACION:
	        	mensaje = Constantes.INSTANCIA.mensajeCancelarEvento(
	        			Constantes.INSTANCIA.votacionLabelCancelarMsg());
	        	break;
	        default:
	        	logger.info("Documento desconocido");
        }
        mensajeHTML.setHTML(mensaje);
	}

    @UiHandler("cancelarEventoButton")
    void handleCancelarEventoButton(ClickEvent e) {
    	popupPanel.hide();
    	DialogoCancelarEvento dialogoCancelarEvento = new DialogoCancelarEvento(evento);
    	dialogoCancelarEvento.show();
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
