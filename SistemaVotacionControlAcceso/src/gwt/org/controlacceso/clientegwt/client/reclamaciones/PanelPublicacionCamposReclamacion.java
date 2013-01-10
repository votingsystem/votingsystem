package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.modelo.CampoDeEventoJso;
import org.controlacceso.clientegwt.client.reclamaciones.PanelCampoReclamacion.Modo;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PanelPublicacionCamposReclamacion extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelPublicacionCamposReclamacion");

	private static PanelPublicacionCamposReclamacionUiBinder uiBinder = GWT
			.create(PanelPublicacionCamposReclamacionUiBinder.class);

	interface PanelPublicacionCamposReclamacionUiBinder extends UiBinder<Widget, PanelPublicacionCamposReclamacion> {	}

	@UiField VerticalPanel panelContenedor;
	@UiField Label panelLabel;
	
	List<CampoDeEventoJso> campos;
	List<PanelCampoReclamacion> listaPanelesCampo;
	PanelCampoReclamacion.Modo modo = Modo.CREAR;

	private PanelPublicacionCamposReclamacion() {
		initWidget(uiBinder.createAndBindUi(this));
		panelLabel.setVisible(false);
	}
    
	public PanelPublicacionCamposReclamacion(Modo modo) {
		this();
		this.modo = modo;
	}

	public List<CampoDeEventoJso> getCampos() {
		if(listaPanelesCampo == null || listaPanelesCampo.size() == 0) 
			return new ArrayList<CampoDeEventoJso>();
		List<CampoDeEventoJso> campos = new ArrayList<CampoDeEventoJso>();
		for(PanelCampoReclamacion panelCampo:listaPanelesCampo) {
			campos.add(panelCampo.getCampoReclamacion());
		}
		return campos;
	}
	
	public boolean isValidForm() {
		if(modo == Modo.CREAR) return true; 
		if(listaPanelesCampo != null && listaPanelesCampo.size() > 0) {
			for(PanelCampoReclamacion panelCampo:listaPanelesCampo) {
				if(!panelCampo.isValidForm()) return false;
			}
		}
		return true;
	}
	
	private void refrescarCampos(boolean camposEnabled) {
		panelContenedor.clear();
		if(campos == null || campos.size() == 0) {
			panelLabel.setVisible(false);
			return;
		} else panelLabel.setVisible(true);
		listaPanelesCampo = new ArrayList<PanelCampoReclamacion>();
		for(CampoDeEventoJso opcion:campos) {
			PanelCampoReclamacion panelOpcion = new PanelCampoReclamacion(
					opcion, modo, this);
			listaPanelesCampo.add(panelOpcion);
			panelOpcion.setEnabled(camposEnabled);
			panelContenedor.add(panelOpcion);
		}
	}

	public void setEnabled(boolean enabled) {
		refrescarCampos(enabled);
	}

	public void borrarCampo(CampoDeEventoJso campo) {
		if(campos != null) campos.remove(campo);
		refrescarCampos(true);
	}

	public void anyadirCampo(CampoDeEventoJso campoCreado) {
		if(campos == null) campos = new ArrayList<CampoDeEventoJso>();
		campos.add(campoCreado);
		refrescarCampos(true);
	}
	
	public void anyadirCampos(List<CampoDeEventoJso> camposAnyadidos) {
		campos = new ArrayList<CampoDeEventoJso>();
		if(camposAnyadidos != null)	campos.addAll(camposAnyadidos);
		refrescarCampos(true);
	}
	
}