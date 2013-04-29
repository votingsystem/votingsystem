package org.controlacceso.clientegwt.client.panel;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.HistoryToken;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

public class PanelSubsistemas extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelSubsistemas");

	private static PanelSubsistemasUiBinder uiBinder = GWT
			.create(PanelSubsistemasUiBinder.class);

	interface PanelSubsistemasUiBinder extends UiBinder<Widget, PanelSubsistemas> {	}

    @UiField Hyperlink sistemaVotacionLink;
    @UiField Hyperlink sistemaFirmasLink;
    @UiField Hyperlink sistemaReclamacionesLink;
    public static PanelSubsistemas INSTANCIA;
 
	public PanelSubsistemas() {
		initWidget(uiBinder.createAndBindUi(this));
		sistemaVotacionLink.setTargetHistoryToken(HistoryToken.VOTACIONES.toString());
		sistemaFirmasLink.setTargetHistoryToken(HistoryToken.MANIFIESTOS.toString());
		sistemaReclamacionesLink.setTargetHistoryToken(HistoryToken.RECLAMACIONES.toString());
		if(PanelCentral.INSTANCIA != null)
			actualizarSistema(PanelCentral.INSTANCIA.getSistemaSeleccionado());
		INSTANCIA = this;
	}

	
	public void actualizarSistema(HistoryToken token) {
		if(token == null) token = HistoryToken.VOTACIONES;
		logger.info("actualizarSistema: " + token.toString());
		sistemaVotacionLink.setVisible(false);
		sistemaReclamacionesLink.setVisible(false);
		sistemaFirmasLink.setVisible(false);
		switch(token) {
			case MANIFIESTOS:
				sistemaVotacionLink.setVisible(true);
				sistemaReclamacionesLink.setVisible(true);
				break;
			case RECLAMACIONES:
				sistemaVotacionLink.setVisible(true);
				sistemaFirmasLink.setVisible(true);
				break;
			case VOTACIONES:
				sistemaFirmasLink.setVisible(true);
				sistemaReclamacionesLink.setVisible(true);
				break;	
			case REPRESENTATIVES_PAGE:
				sistemaFirmasLink.setVisible(true);
				sistemaReclamacionesLink.setVisible(true);
				sistemaVotacionLink.setVisible(true);
			default:
				break;
		}
	}
   
    @UiHandler("sistemaVotacionLink")
    void onClickSistemaVotacionLink(ClickEvent e) {
    	logger.info("onClickSistemaVotacionLink");
		History.newItem(HistoryToken.VOTACIONES.toString());
    }
    
    @UiHandler("sistemaFirmasLink")
    void onClickSistemaFirmasLink(ClickEvent e) {
    	logger.info("onClickSistemaFirmasLink");
		History.newItem(HistoryToken.MANIFIESTOS.toString());
    }
    
    @UiHandler("sistemaReclamacionesLink")
    void onClickSistemaReclamacionesLink(ClickEvent e) {
    	logger.info("sistemaReclamacionesLink");
		History.newItem(HistoryToken.RECLAMACIONES.toString());
    }

}