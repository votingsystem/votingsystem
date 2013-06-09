package org.controlacceso.clientegwt.client.votaciones;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.dialogo.ConfirmacionListener;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.dialogo.PopupAdministrarDocumento;
import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTConsultaEvento;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.EstadisticaJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.modelo.OpcionDeEventoJso;
import org.controlacceso.clientegwt.client.modelo.Tipo;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.DateUtils;
import org.controlacceso.clientegwt.client.util.PopUpLabel;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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


public class PanelVotacion extends Composite implements ConfirmacionListener, ContenedorOpciones,
	 EventoGWTConsultaEvento.Handler, EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("PanelVotacion");

	private static PanelVotacionUiBinder uiBinder = GWT.create(PanelVotacionUiBinder.class);
	interface PanelVotacionUiBinder extends UiBinder<Widget, PanelVotacion> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
        String messageLabel();
        String linkedMessageLabel();
    }

	@UiField HTML pageTitle;
	@UiField HTML piePagina;
    @UiField EditorStyle style;
    @UiField VerticalPanel panelContenidos;
    @UiField HorizontalPanel panelBarrarProgreso;
    @UiField VerticalPanel messagePanel;
    @UiField Label fechaLimiteLabel;
    @UiField VerticalPanel contentPanel;
    @UiField VerticalPanel contenidoPanel;
    @UiField HorizontalPanel autorPanel;
    @UiField Label autorLabel;
    @UiField VerticalPanel contenedorOpcionesPanel;
    @UiField PanelGraficoResultadoDeVotacion panelGraficoVotacion;
    @UiField Label opcionesLabel;
	@UiField Label estatusCentroControlLabel;
    @UiField PopUpLabel administracionDocumentoLabel;
    
    AdministrarEventoEventListener administrarEventoEventListener = new AdministrarEventoEventListener();
    private PopupAdministrarDocumento popupAdministrarDocumento;
    
    private HTML contenidoEvento = null;
    private EventoSistemaVotacionJso evento;
    DialogoOperacionEnProgreso dialogoProgreso;
    public static PanelVotacion INSTANCIA;
    private String hashCertificadoVotoHEX;
    
    
    public PanelVotacion() {
        initWidget(uiBinder.createAndBindUi(this));
        messagePanel.setVisible(false);
		administracionDocumentoLabel.setListener(administrarEventoEventListener);
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

    
	private void setMessage (String... messages) {
		boolean hasMessages = false;
		if(messages == null || messages.length == 0 ) messagePanel.setVisible(false);
		else {
			messagePanel.clear();
			for(String message: messages) {
				if(null != message) hasMessages = true;
				Label messageLabel = new Label();
		    	messageLabel.setText(message);
				messageLabel.setStyleName(style.messageLabel());
		    	messagePanel.add(messageLabel);
			}
			if(hasMessages) messagePanel.setVisible(true);
			else messagePanel.setVisible(false);
		}
	}
	
	private void setLinkedMessage(String text, String url) {
		Anchor linkedMessage = new Anchor();
		linkedMessage.setStyleName(style.linkedMessageLabel());
		linkedMessage.setHref(url);
		linkedMessage.setText(text);
		linkedMessage.setTarget("_blank");
		messagePanel.add(linkedMessage);	
	}

    public void show(EventoSistemaVotacionJso documento) {
    	this.evento = documento;
    	if (documento == null) return;
        messagePanel.setVisible(false);
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
        logger.info("documento: " + documento.toJSONString());
        if(documento.getOpcionDeEventoList() != null) {
        	actualizarPanelOpciones(documento.getOpcionDeEventoList(), documento.isActive());
        }
        if(documento.isActive()) opcionesLabel.setText(Constantes.INSTANCIA.singleSelectionMessage());
        else opcionesLabel.setText(Constantes.INSTANCIA.singleSelectionFinishedMessage());
        EventoSistemaVotacionJso.Estado estadoDocumento = documento.getEstadoEnumValue();
    	String message = null;
    	switch(estadoDocumento) {
        	case CANCELADO:
        		message = Constantes.INSTANCIA.votingCanceledMsg();
        		administracionDocumentoLabel.setVisible(false);
        		break;
        	case FINALIZADO:
        		message = Constantes.INSTANCIA.votingFinishedMsg();
        		administracionDocumentoLabel.setVisible(false);
        		break;
        	case PENDIENTE_COMIENZO:
        		message = Constantes.INSTANCIA.votingPendingMsg();
        		break;
        	case ACTIVO:
        		administracionDocumentoLabel.setVisible(true);
        		break;
    	}
    	setMessage(message);
		RequestHelper.doGet(ServerPaths.getUrlEstadisticasEventoVotacion(evento.getId()), 
        		new ServerRequestEstadisticasCallback());
        panelBarrarProgreso.setVisible(false);
        panelContenidos.setVisible(true);
    }
    
    public void actualizarPanelOpciones(List<OpcionDeEventoJso> opciones, boolean isActive) {
    	contenedorOpcionesPanel.clear();
    	for(OpcionDeEventoJso opcion:opciones) {
        	PanelSeleccionOpcionDeVotacion panelSeleccion = new PanelSeleccionOpcionDeVotacion(opcion, this);
        	panelSeleccion.setEnabled(isActive);
        	contenedorOpcionesPanel.add(panelSeleccion);
        }
    	if(opciones != null && opciones.size() > 0)
    		actualizarStatusCentroControl(null);
    }
	
    public void actualizarStatusCentroControl(String text) {
    	if(text == null) estatusCentroControlLabel.setVisible(false);
    	else {
    		estatusCentroControlLabel.setText(text);
    		estatusCentroControlLabel.setVisible(true);
    	}
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
					hashCertificadoVotoHEX = mensaje.getEvento().getHashCertificadoVotoHex();
					dialogoResultado.show();
				}else if(MensajeClienteFirmaJso.SC_ERROR_ENVIO_VOTO == mensaje.getCodigoEstado()) {
					DialogoAnulacionSolicitudAcceso dialogoAnulacionSolicitud = 
							new DialogoAnulacionSolicitudAcceso(mensaje.getEvento());
					dialogoAnulacionSolicitud.show();
				} else if(MensajeClienteFirmaJso.SC_CANCELADO == mensaje.getCodigoEstado()) {
				} else {
					if(MensajeClienteFirmaJso.SC_ERROR_VOTO_REPETIDO == mensaje.getCodigoEstado()) {
						setMessage(Constantes.INSTANCIA.mensajeVotoRepetido());
						if(mensaje.getEvento() != null && 
								mensaje.getEvento().getVotante() != null) {
							setLinkedMessage(Constantes.INSTANCIA.solicitudAccesoRepetida(), 
									ServerPaths.getUrlSolicitudAccesoPorNif( 
											mensaje.getEvento().getVotante().getNif(),
											mensaje.getEvento().getId()));
						}
					} else {
						setMessage(Constantes.INSTANCIA.mensajeError(
								mensaje.getMensaje()));
					}
				}
				break;
			case ANULAR_VOTO:
				setWidgetsStateFirmando(false);
				if(200 == mensaje.getCodigoEstado()) {
					setMessage(Constantes.INSTANCIA.mensajeAnulacionVotoOK());
					setLinkedMessage(Constantes.INSTANCIA.cancelVoteServerData(), 
							ServerPaths.getUrlSolicitudCancelVote( 
									hashCertificadoVotoHEX));				
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							Constantes.INSTANCIA.mensajeAnulacionVotoERROR() +
							" - " + mensaje.getMensaje()));
				}
				break;
			case GUARDAR_RECIBO_VOTO:
				setWidgetsStateFirmando(false);
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					setMessage(Constantes.INSTANCIA.mensajeGuardarReciboOK(), 
							mensaje.getMensaje());
				} else if(MensajeClienteFirmaJso.SC_CANCELADO == mensaje.getCodigoEstado()) {
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							Constantes.INSTANCIA.mensajeGuardarReciboERROR() +
							" - " + mensaje.getMensaje()));
				}
				break;
			case SOLICITUD_COPIA_SEGURIDAD:
				setWidgetsStateFirmando(false);
				ResultDialog resultDialog = new ResultDialog();
				if(200 == mensaje.getCodigoEstado()) {
            		resultDialog.show(null, Constantes.INSTANCIA.
            				mensajeSolicitudCopiaSeguridadOK(), Boolean.TRUE);
					
				} else {
            		resultDialog.show(null, Constantes.INSTANCIA.mensajeError(
							mensaje.getMensaje()), Boolean.FALSE);
				}
			default:
				break;
		}
		
	}

	@Override
	public void procesarOpcionSeleccionada(OpcionDeEventoJso opcion) {
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.ENVIO_VOTO_SMIME.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		evento.setUrlSolicitudAcceso(ServerPaths.getUrlSolicitudAcceso());
		evento.setUrlRecolectorVotosCentroControl(ServerPaths.
				getUrlVotoCentroControl(evento.getCentroControl().getServerURL()));
		evento.setOpcionSeleccionada(opcion);
		mensajeClienteFirma.setEvento(evento);
    	mensajeClienteFirma.setNombreDestinatarioFirma(
    			PuntoEntrada.INSTANCIA.servidor.getNombre());
    	if(!Browser.isAndroid()) setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
	}
	
	private void mostraEstadisticas(EstadisticaJso estadistica) {
		logger.info("mostraEstadisticas: " + estadistica.toJSONString());
		panelGraficoVotacion.mostraEstadisticas(evento, estadistica, this);
		panelGraficoVotacion.setVisible(true);
	}

    private class ServerRequestEstadisticasCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	ResultDialog resultDialog = new ResultDialog();
    		resultDialog.show(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage(),Boolean.FALSE);             
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
    
    private class ServerAndroidRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	ResultDialog resultDialog = new ResultDialog();
    		resultDialog.show(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage(),Boolean.FALSE);            
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("OK - response.getText(): " + response.getText());
            } else {
            	logger.log(Level.SEVERE, "ERROR - response.getText(): " + response.getText());
            	//new ErrorDialog().show (String.valueOf(response.getStatusCode()), response.getText());
            }
        }

    }

	@Override
	public void confirmed(Integer id, Object param) {
		logger.info("- confirmed - id: " + id + " - email: " + param);
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.SOLICITUD_COPIA_SEGURIDAD.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlSolicitudCopiaSeguridad());
		mensajeClienteFirma.setEvento(evento);
		mensajeClienteFirma.setEmailSolicitante((String)param);
    	mensajeClienteFirma.setNombreDestinatarioFirma(
    			PuntoEntrada.INSTANCIA.servidor.getNombre());
    	if(!Browser.isAndroid()) setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
		
	}

}