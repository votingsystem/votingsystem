package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class PopupSolicitudCopiaSeguridad {
	
    private static Logger logger = Logger.getLogger("PopUpSolicitudCopiaSeguridad");
	
    private static PopUpSolicitudCopiaSeguridadUiBinder uiBinder = GWT.create(PopUpSolicitudCopiaSeguridadUiBinder.class);
    
    
    interface PopUpSolicitudCopiaSeguridadUiBinder extends UiBinder<Widget, PopupSolicitudCopiaSeguridad> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField PushButton solicitarCopiaSeguridadButton;
    SolicitanteEmail solicitanteEmail;
    
	public PopupSolicitudCopiaSeguridad() {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
	}

    @UiHandler("solicitarCopiaSeguridadButton")
    void handleAnyadirCampoReclamacion(ClickEvent e) {
    	popupPanel.hide();
    	DialogoSolicitudEmail dialogoEmail = 
    			new DialogoSolicitudEmail(solicitanteEmail);
    	dialogoEmail.show();
    }
	
    public PopupSolicitudCopiaSeguridad(int clientX, int clientY) {
		this();
		popupPanel.setPopupPosition(clientX, clientY);
	}

    public PopupSolicitudCopiaSeguridad(SolicitanteEmail solicitanteCopiaSeguridad) {
    	this();
		this.solicitanteEmail = solicitanteCopiaSeguridad;
	}

	public void setPopupPosition(int clientX, int clientY) {
    	popupPanel.setPopupPosition(clientX, clientY);
    }
    
	public void show() {
		popupPanel.show();
    }

	public boolean isShowing() {
		return popupPanel.isShowing();
	}

	public void hide() {
		popupPanel.hide();
	}
    
}
