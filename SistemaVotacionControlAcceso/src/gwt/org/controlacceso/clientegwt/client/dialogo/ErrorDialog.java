package org.controlacceso.clientegwt.client.dialogo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class ErrorDialog {
	
    private static ErrorDialogUiBinder uiBinder = GWT.create(ErrorDialogUiBinder.class);
    
    interface ErrorDialogUiBinder extends UiBinder<Widget, ErrorDialog> {}
    
    @UiField PushButton aceptarButton;
    
    @UiField VerticalPanel textPanel;
    @UiField DialogBox dialogBox;

    public ErrorDialog() {
        uiBinder.createAndBindUi(this);
    }

    @UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    public void setTitle(String title) {
    	dialogBox.setTitle(title);
    }
    
    private void setBody(String html) {
    	textPanel.clear();
    	textPanel.add(new HTML(html));
    }
    
    public void show(String title, String body) {
    	dialogBox.setText(title);
    	setBody(body);
    	dialogBox.center();
    	dialogBox.show();
    }
    
}
