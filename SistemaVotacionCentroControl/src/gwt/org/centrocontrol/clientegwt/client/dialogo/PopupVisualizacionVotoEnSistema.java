package org.centrocontrol.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class PopupVisualizacionVotoEnSistema {
	
    private static Logger logger = Logger.getLogger("PopupVisualizacionVotoEnSistema");
	
    private static PopupReciboVotoUiBinder uiBinder = GWT.create(PopupReciboVotoUiBinder.class);
    
    
    interface PopupReciboVotoUiBinder extends UiBinder<Widget, PopupVisualizacionVotoEnSistema> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField HTML message;
    EventoSistemaVotacionJso voto;
    
	public PopupVisualizacionVotoEnSistema(EventoSistemaVotacionJso voto) {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
        message.setHTML(Constantes.INSTANCIA.textoPopupVisualizarVotoEnSistema(
        		ServerPaths.getUrlVoto(
        				voto.getHashCertificadoVotoHex())));
        this.voto = voto;
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
