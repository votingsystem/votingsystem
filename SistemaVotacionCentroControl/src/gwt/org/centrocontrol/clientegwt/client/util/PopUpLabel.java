package org.centrocontrol.clientegwt.client.util;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Label;

public class PopUpLabel extends Label {

	private EventListener eventListener;
	
    public PopUpLabel()  {
        sinkEvents(Event.ONCLICK | Event.ONMOUSEOUT | Event.ONMOUSEOVER);
    }
    
    public void setListener(EventListener eventListener) {
    	this.eventListener = eventListener;
    }
    
    public void onBrowserEvent(Event event)  {
    	if(eventListener != null) eventListener.onBrowserEvent(event);
    }
}
