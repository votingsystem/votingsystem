package org.centrocontrol.clientegwt.client.panel;

import java.util.logging.Logger;
import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.PuntoEntrada;
import org.centrocontrol.clientegwt.client.dialogo.DialogoCargaClienteAndroid;
import org.centrocontrol.clientegwt.client.modelo.OpcionDeEventoJso;
import org.centrocontrol.clientegwt.client.util.Browser;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class PanelSeleccionOpcionDeVotacion extends Composite 
	implements DialogoCargaClienteAndroid.DialogListener{
	
    private static Logger logger = Logger.getLogger("PanelSeleccionOpcionDeVotacion");

	private static PanelSeleccionOpcionDeVotacionUiBinder uiBinder = GWT
			.create(PanelSeleccionOpcionDeVotacionUiBinder.class);

	interface PanelSeleccionOpcionDeVotacionUiBinder extends UiBinder<Widget, PanelSeleccionOpcionDeVotacion> {	}

    @UiField Label opcionImage;
    @UiField Label opcionLabel;
    @UiField Button opcionButton;


	OpcionDeEventoJso opcion;
	ContenedorOpciones contenedorOpciones;

	public PanelSeleccionOpcionDeVotacion(final OpcionDeEventoJso opcion, 
			ContenedorOpciones contenedorOpciones) {
		initWidget(uiBinder.createAndBindUi(this));
		opcionButton.setHTML("<b>" + opcion.getContenido() + "</b>");
		opcionLabel.setVisible(false);
		opcionLabel.setText(" - " + opcion.getContenido());
		this.opcion = opcion;
		this.contenedorOpciones = contenedorOpciones;
        sinkEvents(Event.ONMOUSEOVER);
	}
	
	public void onBrowserEvent(Event event){
		//eventListener.onBrowserEvent(event);
	}
    

	public void setEnabled(boolean enabled) {
		opcionLabel.setVisible(!enabled);
		opcionButton.setVisible(enabled);
	}
    
    @UiHandler("opcionButton")
    void handleOpcionButton(ClickEvent e) {
    	if(Browser.isAndroid() && !PuntoEntrada.INSTANCIA.isAndroidClientLoaded()) {
        	DialogoCargaClienteAndroid dialogoCarga = new DialogoCargaClienteAndroid();
        	dialogoCarga.show(null, this);
    	} else {
        	if(Window.confirm(Constantes.INSTANCIA.votoConfirmLabel(opcion.getContenido()))) {
        		contenedorOpciones.procesarOpcionSeleccioda(opcion);
    		}	
    	}
    }

	@Override
	public void continueOperation() {
		contenedorOpciones.procesarOpcionSeleccioda(opcion);
	}

}