package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.util.Validator;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoSolicitudEmail {
	
    private static Logger logger = Logger.getLogger("DialogoSolicitudEmail");
	
    private static DialogoSolicitudEmailUiBinder uiBinder = 
    		GWT.create(DialogoSolicitudEmailUiBinder.class);
   

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
    }

    interface DialogoSolicitudEmailUiBinder extends UiBinder<Widget, DialogoSolicitudEmail> {}


    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    @UiField TextBox contenidoTextBox;
    @UiField EditorStyle style;
    @UiField HTML emailLabel;
    SolicitanteEmail solicitanteEmail;
    private Integer id;
    SubmitHandler sh = new SubmitHandler();
    
	public DialogoSolicitudEmail(Integer id, SolicitanteEmail solicitanteEmail) {
        uiBinder.createAndBindUi(this);
        messagePanel.setVisible(false);
        this.id = id;
        contenidoTextBox.addKeyDownHandler(sh);
        this.solicitanteEmail = solicitanteEmail;
	}
	
	public DialogoSolicitudEmail(Integer id, SolicitanteEmail solicitanteEmail, 
			String message) {
		this(id, solicitanteEmail);
		emailLabel.setHTML(message);
	}


	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
    	if(isValidForm()) {
    		solicitanteEmail.procesarEmail(
    				id, contenidoTextBox.getText());
    		dialogBox.hide();
    	} 
    }
    
    @UiHandler("cerrarButton")
    void handleCerrarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    
    public void show() {
    	contenidoTextBox.setText("");
    	dialogBox.center();
    	dialogBox.show();
    	
    }
    
	private boolean isValidForm() {
		setMessage(null);
		contenidoTextBox.setStyleName(style.textBox(), true);
		contenidoTextBox.setStyleName(style.errorTextBox(), false);
		if (Validator.isTextBoxEmpty(contenidoTextBox)) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			contenidoTextBox.setStyleName(style.textBox(), false);
			contenidoTextBox.setStyleName(style.errorTextBox(), true);
			return false;
		} if (!Validator.isValidEmail(contenidoTextBox.getText())) {
			setMessage(Constantes.INSTANCIA.mensajeErrorEmail());
			contenidoTextBox.setStyleName(style.textBox(), false);
			contenidoTextBox.setStyleName(style.errorTextBox(), true);
			return false;
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
