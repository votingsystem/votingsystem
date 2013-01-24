package org.centrocontrol.clientegwt.client.panel;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.centrocontrol.clientegwt.client.*;
import org.centrocontrol.clientegwt.client.evento.*;
import org.centrocontrol.clientegwt.client.util.RequestHelper;
import org.centrocontrol.clientegwt.client.util.ServerPaths;
import org.centrocontrol.clientegwt.client.util.StringUtils;
import org.centrocontrol.clientegwt.client.dialogo.DialogoCargaHerramientaValidacion;
import org.centrocontrol.clientegwt.client.dialogo.ErrorDialog;
import org.centrocontrol.clientegwt.client.modelo.*;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.NamedFrame;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PanelCentral extends Composite implements ValueChangeHandler<String>, 
	EventoGWTConsultaEventos.Handler, EventoGWTConsultaEvento.Handler {
	
    private static Logger logger = Logger.getLogger("PanelCentral");

    private static PanelCentralUiBinder uiBinder = GWT.create(PanelCentralUiBinder.class);
    interface PanelCentralUiBinder extends UiBinder<Widget, PanelCentral> { }
    
    @UiField VerticalPanel panelSeleccionado;
    PanelVotaciones panelVotaciones;
    private EventoSistemaVotacionJso eventoSeleccionado;
    public static PanelCentral INSTANCIA;
    PanelBusqueda panelBusqueda;
    Composite selectedPanel;
    private NamedFrame herramientaPublicacionFrame;
    PanelVotacion panelVotacion;
    PanelTest panelTest;
    
    public PanelCentral() {
        initWidget(uiBinder.createAndBindUi(this));
        History.addValueChangeHandler(this);
        BusEventos.addHandler(EventoGWTConsultaEventos.TYPE, this);
        BusEventos.addHandler(EventoGWTConsultaEvento.TYPE, this);
        INSTANCIA = this;
    }

    public void setPanel (HistoryToken historyToken) { 
    	if(historyToken == null) {
    		logger.info("setPanel - historyToken nulo");
    		return;
    	}
    	logger.info("setPanel: " + historyToken.toString());
    	if (panelSeleccionado != null) panelSeleccionado.clear();
    	switch(historyToken) {
    		case VOTACIONES:
    			if(panelVotaciones == null) {
    				panelVotaciones = new PanelVotaciones();
    			}
    			panelVotaciones.gotoPage(0, Constantes.EVENTS_RANGE);
    			selectedPanel = panelVotaciones;
            	panelSeleccionado.add(selectedPanel);
            	break;
    		case VOTAR:
    			if(panelVotacion == null) {
    				panelVotacion = new PanelVotacion();
    			}
				panelVotacion.actualizarEventoSistemaVotacion(eventoSeleccionado);
    			selectedPanel = panelVotacion;
            	panelSeleccionado.add(selectedPanel);
    			break;    			
    		case TEST:
    			if(panelTest == null) {
    				panelTest = new PanelTest();
    			}
    			selectedPanel = panelTest;
            	panelSeleccionado.add(selectedPanel);
    			break;       			
    		case BUSQUEDAS:
    			if(panelBusqueda == null) {
    				panelBusqueda = new PanelBusqueda();
    			}
    			selectedPanel = panelBusqueda;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case HERRAMIENTA_VALIDACION:
    			if (selectedPanel == null) {
    				History.newItem(HistoryToken.VOTACIONES.toString());
    			} else panelSeleccionado.add(selectedPanel);
    			cargarHerramientaValidacion();
            	break;    			
            default:
            	logger.info(" - Token sin procesar -> " + historyToken.toString());
    	}
    }
    
    public void cargarHerramientaValidacion () {
    	if(herramientaPublicacionFrame != null) {
    		RootPanel.get("uiBody").remove(herramientaPublicacionFrame);
    	} else {
    		herramientaPublicacionFrame = new NamedFrame(
    				Constantes.ID_FRAME_HERRAMIENTA_PUBLICACION);
    		herramientaPublicacionFrame.setSize("0px", "0px");
        	herramientaPublicacionFrame.setUrl(
        			ServerPaths.getUrlFrameHerramientaValidacion());
    	}
    	DialogoCargaHerramientaValidacion dialogoCarga = new DialogoCargaHerramientaValidacion();
    	dialogoCarga.show();    	
        RootPanel.get("uiBody").add(herramientaPublicacionFrame);
        DOM.setElementAttribute(herramientaPublicacionFrame.getElement(), "id", 
        		herramientaPublicacionFrame.getName());
    }
    
	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		String historyTokenValue = event.getValue();
		logger.info(" - onValueChange - historyTokenValue: " + historyTokenValue);
		SistemaVotacionQueryString svQueryString = StringUtils.getQueryString(historyTokenValue);
		if(svQueryString.getEventoId() != null) {
			if(eventoSeleccionado == null ||eventoSeleccionado.getId() != 
					svQueryString.getEventoId().intValue() ) {
			logger.info("Hay que recargar evento");
			RequestHelper.doGet(ServerPaths.getUrlEventoVotacion(svQueryString.getEventoId()), 
            		new ServerRequestEventCallback());
			}
		}
		HistoryToken historyToken = svQueryString.getHistoryToken();
		if(historyToken == null) historyToken = HistoryToken.VOTACIONES;
    	EventoGWTMensajeAplicacion evento = new EventoGWTMensajeAplicacion(
    			null, historyToken, svQueryString.getEstadoEvento());
    	BusEventos.fireEvent(evento);
		setPanel(historyToken);
	}

	@Override
	public void actualizarEventoSistemaVotacion(EventoSistemaVotacionJso evento) {
		logger.info("recepcionManfiesto - contenido:" + evento.getContenido());
		this.eventoSeleccionado = evento;
	}
	
    public void lanzarBusqueda(DatosBusquedaJso datosBusqueda) {
		if(panelBusqueda == null) {
			panelBusqueda = new PanelBusqueda();
		}
    	panelBusqueda.lanzarBusqueda(datosBusqueda);
    	History.newItem(HistoryToken.BUSQUEDAS.toString());
    }
    
    private class ServerRequestEventCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	new ErrorDialog().show ("Exception", exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("response.getText(): " + response.getText());
                ConsultaEventosSistemaVotacionJso consulta = 
                		ConsultaEventosSistemaVotacionJso.create(response.getText());
                EventosSistemaVotacionJso eventos = consulta.getEventos();
                if(eventos == null) return;
                List<EventoSistemaVotacionJso> listaEventos = eventos.getEventosList();
        	    if (listaEventos != null && (listaEventos.size() > 0)) {
        	    	eventoSeleccionado = listaEventos.iterator().next();
        	    	BusEventos.fireEvent(new EventoGWTConsultaEvento(eventoSeleccionado));
                } else History.newItem(HistoryToken.VOTACIONES.toString());
            } else {
            	logger.log(Level.SEVERE, "response.getText(): " + response.getText());
            	new ErrorDialog().show (String.valueOf(response.getStatusCode()), response.getText());
            }
        }

    }

	@Override
	public void recepcionConsultaEventos(EventoGWTConsultaEventos event) {
		// TODO Auto-generated method stub
		
	}
}