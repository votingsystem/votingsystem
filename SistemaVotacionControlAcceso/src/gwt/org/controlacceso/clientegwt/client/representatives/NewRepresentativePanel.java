package org.controlacceso.clientegwt.client.representatives;

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
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.Tipo;
import org.controlacceso.clientegwt.client.modelo.UsuarioJso;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.RichTextToolbar;
import org.controlacceso.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class NewRepresentativePanel extends Composite implements 
	EventoGWTMensajeClienteFirma.Handler, EventoGWTMensajeAplicacion.Handler {
	
	public enum EditorMode {NEW, EDIT}
	
    private static Logger logger = Logger.getLogger("NewRepresentativePanel");

	private static NewRepresentativePanelUiBinder uiBinder = 
			GWT.create(NewRepresentativePanelUiBinder.class);
	interface NewRepresentativePanelUiBinder extends UiBinder<Widget, NewRepresentativePanel> { }

    interface EditorStyle extends CssResource {
        String errorTextBox();
        String textBox();
        String richTextArea();
        String submitButtonsPanel();
        String buttonContainerPanelAndroid();
        String imageMissing();
        String pageTitle();
    }

	@UiField HorizontalPanel pageTitle;
	@UiField HorizontalPanel submitButtonsPanel;
    @UiField EditorStyle style;
    @UiField VerticalPanel mainPanel;
    @UiField Label messageLabel;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel contentPanel;
    @UiField VerticalPanel editorPanel;
    @UiField VerticalPanel messagePanel;
    @UiField(provided = true) RichTextToolbar richTextToolbar;
    @UiField VerticalPanel piePagina;
    //@UiField HorizontalPanel uploadImagePanel;
    //@UiField FileUpload fileUpload;
    
    private RichTextArea richTextArea;
    private ActorConIPJso controlAcceso;
    private DialogoOperacionEnProgreso dialogoProgreso;
    private UsuarioJso usuario;
    private EditorMode editorMode;


    public NewRepresentativePanel() {
    	richTextArea = new RichTextArea();
        richTextToolbar = new RichTextToolbar (richTextArea);
        initWidget(uiBinder.createAndBindUi(this));
        richTextArea.setStyleName(style.richTextArea(), true);
        editorPanel.add(richTextArea);
        SubmitHandler sh = new SubmitHandler();
        messagePanel.setVisible(false);
        BusEventos.addHandler(EventoGWTMensajeClienteFirma.TYPE, this);
        BusEventos.addHandler(EventoGWTMensajeAplicacion.TYPE, this);
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
    }

    public void setEditorMode (EditorMode editorMode, Integer representativeId) {
    	HTML pageTitleHTML = null;
    	richTextArea.setHTML("");
    	this.editorMode = editorMode;
    	switch(editorMode) {
	    	case EDIT:
	    		pageTitleHTML = new HTML(Constantes.INSTANCIA.editRepresentativeCaption());
	    		RequestHelper.doGet(ServerPaths.getRepresentativeDetailsMapUrl(representativeId), 
	    				new ServerRequestCallback());
	    		richTextArea.setEnabled(false);
	    		break;
	    	case NEW:
	    		pageTitleHTML = new HTML(Constantes.INSTANCIA.newRepresentativeLabel()); 
	    		richTextArea.setEnabled(true);
	    		break;
    	}
		pageTitleHTML.setStyleName(style.pageTitle(), true);
		pageTitle.clear();
		pageTitle.add(pageTitleHTML);
    }
    
    @UiHandler("aceptarButton")
    void handleaceptarButton(ClickEvent e) {
    	if (!isValidForm()) return;
		logger.info("Formulario válido - richTextArea.getHTML: " + richTextArea.getHTML());
    	MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create();
    	mensajeClienteFirma.setCodigoEstado(MensajeClienteFirmaJso.SC_PROCESANDO);
    	mensajeClienteFirma.setOperacion(MensajeClienteFirmaJso.Operacion.
    			NEW_REPRESENTATIVE.toString());
    	JSONObject contenidoFirma = new JSONObject();
    	contenidoFirma.put("representativeInfo", new JSONString(richTextArea.getHTML()));
    	contenidoFirma.put("operation", new JSONString(Tipo.REPRESENTATIVE_DATA.toString()));
    	mensajeClienteFirma.setContenidoFirma(contenidoFirma.getJavaScriptObject());
    	String urlDocument = null;
    	urlDocument = ServerPaths.getUrlRepresentativeData();
    	mensajeClienteFirma.setUrlEnvioDocumento(urlDocument);
    	mensajeClienteFirma.setNombreDestinatarioFirma(controlAcceso.getNombre());
    	mensajeClienteFirma.setAsuntoMensajeFirmado(
    			Constantes.INSTANCIA.representativeDataSubject());	
    	//mensajeClienteFirma.setRespuestaConRecibo(true);
    	mensajeClienteFirma.setUrlTimeStampServer(ServerPaths.getUrlTimeStampServer());
		if(!Browser.isAndroid()) setWidgetsStatePublicando(true);
		Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
    }
	
    @UiHandler("cerrarButton")
    void handleCancelButton(ClickEvent e) {
    	if(Window.confirm(Constantes.INSTANCIA.salirSinSalvarConfirmLabel())) {
        	History.newItem(HistoryToken.REPRESENTATIVE_CONFIG.toString());
		}
    }
	
	private void setWidgetsStatePublicando(boolean publicando) {

		richTextArea.setEnabled(!publicando);
		aceptarButton.setEnabled(!publicando);
		cerrarButton.setEnabled(!publicando);

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
		if(richTextArea.getHTML() == null || "".equals(richTextArea.getHTML())) {
			setMessage(Constantes.INSTANCIA.documentoSinTexto());
			richTextArea.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			richTextArea.setStyleName(style.errorTextBox(), false);
		}
		/*if(fileUpload.getFilename() == null || 
				"".equals(fileUpload.getFilename().trim())) {
			setMessage(Constantes.INSTANCIA.emptyFieldException());
			//String filename = fileUpload.getFilename();
			uploadImagePanel.setStyleName(style.imageMissing(), true);
			return false;
		} else {
			uploadImagePanel.setStyleName(style.imageMissing(), false);
		}*/
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
			case NEW_REPRESENTATIVE:
				dialogoProgreso.hide();
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					ResultDialog dialogo = new ResultDialog();
					dialogo.show(mensaje.getMensaje());
					History.newItem(HistoryToken.REPRESENTATIVES_PAGE.toString());
				} else if (MensajeClienteFirmaJso.SC_CANCELADO== mensaje.getCodigoEstado()) {
					setWidgetsStatePublicando(false);
				} else {
					setWidgetsStatePublicando(false);
					setMessage(Constantes.INSTANCIA.representativeDataTransferERROR() +
							" - " + mensaje.getMensaje());
				}
				break;
			default:
				break;
		}
	}
	
    public class ServerRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	ResultDialog resultDialog = new ResultDialog();
    		resultDialog.show(Constantes.INSTANCIA.exceptionLbl() , 
    				exception.getMessage(),Boolean.FALSE);              
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
        	ResultDialog resultDialog = new ResultDialog();
        	if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("response.getText(): " + response.getText());
            	usuario = UsuarioJso.create(response.getText());
                String representativeName = usuario.getNombre() + " " + 
                		usuario.getPrimerApellido();
                HTML pageTitleHTML = new HTML(Constantes.INSTANCIA.
                		editRepresentativeHeader(representativeName));
        		pageTitleHTML.setStyleName(style.pageTitle(), true);
        		pageTitle.clear();
        		pageTitle.add(pageTitleHTML);
                richTextArea.setHTML(usuario.getInfo());
	    		richTextArea.setEnabled(true);
            } else {
            	if(response.getStatusCode() == 0) {//Magic Number!!! -> network problem
            		resultDialog.show(Constantes.INSTANCIA.errorLbl() , 
            				Constantes.INSTANCIA.networkERROR(),Boolean.FALSE);
            	} else {
            		resultDialog.show(String.valueOf(
                			response.getStatusCode()), response.getText(), Boolean.FALSE);
                	History.newItem(HistoryToken.REPRESENTATIVES_PAGE.toString());
            	} 
            }
        }

    }

	@Override
	public void procesarMensaje(EventoGWTMensajeAplicacion evento) {
		if(evento.token.equals(HistoryToken.OBTENIDA_INFO_SERVIDOR))
			setInfoServidor((ActorConIPJso) evento.contenidoMensaje);
	}

}