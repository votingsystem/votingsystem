package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.dialogo.ConfirmacionListener;
import org.controlacceso.clientegwt.client.dialogo.DialogoConfirmacion;
import org.controlacceso.clientegwt.client.modelo.CampoDeEventoJso;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class PanelCampoReclamacion extends Composite implements ConfirmacionListener {
	
    private static Logger logger = Logger.getLogger("PanelCampoReclamacion");

	private static PanelCampoReclamacionUiBinder uiBinder = GWT
			.create(PanelCampoReclamacionUiBinder.class);

	interface PanelCampoReclamacionUiBinder extends UiBinder<Widget, PanelCampoReclamacion> {	}

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String valorContenidoTextBox();
    }
	
    @UiField PushButton borrarOpcionButton;
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
				break;
			case EDITAR:
				borrarOpcionButton.setVisible(false);
				break;
		}
		contenidoLabel.setText(" - " + campo.getContenido());
		this.campo = campo;
		this.panelCampos = panelCampos;
	}
    

	public CampoDeEventoJso getCampoReclamacion() {
		if(modo == Modo.EDITAR) campo.setValor(valorContenidoTextBox.getText());
		return campo;
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

	@UiHandler("borrarOpcionButton")
    void handleBorrarOpcionButton(ClickEvent e) {
		DialogoConfirmacion dialogoConfirmacion = new DialogoConfirmacion(null, this);
		dialogoConfirmacion.show(Constantes.INSTANCIA.confirmarBorradoOpcion());
	}


	@Override
	public void confirmed(Integer id, Object param) {
		panelCampos.borrarCampo(campo);
	}
}