package org.centrocontrol.clientegwt.client.evento;

import org.centrocontrol.clientegwt.client.modelo.ConsultaEventosSistemaVotacionJso;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EventoGWTConsultaEventos extends GwtEvent<EventoGWTConsultaEventos.Handler> {

  public interface Handler extends EventHandler {
    void recepcionConsultaEventos(EventoGWTConsultaEventos event);
  }

  public static final Type<EventoGWTConsultaEventos.Handler> TYPE = new Type<EventoGWTConsultaEventos.Handler>();

  public ConsultaEventosSistemaVotacionJso consulta;
  
  public EventoGWTConsultaEventos (ConsultaEventosSistemaVotacionJso consulta) {
	  this.consulta = consulta;
  }
  
  @Override
  public final Type<EventoGWTConsultaEventos.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(EventoGWTConsultaEventos.Handler handler) {
    handler.recepcionConsultaEventos(this);
  }
}
