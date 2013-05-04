package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.util.StringUtils;
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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NifDialog {
	
    private static Logger logger = Logger.getLogger("NifDialog");
	
    private static NifDialogUiBinder uiBinder = 
    		GWT.create(NifDialogUiBinder.class);
   

    interface Style extends CssResource {
        String errorTextBox();
        String nifTextBox();
        String errorMessageLabel();
    }

    interface NifDialogUiBinder extends UiBinder<Widget, NifDialog> {}


    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel errorMessagePanel;
    @UiField HTML errorMessageLabel;
    @UiField TextBox nifTextBox;
    @UiField Style style;
    @UiField HTML messageLabel;
    ConfirmacionListener confirmacionListener;
    private Integer id;
    SubmitHandler sh = new SubmitHandler();
    
	public NifDialog(Integer id, ConfirmacionListener confirmacionListener,
			String caption, String message) {
        uiBinder.createAndBindUi(this);
        errorMessagePanel.setVisible(false);
        this.id = id;
        nifTextBox.addKeyDownHandler(sh);
        this.confirmacionListener = confirmacionListener;
        if(message != null) messageLabel.setHTML(message);
        if(caption != null) dialogBox.setText(caption);
	}


	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
    	if(isValidForm()) {
    		confirmacionListener.confirmed(
    				id, nifTextBox.getText());
    		dialogBox.hide();
    	} 
    }
    
    @UiHandler("cerrarButton")
    void handleCerrarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    
    public void show() {
    	nifTextBox.setText("");
    	dialogBox.center();
    	dialogBox.show();
    	
    }
    
	private boolean isValidForm() {
		setErrorMessage(null);
		nifTextBox.setStyleName(style.errorTextBox(), false);
		if (Validator.isTextBoxEmpty(nifTextBox)) {
			setErrorMessage(Constantes.INSTANCIA.emptyFieldException());
			nifTextBox.setStyleName(style.errorTextBox(), true);
			return false;
		}
		String nif = StringUtils.validarNIF(nifTextBox.getText());
		if (null == nif) {
			setErrorMessage(Constantes.INSTANCIA.nifErrorMsg());
			nifTextBox.setStyleName(style.errorTextBox(), true);
			return false;
		}
		return true;
	}
	
	private void setErrorMessage (String message) {
		if(message == null || "".equals(message)){
			errorMessagePanel.setVisible(false);
		} else {
			errorMessageLabel.setHTML(message);
			errorMessageLabel.setStyleName(style.errorMessageLabel(), true);
			errorMessagePanel.setVisible(true);
		}
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
