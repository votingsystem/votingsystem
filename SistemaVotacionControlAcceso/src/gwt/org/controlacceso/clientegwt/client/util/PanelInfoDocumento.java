package org.controlacceso.clientegwt.client.util;

import java.util.logging.Logger;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class PanelInfoDocumento extends HorizontalPanel {

    private static Logger logger = Logger.getLogger("PanelInfoDocumento");
	
    EventListener eventListener;
    
	public PanelInfoDocumento()  {
		super();
        sinkEvents(Event.ONCLICK);
        sinkEvents(Event.ONMOUSEOVER);
        sinkEvents(Event.ONMOUSEOUT);
    }
	
	public void onBrowserEvent(Event event){
		eventListener.onBrowserEvent(event);
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}
	 
}
