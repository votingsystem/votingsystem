package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.List;
import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.dialogo.ErrorDialog;
import org.controlacceso.clientegwt.client.modelo.ConsultaEventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.panel.BarraNavegacion;
import org.controlacceso.clientegwt.client.panel.PanelEncabezado;
import org.controlacceso.clientegwt.client.panel.PanelEvento;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;
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

public class PanelReclamaciones extends Composite implements BarraNavegacion.Listener {

    private static Logger logger = Logger.getLogger("PanelReclamaciones");

  
    @UiField BarraNavegacion barraNavegacion;
    @UiField VerticalPanel panelEventos;
    @UiField VerticalPanel panelBarrarProgreso;
    @UiField FlowPanel panelContenedorEventos;
    @UiField HTML emptySearchLabel;
    

    private static PanelReclamacionesUiBinder uiBinder = GWT.create(PanelReclamacionesUiBinder.class);

    interface PanelReclamacionesUiBinder extends UiBinder<VerticalPanel, PanelReclamaciones> { }

    public PanelReclamaciones() {
    	initWidget(uiBinder.createAndBindUi(this));
    	panelContenedorEventos.clear();
        panelEventos.setVisible(false);
		emptySearchLabel.setVisible(false);
    }

    private class ServerRequestCallback implements RequestCallback {

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
            } else {
            	showErrorDialog (String.valueOf(response.getStatusCode()), response.getText());
            }
        }

    }
    
    private void showErrorDialog (String text, String body) {
    	ErrorDialog errorDialog = new ErrorDialog();
    	errorDialog.show(text, body);	
    }

	public void recepcionConsultaEventos(ConsultaEventosSistemaVotacionJso consulta) {
		logger.info("recepcionConsultaEventos - ");
        barraNavegacion.addListener(this, consulta.getOffset(), Constantes.EVENTS_RANGE, 
        		 consulta.getNumeroTotalEventosReclamacionEnSistema());
	    EventosSistemaVotacionJso eventos = consulta.getEventos();
	    List<EventoSistemaVotacionJso> reclamaciones;
	    panelContenedorEventos.clear();
		if (eventos != null && (reclamaciones = eventos.getReclamacionesList())!= null &&
				eventos.getReclamacionesList().size() > 0) {
			emptySearchLabel.setVisible(false);
			for(EventoSistemaVotacionJso reclamacion: reclamaciones) {
				PanelEvento panelEvento = new PanelEvento(reclamacion);
				panelContenedorEventos.add(panelEvento);
			}
	    } else {
			emptySearchLabel.setVisible(true);
	    	barraNavegacion.setVisible(false);
	    }
		panelBarrarProgreso.setVisible(false);
		panelEventos.setVisible(true);
	}

	@Override
	public void gotoPage(int offset, int range) {
		RequestHelper.doGet(ServerPaths.getUrlEventosReclamacion(range, offset, 
				PanelEncabezado.INSTANCIA.getEstadoEvento()), 
				new ServerRequestCallback());
		panelBarrarProgreso.setVisible(true);
		panelEventos.setVisible(false);
	}

}