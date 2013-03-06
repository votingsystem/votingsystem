package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoResultadoFirma {
	
    private static Logger logger = Logger.getLogger("DialogoResultadoFirma");
	
    private static DialogoResultadoFirmaUiBinder uiBinder = 
    		GWT.create(DialogoResultadoFirmaUiBinder.class);
   

    interface DialogoResultadoFirmaUiBinder extends UiBinder<Widget, DialogoResultadoFirma> {}


    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    
	public DialogoResultadoFirma() {
        uiBinder.createAndBindUi(this);
	}


	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
		dialogBox.hide();
    }
    
    
    public void show(String message) {
    	messageLabel.setText(message);
    	dialogBox.center();
    	dialogBox.show();
    	
    }

}
