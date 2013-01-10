package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.modelo.*;
import org.controlacceso.clientegwt.client.util.Validator;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoCrearCampoReclamacion {
	
    private static Logger logger = Logger.getLogger("DialogoCrearCampoReclamacion");
	
    private static DialogoCrearCampoReclamacionUiBinder uiBinder = 
    		GWT.create(DialogoCrearCampoReclamacionUiBinder.class);
   

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
    }

    interface DialogoCrearCampoReclamacionUiBinder extends UiBinder<Widget, DialogoCrearCampoReclamacion> {}


    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    @UiField TextBox contenidoTextBox;
    @UiField EditorStyle style;
    CampoDeEventoJso campoCreado;
    PanelPublicacionReclamacion panelPublicacionReclamacion;
    
    SubmitHandler sh = new SubmitHandler();
    
	public DialogoCrearCampoReclamacion() {
        uiBinder.createAndBindUi(this);
        messagePanel.setVisible(false);
        contenidoTextBox.addKeyDownHandler(sh);
	}

    public DialogoCrearCampoReclamacion(
    		PanelPublicacionReclamacion panelPublicacionReclamacion) {
		this();
		this.panelPublicacionReclamacion = panelPublicacionReclamacion;
	}

	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
    	if(isValidForm()) {
    		campoCreado = CampoDeEventoJso.create();
    		campoCreado.setContenido(contenidoTextBox.getText());
    		panelPublicacionReclamacion.anyadirCampoReclamacion(campoCreado);
    		dialogBox.hide();
    	} 
    }
    
    @UiHandler("cerrarButton")
    void handleCerrarButton(ClickEvent e) {
    	campoCreado = null;
    	dialogBox.hide();
    }
    
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    	
    }
    
    public CampoDeEventoJso getcampoCreado() {
    	return campoCreado;
    }
    
	private boolean isValidForm() {
		setMessage(null);
		if (Validator.isTextBoxEmpty(contenidoTextBox)) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			contenidoTextBox.setStyleName(style.textBox(), false);
			contenidoTextBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			contenidoTextBox.setStyleName(style.textBox(), true);
			contenidoTextBox.setStyleName(style.errorTextBox(), false);
		}
		return true;
	}
	private void setMessage (String message) {
		if(message == null || "".equals(message)) messagePanel.setVisible(false);
    	messageLabel.setText(message);
    	messagePanel.setVisible(true);
	}
    
    private class SubmitHandler implements KeyDownHandler {
		@Override
		public void onKeyDown(KeyDownEvent event) {
			if (KeyCodes.KEY_ENTER == event.getNativeKeyCode()) {
				handleAceptarButton(null);
			}		
		}
	}
}
