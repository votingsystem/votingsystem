package org.controlacceso.clientegwt.client.votaciones;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.dialogo.PopupSolicitudCopiaSeguridad;
import org.controlacceso.clientegwt.client.dialogo.SolicitanteEmail;
import org.controlacceso.clientegwt.client.modelo.EstadisticaJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.OpcionDeEventoJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.DateUtils;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.jsonp.client.TimeoutException;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;

public class PanelGraficoResultadoDeVotacion extends Composite implements SolicitanteEmail {
	
    private static Logger logger = Logger.getLogger("PanelGraficoResultadoDeVotacion");

	private static PanelGraficoResultadoDeVotacionUiBinder uiBinder = GWT
			.create(PanelGraficoResultadoDeVotacionUiBinder.class);

	interface PanelGraficoResultadoDeVotacionUiBinder extends UiBinder<Widget, PanelGraficoResultadoDeVotacion> {	}

	EventoSistemaVotacionJso evento;
	EstadisticaJso estadisticasControlAcceso;
	@UiField VerticalPanel panelContenedorGrafico;
	@UiField VerticalPanel panelGrafico;
	@UiField Label numVotosOK;
	@UiField HorizontalPanel datosEstadisticas;
	@UiField Anchor detallesEstadisticas;
	
    private PopupSolicitudCopiaSeguridad popUpSolicitudCopiaSeguridad;
    CopiaSeguridadEventListener eventListener = new CopiaSeguridadEventListener();
    PanelVotacion panelVotacion;
    PanelGraficoResultadoDeVotacion INSTANCIA;

	public PanelGraficoResultadoDeVotacion() {
		initWidget(uiBinder.createAndBindUi(this));
		INSTANCIA = this;
	}
    
