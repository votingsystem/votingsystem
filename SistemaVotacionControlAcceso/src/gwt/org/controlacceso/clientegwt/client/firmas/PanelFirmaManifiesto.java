package org.controlacceso.clientegwt.client.firmas;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.dialogo.DialogoResultadoFirma;
import org.controlacceso.clientegwt.client.dialogo.PopupAdministrarDocumento;
import org.controlacceso.clientegwt.client.dialogo.PopupSolicitudCopiaSeguridad;
import org.controlacceso.clientegwt.client.dialogo.SolicitanteEmail;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTConsultaEvento;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.Tipo;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.DateUtils;
import org.controlacceso.clientegwt.client.util.PanelInfoDocumento;
import org.controlacceso.clientegwt.client.util.PopUpLabel;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.util.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class PanelFirmaManifiesto extends Composite implements SolicitanteEmail,
	 EventoGWTConsultaEvento.Handler, EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("PanelFirmaManifiesto");

	private static PanelFirmaManifiestoUiBinder uiBinder = GWT.create(PanelFirmaManifiestoUiBinder.class);
	interface PanelFirmaManifiestoUiBinder extends UiBinder<Widget, PanelFirmaManifiesto> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
        String labelInfoDocumentoOver();
        String panelInfoDocumentoOver();
    }

	@UiField HTML pageTitle;
	@UiField HTML piePagina;
    @UiField EditorStyle style;
    @UiField VerticalPanel panelContenidos;
    @UiField HorizontalPanel panelBarrarProgreso;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    @UiField Label fechaLimiteLabel;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel contentPanel;
    @UiField VerticalPanel contenidoPanel;
    @UiField HorizontalPanel buttonPanel;
    @UiField HorizontalPanel autorPanel;
    @UiField Label autorLabel;
    @UiField PanelInfoDocumento panelInfoDocumento;
    @UiField Label labelInfoDocumento;
    
    @UiField PopUpLabel administracionDocumentoLabel;
    
    private HTML contenidoEvento = null;
    private EventoSistemaVotacionJso evento;
    DialogoOperacionEnProgreso dialogoProgreso;
    private PopupSolicitudCopiaSeguridad popUpSolicitudCopiaSeguridad;
    CopiaSeguridadEventListener eventListener = new CopiaSeguridadEventListener();
    
    AdministrarEventoEventListener administrarEventoEventListener = new AdministrarEventoEventListener();
    private PopupAdministrarDocumento popupAdministrarDocumento;
    
    public PanelFirmaManifiesto() {
        initWidget(uiBinder.createAndBindUi(this));
        messagePanel.setVisible(false);
        panelInfoDocumento.setEventListener(eventListener);
		administracionDocumentoLabel.setListener(administrarEventoEventListener);
		panelInfoDocumento.setVisible(false);
		panelContenidos.setVisible(false);
        if(Browser.isAndroid()) {
        	piePagina.setHTML(Constantes.INSTANCIA.piePaginaFirmarDocumentoAndroid());
        } else {
        	piePagina.setHTML(Constantes.INSTANCIA.piePaginaFirmarDocumento());
        }
        BusEventos.addHandler(EventoGWTConsultaEvento.TYPE, this);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
    }
	
    @UiHandler("aceptarButton")
    void handleaceptarButton(ClickEvent e) {
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.FIRMA_MANIFIESTO_PDF.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.
				getUrlRecolectorFirmaPDF(evento.getId()));
		mensajeClienteFirma.setNombreDestinatarioFirma(
				PuntoEntrada.INSTANCIA.servidor.getNombre());
		mensajeClienteFirma.setUrlDocumento(ServerPaths.
				getUrlPDFManifiesto(evento.getId()));
		mensajeClienteFirma.setRespuestaConRecibo(false);
		mensajeClienteFirma.setEvento(evento);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
		if(!Browser.isAndroid()) setWidgetsStateFirmando(true);
    }
    
    @UiHandler("cerrarButton")
    void handleCancelButton(ClickEvent e) {
    	if(Window.confirm(Constantes.INSTANCIA.salirSinFirmarConfirmLabel())) {
        	History.newItem(HistoryToken.MANIFIESTOS.toString());
		}
    }
    
	private void setMessage (String message) {
		if(message == null || "".equals(message)) {
			messagePanel.setVisible(false);
		} else {
			messageLabel.setText(message);
	    	messagePanel.setVisible(true);	
		}
	}

    public void show(EventoSistemaVotacionJso documento) {
    	this.evento = documento;
    	if (documento == null) return;
        messagePanel.setVisible(false);
        contenidoPanel.clear();
        if(documento.getUsuario() != null)
        	autorLabel.setText(documento.getUsuario());
        else autorPanel.setVisible(false);
        pageTitle.setHTML(Constantes.INSTANCIA.manifiestoLabel() + " '" 
        		+ StringUtils.partirTexto(evento.getAsunto(), 
        		Constantes.MAX_NUM_CARACTERES_SUBJECT) + "'");
        fechaLimiteLabel.setText(
        		DateUtils.getSpanishStringFromDate(evento.getFechaFin()));
        contenidoEvento = new HTML(evento.getContenido());
        contenidoPanel.add(contenidoEvento);
        String mensajeInfo = null;
		if(documento.getNumeroTotalFirmas() == 1) {
			mensajeInfo = Constantes.INSTANCIA.mensajeInfoManifiestoUnaFirma();
		} else if(documento.getNumeroTotalFirmas() > 1) {
			mensajeInfo = Constantes.INSTANCIA.mensajeInfoManifiesto(documento.getNumeroTotalFirmas());
		}
        EventoSistemaVotacionJso.Estado estadoDocumento = documento.getEstadoEnumValue();
        if(estadoDocumento == EventoSistemaVotacionJso.Estado.ACTIVO) {
        	aceptarButton.setVisible(true);
        	cerrarButton.setVisible(true);
        	administracionDocumentoLabel.setVisible(true);
        	setMessage(null);
        } else {
        	aceptarButton.setVisible(false);
        	cerrarButton.setVisible(false);
        	administracionDocumentoLabel.setVisible(false);
        	String message = null;
        	switch(estadoDocumento) {
	        	case CANCELADO:
	        		message = Constantes.INSTANCIA.manfiestoCanceladoMsg();
	        		break;
	        	case FINALIZADO:
	        		message = Constantes.INSTANCIA.manfiestoFinalizadoMsg();
	        		break;
        	}
        	setMessage(message);
        }
		setInfoEventoMessage(mensajeInfo);
        panelBarrarProgreso.setVisible(false);
        panelContenidos.setVisible(true);
    }
    
	class CopiaSeguridadEventListener implements EventListener {

		@Override
		public void onBrowserEvent(Event event) {
			switch(DOM.eventGetType(event)) {
				case Event.ONCLICK:
					if(popUpSolicitudCopiaSeguridad == null || !popUpSolicitudCopiaSeguridad.isShowing()) {
						mostrarPopupSolicitudCopiaSeguridad(event.getClientX(), event.getClientY());
					} 
					break;
				case Event.ONMOUSEOVER:
					labelInfoDocumento.setStyleName(style.labelInfoDocumentoOver(), true);
					panelInfoDocumento.setStyleName(style.panelInfoDocumentoOver(), true);
					break;
			    case Event.ONMOUSEOUT:
			    	/*if(!DOM.isOrHasChild(getElement(), DOM.eventGetToElement(event))) {
			    		popUpSolicitudCopiaSeguridad.hide();
			    	}*/
			    	labelInfoDocumento.setStyleName(style.labelInfoDocumentoOver(), false);
			    	panelInfoDocumento.setStyleName(style.panelInfoDocumentoOver(), false);
			    	break;
			}
		}
		
	}
	
	private void setWidgetsStateFirmando(boolean publicando) {
		aceptarButton.setEnabled(!publicando);
		cerrarButton.setEnabled(!publicando);
		if(publicando) {
			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
			dialogoProgreso.show();
		} else {
			if(dialogoProgreso != null)	dialogoProgreso.hide();
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
		logger.info("recepcionManifiesto - evento:" + evento);
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
  			evento.setTipoEnumValue(Tipo.EVENTO_FIRMA);
  			popupAdministrarDocumento = new PopupAdministrarDocumento(evento);
  		}
  		popupAdministrarDocumento.setPopupPosition(clientX - 400, clientY);
  		popupAdministrarDocumento.show();
  	}
	
	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case FIRMA_MANIFIESTO_PDF:
				setWidgetsStateFirmando(false);
				if(200 == mensaje.getCodigoEstado()) {
					DialogoResultadoFirma dialogo = new DialogoResultadoFirma();
					dialogo.show(Constantes.INSTANCIA.operationResultMsg(evento.getAsunto()));
			    	History.newItem(HistoryToken.MANIFIESTOS.toString());
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							mensaje.getMensaje()));
					aceptarButton.setEnabled(true);
					cerrarButton.setVisible(true);
				}
				break;
			case SOLICITUD_COPIA_SEGURIDAD:
				setWidgetsStateFirmando(false);
				if(200 == mensaje.getCodigoEstado()) {
					setMessage(Constantes.INSTANCIA.mensajeSolicitudCopiaSeguridadOK());
				} else {
					setMessage(Constantes.INSTANCIA.mensajeError(
							mensaje.getMensaje()));
					aceptarButton.setEnabled(true);
					cerrarButton.setVisible(true);
				}
			default:
				break;
		}
		
	}


	public void setInfoEventoMessage(String infoEvento) {
		if(infoEvento == null) {
			panelInfoDocumento.setVisible(false);
		} else {
			labelInfoDocumento.setText(infoEvento);
			panelInfoDocumento.setVisible(true);	
		}
	}

	@Override
	public void procesarEmail(Integer id, String email) {
		logger.info("--- procesarEmail");
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.SOLICITUD_COPIA_SEGURIDAD.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlSolicitudCopiaSeguridad());
		mensajeClienteFirma.setEvento(evento);
		mensajeClienteFirma.setEmailSolicitante(email);
		if(!Browser.isAndroid()) setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
	}


}