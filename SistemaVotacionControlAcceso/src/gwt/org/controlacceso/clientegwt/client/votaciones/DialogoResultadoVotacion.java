package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.util.PopUpLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoResultadoVotacion implements EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("DialogoResultadoVotacion");
	
    private static DialogoResultadoVotacionUiBinder uiBinder = GWT.create(DialogoResultadoVotacionUiBinder.class);
    
    
    interface DialogoResultadoVotacionUiBinder extends UiBinder<Widget, DialogoResultadoVotacion> {}

    @UiField VerticalPanel textPanel;
    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    PopupReciboVoto popupReciboVoto;
    PopupAnulacionVoto popupAnulacionVoto;
    PopupVisualizacionVotoEnSistema popupVisualizacionVotoEnSistema;
    EventoSistemaVotacionJso voto;
    public static DialogoResultadoVotacion INSTANCIA;
    
    @UiField PopUpLabel votoEnSistemaLabel;
    @UiField PopUpLabel guardarVotoLabel;
    @UiField PopUpLabel anularVotoLabel;
    @UiField HTML resultadoOperacion;
    
    GuardarReciboEventListener guardarReciboEventListener = new GuardarReciboEventListener();
	AnulacionVotoEventListener anulacionVotoEventListener = new AnulacionVotoEventListener();
	VotoEnSistemaEventListener votoEnSistemaEventListener = new VotoEnSistemaEventListener();
    
	public DialogoResultadoVotacion(EventoSistemaVotacionJso voto) {
        uiBinder.createAndBindUi(this);
        this.voto = voto;
        INSTANCIA = this;
        resultadoOperacion.setHTML(Constantes.INSTANCIA.resultadoVotacionMessage(
        		voto.getAsunto(), voto.getOpcionSeleccionada().getContenido()));
        guardarVotoLabel.setListener(guardarReciboEventListener);
        anularVotoLabel.setListener(anulacionVotoEventListener);
        votoEnSistemaLabel.setListener(votoEnSistemaEventListener);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
	}

    @UiHandler("aceptarButton")
    void handleCloseButton(ClickEvent e) {
    	History.newItem(HistoryToken.VOTACIONES.toString());
    	dialogBox.hide();
    }
    
    public void hide() {
    	dialogBox.hide();
    }
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    }
    
	class GuardarReciboEventListener implements EventListener {

		@Override
		public void onBrowserEvent(Event event) {
			switch(DOM.eventGetType(event)) {
				case Event.ONCLICK:
				//case Event.ONMOUSEOVER:
					mostrarPopupReciboVoto(event.getClientX(), event.getClientY());
					break;
			    case Event.ONMOUSEOUT:
			    	break;
			}
		}
		
	}

	
	class VotoEnSistemaEventListener implements EventListener {

		@Override
		public void onBrowserEvent(Event event) {
			switch(DOM.eventGetType(event)) {
				case Event.ONCLICK:
				//case Event.ONMOUSEOVER:
					mostrarPopupVisualizacionVoto(event.getClientX(), event.getClientY());
					break;
			    case Event.ONMOUSEOUT:
			    	break;
			}
		}
		
	}
	
	private void mostrarPopupVisualizacionVoto(int clientX, int clientY) {
		if(popupVisualizacionVotoEnSistema == null) {
			popupVisualizacionVotoEnSistema = new PopupVisualizacionVotoEnSistema(voto);
		}
		if(popupAnulacionVoto != null) popupAnulacionVoto.hide();
		if(popupReciboVoto != null) popupReciboVoto.hide();
		popupVisualizacionVotoEnSistema.setPopupPosition(clientX, clientY);
		popupVisualizacionVotoEnSistema.show();
	}
	
	private void mostrarPopupAnulacionVoto(int clientX, int clientY) {
		if(popupAnulacionVoto == null) {
			popupAnulacionVoto = new PopupAnulacionVoto(voto);
		}
		if(popupVisualizacionVotoEnSistema != null) popupVisualizacionVotoEnSistema.hide();
		if(popupReciboVoto != null) popupReciboVoto.hide();
		popupAnulacionVoto.setPopupPosition(clientX, clientY);
		popupAnulacionVoto.show();
	}
	
	private void mostrarPopupReciboVoto(int clientX, int clientY) {
		if(popupReciboVoto == null) {
			popupReciboVoto = new PopupReciboVoto(voto);
		}
		if(popupVisualizacionVotoEnSistema != null) popupVisualizacionVotoEnSistema.hide();
		if(popupAnulacionVoto != null) popupAnulacionVoto.hide();
		popupReciboVoto.setPopupPosition(clientX, clientY);
		popupReciboVoto.show();
	}
	
	class AnulacionVotoEventListener implements EventListener {

		@Override
		public void onBrowserEvent(Event event) {
			switch(DOM.eventGetType(event)) {
				case Event.ONCLICK:
				//case Event.ONMOUSEOVER:
					mostrarPopupAnulacionVoto(event.getClientX(), event.getClientY());
					break;
			    case Event.ONMOUSEOUT:
			    	break;
			}
		}
		
	}

	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case ANULAR_VOTO:
				if(200 == mensaje.getCodigoEstado()) dialogBox.hide();
				break;
			default: break;
		}
		
	}
    
}
