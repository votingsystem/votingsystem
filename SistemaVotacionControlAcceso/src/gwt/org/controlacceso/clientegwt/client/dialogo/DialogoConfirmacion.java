package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoConfirmacion {
	
    private static Logger logger = Logger.getLogger("DialogoConfirmacion");
	
    private static DialogoConfirmacionUiBinder uiBinder = 
    		GWT.create(DialogoConfirmacionUiBinder.class);
   

    interface DialogoConfirmacionUiBinder extends UiBinder<Widget, DialogoConfirmacion> {}


    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    ConfirmacionListener listener;
    Integer id;
    
    
	public DialogoConfirmacion(Integer id, ConfirmacionListener listener) {
        uiBinder.createAndBindUi(this);
        this.listener = listener;
        this.id = id;
	}


	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
		listener.confirmed(id);
		dialogBox.hide();
    }
    
    @UiHandler("cerrarButton")
    void handleCerrarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    
    public void show(String message) {
    	messageLabel.setText(message);
    	dialogBox.center();
    	dialogBox.show();
    	
    }

}
