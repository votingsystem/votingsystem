package org.controlacceso.clientegwt.client.votaciones;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.modelo.OpcionDeEventoJso;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PanelPublicacionOpcionesVotacion extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelPublicacionOpcionesVotacion");

	private static PanelPublicacionOpcionesVotacionUiBinder uiBinder = GWT
			.create(PanelPublicacionOpcionesVotacionUiBinder.class);

	interface PanelPublicacionOpcionesVotacionUiBinder extends UiBinder<Widget, PanelPublicacionOpcionesVotacion> {	}

	@UiField VerticalPanel panelContenedor;
	@UiField Label panelLabel;
	
	List<OpcionDeEventoJso> opciones;

	public PanelPublicacionOpcionesVotacion() {
		initWidget(uiBinder.createAndBindUi(this));
		panelLabel.setVisible(false);
	}
    
	public List<OpcionDeEventoJso> getOpciones() {
		if(opciones == null || opciones.size() == 0) return null;
		return opciones;
	}
    
	public void borrarOpcion(OpcionDeEventoJso opcion) {
		if(opciones != null) opciones.remove(opcion);
		refrescarOpciones(true);
	}
	
	public void borrarOpciones() {
		opciones = null;
		refrescarOpciones(true);
	}

	public void anyadirOpcion(OpcionDeEventoJso opcion) {
		if(opciones == null) opciones = new ArrayList<OpcionDeEventoJso>();
		opciones.add(opcion);
		refrescarOpciones(true);
	}
	
	private void refrescarOpciones(boolean opcionesEnabled) {
		panelContenedor.clear();
		if(opciones == null || opciones.size() == 0) {
			panelLabel.setVisible(false);
			return;
		} else panelLabel.setVisible(true);
		for(OpcionDeEventoJso opcion:opciones) {
			PanelOpcionDeVotacion panelOpcion = new PanelOpcionDeVotacion(opcion, this);
			panelOpcion.setEnabled(opcionesEnabled);
			panelContenedor.add(panelOpcion);
		}
	}

	public void setEnabled(boolean enabled) {
		refrescarOpciones(enabled);
	}
}