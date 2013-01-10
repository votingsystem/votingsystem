package org.controlacceso.clientegwt.client.dialogo;

import java.util.List;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.modelo.CampoDeEventoJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.OpcionDeEventoJso;
import org.controlacceso.clientegwt.client.util.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PopupInfoEvento {
	
    private static Logger logger = Logger.getLogger("PopupInfoEvento");
	
    private static PopupInfoEventoUiBinder uiBinder = GWT.create(PopupInfoEventoUiBinder.class);
    
    interface EditorStyle extends CssResource {
        String campoLabel();
    }
    
    int MAX_NUM_CARACTERES_SUBJECT = 80;
    
    interface PopupInfoEventoUiBinder extends UiBinder<Widget, PopupInfoEvento> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField EditorStyle style;
    @UiField HTML asunto;
    @UiField HTML contenido;
    @UiField Label tipoCampoLabel;
    @UiField VerticalPanel camposPanel;

    
	public PopupInfoEvento(EventoSistemaVotacionJso evento) {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
        asunto.setHTML(StringUtils.partirTexto(evento.getAsunto(), MAX_NUM_CARACTERES_SUBJECT));
        contenido.setHTML(evento.getContenido());
        String contenidoStr = contenido.getText();
        if(contenidoStr.length() >= 1000) {
        	contenidoStr = 
            		contenidoStr.substring(0, 1000);
        	 contenido.setText(contenidoStr + "   "
             		+ Constantes.INSTANCIA.continuaLabel());
        }
        tipoCampoLabel.setVisible(false);
        camposPanel.setVisible(false);
        switch(evento.getTipoEnumValue()) {
	        case EVENTO_VOTACION:
	        	tipoCampoLabel.setText(Constantes.INSTANCIA.tipoCampoLabelOpciones());
	        	tipoCampoLabel.setVisible(true);
	        	List<OpcionDeEventoJso> opciones = evento.getOpcionDeEventoList();
	        	if(opciones != null) {
		        	for(OpcionDeEventoJso opcion : opciones) {
		        		Label opcionLabel = new Label(" - " + opcion.getContenido());
		        		opcionLabel.setStyleName(style.campoLabel(), true);
		        		camposPanel.add(opcionLabel);
		        	}	
	        	}
	        	camposPanel.setVisible(true);
	        	break;
	        case EVENTO_RECLAMACION:
	        	List<CampoDeEventoJso> campos;
	        	if((campos = evento.getCampoDeEventoList()) != null) {
	        		tipoCampoLabel.setText(Constantes.INSTANCIA.tipoCampoLabelCamposReclamacion());
	        		tipoCampoLabel.setVisible(true);
	        		for(CampoDeEventoJso campo : campos) {
		        		Label campoLabel = new Label(" - " + campo.getContenido());
		        		campoLabel.setStyleName(style.campoLabel(), true);
		        		camposPanel.add(campoLabel);
		        	}
		        	camposPanel.setVisible(true);
	        	}
	        	break;
        }
        
	}

	public void setPopupPosition(int clientX, int clientY) {
		if(!popupPanel.isShowing()) {
			popupPanel.setPopupPosition(clientX, clientY);
	    	popupPanel.show();
		}
    }
	
	public boolean isShowing() {
		return popupPanel.isShowing();
	}
	
	public void show() {
		popupPanel.show();
    }
    
	public void hide() {
		popupPanel.hide();
    }
}
