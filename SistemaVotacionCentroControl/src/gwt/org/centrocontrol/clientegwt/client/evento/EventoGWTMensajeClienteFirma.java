package org.centrocontrol.clientegwt.client.evento;

import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EventoGWTMensajeClienteFirma extends GwtEvent<EventoGWTMensajeClienteFirma.Handler> {

  public interface Handler extends EventHandler {
    void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje);
  }

  public static final Type<EventoGWTMensajeClienteFirma.Handler> TYPE = new Type<EventoGWTMensajeClienteFirma.Handler>();

  public MensajeClienteFirmaJso mensaje;
  
  public EventoGWTMensajeClienteFirma (MensajeClienteFirmaJso mensaje) {
	  this.mensaje = mensaje;
  }
  
  @Override
  public final Type<EventoGWTMensajeClienteFirma.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(EventoGWTMensajeClienteFirma.Handler handler) {
    handler.procesarMensajeClienteFirma(mensaje);
  }
}
