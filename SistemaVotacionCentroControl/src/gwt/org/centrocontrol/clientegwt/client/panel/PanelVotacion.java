package org.centrocontrol.clientegwt.client.panel;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.PuntoEntrada;
import org.centrocontrol.clientegwt.client.dialogo.DialogoAnulacionSolicitudAcceso;
import org.centrocontrol.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.centrocontrol.clientegwt.client.dialogo.DialogoResultadoVotacion;
import org.centrocontrol.clientegwt.client.dialogo.ErrorDialog;
import org.centrocontrol.clientegwt.client.dialogo.PopupAdministrarDocumento;
import org.centrocontrol.clientegwt.client.dialogo.PopupSolicitudCopiaSeguridad;
import org.centrocontrol.clientegwt.client.dialogo.SolicitanteEmail;
import org.centrocontrol.clientegwt.client.evento.BusEventos;
import org.centrocontrol.clientegwt.client.evento.EventoGWTConsultaEvento;
import org.centrocontrol.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.centrocontrol.clientegwt.client.modelo.ActorConIPJso;
import org.centrocontrol.clientegwt.client.modelo.EstadisticaJso;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.centrocontrol.clientegwt.client.modelo.OpcionDeEventoJso;
import org.centrocontrol.clientegwt.client.modelo.Tipo;
import org.centrocontrol.clientegwt.client.util.Browser;
import org.centrocontrol.clientegwt.client.util.DateUtils;
import org.centrocontrol.clientegwt.client.util.PopUpLabel;
import org.centrocontrol.clientegwt.client.util.RequestHelper;
import org.centrocontrol.clientegwt.client.util.ServerPaths;
import org.centrocontrol.clientegwt.client.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class PanelVotacion extends Composite implements SolicitanteEmail, ContenedorOpciones,
	 EventoGWTConsultaEvento.Handler, EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("PanelVotacion");

	private static PanelVotacionUiBinder uiBinder = GWT.create(PanelVotacionUiBinder.class);
	interface PanelVotacionUiBinder extends UiBinder<Widget, PanelVotacion> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
    }

	@UiField HTML pageTitle;
	@UiField HTML piePagina;
    @UiField EditorStyle style;
    @UiField VerticalPanel panelContenidos;
    @UiField HorizontalPanel panelBarrarProgreso;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    @UiField Label fechaLimiteLabel;
    @UiField VerticalPanel contentPanel;
    @UiField VerticalPanel contenidoPanel;
    @UiField HorizontalPanel autorPanel;
    @UiField Label autorLabel;
    @UiField VerticalPanel contenedorOpcionesPanel;
    @UiField Anchor enlaceJustificante;
    @UiField PanelGraficoResultadoDeVotacion panelGraficoVotacion;
    @UiField Label opcionesLabel;
    
    @UiField PopUpLabel administracionDocumentoLabel;
    
    AdministrarEventoEventListener administrarEventoEventListener = new AdministrarEventoEventListener();
    private PopupAdministrarDocumento popupAdministrarDocumento;
    
    private HTML contenidoEvento = null;
    private EventoSistemaVotacionJso evento;
    DialogoOperacionEnProgreso dialogoProgreso;
    private PopupSolicitudCopiaSeguridad popUpSolicitudCopiaSeguridad;
    public static PanelVotacion INSTANCIA;
    
    
    public PanelVotacion() {
        initWidget(uiBinder.createAndBindUi(this));
        messagePanel.setVisible(false);
		administracionDocumentoLabel.setListener(administrarEventoEventListener);
        enlaceJustificante.setTarget("_blank");
        INSTANCIA = this;
        panelGraficoVotacion.setVisible(false);
        panelContenidos.setVisible(false);
        if(Browser.isAndroid()) {
        	piePagina.setHTML(Constantes.INSTANCIA.piePaginaVotarAndroid());
        } else {
        	piePagina.setHTML(Constantes.INSTANCIA.piePaginaVotar());
        }
        BusEventos.addHandler(EventoGWTConsultaEvento.TYPE, this);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
    }

    
	private void setMessage (String message) {
		if(message == null || "".equals(message)) messagePanel.setVisible(false);
		else {
			messageLabel.setText(message);
	    	messagePanel.setVisible(true);	
		}
	}

    public void show(EventoSistemaVotacionJso documento) {
    	this.evento = documento;
    	if (documento == null) return;
        messagePanel.setVisible(false);
        enlaceJustificante.setVisible(false);
        contenidoPanel.clear();
        pageTitle.setHTML(Constantes.INSTANCIA.votacionLabel() + " '" + 
        		StringUtils.partirTexto(evento.getAsunto(), 
        		Constantes.MAX_NUM_CARACTERES_SUBJECT)  + "'");
        if(documento.getUsuario() != null)
        	autorLabel.setText(documento.getUsuario());
        else autorPanel.setVisible(false);
        fechaLimiteLabel.setText(
        		DateUtils.getSpanishStringFromDate(evento.getFechaFin()));
        contenidoEvento = new HTML(evento.getContenido());
        contenidoPanel.add(contenidoEvento);
        contenedorOpcionesPanel.clear();
        if(documento.getOpcionDeEventoList() != null) {
            for(OpcionDeEventoJso opcion:documento.getOpcionDeEventoList()) {
            	PanelSeleccionOpcionDeVotacion panelSeleccion = new PanelSeleccionOpcionDeVotacion(opcion, this);
            	panelSeleccion.setEnabled(documento.isActive());
            	contenedorOpcionesPanel.add(panelSeleccion);
            }
        }
        if(documento.isActive()) opcionesLabel.setText(Constantes.INSTANCIA.singleSelectionMessage());
        else opcionesLabel.setText(Constantes.INSTANCIA.singleSelectionFinishedMessage());
        EventoSistemaVotacionJso.Estado estadoDocumento = documento.getEstadoEnumValue();
    	String message = null;
    	switch(estadoDocumento) {
        	case CANCELADO:
        		message = Constantes.INSTANCIA.votingCanceledMsg();
        		break;
        	case FINALIZADO:
        		message = Constantes.INSTANCIA.votingFinishedMsg();
        		break;
        	case PENDIENTE_COMIENZO:
        		message = Constantes.INSTANCIA.votingPendingMsg();
    	}
    	setMessage(message);
		/*if(documento.getNumeroTotalFirmas() == 1) {
			mensajeInfo = Constantes.INSTANCIA.mensajeInfoManifiestoUnaFirma();
		} else if(documento.getNumeroTotalFirmas() > 1) {
			mensajeInfo = Constantes.INSTANCIA.mensajeInfoManifiesto(documento.getNumeroTotalFirmas());
		}*/
		RequestHelper.doGet(ServerPaths.getUrlEstadisticas(evento.getId()), 
        		new ServerRequestEstadisticasCallback());
        panelBarrarProgreso.setVisible(false);
        panelContenidos.setVisible(true);
    }
    
    
    @UiHandler("enlaceJustificante")
    void onClickEnlaceJustificante(ClickEvent e) {

    }
    
	
	public void setWidgetsStateFirmando(boolean publicando) {
		if(publicando) {
			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
			dialogoProgreso.show();
		} else {
			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
			dialogoProgreso.hide();
		}
	}
	
	private void mostrarPopupSolicitudCopiaSeguridad(int clientX, int clientY) {
		if(popUpSolicitudCopiaSeguridad == null) {
			popUpSolicitudCopiaSeguridad = new PopupSolicitudCopiaSeguridad(this);
		}
		popUpSolicitudCopiaSeguridad.setPopupPosition(clientX, clientY);
		popUpSolicitudCopiaSeguridad.show();
	}

	@Override
	public void actualizarEventoSistemaVotacion(EventoSistemaVotacionJso evento) {
		logger.info("actualizarEventoSistemaVotacion - evento:" + evento);
		this.evento = evento;
        show(evento);
	}
	
  	class AdministrarEventoEventListener implements EventListener {

  		@Override
  		public void onBrowserEvent(Event event) {
  			switch(DOM.eventGetType(event)) {
  				case Event.ONCLICK:
  				//case Event.ONMOUSEOVER:
  					mostrarPopupAdministrarEvento(event.getClientX(), event.getClientY());
  					break;
  			    case Event.ONMOUSEOUT:
  			    	break;
  			}
  		}
  	}
  	
  	private void mostrarPopupAdministrarEvento(int clientX, int clientY) {
  		if(popupAdministrarDocumento == null) {
  			evento.setTipoEnumValue(Tipo.EVENTO_VOTACION);
  			popupAdministrarDocumento = new PopupAdministrarDocumento(evento);
  		}
  		popupAdministrarDocumento.setPopupPosition(clientX - 400, clientY);
  		popupAdministrarDocumento.show();
  	}
	
	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case ENVIO_VOTO_SMIME:
				setWidgetsStateFirmando(false);
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					DialogoResultadoVotacion dialogoResultado = new DialogoResultadoVotacion(mensaje.getEvento());
					dialogoResultado.show();
				} else if(MensajeClienteFirmaJso.SC_ERROR_ENVIO_VOTO == mensaje.getCodigoEstado()) {
					DialogoAnulacionSolicitudAcceso dialogoAnulacionSolicitud = 
							new DialogoAnulacionSolicitudAcceso(mensaje.getEvento());
					dialogoAnulacionSolicitud.show();
				}else if(MensajeClienteFirmaJso.SC_CANCELADO == mensaje.getCodigoEstado()) {
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							mensaje.getMensaje()));
					if(MensajeClienteFirmaJso.SC_ERROR_VOTO_REPETIDO == mensaje.getCodigoEstado()) {
						if(mensaje.getEvento() != null && 
								mensaje.getEvento().getVotante() != null) {
							enlaceJustificante.setVisible(true);
							enlaceJustificante.setHref(ServerPaths.getUrlSolicitudAccesoPorNif( 
									evento.getControlAcceso().getServerURL(),
									mensaje.getEvento().getVotante().getNif(),
									mensaje.getEvento().getId()));
							enlaceJustificante.setText(Constantes.INSTANCIA.solicitudAccesoRepetida());
						}
					}
				}
				break;
			case ANULAR_VOTO:
				setWidgetsStateFirmando(false);
				if(200 == mensaje.getCodigoEstado()) {
					setMessage(Constantes.INSTANCIA.mensajeAnulacionVotoOK());
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							Constantes.INSTANCIA.mensajeAnulacionVotoERROR() +
							" - " + mensaje.getMensaje()));
				}
				break;
			case GUARDAR_RECIBO_VOTO:
				setWidgetsStateFirmando(false);
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					setMessage(Constantes.INSTANCIA.mensajeGuardarReciboOK(
						mensaje.getArgsJsArray().get(0)));
				} else if(MensajeClienteFirmaJso.SC_CANCELADO == mensaje.getCodigoEstado()) {
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							Constantes.INSTANCIA.mensajeGuardarReciboERROR() +
							" - " + mensaje.getMensaje()));
				}
				break;
			case SOLICITUD_COPIA_SEGURIDAD:
				setWidgetsStateFirmando(false);
				if(200 == mensaje.getCodigoEstado()) {
					setMessage(Constantes.INSTANCIA.mensajeSolicitudCopiaSeguridadOK());
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							mensaje.getMensaje()));
				}
			default:
				break;
		}
		
	}



	@Override
	public void procesarEmail(String email) {
		logger.info("--- procesarEmail");
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.SOLICITUD_COPIA_SEGURIDAD.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlSolicitudCopiaSeguridad());
		mensajeClienteFirma.setEvento(evento);
		mensajeClienteFirma.setEmailSolicitante(email);
    	mensajeClienteFirma.setNombreDestinatarioFirma(
    			PuntoEntrada.INSTANCIA.servidor.getNombre());
		setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
	}

	@Override
	public void procesarOpcionSeleccioda(OpcionDeEventoJso opcion) {
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.ENVIO_VOTO_SMIME.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		EventoSistemaVotacionJso voto = EventoSistemaVotacionJso.create();
		ActorConIPJso centroControl = ActorConIPJso.create();
		centroControl.setNombre(PuntoEntrada.INSTANCIA.servidor.getNombre());
		centroControl.setServerURL(PuntoEntrada.INSTANCIA.servidor.getServerURL());
		voto.setUrl(evento.getUrl());
		voto.setAsunto(evento.getAsunto());
		voto.setCentroControl(centroControl);
		voto.setControlAcceso(evento.getControlAcceso());
		voto.setId(new Integer(evento.getEventoVotacionId()));
		voto.setUrlSolicitudAcceso(ServerPaths.getUrlSolicitudAcceso(
				evento.getControlAcceso().getServerURL()));
		voto.setUrlRecolectorVotosCentroControl(ServerPaths.getUrlVotoCentroControl());
		opcion.setId(new Integer(opcion.getOpcionDeEventoId()));
		voto.setOpcionSeleccionada(opcion);
		mensajeClienteFirma.setEvento(voto);
    	mensajeClienteFirma.setNombreDestinatarioFirma(
    			PuntoEntrada.INSTANCIA.servidor.getNombre());
		//mensajeClienteFirma.setUrlEnvioDocumento();
    	if(!Browser.isAndroid()) setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
	}
	
	private void mostraEstadisticas(EstadisticaJso estadistica) {
		panelGraficoVotacion.mostraEstadisticas(evento, estadistica);
		panelGraficoVotacion.setVisible(true);
	}

    private class ServerRequestEstadisticasCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	new ErrorDialog().show ("Exception", exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("response.getText(): " + response.getText());
            	EstadisticaJso estadistica = EstadisticaJso.create(response.getText());
            	mostraEstadisticas(estadistica);
            } else {
            	logger.log(Level.SEVERE, "response.getText(): " + response.getText());
            	//new ErrorDialog().show (String.valueOf(response.getStatusCode()), response.getText());
            }
        }

    }

}