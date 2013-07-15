package org.controlacceso.clientegwt.client.firmas;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.PuntoEntradaEditor;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeAplicacion;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.ActorConIPJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.RichTextToolbar;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.util.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;


public class PanelPublicacionManifiesto extends Composite 
		implements EventoGWTMensajeClienteFirma.Handler, EventoGWTMensajeAplicacion.Handler  {
	
    private static Logger logger = Logger.getLogger("PanelPublicacionManifiesto");

	private static PanelPublicacionManifiestoUiBinder uiBinder = GWT.create(PanelPublicacionManifiestoUiBinder.class);
	interface PanelPublicacionManifiestoUiBinder extends UiBinder<Widget, PanelPublicacionManifiesto> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
        String richTextArea();
        String buttonContainerPanel();
        String buttonContainerPanelAndroid();
    }
    
    @UiField org.controlacceso.clientegwt.client.Recursos res;
	@UiField HorizontalPanel pageTitle;
	@UiField HorizontalPanel buttonContainerPanel;
    @UiField VerticalPanel piePagina;
    @UiField EditorStyle style;
    @UiField VerticalPanel mainPanel;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    
    @UiField VerticalPanel contentPanel;
    @UiField VerticalPanel editorPanel;
    @UiField(provided = true) RichTextToolbar richTextToolbar;
    @UiField TextBox titulo;
    @UiField DateBox fechaFinalDateBox;
    RichTextArea richTextArea;
    DialogoOperacionEnProgreso dialogoProgreso;
    ActorConIPJso controlAcceso;

    private EventoSistemaVotacionJso evento;


    public PanelPublicacionManifiesto() {
		initGetEditorDataJS(this);
		initUpdateEditorDataJS(this);
    	richTextArea = new RichTextArea();
        richTextToolbar = new RichTextToolbar (richTextArea);
        initWidget(uiBinder.createAndBindUi(this));
        res.style().ensureInjected();
        richTextArea.setStyleName(style.richTextArea(), true);
        editorPanel.add(richTextArea);
        SubmitHandler sh = new SubmitHandler();
        titulo.addKeyDownHandler(sh);
        this.evento = EventoSistemaVotacionJso.create();
        messagePanel.setVisible(false);
        if(PuntoEntrada.INSTANCIA != null)
        	controlAcceso = PuntoEntrada.INSTANCIA.servidor;
        BusEventos.addHandler(EventoGWTMensajeClienteFirma.TYPE, this);
        BusEventos.addHandler(EventoGWTMensajeAplicacion.TYPE, this);
		if(PuntoEntradaEditor.INSTANCIA != null && 
				PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
			piePagina.setVisible(false);
			pageTitle.setVisible(false);
			cerrarButton.setVisible(false);
			
			buttonContainerPanel.setStyleName(style.buttonContainerPanel(), false);
			buttonContainerPanel.setStyleName(style.buttonContainerPanelAndroid(), true);
			
			MensajeClienteFirmaJso mensaje = MensajeClienteFirmaJso.create();
			mensaje.setCodigoEstado(MensajeClienteFirmaJso.SC_PING);
			Browser.setAndroidClientMessage(mensaje.toJSONString());
		}
    }

    @UiHandler("aceptarButton")
    void handleaceptarButton(ClickEvent e) {
    	if (!isValidForm()) return;
		evento.setAsunto(titulo.getText());
		logger.info("Formulario válido - richTextArea.getHTML: " + richTextArea.getHTML());
		evento.setContenido(richTextToolbar.getHTML());
		evento.setFechaFin(fechaFinalDateBox.getValue());
		setWidgetsStatePublicando(true);
		RequestHelper.doPost(ServerPaths.getUrlPublicarPDF(), 
				evento.toJSONString(), new PostDocumentRequestCallback());
    }
    
    @UiHandler("cerrarButton")
    void handleCancelButton(ClickEvent e) {
    	if(Window.confirm(Constantes.INSTANCIA.salirSinSalvarConfirmLabel())) {
        	History.newItem(HistoryToken.MANIFIESTOS.toString());
		}
    }
    
	private void setWidgetsStatePublicando(boolean publicando) {
		titulo.setEnabled(!publicando);
		richTextArea.setEnabled(!publicando);
		aceptarButton.setEnabled(!publicando);
		cerrarButton.setEnabled(!publicando);
		fechaFinalDateBox.setEnabled(!publicando);
		if(publicando) {
			if(PuntoEntradaEditor.INSTANCIA != null && 
					PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
				Browser.showProgressDialog(Constantes.INSTANCIA.publishingDocument());
			} else {
				if(dialogoProgreso == null) dialogoProgreso = new DialogoOperacionEnProgreso();
				dialogoProgreso.show();
			}
		} else {
			if(PuntoEntradaEditor.INSTANCIA != null && 
					PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
				MensajeClienteFirmaJso mensaje = MensajeClienteFirmaJso.create();
				mensaje.setCodigoEstado(MensajeClienteFirmaJso.SC_PING);
				Browser.setAndroidClientMessage(mensaje.toJSONString());
			} else if(dialogoProgreso != null) dialogoProgreso.hide();
		}
	}
    
	private boolean isValidForm() {
		setMessage(null);
		if (Validator.isTextBoxEmpty(titulo)) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			titulo.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			titulo.setStyleName(style.errorTextBox(), false);
		}
		if(fechaFinalDateBox.getValue() == null) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			fechaFinalDateBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			fechaFinalDateBox.setStyleName(style.errorTextBox(), false);
		}
		if(richTextArea.getHTML() == null || "".equals(richTextArea.getHTML())) {
			setMessage(Constantes.INSTANCIA.documentoSinTexto());
			richTextArea.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			richTextArea.setStyleName(style.errorTextBox(), false);
		}
		return true;
	}
    
	private void setMessage (String message) {
		if(message == null || "".equals(message)) messagePanel.setVisible(false);
		else {
			if(PuntoEntradaEditor.INSTANCIA != null && 
					PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
				Browser.setAndroidMsg(message);
			} else {
				messageLabel.setText(message);
		    	messagePanel.setVisible(true);
			}
		}
	}
	
    private class PostDocumentRequestCallback implements RequestCallback {
		
		public PostDocumentRequestCallback() { }

		@Override
    	public void onError(Request request, Throwable exception) {
			setWidgetsStatePublicando(false);
	    	ResultDialog resultDialog = new ResultDialog();
			resultDialog.show(Constantes.INSTANCIA.exceptionLbl(), 
					exception.getMessage(), Boolean.FALSE);  
        	logger.log(Level.SEVERE, exception.getMessage());
        	aceptarButton.setEnabled(true);
    	}

        @Override
    	public void onResponseReceived(Request request, Response response) {
        	if (response.getStatusCode() == Response.SC_OK) {
        		//logger.info(" response.getText(): " + response.getText());
        		evento.setId(new Integer(response.getText()));
        		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
        				Operacion.PUBLICACION_MANIFIESTO_PDF.toString(), 
        				MensajeClienteFirmaJso.SC_PROCESANDO);
        		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.
        				getUrlValidacionPDFPendientePublicacion(evento.getId()));
        		mensajeClienteFirma.setUrlDocumento(ServerPaths.
        				getUrlManifiesto(evento.getId()));
        		mensajeClienteFirma.setNombreDestinatarioFirma(controlAcceso.getNombre());
            	mensajeClienteFirma.setAsuntoMensajeFirmado(
            			Constantes.INSTANCIA.asuntoPublicarManifiesto());		
            	mensajeClienteFirma.setRespuestaConRecibo(false);
        		if(PuntoEntradaEditor.INSTANCIA != null && 
        				PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
        			Browser.setAndroidClientMessage(mensajeClienteFirma.toJSONString());
        			setWidgetsStatePublicando(false);
            	} else {
            		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
            	}
            	return;
        	} else {
        		setWidgetsStatePublicando(false);
				setMessage(Constantes.INSTANCIA.publicacionManifiestoERROR() +
						" - " + response.getText());
    		}
    	}
    }
    
    private class SubmitHandler implements KeyDownHandler {
		@Override
		public void onKeyDown(KeyDownEvent event) {
			if (KeyCodes.KEY_ENTER == event.getNativeKeyCode()) {
				handleaceptarButton(null);
			}		
		}
	}

	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case PUBLICACION_MANIFIESTO_PDF:
				dialogoProgreso.hide();
				if(200 == mensaje.getCodigoEstado()) {
					ResultDialog dialogo = new ResultDialog();
					dialogo.show(Constantes.INSTANCIA.publicacionManifiestoOK());
			    	History.newItem(HistoryToken.MANIFIESTOS.toString());
				} else if (MensajeClienteFirmaJso.SC_CANCELADO== mensaje.getCodigoEstado()) {
					setWidgetsStatePublicando(false);
				} else {
					setWidgetsStatePublicando(false);
					setMessage(Constantes.INSTANCIA.publicacionManifiestoERROR() +
							" - " + mensaje.getMensaje());
				}
				break;
			default:
				break;
		}
		
	}

	@Override
	public void procesarMensaje(EventoGWTMensajeAplicacion evento) {
		if(evento.token.equals(HistoryToken.OBTENIDA_INFO_SERVIDOR))
			this.controlAcceso = (ActorConIPJso) evento.contenidoMensaje;
	}
	
	public void updateEditorData(String editorData) {
		logger.info(" -- updateEditorData - editorData: " + editorData);
		MensajeClienteFirmaJso mensaje = MensajeClienteFirmaJso.getMensajeClienteFirma(editorData);
		if(mensaje != null && mensaje.getEvento() != null) {
			logger.info(" -- asunto: " + evento.getAsunto());
			logger.info(" -- contenido: " + evento.getContenido());
			logger.info(" -- fecha fin: " + evento.getFechaFin());
			if(evento.getAsunto() != null)
				titulo.setText(evento.getAsunto());
			if(evento.getContenido() != null)
				richTextArea.setHTML(evento.getContenido());
			if(evento.getFechaFin() != null)
				fechaFinalDateBox.setValue(evento.getFechaFin());
		}
	}
	
	public void getEditorData(String sessionId) {
		logger.info(" -- getEditorData - sessionId: " + sessionId);
		EventoSistemaVotacionJso evento = EventoSistemaVotacionJso.create();
		evento.setAsunto(titulo.getText());
		evento.setContenido(richTextToolbar.getHTML());
		evento.setFechaFin(fechaFinalDateBox.getValue());
		MensajeClienteFirmaJso mensaje = MensajeClienteFirmaJso.create();
		mensaje.setEvento(evento);
		mensaje.setSessionId(sessionId);
		setEditorData(mensaje.toJSONString());
	}
	
	public static native void setEditorData(String msg) /*-{
		$wnd.setEditorData(msg);
	}-*/;	
	
    private native void initGetEditorDataJS (PanelPublicacionManifiesto panelPub) /*-{
		$wnd.getEditorData = function (sessionId) {
	    	return panelPub.@org.controlacceso.clientegwt.client.firmas.PanelPublicacionManifiesto::getEditorData(Ljava/lang/String;)(sessionId);
		};
	}-*/;
    
    private native void initUpdateEditorDataJS (PanelPublicacionManifiesto panelPub) /*-{
		$wnd.updateEditorData = function (editorData) {
	    	return panelPub.@org.controlacceso.clientegwt.client.firmas.PanelPublicacionManifiesto::updateEditorData(Ljava/lang/String;)(editorData);
		};
	}-*/;
    
	
}