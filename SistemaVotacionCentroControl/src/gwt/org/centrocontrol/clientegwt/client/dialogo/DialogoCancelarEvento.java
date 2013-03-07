package org.centrocontrol.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.PuntoEntrada;
import org.centrocontrol.clientegwt.client.evento.BusEventos;
import org.centrocontrol.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.centrocontrol.clientegwt.client.util.Browser;
import org.centrocontrol.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoCancelarEvento implements EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("DialogoCancelarEvento");
	
    private static DialogoCancelarEventoUiBinder uiBinder = GWT.create(DialogoCancelarEventoUiBinder.class);
    
    
    interface DialogoCancelarEventoUiBinder extends UiBinder<Widget, DialogoCancelarEvento> {}

    @UiField VerticalPanel textPanel;
    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton siguientePasoButton;
    @UiField PushButton cancelarButton;
    @UiField CheckBox checkBoxCancelarDocumento;
    @UiField CheckBox checkBoxAnularDocumento;
    @UiField VerticalPanel panelResultadoOperacion;
    @UiField VerticalPanel panelTomaDatos;
    @UiField HorizontalPanel panelBarrarProgreso;
    @UiField HTML resultadoOperacionMessage;

    EventoSistemaVotacionJso evento;
	EventoSistemaVotacionJso.Estado estadoEvento = null;
    
	public DialogoCancelarEvento(EventoSistemaVotacionJso evento) {
        uiBinder.createAndBindUi(this);
        this.evento = evento;
        panelResultadoOperacion.setVisible(false);
        panelBarrarProgreso.setVisible(false);
        checkBoxCancelarDocumento.setValue(true);
        checkBoxAnularDocumento.setValue(false);
        switch(evento.getTipoEnumValue()) {
	        case EVENTO_FIRMA:
	        	dialogBox.setText(Constantes.INSTANCIA.cancelarManifiestoCaption());
	        	break;
	        case EVENTO_RECLAMACION:
	        	dialogBox.setText(Constantes.INSTANCIA.cancelarReclamacionCaption());
	        	break;
	        case EVENTO_VOTACION:
	        	dialogBox.setText(Constantes.INSTANCIA.cancelarVotacionCaption());
	        	break;
	        default:
	        	logger.info("Documento desconocido");
        }
        
        
        checkBoxAnularDocumento.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				checkBoxCancelarDocumento.setValue(false);
				checkBoxAnularDocumento.setValue(true);
			}});
        checkBoxCancelarDocumento.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				if(checkBoxCancelarDocumento.getValue()) {
					checkBoxCancelarDocumento.setValue(true);
					checkBoxAnularDocumento.setValue(false);
				}
			}});        
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
	}

    @UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
	@UiHandler("cancelarButton")
    void handleCancelarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    
    @UiHandler("siguientePasoButton")
    void handleSiguientePasoButton(ClickEvent e) {
    	panelBarrarProgreso.setVisible(true);
    	panelTomaDatos.setVisible(false);
    	if(checkBoxCancelarDocumento.getValue()) {
    		estadoEvento = EventoSistemaVotacionJso.Estado.CANCELADO;
    	} else if(checkBoxAnularDocumento.getValue()) {
    		estadoEvento = EventoSistemaVotacionJso.Estado.BORRADO_DE_SISTEMA;
    	}
    	JavaScriptObject contenidoFirma = MensajeClienteFirmaJso.
    			createMensajeCancelacionEvento(evento.getUrl(), estadoEvento.toString());
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.CANCELAR_EVENTO.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlCancelarEvento(
				evento.getControlAcceso().getServerURL()));
		mensajeClienteFirma.setAsuntoMensajeFirmado(
				Constantes.INSTANCIA.asuntoCancelarDocumento(evento.getAsunto()));
		mensajeClienteFirma.setEvento(evento);
		mensajeClienteFirma.setNombreDestinatarioFirma(evento.getControlAcceso().getNombre());
		mensajeClienteFirma.setContenidoFirma(contenidoFirma);
		mensajeClienteFirma.setRespuestaConRecibo(false);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
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
			case CANCELAR_EVENTO:
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
			    	panelBarrarProgreso.setVisible(false);
			    	switch(estadoEvento) {
			    	case BORRADO_DE_SISTEMA:
			    		resultadoOperacionMessage.setHTML(Constantes.INSTANCIA.eventoBorradoOK());
			    		break;
			    	case CANCELADO:
			    		resultadoOperacionMessage.setHTML(Constantes.INSTANCIA.eventoCanceladoOK());
			    		break;
			    	}
			        panelResultadoOperacion.setVisible(true);
			        return;
				}
				hide();
				break;
			default:
				break;
		}
		
	}
    
}
