package org.controlacceso.clientegwt.client.evento;

import org.controlacceso.clientegwt.client.modelo.UsuarioJso;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EventGWTRepresentativeDetails extends 
	GwtEvent<EventGWTRepresentativeDetails.Handler> {

	  public interface Handler extends EventHandler {
	    void setRepresentativeDetails(UsuarioJso usuario);
	  }
	
	  public static final Type<EventGWTRepresentativeDetails.Handler> TYPE = 
			  new Type<EventGWTRepresentativeDetails.Handler>();
	
	  public UsuarioJso usuario;
	  
	  public EventGWTRepresentativeDetails (UsuarioJso usuario) {
		  this.usuario = usuario;
	  }
	  
	  @Override
	  public final Type<EventGWTRepresentativeDetails.Handler> getAssociatedType() {
	    return TYPE;
	  }
	
	  @Override
	  protected void dispatch(EventGWTRepresentativeDetails.Handler handler) {
	    handler.setRepresentativeDetails(usuario);
	  }
}
