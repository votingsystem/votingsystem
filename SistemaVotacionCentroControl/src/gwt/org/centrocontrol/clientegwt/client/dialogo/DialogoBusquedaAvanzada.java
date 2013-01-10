package org.centrocontrol.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.modelo.DatosBusquedaJso;
import org.centrocontrol.clientegwt.client.modelo.Tipo;
import org.centrocontrol.clientegwt.client.panel.PanelCentral;
import org.centrocontrol.clientegwt.client.panel.PanelEncabezado;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;

public class DialogoBusquedaAvanzada {
	
    private static Logger logger = Logger.getLogger("DialogoBusquedaAvanzada");
	
    private static DialogoBusquedaAvanzadaUiBinder uiBinder = GWT.create(DialogoBusquedaAvanzadaUiBinder.class);
    
    
    interface DialogoBusquedaAvanzadaUiBinder extends UiBinder<Widget, DialogoBusquedaAvanzada> {}

    @UiField VerticalPanel textPanel;
    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField TextBox searchText;
    @UiField DateBox fechaInicioDesde;
    @UiField DateBox fechaInicioHasta;
    @UiField DateBox fechaFinDesde;
    @UiField DateBox fechaFinHasta;
    
	public DialogoBusquedaAvanzada() {
        uiBinder.createAndBindUi(this);
		SubmitListener sl = new SubmitListener();
		searchText.addKeyDownHandler(sl);
	}

    @UiHandler("cerrarButton")
    void handleCloseButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    @UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
		DatosBusquedaJso datosBusqueda = DatosBusquedaJso.create(Tipo.EVENTO_VOTACION);
		datosBusqueda.setFechaInicioDesde(fechaInicioDesde.getValue());
		datosBusqueda.setFechaInicioHasta(fechaInicioHasta.getValue());
		datosBusqueda.setFechaFinDesde(fechaFinDesde.getValue());
		datosBusqueda.setFechaFinHasta(fechaFinHasta.getValue());
		datosBusqueda.setTextQuery(searchText.getText());
		logger.info("Datos busqueda: " + datosBusqueda.toJSONString());
		PanelEncabezado.INSTANCIA.setDatosBusqueda(datosBusqueda);
		dialogBox.hide();
    }
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    }

	private class SubmitListener implements KeyDownHandler {
		
		@Override
		public void onKeyDown(KeyDownEvent event) {
			if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
				handleAceptarButton(null);
			}
		}
	}
	
}
