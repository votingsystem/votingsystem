package org.controlacceso.clientegwt.client.representatives;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.util.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
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

public class CheckRepresentativeDialog {
	
    private static Logger logger = Logger.getLogger("CheckRepresentativeDialog");
	
    private static CheckRepresentativeDialogUiBinder uiBinder = 
    		GWT.create(CheckRepresentativeDialogUiBinder.class);
   

    interface CheckRepresentativeDialogUiBinder extends UiBinder<Widget, CheckRepresentativeDialog> {}


    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel messagePanel;
    @UiField TextBox nifText;
    @UiField Label errorMsg;
    DialogoOperacionEnProgreso dialogoProgreso;
    
	public CheckRepresentativeDialog( ) {
        uiBinder.createAndBindUi(this);
        errorMsg.setVisible(false);
	}


	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
		logger.info("Validating nif ->" + nifText.getText());
    	String nif = StringUtils.validarNIF(nifText.getText());
    	if(null == nif) {
    		setMessage(Constantes.INSTANCIA.nifErrorMsg());
    	} else {
    		logger.info("Validating nif with server");
    		setMessage(null);
			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
			dialogoProgreso.showIndeterminate(Constantes.INSTANCIA.checkRepresentativeCaption());
    	}
    }
	
	private void setMessage(String message) {
		if(message == null || "".equals(message)) {
			 errorMsg.setVisible(false);
		} else {
			errorMsg.setText(message);
			errorMsg.setVisible(true);
		}
	}
    
    @UiHandler("cerrarButton")
    void handleCerrarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    }

}
