package org.centrocontrol.clientegwt.client.panel;

import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.HistoryToken;
import org.centrocontrol.clientegwt.client.dialogo.DialogoBusquedaAvanzada;
import org.centrocontrol.clientegwt.client.evento.BusEventos;
import org.centrocontrol.clientegwt.client.evento.EventoGWTMensajeAplicacion;
import org.centrocontrol.clientegwt.client.modelo.DatosBusquedaJso;
import org.centrocontrol.clientegwt.client.modelo.Tipo;
import org.centrocontrol.clientegwt.client.util.ServerPaths;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso.Estado;
import org.centrocontrol.clientegwt.client.panel.PanelCentral;
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

public class PanelEncabezado extends Composite implements EventoGWTMensajeAplicacion.Handler {
	
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
    @UiField HorizontalPanel subscripcionesPanel;
    @UiField Anchor subscripciones;
    @UiField ListBox listaEstados;
    @UiField HorizontalPanel seleccionEstadoPanel;
    @UiField HorizontalPanel optionsPanel;

    
    Tipo tipoBusqueda = null;
    ListboxStateChangeHandler listboxStateChangeHandler = new ListboxStateChangeHandler();
    private boolean isShowingPanelBusquedas = false;

    public static PanelEncabezado INSTANCIA;
    private DatosBusquedaJso datosBusqueda;

	public PanelEncabezado() {
		initWidget(uiBinder.createAndBindUi(this));
		BusEventos.addHandler(EventoGWTMensajeAplicacion.TYPE, this);
		String nombreServidor = com.google.gwt.user.client.Window.Location.getHostName();
		String puertoServidor = com.google.gwt.user.client.Window.Location.getPort();
		if (puertoServidor != null && !"".equals(puertoServidor.trim())) {
			nombreServidor.concat(":" + puertoServidor);
		}
		tituloAnchor.setText(Constantes.INSTANCIA.headerPanelTitle());
		subscripciones.setText(Constantes.INSTANCIA.feedsVotacionesLabel());
		subscripciones.setHref(ServerPaths.getUrlSubscripcionVotaciones());
		SearchTextBoxFocusHandler textBoxFocusHandler = new SearchTextBoxFocusHandler();
		buscarTextBox.addFocusHandler(textBoxFocusHandler);
		buscarTextBox.addBlurHandler(textBoxFocusHandler);
		buscarTextBox.addKeyDownHandler(new SubmitListener());
		AnchorElement anchorElement = (AnchorElement)
				(com.google.gwt.dom.client.Element)subscripciones.getElement();
		anchorElement.setType("application/rss+xml");
		anchorElement.setRel("alternate");
		listaEstados.addChangeHandler(listboxStateChangeHandler);
        listaEstados.addItem(Constantes.INSTANCIA.allVotingsListboxOption());
        listaEstados.getElement().getElementsByTagName("option").getItem(0).setClassName("todosLosEstados");
        listaEstados.addItem(Constantes.INSTANCIA.onlyOpenVotingsListboxOption());
        listaEstados.getElement().getElementsByTagName("option").getItem(1).setClassName("estadoAbierto");
        listaEstados.addItem(Constantes.INSTANCIA.onlyPendingVotingsListboxOption());
        listaEstados.getElement().getElementsByTagName("option").getItem(2).setClassName("estadoPendiente");
        listaEstados.addItem(Constantes.INSTANCIA.onlyClosedVotingsListboxOption());
        listaEstados.getElement().getElementsByTagName("option").getItem(3).setClassName("estadoFinalizado");
		INSTANCIA = this;
	}
	
	public void setDatosBusqueda(DatosBusquedaJso datosBusqueda) {
		this.datosBusqueda = datosBusqueda;
		buscarTextBox.setText("");
		PanelCentral.INSTANCIA.lanzarBusqueda(datosBusqueda);
	}
    
	private class SearchTextBoxFocusHandler implements FocusHandler, BlurHandler {

		@Override
		public void onFocus(FocusEvent event) {
			if (Constantes.INSTANCIA.searchLabel()
					.equals(buscarTextBox.getText().trim())) {
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
    	History.newItem(HistoryToken.VOTACIONES.toString());
    }
    
    
    @UiHandler("busquedaAvanzada")
    void onClickBusquedaAvanzada(ClickEvent e) {
		DialogoBusquedaAvanzada dialogoBusqueda = new DialogoBusquedaAvanzada();
		dialogoBusqueda.show();
    }
    
	class ListboxStateChangeHandler implements ChangeHandler {
		@Override
		public void onChange(ChangeEvent event) {
			String sufix = updateListboxStates(getEstadoEvento());
			if(isShowingPanelBusquedas) {
				if(datosBusqueda == null) return;
				datosBusqueda.setEstadoEventoEnumValue(getEstadoEvento());
				PanelCentral.INSTANCIA.lanzarBusqueda(datosBusqueda);
			} else History.newItem(HistoryToken.VOTACIONES.toString() + sufix);
		}
	}
	
	private void setListaEstados(HistoryToken sistema, 
			EventoSistemaVotacionJso.Estado estado) {
		switch(sistema) {
			case VOTACIONES:
			case BUSQUEDAS:
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
		HistoryToken historyToken = HistoryToken.VOTACIONES;
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
			default:break;
		}
		return estado;
	}
    
	private class SubmitListener implements KeyDownHandler {
		
		@Override
		public void onKeyDown(KeyDownEvent event) {
			if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
				if (!"".equals(buscarTextBox.getText())) {
					datosBusqueda = DatosBusquedaJso.create(Tipo.EVENTO_VOTACION);
					datosBusqueda.setEstadoEventoEnumValue(getEstadoEvento());
					datosBusqueda.setTextQuery(buscarTextBox.getText());
					PanelCentral.INSTANCIA.lanzarBusqueda(datosBusqueda);
				} else {
					datosBusqueda = null;
					History.newItem(HistoryToken.VOTACIONES.toString());
				}
			}
		}
	}
	
	@Override
	public void procesarMensaje(EventoGWTMensajeAplicacion evento) {
		logger.info("procesarMensaje - token: " + evento.token);
		switch(evento.token) {
			case VOTACIONES:
				isShowingPanelBusquedas = false;
				setListaEstados(evento.getToken(), evento.getEstadoEvento());
				break;
			case VOTAR:
				isShowingPanelBusquedas = false;
				seleccionEstadoPanel.setVisible(false);
				break;				
			case BUSQUEDAS:
				isShowingPanelBusquedas = true;
				break;
			default: break;
		}
	}

}