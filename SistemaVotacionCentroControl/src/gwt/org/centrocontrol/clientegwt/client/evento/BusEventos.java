package org.centrocontrol.clientegwt.client.evento;

import com.google.gwt.event.shared.*;
import com.google.gwt.event.shared.GwtEvent.Type;

public class BusEventos {

	private static final EventBus eventBus = new SimpleEventBus();
	
	public static void fireEvent (GwtEvent event) {
		eventBus.fireEvent(event);
	}
	
	public static <H> void addHandler(Type<H> type, H handler) {
		eventBus.addHandler(type, handler);
	}
}
