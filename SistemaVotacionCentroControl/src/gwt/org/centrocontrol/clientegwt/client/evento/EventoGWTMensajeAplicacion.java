package org.centrocontrol.clientegwt.client.evento;

import org.centrocontrol.clientegwt.client.HistoryToken;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EventoGWTMensajeAplicacion extends GwtEvent<EventoGWTMensajeAplicacion.Handler> {
	
	public interface Handler extends EventHandler {
		void procesarMensaje(EventoGWTMensajeAplicacion evento);
	}	

	public static final Type<EventoGWTMensajeAplicacion.Handler> TYPE = new Type<EventoGWTMensajeAplicacion.Handler>();

	public Object contenidoMensaje;
	public HistoryToken token;
	public EventoSistemaVotacionJso.Estado estadoEvento;
  

	public EventoGWTMensajeAplicacion (Object contenidoMensaje, HistoryToken token) {
		this.contenidoMensaje = contenidoMensaje;
		this.token = token;
	}
	
	public EventoGWTMensajeAplicacion (Object contenidoMensaje, HistoryToken token, 
			EventoSistemaVotacionJso.Estado estadoEvento) {
		this.contenidoMensaje = contenidoMensaje;
		this.token = token;
		this.estadoEvento = estadoEvento;
	}
  
	@Override
	public final Type<EventoGWTMensajeAplicacion.Handler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(EventoGWTMensajeAplicacion.Handler handler) {
		handler.procesarMensaje(this);
	}
	
	public Object getContenidoMensaje() {return contenidoMensaje;}
	
	public HistoryToken getToken() {return token;}
	
	public EventoSistemaVotacionJso.Estado getEstadoEvento() {return estadoEvento;}

}
