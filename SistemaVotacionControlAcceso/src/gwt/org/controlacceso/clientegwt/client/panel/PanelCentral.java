package org.controlacceso.clientegwt.client.panel;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.dialogo.DialogoCargaHerramientaValidacion;
import org.controlacceso.clientegwt.client.dialogo.ErrorDialog;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTConsultaEvento;
import org.controlacceso.clientegwt.client.evento.EventoGWTConsultaEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeAplicacion;
import org.controlacceso.clientegwt.client.firmas.PanelFirmaManifiesto;
import org.controlacceso.clientegwt.client.firmas.PanelManifiestos;
import org.controlacceso.clientegwt.client.firmas.PanelPublicacionManifiesto;
import org.controlacceso.clientegwt.client.modelo.ConsultaEventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.DatosBusquedaJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.SistemaVotacionQueryString;
import org.controlacceso.clientegwt.client.reclamaciones.PanelFirmaReclamacion;
import org.controlacceso.clientegwt.client.reclamaciones.PanelPublicacionReclamacion;
import org.controlacceso.clientegwt.client.reclamaciones.PanelReclamaciones;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.util.StringUtils;
import org.controlacceso.clientegwt.client.votaciones.PanelPublicacionVotacion;
import org.controlacceso.clientegwt.client.votaciones.PanelVotacion;
import org.controlacceso.clientegwt.client.votaciones.PanelVotaciones;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.NamedFrame;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PanelCentral extends Composite implements ValueChangeHandler<String>, 
		EventoGWTConsultaEventos.Handler, EventoGWTConsultaEvento.Handler {

    private static Logger logger = Logger.getLogger("PanelCentral");
    
    private static PanelCentralUiBinder uiBinder = GWT.create(PanelCentralUiBinder.class);
    interface PanelCentralUiBinder extends UiBinder<Widget, PanelCentral> { }
    
    @UiField VerticalPanel panelSeleccionado;
    PanelVotaciones panelVotaciones;
    PanelManifiestos panelManifiestos;
    PanelReclamaciones panelReclamaciones;
    PanelVotacion panelVotacion;
    PanelFirmaManifiesto panelFirmaManifiesto;
    PanelFirmaReclamacion panelFirmaReclamacion;
    PanelTest panelTest;

    PanelBusqueda panelBusqueda;
    Composite selectedPanel;
    public static PanelCentral INSTANCIA;
    ConsultaEventosSistemaVotacionJso consultaEnSesion;
    private EventoSistemaVotacionJso eventoSeleccionado;
    HistoryToken sistemaSeleccionado;
    
    private NamedFrame herramientaPublicacionFrame;
    
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
    			sistemaSeleccionado = HistoryToken.VOTACIONES;
    			if(panelVotaciones == null) {
    				panelVotaciones = new PanelVotaciones();
    			}
    			panelVotaciones.gotoPage(0, Constantes.EVENTS_RANGE);
    			selectedPanel = panelVotaciones;
            	panelSeleccionado.add(selectedPanel);
            	break;
    		case MANIFIESTOS:
    			sistemaSeleccionado = HistoryToken.MANIFIESTOS;
    			if(panelManifiestos == null) {
    				panelManifiestos = new PanelManifiestos();
    			} 
    			panelManifiestos.gotoPage(0, Constantes.EVENTS_RANGE);
            	selectedPanel = panelManifiestos;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case RECLAMACIONES:
    			sistemaSeleccionado = HistoryToken.RECLAMACIONES;
    			if(panelReclamaciones == null) {
    				panelReclamaciones = new PanelReclamaciones();
    			} 
    			panelReclamaciones.gotoPage(0, Constantes.EVENTS_RANGE);
    			selectedPanel = panelReclamaciones;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case CREAR_MANIFIESTO:
            	sistemaSeleccionado = HistoryToken.MANIFIESTOS;
    			PanelPublicacionManifiesto panelPublicacionManifiesto= new PanelPublicacionManifiesto();
    			selectedPanel = panelPublicacionManifiesto;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case CREAR_RECLAMACION:
            	sistemaSeleccionado = HistoryToken.RECLAMACIONES;
    			PanelPublicacionReclamacion panelPublicacionReclamacion = 
    					new PanelPublicacionReclamacion();
    			selectedPanel = panelPublicacionReclamacion;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case CREAR_VOTACION:
            	sistemaSeleccionado = HistoryToken.VOTACIONES;
    		    PanelPublicacionVotacion panelPublicacionVotacion = new PanelPublicacionVotacion();
    			selectedPanel = panelPublicacionVotacion;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case FIRMAR_MANIFIESTO:
            	sistemaSeleccionado = HistoryToken.MANIFIESTOS;
    			if(panelFirmaManifiesto == null) {
    				panelFirmaManifiesto = new PanelFirmaManifiesto();
    			}
    			panelFirmaManifiesto.actualizarEventoSistemaVotacion(eventoSeleccionado);
    			selectedPanel = panelFirmaManifiesto;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case FIRMAR_RECLAMACION:
            	sistemaSeleccionado = HistoryToken.RECLAMACIONES;
    			if(panelFirmaReclamacion == null) {
    				panelFirmaReclamacion = new PanelFirmaReclamacion();
    			}
    			panelFirmaReclamacion.actualizarEventoSistemaVotacion(eventoSeleccionado);
    			selectedPanel = panelFirmaReclamacion;
            	panelSeleccionado.add(selectedPanel);
    			break;
    		case VOTAR:
            	sistemaSeleccionado = HistoryToken.VOTACIONES;
    			if(panelVotacion == null) {
    				panelVotacion = new PanelVotacion();
    			}
				panelVotacion.actualizarEventoSistemaVotacion(eventoSeleccionado);
    			selectedPanel = panelVotacion;
            	panelSeleccionado.add(selectedPanel);
    			break;    
    		case TEST:
    			sistemaSeleccionado = HistoryToken.VOTACIONES;
    			if(panelTest == null) {
    				panelTest = new PanelTest();
    			}
    			selectedPanel = panelTest;
            	panelSeleccionado.add(selectedPanel);
    			break;        			
    		case BUSQUEDAS:
    	    	if(sistemaSeleccionado == null) {
    	    		History.newItem(HistoryToken.VOTACIONES.toString());
    	    		logger.info("Deberían salir las votaciones");
    	    		return;
    	    	} 
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
    	actualizarSusbsistema(sistemaSeleccionado);
    }
    

    private void actualizarSusbsistema (HistoryToken seleccion) {
    	if(PanelSubsistemas.INSTANCIA == null) return;
    	switch (seleccion) {
    		case MANIFIESTOS:
    		case RECLAMACIONES:
    		case VOTACIONES:
    			PanelSubsistemas.INSTANCIA.actualizarSistema(seleccion);
    			break;
    		default: break;
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
    
    public void lanzarBusqueda(DatosBusquedaJso datosBusqueda) {
    	if(sistemaSeleccionado == null) {
    		History.newItem(HistoryToken.VOTACIONES.toString());
    		return;
    	} 
    	if(panelBusqueda == null) {
			panelBusqueda = new PanelBusqueda();
		}
    	panelBusqueda.lanzarBusqueda(datosBusqueda);
    	History.newItem(HistoryToken.BUSQUEDAS.toString());
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
			RequestHelper.doGet(ServerPaths.getUrlEvento(svQueryString.getEventoId()), 
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
	
	public HistoryToken getSistemaSeleccionado () {
		return sistemaSeleccionado;
	}

	@Override
	public void recepcionConsultaEventos(EventoGWTConsultaEventos event) {
		consultaEnSesion = event.consulta;
	}
	
	
	public ConsultaEventosSistemaVotacionJso getConsultaEnSesion() {
		return consultaEnSesion;
	}

	@Override
	public void actualizarEventoSistemaVotacion(EventoSistemaVotacionJso evento) {
		logger.info("recepcionManfiesto - contenido:" + evento.getContenido());
		this.eventoSeleccionado = evento;
	}

	public EventoSistemaVotacionJso getEventoSeleccionado() {
		return eventoSeleccionado;
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
                } else {
                	if(sistemaSeleccionado != null) 
                		History.newItem(sistemaSeleccionado.toString());
                	else History.newItem(HistoryToken.VOTACIONES.toString());
                }
            } else {
            	logger.log(Level.SEVERE, "response.getText(): " + response.getText());
            	new ErrorDialog().show (String.valueOf(response.getStatusCode()), response.getText());
            	History.newItem(sistemaSeleccionado.toString());
            }
        }

    }
}