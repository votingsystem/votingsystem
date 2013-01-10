package org.centrocontrol.clientegwt.client.evento;

import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EventoGWTConsultaEvento extends GwtEvent<EventoGWTConsultaEvento.Handler> {

  public interface Handler extends EventHandler {
    void actualizarEventoSistemaVotacion(EventoSistemaVotacionJso event0);
  }

  public static final Type<EventoGWTConsultaEvento.Handler> TYPE = new Type<EventoGWTConsultaEvento.Handler>();

  public EventoSistemaVotacionJso evento;
  
  public EventoGWTConsultaEvento (EventoSistemaVotacionJso evento) {
	  this.evento = evento;
  }
  
  @Override
  public final Type<EventoGWTConsultaEvento.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(EventoGWTConsultaEvento.Handler handler) {
    handler.actualizarEventoSistemaVotacion(evento);
  }
}
