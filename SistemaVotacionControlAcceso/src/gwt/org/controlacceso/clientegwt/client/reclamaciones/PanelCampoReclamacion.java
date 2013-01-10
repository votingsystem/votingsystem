package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.modelo.CampoDeEventoJso;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class PanelCampoReclamacion extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelCampoReclamacion");

	private static PanelCampoReclamacionUiBinder uiBinder = GWT
			.create(PanelCampoReclamacionUiBinder.class);

	interface PanelCampoReclamacionUiBinder extends UiBinder<Widget, PanelCampoReclamacion> {	}

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String valorContenidoTextBox();
    }
	
    @UiField Image borrarImage;
    @UiField EditorStyle style;
    @UiField Label contenidoLabel;
    @UiField TextBox valorContenidoTextBox;
    Modo modo = Modo.CREAR;
	CampoDeEventoJso campo;
	PanelPublicacionCamposReclamacion panelCampos;

	private boolean enabled = true;
	
	public enum Modo {CREAR, EDITAR}

	public PanelCampoReclamacion(final CampoDeEventoJso campo, Modo modo,
			final PanelPublicacionCamposReclamacion panelCampos) {
		initWidget(uiBinder.createAndBindUi(this));
		this.modo = modo;
		switch(modo) {
			case CREAR:
				valorContenidoTextBox.setVisible(false);
				borrarImage.addClickHandler(new ClickHandler(){
					@Override
					public void onClick(ClickEvent event) {
						borrarCampoReclamacion();
					}});
				break;
			case EDITAR:
				borrarImage.setVisible(false);
				break;
		}
		contenidoLabel.setText(campo.getContenido());
		this.campo = campo;
		this.panelCampos = panelCampos;
	}
    

	public CampoDeEventoJso getCampoReclamacion() {
		if(modo == Modo.EDITAR) campo.setContenido(valorContenidoTextBox.getText());
		return campo;
	}

	private void borrarCampoReclamacion() {
		if(enabled) {
			if(Window.confirm(Constantes.INSTANCIA.confirmarBorradoOpcion())){
				panelCampos.borrarCampo(campo);
			}
		}
	}

	public void setEnabled(boolean camposEnabled) {
		this.enabled  = camposEnabled;
	}
    
	public boolean isValidForm() {
		if(modo == Modo.CREAR) return true;
		if("".equals(valorContenidoTextBox.getText().trim())) {
			valorContenidoTextBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			valorContenidoTextBox.setStyleName(style.errorTextBox(), false);
			return true;
		}
	}

}