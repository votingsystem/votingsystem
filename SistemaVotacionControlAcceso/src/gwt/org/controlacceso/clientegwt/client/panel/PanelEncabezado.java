package org.controlacceso.clientegwt.client.panel;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.dialogo.DialogoBusquedaAvanzada;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeAplicacion;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.DatosBusquedaJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso.Estado;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.Tipo;
import org.controlacceso.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class PanelEncabezado extends Composite implements EventoGWTMensajeAplicacion.Handler,
		EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("PanelEncabezado");

	private static PanelEncabezadoUiBinder uiBinder = GWT
			.create(PanelEncabezadoUiBinder.class);

	interface PanelEncabezadoUiBinder extends UiBinder<Widget, PanelEncabezado> {	}

    interface Style extends CssResource {
        String feedImage();
        String listaEstados();
        String listaEstadoAbierto();
        String listaEstadoFinalizado();
        String listaEstadoPendiente();
    }
    
    @UiField Style style;
    @UiField Anchor tituloAnchor;
    @UiField TextBox buscarTextBox;
    @UiField Anchor busquedaAvanzada; 
    @UiField PushButton botonPublicarManifiesto;
    @UiField PushButton botonPublicarVotacion;
    @UiField PushButton botonPublicarReclamacion; 
    @UiField PushButton representativesPageButton;
    @UiField HorizontalPanel subscripcionesPanel;
    @UiField HorizontalPanel seleccionEstadoPanel;
    @UiField Anchor subscripciones;
    @UiField PanelSubsistemas panelSubsistemas;
    @UiField ListBox listaEstados;

    DialogoOperacionEnProgreso dialogoProgreso;
    Tipo tipoBusqueda = null;
    ListboxStateChangeHandler listboxStateChangeHandler = new ListboxStateChangeHandler();
    private DatosBusquedaJso datosBusqueda;
    private Boolean isShowingPanelBusquedas = false;

    public static PanelEncabezado INSTANCIA;
    
	public PanelEncabezado() {
		initWidget(uiBinder.createAndBindUi(this));
		BusEventos.addHandler(EventoGWTMensajeAplicacion.TYPE, this);
		String nombreServidor = com.google.gwt.user.client.Window.Location.getHostName();
		String puertoServidor = com.google.gwt.user.client.Window.Location.getPort();
		if (puertoServidor != null && !"".equals(puertoServidor.trim())) {
			nombreServidor.concat(":" + puertoServidor);
		}
		SearchTextBoxFocusHandler textBoxFocusHandler = new SearchTextBoxFocusHandler();
		buscarTextBox.addFocusHandler(textBoxFocusHandler);
		buscarTextBox.addBlurHandler(textBoxFocusHandler);
		SubmitListener sl = new SubmitListener();
		buscarTextBox.addKeyDownHandler(sl);
		AnchorElement anchorElement = (AnchorElement)
				(com.google.gwt.dom.client.Element)subscripciones.getElement();
		anchorElement.setType("application/rss+xml");
		anchorElement.setRel("alternate");
		listaEstados.addChangeHandler(listboxStateChangeHandler);
		INSTANCIA = this;
	}
    
	private class SearchTextBoxFocusHandler implements FocusHandler, BlurHandler {

		@Override
		public void onFocus(FocusEvent event) {
			if (Constantes.INSTANCIA.searchLabel().
					equals(buscarTextBox.getText().trim())) {
				buscarTextBox.setText("");
			}
		}

		@Override
		public void onBlur(BlurEvent event) {
			if ("".equals(buscarTextBox.getText().trim())) {
				buscarTextBox.setText(Constantes.INSTANCIA.searchLabel());
			}
		}
	}
	
	
    @UiHandler("tituloAnchor")
    void onClickTextAnchor(ClickEvent e) {
    	buscarTextBox.setText(Constantes.INSTANCIA.searchLabel());
    	switch(PanelCentral.INSTANCIA.getSistemaSeleccionado()) {
	    	case MANIFIESTOS:
	    		History.newItem(HistoryToken.MANIFIESTOS.toString());
	    		break;
	    	case RECLAMACIONES:
	    		History.newItem(HistoryToken.RECLAMACIONES.toString());
	    		break;
	    	case VOTACIONES:
	    		History.newItem(HistoryToken.VOTACIONES.toString());
	    		break;
	    	case REPRESENTATIVES_PAGE:
	    		History.newItem(HistoryToken.REPRESENTATIVES_PAGE.toString());
	    		break;
	    	default:break;
    	}
    }

    
    @UiHandler("busquedaAvanzada")
    void onClickBusquedaAvanzada(ClickEvent e) {
		DialogoBusquedaAvanzada dialogoBusqueda = new DialogoBusquedaAvanzada();
		dialogoBusqueda.show();
    }
    
    @UiHandler("botonPublicarVotacion")
    void onClickBotonPublicarVotacion(ClickEvent e) {
    	History.newItem(HistoryToken.CREAR_VOTACION.toString());
    }
    
    @UiHandler("botonPublicarManifiesto")
    void onClickBotonPublicarManifiesto(ClickEvent e) {
    	History.newItem(HistoryToken.CREAR_MANIFIESTO.toString());
    }
    
    @UiHandler("botonPublicarReclamacion")
    void onClickBotonPublicarReclamacion(ClickEvent e) {
    	History.newItem(HistoryToken.CREAR_RECLAMACION.toString());
    }
    
	
    @UiHandler("representativesPageButton")
    void onClickRepresentativesButton(ClickEvent e) {
    	History.newItem(HistoryToken.REPRESENTATIVES_PAGE.toString());
    }
    
	private class SubmitListener implements KeyDownHandler {
		
		@Override
		public void onKeyDown(KeyDownEvent event) {
			if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
				if (!"".equals(buscarTextBox.getText())) {
					datosBusqueda = DatosBusquedaJso.create(tipoBusqueda);
					datosBusqueda.setEstadoEventoEnumValue(getEstadoEvento());
					datosBusqueda.setTextQuery(buscarTextBox.getText());
					PanelCentral.INSTANCIA.lanzarBusqueda(datosBusqueda);
				} else {
					datosBusqueda = null;
					History.newItem(PanelCentral.INSTANCIA.getSistemaSeleccionado().toString());
				}
			}
		}
	}

	@Override
	public void procesarMensaje(EventoGWTMensajeAplicacion evento) {
		logger.info("procesarMensaje - token: " + evento.token);
		isShowingPanelBusquedas = null;
		switch(evento.token) {
			case MANIFIESTOS:
				subscripcionesPanel.setVisible(true);
				actualizarSistema(HistoryToken.MANIFIESTOS);
				setListaEstados(evento.getToken(), evento.getEstadoEvento());
				break;
			case RECLAMACIONES:
				subscripcionesPanel.setVisible(true);
				actualizarSistema(HistoryToken.RECLAMACIONES);
				setListaEstados(evento.getToken(), evento.getEstadoEvento());
				break;
			case VOTACIONES:
				subscripcionesPanel.setVisible(true);
				actualizarSistema(HistoryToken.VOTACIONES);	
				setListaEstados(evento.getToken(), evento.getEstadoEvento());
				break;
			case CREAR_MANIFIESTO:
				subscripcionesPanel.setVisible(false);
				seleccionEstadoPanel.setVisible(false);
				actualizarSistema(HistoryToken.MANIFIESTOS);
				botonPublicarManifiesto.setVisible(false);
				break;
			case FIRMAR_MANIFIESTO:
				subscripcionesPanel.setVisible(false);
				seleccionEstadoPanel.setVisible(false);
				actualizarSistema(HistoryToken.MANIFIESTOS);
				botonPublicarManifiesto.setVisible(false);
				break;				
			case CREAR_RECLAMACION:
				subscripcionesPanel.setVisible(false);
				seleccionEstadoPanel.setVisible(false);
				actualizarSistema(HistoryToken.RECLAMACIONES);
				botonPublicarReclamacion.setVisible(false);
				break;
			case FIRMAR_RECLAMACION:
				subscripcionesPanel.setVisible(false);
				seleccionEstadoPanel.setVisible(false);
				actualizarSistema(HistoryToken.RECLAMACIONES);
				botonPublicarReclamacion.setVisible(false);
				break;
			case CREAR_VOTACION:
				subscripcionesPanel.setVisible(false);
				seleccionEstadoPanel.setVisible(false);
				actualizarSistema(HistoryToken.VOTACIONES);
				representativesPageButton.setVisible(false);
				botonPublicarVotacion.setVisible(false);
				break;
			case VOTAR:
				subscripcionesPanel.setVisible(false);
				seleccionEstadoPanel.setVisible(false);
				actualizarSistema(HistoryToken.VOTACIONES);
				representativesPageButton.setVisible(false);
				botonPublicarVotacion.setVisible(false);
				break;	
			case BUSQUEDAS:
				isShowingPanelBusquedas = true;
				break;
			case REPRESENTATIVE_CONFIG:
			case NEW_REPRESENTATIVE:
			case EDIT_REPRESENTATIVE:
			case REPRESENTATIVE_DETAILS:
			case REPRESENTATIVES_PAGE:
				subscripcionesPanel.setVisible(false);
				seleccionEstadoPanel.setVisible(false);
				actualizarSistema(HistoryToken.REPRESENTATIVES_PAGE);
				break;				
			default:
				logger.info("procesarMensaje - token sin procesar: " + evento.token);
		}
		if(isShowingPanelBusquedas == null) isShowingPanelBusquedas = false;
	}
	
	private void setListaEstados(HistoryToken sistema, 
			EventoSistemaVotacionJso.Estado estado) {
		switch(sistema) {
			case MANIFIESTOS:
	    		listaEstados.clear();
	    		listaEstados.addItem(Constantes.INSTANCIA.allManifestListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(0).setClassName("todosLosEstados");
	            listaEstados.addItem(Constantes.INSTANCIA.onlyOpenManifestListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(1).setClassName("estadoAbierto");
	            listaEstados.addItem(Constantes.INSTANCIA.onlyClosedManifestListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(2).setClassName("estadoFinalizado");
				seleccionEstadoPanel.setVisible(true);
				break;
			case RECLAMACIONES:
	    		listaEstados.clear();
	    		listaEstados.addItem(Constantes.INSTANCIA.allClaimsListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(0).setClassName("todosLosEstados");
	            listaEstados.addItem(Constantes.INSTANCIA.onlyOpenClaimsListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(1).setClassName("estadoAbierto");
	            listaEstados.addItem(Constantes.INSTANCIA.onlyClosedClaimsListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(2).setClassName("estadoFinalizado");
				seleccionEstadoPanel.setVisible(true);
				break;
			case VOTACIONES:
	    		listaEstados.clear();
	            listaEstados.addItem(Constantes.INSTANCIA.allVotingsListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(0).setClassName("todosLosEstados");
	            listaEstados.addItem(Constantes.INSTANCIA.onlyOpenVotingsListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(1).setClassName("estadoAbierto");
	            listaEstados.addItem(Constantes.INSTANCIA.onlyPendingVotingsListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(2).setClassName("estadoPendiente");
	            listaEstados.addItem(Constantes.INSTANCIA.onlyClosedVotingsListboxOption());
	            listaEstados.getElement().getElementsByTagName("option").getItem(3).setClassName("estadoFinalizado");
				seleccionEstadoPanel.setVisible(true);
				break;
			default: break;
		}
		if(estado != null) {
			switch(estado) {
				case ACTIVO:
					listaEstados.setSelectedIndex(1);
					break;
				case CANCELADO:
				case FINALIZADO:
					if(sistema == HistoryToken.VOTACIONES) {
						listaEstados.setSelectedIndex(3);
					} else listaEstados.setSelectedIndex(2);
					break;
				case PENDIENTE_COMIENZO:
					listaEstados.setSelectedIndex(2);
					break;
			}
		} else listaEstados.setSelectedIndex(0);
		updateListboxStates(estado);
	}
	
  	private void setWidgetsStateFirmando(boolean publicando) {
  		if(publicando) {
  			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
  			dialogoProgreso.show();
  		} else {
  			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
  			dialogoProgreso.hide();
  		}
  	}
	
	private void actualizarSistema(HistoryToken token) {
		switch(token) {
			case MANIFIESTOS:
				tipoBusqueda = Tipo.EVENTO_FIRMA;
				tituloAnchor.setText(Constantes.INSTANCIA.sistemaFirmasLabel());
				subscripciones.setText(Constantes.INSTANCIA.feedsManifiestosLabel());
				botonPublicarManifiesto.setVisible(true);
				botonPublicarVotacion.setVisible(false);
				botonPublicarReclamacion.setVisible(false);
				representativesPageButton.setVisible(false);
				subscripciones.setHref(ServerPaths.getUrlSubscripcionManifiestos());
				break;
			case RECLAMACIONES:
				tipoBusqueda = Tipo.EVENTO_RECLAMACION;
				tituloAnchor.setText(Constantes.INSTANCIA.sistemaReclamacionesLabel());
				subscripciones.setText(Constantes.INSTANCIA.feedsReclamacionesLabel());				
				botonPublicarReclamacion.setVisible(true);
				botonPublicarVotacion.setVisible(false);
				botonPublicarManifiesto.setVisible(false);
				representativesPageButton.setVisible(false);
				subscripciones.setHref(ServerPaths.getUrlSubscripcionReclamaciones());
				break;
			case VOTACIONES:
				tipoBusqueda = Tipo.EVENTO_VOTACION;
				tituloAnchor.setText(Constantes.INSTANCIA.sistemaVotacionLabel());
				subscripciones.setText(Constantes.INSTANCIA.feedsVotacionesLabel());				
				botonPublicarVotacion.setVisible(true);
				botonPublicarReclamacion.setVisible(false);
				botonPublicarManifiesto.setVisible(false);	
				representativesPageButton.setVisible(true);
				subscripciones.setHref(ServerPaths.getUrlSubscripcionVotaciones());
				break;
			case REPRESENTATIVES_PAGE:
				tipoBusqueda = Tipo.REPRESENTATIVES_PAGE;
				tituloAnchor.setText(Constantes.INSTANCIA.representativesLabel());			
				botonPublicarVotacion.setVisible(false);
				botonPublicarReclamacion.setVisible(false);
				botonPublicarManifiesto.setVisible(false);
				representativesPageButton.setVisible(false);
				break;				
			default:break;
		}
	}

	class ListboxStateChangeHandler implements ChangeHandler {
		@Override
		public void onChange(ChangeEvent event) {
			String sufix = updateListboxStates(getEstadoEvento());
			if(isShowingPanelBusquedas) {
				if(datosBusqueda == null) return;
				datosBusqueda.setEstadoEventoEnumValue(getEstadoEvento());
				PanelCentral.INSTANCIA.lanzarBusqueda(datosBusqueda);
			} else History.newItem(PanelCentral.INSTANCIA.getSistemaSeleccionado().toString() 
					+ sufix);
		}
	}
	
	private String updateListboxStates(EventoSistemaVotacionJso.Estado estado){
		String sufix = "";
		listaEstados.removeStyleName(style.listaEstadoPendiente());
		listaEstados.removeStyleName(style.listaEstadoAbierto());
		listaEstados.removeStyleName(style.listaEstadoFinalizado());
		listaEstados.removeStyleName(style.listaEstados());
		if(estado != null) {
			sufix = "&estadoEvento=" + estado.toString();
			switch(estado) {
				case PENDIENTE_COMIENZO:
		            listaEstados.getElement().setClassName("estadoPendiente");
					listaEstados.setStyleName(style.listaEstadoPendiente(), true);
					break;
				case ACTIVO:
					listaEstados.getElement().setClassName("estadoAbierto");
					listaEstados.setStyleName(style.listaEstadoAbierto(), true);
					break;		
				case FINALIZADO:
					listaEstados.getElement().setClassName("estadoFinalizado");
					listaEstados.setStyleName(style.listaEstadoFinalizado(), true);
					break;	
				default:break;
			}
			
		} else listaEstados.setStyleName(style.listaEstados(), true);
		return sufix;
	}
	
	public EventoSistemaVotacionJso.Estado getEstadoEvento() {
		EventoSistemaVotacionJso.Estado estado = null;
		logger.info("getSistemaSeleccionado: " + PanelCentral.INSTANCIA.getSistemaSeleccionado());
		HistoryToken historyToken = PanelCentral.INSTANCIA.getSistemaSeleccionado();
		int selectedIndex = listaEstados.getSelectedIndex();
		switch(historyToken) {
			case VOTACIONES:
				switch(selectedIndex) {
					case 1:
						estado = Estado.ACTIVO;
						break;
					case 2:
						estado = Estado.PENDIENTE_COMIENZO;
						break;
					case 3:
						estado = Estado.FINALIZADO;
						break;
				}
				break;
			case RECLAMACIONES:
				switch(selectedIndex) {
					case 1:
						estado = Estado.ACTIVO;
						break;
					case 2:
						estado = Estado.FINALIZADO;
						break;
				}
				break;
			case MANIFIESTOS:
				switch(selectedIndex) {
					case 1:
						estado = Estado.ACTIVO;
						break;
					case 2:
						estado = Estado.FINALIZADO;
						break;
				}
				break;
			default:break;
		}
		return estado;
	}
	
	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		switch(mensaje.getOperacionEnumValue()) {
			case SOLICITUD_COPIA_SEGURIDAD:
				setWidgetsStateFirmando(false);
			default:
				break;
		}
	}


	public void setDatosBusqueda(DatosBusquedaJso datosBusqueda) {
		this.datosBusqueda = datosBusqueda;
		buscarTextBox.setText("");
		PanelCentral.INSTANCIA.lanzarBusqueda(datosBusqueda);
	}

}