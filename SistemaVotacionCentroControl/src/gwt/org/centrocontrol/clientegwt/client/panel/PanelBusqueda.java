package org.centrocontrol.clientegwt.client.panel;

import java.util.List;
import java.util.logging.Logger;
import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.dialogo.ErrorDialog;
import org.centrocontrol.clientegwt.client.modelo.ConsultaEventosSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.modelo.DatosBusquedaJso;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.modelo.EventosSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.util.RequestHelper;
import org.centrocontrol.clientegwt.client.util.ServerPaths;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PanelBusqueda extends Composite implements BarraNavegacion.Listener {

    private static Logger logger = Logger.getLogger("PanelBusqueda");

        
    @UiField BarraNavegacion barraNavegacion;
    @UiField VerticalPanel panelEventos;
    @UiField VerticalPanel panelBarrarProgreso;
    @UiField FlowPanel panelContenedorEventos;
    @UiField HTML tituloLabel;
    @UiField HTML emptySearchLabel;
    
    DatosBusquedaJso consultaBusqueda;
    
    private int offset = 0;

    private static PanelBusquedaUiBinder uiBinder = GWT.create(PanelBusquedaUiBinder.class);

    interface PanelBusquedaUiBinder extends UiBinder<VerticalPanel, PanelBusqueda> { }

    public PanelBusqueda() {
    	initWidget(uiBinder.createAndBindUi(this));
        panelEventos.setVisible(false);
		panelBarrarProgreso.setVisible(false);
		tituloLabel.setHTML(getPanelTitle(null));
    }
    
 
 class ServerRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	showErrorDialog ("Exception", exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
                ConsultaEventosSistemaVotacionJso consulta = 
                		ConsultaEventosSistemaVotacionJso.create(response.getText());
                recepcionConsultaEventos(consulta);
        		panelBarrarProgreso.setVisible(false);
        		tituloLabel.setVisible(true);
                panelEventos.setVisible(true);
            } else {
            	showErrorDialog (String.valueOf(response.getStatusCode()), response.getText());
            }
        }

    }
    
	public void recepcionConsultaEventos(ConsultaEventosSistemaVotacionJso consulta) {
		logger.info("recepcionConsultaEventos - ");
        barraNavegacion.addListener(this, consulta.getOffset(), Constantes.EVENTS_RANGE, 
        		 consulta.getNumeroTotalEventosVotacionEnSistema());
	    EventosSistemaVotacionJso eventos = consulta.getEventos();
	    List<EventoSistemaVotacionJso> votaciones;
	    panelContenedorEventos.clear();
		if (eventos != null && (votaciones = eventos.getVotacionesList())!= null &&
				eventos.getVotacionesList().size() > 0) {
			emptySearchLabel.setVisible(false);
			for(EventoSistemaVotacionJso votacion: votaciones) {
				PanelEvento panelEvento = new PanelEvento(votacion);
				panelContenedorEventos.add(panelEvento);
			}
	    } else {
	    	barraNavegacion.setVisible(false);
	    	tituloLabel.setHTML(getPanelTitle(null));
	    	emptySearchLabel.setVisible(true);
	    }
		tituloLabel.setHTML(getPanelTitle(consultaBusqueda));
		panelBarrarProgreso.setVisible(false);
		panelEventos.setVisible(true);
	}
 
 
    private void showErrorDialog (String text, String body) {
    	ErrorDialog errorDialog = new ErrorDialog();
    	errorDialog.show(text, body);	
    }

	public void lanzarBusqueda(DatosBusquedaJso datosBusqueda) {
		panelBarrarProgreso.setVisible(true);
    	emptySearchLabel.setVisible(false);
		tituloLabel.setVisible(false);
        panelEventos.setVisible(false);
        panelContenedorEventos.clear();
		this.consultaBusqueda = datosBusqueda;
		RequestHelper.doPost(ServerPaths.getUrlBusquedas(0, Constantes.EVENTS_RANGE), 
				datosBusqueda.toJSONString() , new ServerRequestCallback());
	}

	private String getPanelTitle(DatosBusquedaJso datosBusqueda) {
		String result = "<html>";
		if(datosBusqueda == null) return Constantes.INSTANCIA.searchPanelTitle();
		if(datosBusqueda.getTextQuery() != null && !"".equals(datosBusqueda.getTextQuery())) {
			result = result + Constantes.INSTANCIA.searchStringLabel() + " '" + datosBusqueda.getTextQuery() + "'";
		}
		if(datosBusqueda.getFechaInicioDesdeStr() != null && datosBusqueda.getFechaInicioHasta() != null) {
			result = result + "<br/>" + Constantes.INSTANCIA.beginAfterLabel() + " '" + datosBusqueda.getFechaInicioDesdeStr() + "'" + 
					" " + Constantes.INSTANCIA.beginBeforeLabel() + " '" + datosBusqueda.getFechaInicioHastaStr() + "'";
		} else if(datosBusqueda.getFechaInicioDesdeStr() != null) {
			result = result + "<br/>" + Constantes.INSTANCIA.beginAfterLabel() + " '" + datosBusqueda.getFechaInicioDesdeStr() + "'";
		} else if(datosBusqueda.getFechaInicioHasta() != null) {
			result = result + "<br/>" + Constantes.INSTANCIA.beginBeforeLabel1() + " '" + datosBusqueda.getFechaInicioHastaStr() + "'";
		}
		if(datosBusqueda.getFechaFinDesdeStr() != null && datosBusqueda.getFechaFinHasta() != null) {
			result = result + "<br/>" + Constantes.INSTANCIA.endAfterLabel() + " '" + datosBusqueda.getFechaFinDesdeStr() + "'" + 
					" " + Constantes.INSTANCIA.endBeforeLabel() + " '" + datosBusqueda.getFechaFinHastaStr() + "'";
		} else if(datosBusqueda.getFechaFinDesdeStr() != null) {
			result = result + "<br/>" + Constantes.INSTANCIA.endAfterLabel() + " '" + datosBusqueda.getFechaFinDesdeStr() + "'";
		} else if(datosBusqueda.getFechaFinHasta() != null) {
			result = result + "<br/>" + Constantes.INSTANCIA.endBeforeLabel1() + " '" + datosBusqueda.getFechaFinHastaStr() + "'";
		}
		return result + "</html>";
	}


	@Override
	public void gotoPage(int offset, int range) {
		logger.info("--- gotoPage ---");
		this.offset = offset;
		RequestHelper.doPost(ServerPaths.getUrlBusquedas(offset, range), 
				consultaBusqueda.toJSONString() , new ServerRequestCallback());
	}
	
}