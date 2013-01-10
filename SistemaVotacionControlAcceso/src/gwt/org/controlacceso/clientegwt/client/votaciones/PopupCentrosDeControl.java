package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Logger;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class PopupCentrosDeControl {
	
    private static Logger logger = Logger.getLogger("PopUpCentrosDeControl");
	
    private static PopUpCentrosDeControlUiBinder uiBinder = GWT.create(PopUpCentrosDeControlUiBinder.class);
    
    
    interface PopUpCentrosDeControlUiBinder extends UiBinder<Widget, PopupCentrosDeControl> {}

    @UiField(provided = true) DecoratedPopupPanel popupPanel;
    @UiField PushButton asociarCentroControlButton;
    
	public PopupCentrosDeControl() {
		popupPanel = new DecoratedPopupPanel(true);
        uiBinder.createAndBindUi(this);
	}

    @UiHandler("asociarCentroControlButton")
    void handleCloseButton(ClickEvent e) {
    	popupPanel.hide();
    	DialogoAsociarCentroControl dialogoAsociar = new DialogoAsociarCentroControl();
    	dialogoAsociar.show();
    }
	
    public PopupCentrosDeControl(int clientX, int clientY) {
		this();
		popupPanel.setPopupPosition(clientX, clientY);
	}

    public void setPopupPosition(int clientX, int clientY) {
    	popupPanel.setPopupPosition(clientX, clientY);
    }
    
	public void show() {
    	popupPanel.show();
    }
    
}
