package org.controlacceso.clientegwt.client.votaciones;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.PuntoEntradaEditor;
import org.controlacceso.clientegwt.client.Recursos;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeAplicacion;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.ActorConIPJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.OpcionDeEventoJso;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.PopUpLabel;
import org.controlacceso.clientegwt.client.util.RichTextToolbar;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.util.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;


public class PanelPublicacionVotacion extends Composite implements 
	EventoGWTMensajeClienteFirma.Handler, EventoGWTMensajeAplicacion.Handler {
	
    private static Logger logger = Logger.getLogger("PanelPublicacionVotacion");

	private static PanelPublicacionVotacionUiBinder uiBinder = 
			GWT.create(PanelPublicacionVotacionUiBinder.class);
	interface PanelPublicacionVotacionUiBinder extends UiBinder<Widget, PanelPublicacionVotacion> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
        String richTextArea();
        String submitButtonsPanel();
        String buttonContainerPanelAndroid();
    }

	@UiField HorizontalPanel pageTitle;
	@UiField HorizontalPanel submitButtonsPanel;
    @UiField EditorStyle style;
    @UiField VerticalPanel mainPanel;
    @UiField Label messageLabel;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField PushButton anyadirOpcionButton;
    @UiField VerticalPanel contentPanel;
    @UiField VerticalPanel editorPanel;
    @UiField VerticalPanel messagePanel;
    @UiField(provided = true) RichTextToolbar richTextToolbar;
    @UiField TextBox titulo;
    @UiField DateBox fechaIncioDateBox;
    @UiField DateBox fechaFinalDateBox;
    
    @UiField ListBox listaCentrosControl;
    @UiField PopUpLabel labelCentrosControl;
    @UiField Label imageInfoLabel;
    @UiField PanelPublicacionOpcionesVotacion panelOpciones;
    @UiField VerticalPanel piePagina;
    
    RichTextArea richTextArea;
    PopupCentrosDeControl popUpCentrosDeControl;
    ActorConIPJso controlAcceso;
    DialogoOperacionEnProgreso dialogoProgreso;

    private EventoSistemaVotacionJso evento;
    private HashMap<Integer, ActorConIPJso> centrosControlMap;

    public PanelPublicacionVotacion() {
    	richTextArea = new RichTextArea();
        richTextToolbar = new RichTextToolbar (richTextArea);
        initWidget(uiBinder.createAndBindUi(this));
        listaCentrosControl.addItem(Constantes.INSTANCIA.centroControlListBoxFirstItem());
        richTextArea.setStyleName(style.richTextArea(), true);
        editorPanel.add(richTextArea);
        SubmitHandler sh = new SubmitHandler();
        titulo.addKeyDownHandler(sh);
        this.evento = EventoSistemaVotacionJso.create();
        messagePanel.setVisible(false);
        BusEventos.addHandler(EventoGWTMensajeClienteFirma.TYPE, this);
        BusEventos.addHandler(EventoGWTMensajeAplicacion.TYPE, this);
        LabelCentroControlEventHandler handler = new LabelCentroControlEventHandler();
        labelCentrosControl.setListener(handler);
		Image infoImage = new Image(Recursos.INSTANCIA.info_16x16_Image());
		DOM.insertBefore(imageInfoLabel.getElement(), infoImage.getElement(), 
				DOM.getFirstChild(imageInfoLabel.getElement()));
		imageInfoLabel.addClickHandler(new ClickHandler() {	
			@Override
			public void onClick(ClickEvent event) {
				if(PuntoEntradaEditor.INSTANCIA != null) {
					logger.info(" --- Funcionalidad deshabilitada para clientes android --- ");
					return;
				}
				mostrarPopUpCentroDeControl(event.getClientX(), event.getClientY());
		}});
		if(PuntoEntrada.INSTANCIA != null)
			setInfoServidor(PuntoEntrada.INSTANCIA.servidor);
		else if(PuntoEntradaEditor.INSTANCIA != null)
			setInfoServidor(PuntoEntradaEditor.INSTANCIA.servidor);
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
		
    private void setInfoServidor(ActorConIPJso controlAcceso) {
    	this.controlAcceso = controlAcceso;
    	if(controlAcceso == null) return;
		List<ActorConIPJso>  centrosDeControl = controlAcceso.getCentrosDeControlList();
		if(centrosDeControl == null) return;
		centrosControlMap = new HashMap<Integer, ActorConIPJso>();
		int counter = 1;
		for(ActorConIPJso centroControl:centrosDeControl) {
			listaCentrosControl.addItem(centroControl.getNombre() + " - " + 
					centroControl.getServerURL());
			centrosControlMap.put(counter++, centroControl);
		}
    }

    
    @UiHandler("aceptarButton")
    void handleaceptarButton(ClickEvent e) {
    	if (!isValidForm()) return;
		evento.setAsunto(titulo.getText());
		logger.info("Formulario válido - richTextArea.getHTML: " + richTextArea.getHTML());
		evento.setContenido(richTextToolbar.getHTML());
		evento.setFechaFin(fechaFinalDateBox.getValue());
		evento.setFechaInicio(fechaIncioDateBox.getValue());
		ActorConIPJso centroControl = centrosControlMap.get(listaCentrosControl.getSelectedIndex());
		evento.setOpcionDeEventoList(panelOpciones.getOpciones());
		evento.setCentroControl(centroControl);
		
    	MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create();
    	mensajeClienteFirma.setCodigoEstado(MensajeClienteFirmaJso.SC_PROCESANDO);
    	mensajeClienteFirma.setOperacion(MensajeClienteFirmaJso.Operacion.
    			PUBLICACION_VOTACION_SMIME.toString());
    	mensajeClienteFirma.setContenidoFirma(evento);
    	mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlPublicacionVotacion());
    	mensajeClienteFirma.setNombreDestinatarioFirma(controlAcceso.getNombre());
    	mensajeClienteFirma.setAsuntoMensajeFirmado(
    			Constantes.INSTANCIA.asuntoPublicarVotacion());	
    	mensajeClienteFirma.setRespuestaConRecibo(true);
    	mensajeClienteFirma.setUrlTimeStampServer(ServerPaths.getUrlTimeStampServer());
		if(PuntoEntradaEditor.INSTANCIA != null && 
				PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
			Browser.setAndroidClientMessage(mensajeClienteFirma.toJSONString());
    	} else {
    		setWidgetsStatePublicando(true);
    		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
    	}
    }
	
    @UiHandler("cerrarButton")
    void handleCancelButton(ClickEvent e) {
    	if(Window.confirm(Constantes.INSTANCIA.salirSinSalvarConfirmLabel())) {
        	History.newItem(HistoryToken.VOTACIONES.toString());
		}
    }
    
    @UiHandler("anyadirOpcionButton")
    void handleAnyadirOpcionButton(ClickEvent e) {
    	DialogoCrearOpciondeVotacion dialogoOpcion = new DialogoCrearOpciondeVotacion(this);
    	dialogoOpcion.show();
    }
    
    protected void anyadirOpcionDeVotacion(OpcionDeEventoJso opcion) {
    	panelOpciones.anyadirOpcion(opcion);
    }
	
	private void setWidgetsStatePublicando(boolean publicando) {
		titulo.setEnabled(!publicando);
		richTextArea.setEnabled(!publicando);
		anyadirOpcionButton.setEnabled(!publicando);
		aceptarButton.setEnabled(!publicando);
		cerrarButton.setEnabled(!publicando);
		fechaFinalDateBox.setEnabled(!publicando);
		fechaIncioDateBox.setEnabled(!publicando);
		listaCentrosControl.setEnabled(!publicando);
		panelOpciones.setEnabled(!publicando);
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
		if(fechaIncioDateBox.getValue() == null) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			fechaIncioDateBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			fechaIncioDateBox.setStyleName(style.errorTextBox(), false);
		}
		if(fechaFinalDateBox.getValue() == null) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			fechaFinalDateBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			fechaFinalDateBox.setStyleName(style.errorTextBox(), false);
		}
		if(fechaIncioDateBox.getValue().after(fechaFinalDateBox.getValue())) {
			setMessage(Constantes.INSTANCIA.fechaInicioAfterFechaFinalMsg());
			fechaIncioDateBox.setStyleName(style.errorTextBox(), true);
			fechaFinalDateBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			fechaFinalDateBox.setStyleName(style.errorTextBox(), false);
			fechaIncioDateBox.setStyleName(style.errorTextBox(), false);
		}
		if(richTextArea.getHTML() == null || "".equals(richTextArea.getHTML())) {
			setMessage(Constantes.INSTANCIA.documentoSinTexto());
			richTextArea.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			richTextArea.setStyleName(style.errorTextBox(), false);
		}if(listaCentrosControl.getSelectedIndex() == 0) {
			setMessage(Constantes.INSTANCIA.centroControlNotSelected());
			listaCentrosControl.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			listaCentrosControl.setStyleName(style.errorTextBox(), false);
		} if (panelOpciones.getOpciones() == null || 
				panelOpciones.getOpciones().size() < 2) {
			setMessage(Constantes.INSTANCIA.documentoSinOpciones());
			anyadirOpcionButton.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			anyadirOpcionButton.setStyleName(style.errorTextBox(), false);
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
			case PUBLICACION_VOTACION_SMIME:
				dialogoProgreso.hide();
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					ResultDialog dialogo = new ResultDialog();
					dialogo.show(Constantes.INSTANCIA.publicacionVotacionOK());
					History.newItem(HistoryToken.VOTACIONES.toString());
				} else if (MensajeClienteFirmaJso.SC_CANCELADO== mensaje.getCodigoEstado()) {
					setWidgetsStatePublicando(false);
				} else {
					setWidgetsStatePublicando(false);
					setMessage(Constantes.INSTANCIA.publicacionVotacionERROR() +
							" - " + mensaje.getMensaje());
				}
				break;
			default:
				break;
		}
	}
	
	private void mostrarPopUpCentroDeControl(int clientX, int clientY) {
		if(popUpCentrosDeControl == null) {
			popUpCentrosDeControl = new PopupCentrosDeControl();
		}
		popUpCentrosDeControl.setPopupPosition(clientX, clientY);
		popUpCentrosDeControl.show();
	}
	
	class LabelCentroControlEventHandler implements EventListener {

		@Override
		public void onBrowserEvent(Event event) {
			switch(DOM.eventGetType(event)) {
				case Event.ONCLICK:
				//case Event.ONMOUSEOVER:
					mostrarPopUpCentroDeControl(event.getClientX(), event.getClientY());
					break;
			    case Event.ONMOUSEOUT:
			    	break;
			}
		}
		
	}

	@Override
	public void procesarMensaje(EventoGWTMensajeAplicacion evento) {
		if(evento.token.equals(HistoryToken.OBTENIDA_INFO_SERVIDOR))
			setInfoServidor((ActorConIPJso) evento.contenidoMensaje);
	}

}