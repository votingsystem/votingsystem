package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.dialogo.DialogoSolicitudEmail;
import org.controlacceso.clientegwt.client.dialogo.SolicitanteEmail;
import org.controlacceso.clientegwt.client.modelo.EstadisticaJso;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class PopupEstadisticasVotacion {
	
    private static Logger logger = Logger.getLogger("PopupEstadisticasVotacion");
	
    private static PopupEstadisticasVotacionUiBinder uiBinder = GWT.create(PopupEstadisticasVotacionUiBinder.class);
    
    
    interface PopupEstadisticasVotacionUiBinder extends UiBinder<Widget, PopupEstadisticasVotacion> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField PushButton solicitarCopiaSeguridadButton;
    SolicitanteEmail solicitanteEmail;
	@UiField Label numSolicitudesAcceso;
	@UiField Label numSolicitudesAccesoOK;
	@UiField Label numSolicitudesAccesoANULADAS;
	@UiField Label numVotos;
	@UiField Label numVotosOK;
	@UiField Label numVotosANULADOS;
	@UiField HTML mensajeSolicitudCopiaSeguridadVotacionAbierta;
    
	public PopupEstadisticasVotacion(EstadisticaJso estadisticas, 
			SolicitanteEmail solicitanteCopiaSeguridad, boolean puedeSolicitarCopias) {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
        this.solicitanteEmail = solicitanteCopiaSeguridad;
	    numSolicitudesAcceso.setText(new Integer(estadisticas.getNumeroSolicitudesDeAcceso()).toString());
	    numSolicitudesAccesoOK.setText(new Integer(estadisticas.getNumeroSolicitudesDeAccesoOK()).toString());
	    numSolicitudesAccesoANULADAS.setText(new Integer(estadisticas.getNumeroSolicitudesDeAccesoANULADAS()).toString());
	    numVotos.setText(new Integer(estadisticas.getNumeroVotos()).toString());
	    numVotosOK.setText(new Integer(estadisticas.getNumeroVotosOK()).toString());
	    numVotosANULADOS.setText(new Integer(estadisticas.getNumeroVotosANULADOS()).toString());
	    if(puedeSolicitarCopias) mensajeSolicitudCopiaSeguridadVotacionAbierta.setVisible(false);
	    else solicitarCopiaSeguridadButton.setVisible(false);
	}

    @UiHandler("solicitarCopiaSeguridadButton")
    void handleAnyadirCampoReclamacion(ClickEvent e) {
    	popupPanel.hide();
    	DialogoSolicitudEmail dialogoEmail = 
    			new DialogoSolicitudEmail(null, solicitanteEmail);
    	dialogoEmail.show();
    }
	
	public void setPopupPosition(int clientX, int clientY) {
    	popupPanel.setPopupPosition(clientX, clientY);
    	popupPanel.show();
    }
    
	public void show() {
		popupPanel.show();
    }
    
}
