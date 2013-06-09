package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Recursos;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ResultDialog {
	
    private static Logger logger = Logger.getLogger("ResultDialog");
	
    private static ResultDialogUiBinder uiBinder = 
    		GWT.create(ResultDialogUiBinder.class);
   

    interface ResultDialogUiBinder extends UiBinder<Widget, ResultDialog> {}
    
    interface Style extends CssResource {
        String messageLabel();
    }

    @UiField DialogBox dialogBox;
    @UiField Style style;
    @UiField PushButton aceptarButton;
    @UiField VerticalPanel messagePanel;
    @UiField Image resultImage;
    
	public ResultDialog() {
        uiBinder.createAndBindUi(this);
        resultImage.setVisible(false);
        Scheduler.get().scheduleDeferred(new Command() {
            public void execute() {
            	aceptarButton.setFocus(true);
            }
        });
	}


	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
		dialogBox.hide();
    }
    
    
    public void show(String message) {
    	HTML htmlMessage = new HTML(message);
    	htmlMessage.setStyleName(style.messageLabel(), true);
    	messagePanel.add(htmlMessage);
    	dialogBox.center();
    	dialogBox.show();
    }

    public void show(String caption, String message, Boolean isOK) {
    	HTML htmlMessage = new HTML(message);
    	htmlMessage.setStyleName(style.messageLabel(), true);
    	messagePanel.add(htmlMessage);
    	if(caption != null) dialogBox.setText(caption);
    	dialogBox.center();
    	if(isOK != null) {
    		if(isOK) {
    			resultImage.setResource(
    					Recursos.INSTANCIA.accept_48x48());
    		} else {
    			resultImage.setResource(
    					Recursos.INSTANCIA.cancel_48x48());
    		}
    		resultImage.setVisible(true);
    		htmlMessage.setStyleDependentName(style.messageLabel(), true);
    	}
    	dialogBox.show();
    }

}