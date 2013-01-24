package org.centrocontrol.clientegwt.client.panel;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.HistoryToken;
import org.centrocontrol.clientegwt.client.Recursos;
import org.centrocontrol.clientegwt.client.dialogo.ErrorDialog;
import org.centrocontrol.clientegwt.client.dialogo.PopupInfoEvento;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.util.DateUtils;
import org.centrocontrol.clientegwt.client.util.RequestHelper;
import org.centrocontrol.clientegwt.client.util.ServerPaths;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PanelEvento extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelEvento");

	private static PanelEventoUiBinder uiBinder = GWT
			.create(PanelEventoUiBinder.class);

	interface PanelEventoUiBinder extends UiBinder<Widget, PanelEvento> {}
	
    interface EditorStyle extends CssResource {
        String abiertoLabel();
        String canceladoLabel();
        String finalizadoLabel();
        String pendienteAbrirLabel();
        String abiertoHeader();
        String pendienteAbrirHeader();
        String canceladoHeader();
        String finalizadoHeader();
        String mainPanelPendiente();
        String mainPanelCancelado();
        String mainPanelAbierto();
    }

	@UiField Label asuntoLabel;
    @UiField EditorStyle style;
	@UiField HTML autorLabel;
	@UiField HTML comienzoLabel;
	@UiField Label duracionLabel;
	@UiField Label estadoLabel;
	@UiField VerticalPanel mainPanel;
	@UiField VerticalPanel headerPanel;
	
	private EventoSistemaVotacionJso evento;
	private PopupInfoEvento popupInfoEvento;

	public PanelEvento(EventoSistemaVotacionJso evento) {
		initWidget(uiBinder.createAndBindUi(this));
        sinkEvents(Event.ONCLICK);
        //sinkEvents(Event.ONMOUSEOVER);
        //sinkEvents(Event.ONMOUSEOUT);
		this.evento = evento;
		asuntoLabel.setText(truncateEventSubject(evento.getAsunto()));
		if(evento.getUsuario() != null) {
			autorLabel.setHTML("<b>" + Constantes.INSTANCIA.publicadoPorLabel() 
					+ "</b>: " + evento.getUsuario());
		}
		comienzoLabel.setHTML("<b>" + Constantes.INSTANCIA.fechaInicioLabel() + "</b>: " + 
				DateUtils.getSpanishStringFromDate(evento.getFechaInicio()));
		duracionLabel.setText(Constantes.INSTANCIA.duracionLabel() + ": " + 
				DateUtils.getElpasedTimeHours(evento.getFechaInicio(), 
				evento.getFechaFin()) + " " + Constantes.INSTANCIA.horasLabel());
		Image pushButtonImage;
		PushButton botonAccion = null;
		pushButtonImage = new Image(Recursos.INSTANCIA.botonVotar());
		if(evento.getFechaFin().after(DateUtils.getTodayDate()) &&
				evento.getFechaInicio().before(DateUtils.getTodayDate()) ) {
			botonAccion = new PushButton(pushButtonImage);
		}
		evento.setEstadoEnumValue(comprobarFechasEvento(evento));
		switch(evento.getEstadoEnumValue()) {
			case ACTIVO:
				estadoLabel.setText(Constantes.INSTANCIA.abiertoLabel());
				estadoLabel.setStyleName(style.abiertoLabel(), true);
				headerPanel.setStyleName(style.abiertoHeader(), true);
				duracionLabel.setText(Constantes.INSTANCIA.remainLabel() + ": " + 
						DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin()) + 
						" " + Constantes.INSTANCIA.horasLabel());
				mainPanel.setStyleName(style.mainPanelAbierto(), true);
				break;
			case CANCELADO:
				estadoLabel.setText(Constantes.INSTANCIA.canceladoLabel());
				estadoLabel.setStyleName(style.canceladoLabel(), true);
				headerPanel.setStyleName(style.canceladoHeader(), true);
				mainPanel.setStyleName(style.mainPanelCancelado(), true);
				break;
			case FINALIZADO:
				estadoLabel.setText(Constantes.INSTANCIA.finalizadoLabel());
				estadoLabel.setStyleName(style.finalizadoLabel(), true);
				headerPanel.setStyleName(style.finalizadoHeader(), true);
				mainPanel.setStyleName(style.mainPanelCancelado(), true);
				break;
			case PENDIENTE_COMIENZO:
				estadoLabel.setText(Constantes.INSTANCIA.pendingLabel());
				estadoLabel.setStyleName(style.pendienteAbrirLabel(), true);
				headerPanel.setStyleName(style.pendienteAbrirHeader(), true);
				mainPanel.setStyleName(style.mainPanelPendiente(), true);
				break;			
		}
		//DOM.setStyleAttribute(label.getElement(),"border", "1px solid #00f");

	}
	
	private EventoSistemaVotacionJso.Estado 
			comprobarFechasEvento (EventoSistemaVotacionJso evento) {
		EventoSistemaVotacionJso.Estado estado = null;
	    Date fecha = DateUtils.getTodayDate();
	    if (fecha.after(evento.getFechaFin())) estado = EventoSistemaVotacionJso.Estado.FINALIZADO;
	    if (fecha.after(evento.getFechaInicio()) && fecha.before(evento.getFechaFin()))   
	        estado = EventoSistemaVotacionJso.Estado.ACTIVO;
	    if (fecha.before(evento.getFechaInicio()))  estado = EventoSistemaVotacionJso.Estado.PENDIENTE_COMIENZO;
	    if(evento.getEstadoEnumValue() != estado) {
	    	logger.info("--------- Discrepancia en los estados del evento " + evento.getId());
	    	RequestHelper.doGet(ServerPaths.getUrlComprobacionFechasEvento(evento.getId()), 
	    			new ServerRequestCallback());
	    }
	    return estado;
	}


	public void onBrowserEvent(Event event){
		switch(DOM.eventGetType(event)) {
			case Event.ONCLICK:
	    		PanelCentral.INSTANCIA.actualizarEventoSistemaVotacion(evento);
	    		History.newItem(HistoryToken.VOTAR.toString() 
	    				+ "&eventoId=" +  evento.getId());
	       		break;
			/*case Event.ONMOUSEOVER:
				if(popupInfoEvento == null || !popupInfoEvento.isShowing()) {
					mostrarPopupInfoEvento(event.getClientX(), event.getClientY());
				} 
				break;
		    case Event.ONMOUSEOUT:
	    		if(DOM.eventGetToElement(event) == null) return;
		    	if(!DOM.isOrHasChild(getElement(), DOM.eventGetToElement(event))) {
			    	popupInfoEvento.hide();
		    	}
		    	break;*/
		}
	}
	
  	private void mostrarPopupInfoEvento(int clientX, int clientY) {
  		if(popupInfoEvento == null) {
  			popupInfoEvento = new PopupInfoEvento(evento);
  		}
  		popupInfoEvento.setPopupPosition(clientX, clientY);
  		popupInfoEvento.show();
  	}
  	
  	private String truncateEventSubject(String subject) {
  		String result = "";
  		if(subject == null || "".equals(subject)) return "";
  		if(subject.length() < 20) return subject;
  		String[] words = subject.split(" ");
  		for(int i = 0; i < words.length; i++) {
  			if(words[i].length() >= 20) {
  				result = result + words[i].substring(0, 20) + " " + Constantes.INSTANCIA.continuaLabel();
  				return result;
  			} else result = result + " " + words[i];
  		}
  		if(result.length() >= 80) {
  			return result.substring(0, 80) + " " + Constantes.INSTANCIA.continuaLabel();
  		}
  		return result.trim();	
	}

    private class ServerRequestCallback implements RequestCallback {

        @Override
    	public void onError(Request request, Throwable exception) {
        	new ErrorDialog().show(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());
        	logger.log(Level.SEVERE, exception.getMessage(), exception);
    	}

        @Override
    	public void onResponseReceived(Request request, Response response) {
			logger.log(Level.INFO, "StatusCode: " + response.getStatusCode() + 
					", Response Text: " + response.getText());
    	}

    }
}