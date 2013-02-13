package org.controlacceso.clientegwt.client.reclamaciones;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.PuntoEntradaEditor;
import org.controlacceso.clientegwt.client.dialogo.ConfirmacionListener;
import org.controlacceso.clientegwt.client.dialogo.DialogoConfirmacion;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.CampoDeEventoJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso.Cardinalidad;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.PopUpLabel;
import org.controlacceso.clientegwt.client.util.RichTextToolbar;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.util.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;


public class PanelPublicacionReclamacion extends Composite 
	implements EventoGWTMensajeClienteFirma.Handler, ConfirmacionListener {
	
    private static Logger logger = Logger.getLogger("PanelPublicacionReclamacion");

	private static PanelPublicacionReclamacionUiBinder uiBinder = GWT.create(PanelPublicacionReclamacionUiBinder.class);
	interface PanelPublicacionReclamacionUiBinder extends UiBinder<Widget, PanelPublicacionReclamacion> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
        String richTextArea();
        String submitButtonsPanel();
        String buttonContainerPanelAndroid();
    }

	@UiField HorizontalPanel pageTitle;
	@UiField HorizontalPanel submitButtonsPanel;
    @UiField VerticalPanel piePagina;
    @UiField EditorStyle style;
    @UiField VerticalPanel messagePanel;
    @UiField Label messageLabel;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel editorPanel;
    @UiField(provided = true) RichTextToolbar richTextToolbar;
    @UiField TextBox titulo;
    @UiField DateBox fechaLimiteDateBox;
    RichTextArea richTextArea;

    @UiField CheckBox multiplesRepresentacionesCheckBox;
    @UiField CheckBox permisoSolicitudBackupCheckbox;
    @UiField PopUpLabel labelCampoReclamacion;
    
    @UiField(provided=true) PanelPublicacionCamposReclamacion panelCampos;
    PopUpCampoReclamacion popUpCampoReclamacion;
    DialogoOperacionEnProgreso dialogoProgreso;
    
    LabelCamporeclamacionEventHandler handler = new LabelCamporeclamacionEventHandler();

    public PanelPublicacionReclamacion() {
    	richTextArea = new RichTextArea();
        richTextToolbar = new RichTextToolbar (richTextArea);
        panelCampos = new PanelPublicacionCamposReclamacion(PanelCampoReclamacion.Modo.CREAR);
        initWidget(uiBinder.createAndBindUi(this));
        richTextArea.setStyleName(style.richTextArea(), true);
        editorPanel.add(richTextArea);
        SubmitHandler sh = new SubmitHandler();
        titulo.addKeyDownHandler(sh);
        messagePanel.setVisible(false);
        labelCampoReclamacion.setListener(handler);
        permisoSolicitudBackupCheckbox.setValue(Boolean.TRUE);
        BusEventos.addHandler(EventoGWTMensajeClienteFirma.TYPE, this);
		if(PuntoEntradaEditor.INSTANCIA != null && 
				PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
			piePagina.setVisible(false);
			pageTitle.setVisible(false);
			cerrarButton.setVisible(false);
			submitButtonsPanel.setStyleName(style.submitButtonsPanel(), false);
			submitButtonsPanel.setStyleName(style.buttonContainerPanelAndroid(), true);
			
			MensajeClienteFirmaJso mensaje = MensajeClienteFirmaJso.create();
			mensaje.setCodigoEstado(MensajeClienteFirmaJso.SC_PING);
			Browser.setAndroidClientMessage(mensaje.toJSONString());
		}
    }
    
	class LabelCamporeclamacionEventHandler implements EventListener {

		@Override
		public void onBrowserEvent(Event event) {
			switch(DOM.eventGetType(event)) {
				case Event.ONCLICK:
				case Event.ONMOUSEOVER:
					mostrarPopUpAnyadirReclamacion(event.getClientX(), event.getClientY());
					break;
			    case Event.ONMOUSEOUT:
			    	break;
			}
		}
		
	}
    
    @UiHandler("aceptarButton")
    void handleaceptarButton(ClickEvent e) {
    	if (!isValidForm()) return;
        EventoSistemaVotacionJso evento = EventoSistemaVotacionJso.create();
		evento.setAsunto(titulo.getText());
		logger.info("Formulario válido - richTextArea.getHTML: " + richTextArea.getHTML());
		evento.setContenido(richTextToolbar.getHTML());
		evento.setFechaFin(fechaLimiteDateBox.getValue());
		if(multiplesRepresentacionesCheckBox.getValue()) 
			evento.setCardinalidadEnumValue(Cardinalidad.MULTIPLES);
		else evento.setCardinalidadEnumValue(Cardinalidad.UNA);
		evento.setCopiaSeguridadDisponible(permisoSolicitudBackupCheckbox.getValue());
		evento.setCampoDeEventoList(panelCampos.getCampos());
		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
				Operacion.PUBLICACION_RECLAMACION_SMIME.toString(), 
				MensajeClienteFirmaJso.SC_PROCESANDO);
		mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.
				getUrlPublicacionReclamacion());
    	mensajeClienteFirma.setAsuntoMensajeFirmado(
    			Constantes.INSTANCIA.asuntoPublicarReclamacion());
    	mensajeClienteFirma.setContenidoFirma(evento);
    	mensajeClienteFirma.setRespuestaConRecibo(true);
		if(PuntoEntradaEditor.INSTANCIA != null && 
				PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
    		mensajeClienteFirma.setNombreDestinatarioFirma(
    				PuntoEntradaEditor.INSTANCIA.servidor.getNombre());
			Browser.setAndroidClientMessage(mensajeClienteFirma.toJSONString());
    	} else {
    		mensajeClienteFirma.setNombreDestinatarioFirma(
    				PuntoEntrada.INSTANCIA.servidor.getNombre());
    		setWidgetsStatePublicando(true);
    		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
    	}
    }
    
    @UiHandler("cerrarButton")
    void handleCancelButton(ClickEvent e) {
    	DialogoConfirmacion dialogoConfirmacion = new DialogoConfirmacion(null, this);
    	dialogoConfirmacion.show(Constantes.INSTANCIA.salirSinSalvarConfirmLabel());
    }
    
	@Override
	public void confirmed(Integer id) {
		History.newItem(HistoryToken.RECLAMACIONES.toString());
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
		if(fechaLimiteDateBox.getValue() == null) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			fechaLimiteDateBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			fechaLimiteDateBox.setStyleName(style.errorTextBox(), false);
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
	
	private void setWidgetsStatePublicando(boolean publicando) {
		titulo.setEnabled(!publicando);
		richTextArea.setEnabled(!publicando);
		if(publicando) labelCampoReclamacion.setListener(null);
		else labelCampoReclamacion.setListener(handler);
		aceptarButton.setEnabled(!publicando);
		cerrarButton.setEnabled(!publicando);
		fechaLimiteDateBox.setEnabled(!publicando);
		panelCampos.setEnabled(!publicando);
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
    
    private class SubmitHandler implements KeyDownHandler {
		@Override
		public void onKeyDown(KeyDownEvent event) {
			if (KeyCodes.KEY_ENTER == event.getNativeKeyCode()) {
				handleaceptarButton(null);
			}		
		}
	}

	private void mostrarPopUpAnyadirReclamacion(int clientX, int clientY) {
		if(popUpCampoReclamacion == null) {
			popUpCampoReclamacion = new PopUpCampoReclamacion(this);
		}
		popUpCampoReclamacion.setPopupPosition(clientX, clientY);
		popUpCampoReclamacion.show();
	}
    
	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case PUBLICACION_RECLAMACION_SMIME:
				setWidgetsStatePublicando(false);
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					Window.alert(Constantes.INSTANCIA.publicacionReclamacionOK());
			    	History.newItem(HistoryToken.RECLAMACIONES.toString());
				} else if (MensajeClienteFirmaJso.SC_CANCELADO== mensaje.getCodigoEstado()) {
				} else {
					setMessage(Constantes.INSTANCIA.publicacionReclamacionERROR() +
							" - " + mensaje.getMensaje());
				}
				break;
			default:
				break;
		}
		
	}

	public void anyadirCampoReclamacion(CampoDeEventoJso campoCreado) {
		panelCampos.anyadirCampo(campoCreado);
	}

}