package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.dialogo.PopupAdministrarDocumento;
import org.controlacceso.clientegwt.client.dialogo.PopupSolicitudCopiaSeguridad;
import org.controlacceso.clientegwt.client.dialogo.SolicitanteEmail;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTConsultaEvento;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.modelo.Tipo;
import org.controlacceso.clientegwt.client.panel.PanelCentral;
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


public class PanelFirmaReclamacion extends Composite implements SolicitanteEmail,
	EventoGWTConsultaEvento.Handler, EventoGWTMensajeClienteFirma.Handler {
	
    private static Logger logger = Logger.getLogger("PanelFirmaReclamacion");

	private static PanelFirmaReclamacionUiBinder uiBinder = GWT.create(PanelFirmaReclamacionUiBinder.class);
	interface PanelFirmaReclamacionUiBinder extends UiBinder<Widget, PanelFirmaReclamacion> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
        String labelInfoDocumentoOver();
        String panelInfoDocumentoOver();
    }

	@UiField HTML pageTitle;
    @UiField EditorStyle style;
    @UiField VerticalPanel panelContenidos;
    @UiField HorizontalPanel panelBarrarProgreso;
    @UiField Label messageLabel;
    @UiField VerticalPanel messagePanel;
    @UiField Label fechaLimiteLabel;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel contentPanel;
    @UiField VerticalPanel contenidoPanel;
    @UiField HorizontalPanel buttonPanel;
    @UiField HorizontalPanel autorPanel;
    @UiField Label autorLabel;
    @UiField Label labelInfoDocumento;
    @UiField PanelInfoDocumento panelInfoDocumento;
    @UiField(provided=true) PanelPublicacionCamposReclamacion panelCampos;
    
    @UiField PopUpLabel administracionDocumentoLabel;

    private HTML contenidoEvento = null;
    private EventoSistemaVotacionJso evento;
    DialogoOperacionEnProgreso dialogoProgreso;
    private PopupSolicitudCopiaSeguridad popUpSolicitudCopiaSeguridad;

    CopiaSeguridadEventListener infoDocumentoEventListener = new CopiaSeguridadEventListener();
    AdministrarEventoEventListener administrarEventoEventListener = new AdministrarEventoEventListener();
    private PopupAdministrarDocumento popupAdministrarDocumento;
    
    public PanelFirmaReclamacion() {
        panelCampos = new PanelPublicacionCamposReclamacion(
        		PanelCampoReclamacion.Modo.EDITAR);
        initWidget(uiBinder.createAndBindUi(this));
        messagePanel.setVisible(false);
		panelInfoDocumento.setVisible(false);
		panelContenidos.setVisible(false);
		administracionDocumentoLabel.setListener(administrarEventoEventListener);
		panelInfoDocumento.setEventListener(infoDocumentoEventListener);
        BusEventos.addHandler(EventoGWTConsultaEvento.TYPE, this);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
    }
	
    @UiHandler("aceptarButton")
    void handleaceptarButton(ClickEvent e) {
    	if(!panelCampos.isValidForm()) {
    		setMessage(Constantes.INSTANCIA.emptyFieldException());
    		return;
    	}
    	setMessage(null);
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.FIRMA_RECLAMACION_SMIME.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlRecolectorReclamaciones());
		mensajeClienteFirma.setAsuntoMensajeFirmado(
				Constantes.INSTANCIA.asuntoFirmaReclamacion(evento.getAsunto()));
		evento.setCampoDeEventoList(panelCampos.getCampos());
		mensajeClienteFirma.setEvento(evento);
		mensajeClienteFirma.setNombreDestinatarioFirma(PuntoEntrada.INSTANCIA.servidor.getNombre());
		mensajeClienteFirma.setContenidoFirma(evento);
		mensajeClienteFirma.setRespuestaConRecibo(true);
		setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
    }
    
    @UiHandler("cerrarButton")
    void handleCancelButton(ClickEvent e) {
    	if(Window.confirm(Constantes.INSTANCIA.salirSinFirmarConfirmLabel())) {
        	History.newItem(HistoryToken.RECLAMACIONES.toString());
		}
    }
    
	private void setMessage (String message) {
		if(message == null || "".equals(message)) {
			messagePanel.setVisible(false);
		}  else {
	    	messageLabel.setText(message);
	    	messagePanel.setVisible(true);
		}
	}

    public void show(EventoSistemaVotacionJso documento) {
    	this.evento = documento;
    	if (documento == null) return;
    	setMessage(null);
    	panelCampos.anyadirCampos(documento.getCampoDeEventoList());
        contenidoPanel.clear();
        pageTitle.setHTML(Constantes.INSTANCIA.reclamacionLabel() + " '" + 
        		StringUtils.partirTexto(evento.getAsunto(), 
        		Constantes.MAX_NUM_CARACTERES_SUBJECT)  + "'");
        if(evento.getUsuario() != null)
        	autorLabel.setText(documento.getUsuario());
        else autorPanel.setVisible(false);
        fechaLimiteLabel.setText(
        		DateUtils.getSpanishStringFromDate(evento.getFechaFin()));
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
	        		message = Constantes.INSTANCIA.reclamacionCanceladaMsg();
	        		break;
	        	case FINALIZADO:
	        		message = Constantes.INSTANCIA.reclamacionFinalizadaMsg();
	        		break;
        	}
        	setMessage(message);
        }
        contenidoEvento = new HTML(evento.getContenido());
        contenidoPanel.add(contenidoEvento);
        String mensajeInfo = null;
		if(documento.getNumeroTotalFirmas() == 1) {
			mensajeInfo = Constantes.INSTANCIA.mensajeInfoReclamacionUnaFirma();
		} else if(documento.getNumeroTotalFirmas() > 1) {
			mensajeInfo = Constantes.INSTANCIA.mensajeInfoReclamacion(documento.getNumeroTotalFirmas());
		}
		setInfoEventoMessage(mensajeInfo);
		panelBarrarProgreso.setVisible(false);
		panelContenidos.setVisible(true);
    }

  	private void setWidgetsStateFirmando(boolean publicando) {
  		aceptarButton.setEnabled(!publicando);
  		cerrarButton.setEnabled(!publicando);
  		if(publicando) {
  			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
  			dialogoProgreso.show();
  		} else {
  			if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
  			dialogoProgreso.hide();
  		}
  	}

  	class CopiaSeguridadEventListener implements EventListener {

  		@Override
  		public void onBrowserEvent(Event event) {
  			switch(DOM.eventGetType(event)) {
  				case Event.ONCLICK:
  					mostrarPopupSolicitudCopiaSeguridad(event.getClientX(), event.getClientY());
  					break;
				case Event.ONMOUSEOVER:
					labelInfoDocumento.setStyleName(style.labelInfoDocumentoOver(), true);
					panelInfoDocumento.setStyleName(style.panelInfoDocumentoOver(), true);
					break;  					
  			    case Event.ONMOUSEOUT:
					labelInfoDocumento.setStyleName(style.labelInfoDocumentoOver(), false);
					panelInfoDocumento.setStyleName(style.panelInfoDocumentoOver(), false);
  			    	break;
  			}
  		}
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
  			evento.setTipoEnumValue(Tipo.EVENTO_RECLAMACION);
  			popupAdministrarDocumento = new PopupAdministrarDocumento(evento);
  		}
  		popupAdministrarDocumento.setPopupPosition(clientX - 400, clientY);
  		popupAdministrarDocumento.show();
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
	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case FIRMA_RECLAMACION_SMIME:
				setWidgetsStateFirmando(false);
				if(200 == mensaje.getCodigoEstado()) {
			    	History.newItem(HistoryToken.RECLAMACIONES.toString());
				} else if(0 == mensaje.getCodigoEstado()) {} 
				else {
					String mensajeError = (mensaje.getMensaje() == null)? "" :mensaje.getMensaje();
					setMessage(Constantes.INSTANCIA.mensajeError(mensajeError));
					aceptarButton.setEnabled(true);
					cerrarButton.setVisible(true);
				}
				break;
				
			case CANCELAR_EVENTO:
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					History.newItem(HistoryToken.RECLAMACIONES.toString());
					logger.info("Cancelado envento '" + evento.getAsunto() + "'");	
				}
				break;				
			case SOLICITUD_COPIA_SEGURIDAD:
				setWidgetsStateFirmando(false);
				if(200 == mensaje.getCodigoEstado()) {
					setMessage(Constantes.INSTANCIA.mensajeSolicitudCopiaSeguridadOK());
				} else if(0 == mensaje.getCodigoEstado()) {}
				else {
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
	public void procesarEmail(String email) {
		logger.info("--- procesarEmail");
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.SOLICITUD_COPIA_SEGURIDAD.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlSolicitudCopiaSeguridad());
		mensajeClienteFirma.setEvento(PanelCentral.INSTANCIA.getEventoSeleccionado());
    	mensajeClienteFirma.setNombreDestinatarioFirma(
    			PuntoEntrada.INSTANCIA.servidor.getNombre());
		mensajeClienteFirma.setEmailSolicitante(email);
		setWidgetsStateFirmando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
	}

}