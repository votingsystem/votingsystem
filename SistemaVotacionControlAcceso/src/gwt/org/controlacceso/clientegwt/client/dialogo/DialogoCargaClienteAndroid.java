package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoCargaClienteAndroid implements ValueChangeHandler<String>{
	
    private static Logger logger = Logger.getLogger("DialogoCargaClienteAndroid");
	
    private static DialogoCargaClienteAndroidUiBinder uiBinder = GWT.create(DialogoCargaClienteAndroidUiBinder.class);
    
    interface DialogoCargaClienteAndroidUiBinder extends UiBinder<Widget, DialogoCargaClienteAndroid> {}

    @UiField VerticalPanel textPanel;
    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField HTML userMsg;
    ConfirmacionListener confirmacionListener;
    public static DialogoCargaClienteAndroid INSTANCIA;
    
	public DialogoCargaClienteAndroid() {
        uiBinder.createAndBindUi(this);
        History.addValueChangeHandler(this);
        INSTANCIA = this;
	}

    @UiHandler("aceptarButton")
    void handleCloseButton(ClickEvent e) {
    	dialogBox.hide();
    	if(confirmacionListener != null) confirmacionListener.confirmed(null, null);
    }
    
    public void hide() {
    	dialogBox.hide();
    }
    
    public void show(String urlAction, ConfirmacionListener confirmacionListener) {
    	logger.info("- urlAction: " + urlAction);
    	if(urlAction != null) {
            userMsg.setHTML(Constantes.INSTANCIA.cargaClienteAndroidMsg(
            		urlAction, ServerPaths.getUrlAppAndroid()));
    	} else {
            userMsg.setHTML(Constantes.INSTANCIA.androidOperationMsg(
            		ServerPaths.getUrlAppAndroid()));
    	}
    	this.confirmacionListener = confirmacionListener;
    	dialogBox.center();
    	dialogBox.show();
    	
    }
	
	@Override public void onValueChange(ValueChangeEvent<String> event) {
		dialogBox.hide();
	}
    
}