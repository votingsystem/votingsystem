package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.votaciones.DialogoAsociarCentroControl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class PopUpCampoReclamacion {
	
    private static Logger logger = Logger.getLogger("PopUpCampoReclamacion");
	
    private static PopUpCampoReclamacionUiBinder uiBinder = GWT.create(PopUpCampoReclamacionUiBinder.class);
    
    
    interface PopUpCampoReclamacionUiBinder extends UiBinder<Widget, PopUpCampoReclamacion> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField PushButton anyadirCampoReclamacionButton;
    PanelPublicacionReclamacion panelPublicacionReclamacion;
    
	public PopUpCampoReclamacion() {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
	}

    @UiHandler("anyadirCampoReclamacionButton")
    void handleAnyadirCampoReclamacion(ClickEvent e) {
    	popupPanel.hide();
    	DialogoCrearCampoReclamacion dialogoCrear = 
    			new DialogoCrearCampoReclamacion(panelPublicacionReclamacion);
    	dialogoCrear.show();
    }
	
    public PopUpCampoReclamacion(int clientX, int clientY) {
		this();
		popupPanel.setPopupPosition(clientX, clientY);
	}

    public PopUpCampoReclamacion(
			PanelPublicacionReclamacion panelPublicacionReclamacion) {
    	this();
		this.panelPublicacionReclamacion = panelPublicacionReclamacion;
	}

	public void setPopupPosition(int clientX, int clientY) {
    	popupPanel.setPopupPosition(clientX, clientY);
    }
    
	public void show() {
    	popupPanel.show();
    }
    
}
