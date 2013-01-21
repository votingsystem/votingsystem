package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.util.Browser;

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

public class DialogoCargaClienteFirma implements ValueChangeHandler<String>{
	
    private static Logger logger = Logger.getLogger("DialogoCargaClienteFirma");
	
    private static DialogoCargaClienteFirmaUiBinder uiBinder = GWT.create(DialogoCargaClienteFirmaUiBinder.class);
    
    
    interface DialogoCargaClienteFirmaUiBinder extends UiBinder<Widget, DialogoCargaClienteFirma> {}

    @UiField VerticalPanel textPanel;
    @UiField DialogBox dialogBox;
    @UiField PushButton aceptarButton;
    @UiField HTML textoChrome;
    @UiField HTML textoAdvertencia;
    public static DialogoCargaClienteFirma INSTANCIA;
    
	public DialogoCargaClienteFirma() {
        uiBinder.createAndBindUi(this);
        INSTANCIA = this;
        History.addValueChangeHandler(this);
        if(Browser.isChrome()) textPanel.remove(textoAdvertencia);
        else textPanel.remove(textoChrome); 
	}

    @UiHandler("aceptarButton")
    void handleCloseButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    public void hide() {
    	dialogBox.hide();
    }
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    	
    }

	@Override public void onValueChange(ValueChangeEvent<String> event) {
		dialogBox.hide();
	}
    
}
