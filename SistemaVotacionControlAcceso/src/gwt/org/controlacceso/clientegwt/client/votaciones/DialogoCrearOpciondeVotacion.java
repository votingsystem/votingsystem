package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.modelo.OpcionDeEventoJso;
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
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoCrearOpciondeVotacion {
	
    private static Logger logger = Logger.getLogger("DialogoCrearOpciondeVotacion");
	
    private static DialogoCrearOpciondeVotacionUiBinder uiBinder = 
    		GWT.create(DialogoCrearOpciondeVotacionUiBinder.class);
   

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String contenidoTextBox();
    }

    interface DialogoCrearOpciondeVotacionUiBinder extends UiBinder<Widget, DialogoCrearOpciondeVotacion> {}


    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    @UiField TextBox contenidoTextBox;
    @UiField EditorStyle style;
    @UiField FormPanel formPanel;
    OpcionDeEventoJso opcionCreada;
    PanelPublicacionVotacion panelPublicacionVotacion;
    
    SubmitHandler sh = new SubmitHandler();
    
	public DialogoCrearOpciondeVotacion() {
        uiBinder.createAndBindUi(this);
        messagePanel.setVisible(false);
        formPanel.setVisible(false);
        contenidoTextBox.addKeyDownHandler(sh);
	}

    public DialogoCrearOpciondeVotacion(
			PanelPublicacionVotacion panelPublicacionVotacion) {
		this();
		this.panelPublicacionVotacion = panelPublicacionVotacion;
	}

	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
    	if(isValidForm()) {
    		opcionCreada = OpcionDeEventoJso.create();
    		opcionCreada.setContenido(contenidoTextBox.getText());
    		panelPublicacionVotacion.anyadirOpcionDeVotacion(opcionCreada);
    		dialogBox.hide();
    	} 
    }
    
    @UiHandler("cerrarButton")
    void handleCerrarButton(ClickEvent e) {
    	opcionCreada = null;
    	dialogBox.hide();
    }
    
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    	
    }
    
    public OpcionDeEventoJso getOpcionCreada() {
    	return opcionCreada;
    }
    
	private boolean isValidForm() {
		setMessage(null);
		if (Validator.isTextBoxEmpty(contenidoTextBox)) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			contenidoTextBox.setStyleName(style.contenidoTextBox(), false);
			contenidoTextBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			contenidoTextBox.setStyleName(style.contenidoTextBox(), true);
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