	  private PieChart.PieOptions createOptions() {
		  	PieChart.PieOptions options = PieChart.createPieOptions();
		  	detallesEstadisticas.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					boolean conCopiaSeguridad = false;
					if(DateUtils.getTodayDate().after(evento.getFechaFin())) 
						conCopiaSeguridad = true;
					PopupEstadisticasVotacion popupEstadisticasVotacion = 
							new PopupEstadisticasVotacion(estadisticasControlAcceso, INSTANCIA, conCopiaSeguridad);
					popupEstadisticasVotacion.setPopupPosition(event.getClientX(), event.getClientY() - 300);
				}
			});
		    options.setWidth(400);
		    options.setHeight(200);
		    options.set3D(true);
		    return options;
	  }

	private SelectHandler createSelectHandler(final PieChart chart) {
		return new SelectHandler() {
			@Override
			public void onSelect(SelectEvent event) {
				String message = "";
				// May be multiple selections.
				JsArray<Selection> selections = chart.getSelections();
				for (int i = 0; i < selections.length(); i++) {
					// add a new line for each selection
					message += i == 0 ? "" : "\n";
					Selection selection = selections.get(i);
					if (selection.isCell()) {
						// isCell() returns true if a cell has been selected.
						// getRow() returns the row number of the selected cell.
						int row = selection.getRow();
						// getColumn() returns the column number of the selected cell.
						int column = selection.getColumn();
						message += "cell " + row + ":" + column + " selected";
					} else if (selection.isRow()) {
						// isRow() returns true if an entire row has been selected.
						// getRow() returns the row number of the selected row.
						int row = selection.getRow();
						message += "row " + row + " selected";
					} else {
						// unreachable
						message += "Pie chart selections should be either row selections or cell selections.";
						message += "  Other visualizations support column selections as well.";
					}
				}
				//Window.alert(message);
			}
		};
	}

	private void mostrarPopupSolicitudCopiaSeguridad(int clientX, int clientY) {
		if(popUpSolicitudCopiaSeguridad == null) {
			popUpSolicitudCopiaSeguridad = new PopupSolicitudCopiaSeguridad(this);
		}
		popUpSolicitudCopiaSeguridad.setPopupPosition(clientX, clientY);
		popUpSolicitudCopiaSeguridad.show();
	}	  
	
	
	  private AbstractDataTable createTable(EstadisticaJso estadistica) {
		    DataTable data = DataTable.create();
		    data.addColumn(ColumnType.STRING, Constantes.INSTANCIA.opcionGraficoLabel());
		    data.addColumn(ColumnType.NUMBER, Constantes.INSTANCIA.numVotosGraficoLabel());
		    List<OpcionDeEventoJso> opciones = estadistica.getOpcionesList();
		    /*OpcionDeEventoJso opcion1 = OpcionDeEventoJso.create("Opción 0001", 10, 10, null);
		    OpcionDeEventoJso opcion2 = OpcionDeEventoJso.create("Opción 0002", 5, 10, null);
		    opciones.add(opcion2);
		    opciones.add(opcion1);*/
		    if(opciones != null && opciones.size() > 0) {
		    	data.addRows(opciones.size());
		    	int opcionNumber = 0;
		    	for(OpcionDeEventoJso opcion : opciones) {
		    		data.setValue(opcionNumber, 0, opcion.getContenido());
				    data.setValue(opcionNumber, 1, opcion.getNumeroVotos());
				    opcionNumber++;	
		    	}
		    } 
		    return data;
	  }

		  
	public void mostraEstadisticas(EventoSistemaVotacionJso evento, 
			final EstadisticaJso estadisticas, PanelVotacion panelVotacion) {
		panelGrafico.clear();
		this.evento = evento;
		this.panelVotacion = panelVotacion;
		detallesEstadisticas.setVisible(false);
		panelContenedorGrafico.setVisible(false);
		datosEstadisticas.setVisible(false);
		if(estadisticas.getNumeroSolicitudesDeAcceso() == 0) return;
		Runnable onLoadCallback = new Runnable() {
		      public void run() {
		        PieChart pie = new PieChart(createTable(estadisticas), createOptions());
		        pie.addSelectHandler(createSelectHandler(pie));
		        panelGrafico.add(pie);
				datosEstadisticas.setVisible(true);
				panelContenedorGrafico.setVisible(true);
				detallesEstadisticas.setVisible(true);
		      }
	    };
	    numVotosOK.setText(new Integer(estadisticas.getNumeroVotosOK()).toString());
	    
	    VisualizationUtils.loadVisualizationApi(onLoadCallback, PieChart.PACKAGE);	
	    this.estadisticasControlAcceso  = estadisticas;
	    getEstadisticasControlAcceso(evento.getCentroControl().getEstadisticasEventoURL());
	}

	public void mostraEstadisticasCentroControl(final EstadisticaJso estadisticas) {
		logger.info("mostraEstadisticasCentroControl - estadisticas:" + estadisticas.toJSONString());
		//if(estadisticasControlAcceso.getNumeroVotos() !)
	}
	
    public void getEstadisticasControlAcceso (String url) {
    	JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
    	jsonp.setTimeout(4000);
    	jsonp.setCallbackParam("callback");
    	jsonp.requestObject(url, new AsyncCallback<EstadisticaJso>() {
    		public void onFailure(Throwable throwable) {
    			logger.log(Level.SEVERE, throwable.getMessage(), throwable);
    			if(throwable instanceof TimeoutException) {
    				if(evento.isActive()) {
    					panelVotacion.actualizarStatusCentroControl(
        						Constantes.INSTANCIA.estatusCentroControlErrorNoVoting());
    				} else panelVotacion.actualizarStatusCentroControl(
    						Constantes.INSTANCIA.estatusCentroControlError());
    				panelVotacion.actualizarPanelOpciones(evento.getOpcionDeEventoList(), false);
    			}
    		}

    		public void onSuccess(EstadisticaJso estadisticas) {
    			logger.info("Obtenidas estadísticas del centro de control");
    			mostraEstadisticasCentroControl(estadisticas);
    		}
	    });
    }

	class CopiaSeguridadEventListener implements EventListener {

		@Override
		public void onBrowserEvent(Event event) {
			switch(DOM.eventGetType(event)) {
				case Event.ONCLICK:
				case Event.ONMOUSEOVER:
					mostrarPopupSolicitudCopiaSeguridad(event.getClientX(), event.getClientY());
					break;
			    case Event.ONMOUSEOUT:
			    	break;
			}
		}
		
	}
	
	@Override
	public void procesarEmail(String email) {
		logger.info("--- procesarEmail");
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.SOLICITUD_COPIA_SEGURIDAD.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlSolicitudCopiaSeguridad());
		mensajeClienteFirma.setEvento(evento);
		mensajeClienteFirma.setEmailSolicitante(email);
		if(!Browser.isAndroid()) PanelVotacion.INSTANCIA.setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
	}

	
}