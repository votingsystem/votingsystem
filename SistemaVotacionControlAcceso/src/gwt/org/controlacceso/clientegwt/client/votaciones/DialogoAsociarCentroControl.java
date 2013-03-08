package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.dialogo.DialogoResultadoFirma;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.InfoServidorJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.Tipo;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.sistemavotacion.controlacceso.modelo.Respuesta;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoAsociarCentroControl implements EventoGWTMensajeClienteFirma.Handler{
	
    private static Logger logger = Logger.getLogger("DialogoAsociarCentroControl");
	
    private static DialogoAsociarCentroControlUiBinder uiBinder = GWT.create(DialogoAsociarCentroControlUiBinder.class);  
    
    interface DialogoAsociarCentroControlUiBinder extends UiBinder<Widget, DialogoAsociarCentroControl> {}

    interface EditorStyle extends CssResource {
        String errorTextBox();
    }
    
    @UiField EditorStyle style;
    @UiField DialogBox dialogBox;
    @UiField PushButton asociarButton;
    @UiField PushButton cerrarButton;
    @UiField PushButton comprobarConexion;
    @UiField TextBox urlCentroControlTextBox;
    @UiField VerticalPanel messagePanel;
    @UiField HorizontalPanel indeterminatePanel;
    @UiField Label messageLabel;
    @UiField Label indeterminateLabel;
    
    InfoServidorJso infoServidorJso;

	public DialogoAsociarCentroControl() {
        uiBinder.createAndBindUi(this);
        asociarButton.setEnabled(false);
        messagePanel.setVisible(false);
        indeterminatePanel.setVisible(false);
        BusEventos.addHandler(EventoGWTMensajeClienteFirma.TYPE, this);
	}

    @UiHandler("cerrarButton")
    void handleCloseButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    @UiHandler("asociarButton")
    void handleConectarButton(ClickEvent e) {
    	setIndeterminateProgressBarMessage(Constantes.INSTANCIA.asociandoCentroControl());
    	MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create();
    	mensajeClienteFirma.setCodigoEstado(MensajeClienteFirmaJso.SC_PROCESANDO);
    	mensajeClienteFirma.setOperacion(MensajeClienteFirmaJso.Operacion.
    			ASOCIAR_CENTRO_CONTROL_SMIME.toString());
    	mensajeClienteFirma.setContenidoFirma(crearContenidoFirma(infoServidorJso.getServerURL()));
    	mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlAsociacionCentroControl());
    	mensajeClienteFirma.setNombreDestinatarioFirma(infoServidorJso.getNombre());
    	mensajeClienteFirma.setAsuntoMensajeFirmado(
    			Constantes.INSTANCIA.asuntoAsociarCentroControl());
    	urlCentroControlTextBox.setEnabled(false);
    	comprobarConexion.setEnabled(false);
    	asociarButton.setEnabled(false);
    	Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
    }
    
    @UiHandler("comprobarConexion")
    void handleComprobarConexion(ClickEvent e) {
    	urlCentroControlTextBox.setEnabled(false);
    	comprobarConexion.setEnabled(false);
    	setIndeterminateProgressBarMessage(Constantes.INSTANCIA.conectandoConServidor());
    	String url = urlCentroControlTextBox.getText();
    	if(!url.endsWith("infoServidor/obtener")) {
    		if(!url.endsWith("/")) url = url + "/";
    		url = url + "infoServidor/obtener";
    	}
    	getJson(url);
    }
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    }
    
    private void setIndeterminateProgressBarMessage (String message) {
    	if(message == null) {
    		indeterminatePanel.setVisible(false);
    		return;
    	} 
    	indeterminatePanel.setVisible(true);
    	indeterminateLabel.setText(message);
    }
    
	private void setMessage (String message) {
		if(message == null || "".equals(message)) messagePanel.setVisible(false);
    	messageLabel.setText(message);
    	messagePanel.setVisible(true);
	}

    public void getJson (String url) {
    	JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
    	jsonp.setTimeout(10 * 5000);
    	jsonp.setCallbackParam("callback");
    	jsonp.requestObject(url, new AsyncCallback<InfoServidorJso>() {
    		public void onFailure(Throwable throwable) {
    			logger.log(Level.SEVERE, throwable.getMessage(), throwable);
    			setMessage(Constantes.INSTANCIA.centroControlNoValido());
    			setIndeterminateProgressBarMessage(null);
    			comprobarConexion.setEnabled(true);
				urlCentroControlTextBox.setStyleName(style.errorTextBox(), true);
    		}

    		public void onSuccess(InfoServidorJso infoServidor) {
    			setIndeterminateProgressBarMessage(null);
    			infoServidorJso = infoServidor;
    			logger.info("infoServidorJso: " + infoServidor.toJSONString());
    			logger.info("TipoServidor: " + infoServidor.getTipoServidorEnumValue().toString());
    			comprobarConexion.setEnabled(true);
    			urlCentroControlTextBox.setEnabled(true);
    			if(Tipo.CENTRO_CONTROL == infoServidor.getTipoServidorEnumValue()) {
    				urlCentroControlTextBox.setStyleName(style.errorTextBox(), false);
    				asociarButton.setEnabled(true);
    			} else {
    				setMessage(Constantes.INSTANCIA.centroControlNoValido());
    				urlCentroControlTextBox.setStyleName(style.errorTextBox(), true);
    			}
    		}
	    });
    }
    

	public static native JavaScriptObject crearContenidoFirma(String value) /*-{
		return {serverURL: value};
	}-*/;

	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info("procesarMensajeClienteFirma - Operacion: " + mensaje.getOperacion() +
				" - Codigo Estado: " + mensaje.getCodigoEstado());
		if(MensajeClienteFirmaJso.Operacion.ASOCIAR_CENTRO_CONTROL_SMIME == mensaje.getOperacionEnumValue()) {
			urlCentroControlTextBox.setEnabled(true);
	    	comprobarConexion.setEnabled(true);
	    	asociarButton.setEnabled(true);
			if(Respuesta.SC_OK == mensaje.getCodigoEstado()) {
				dialogBox.hide();
				DialogoResultadoFirma dialogo = new DialogoResultadoFirma();
				dialogo.show(Constantes.INSTANCIA.asociacionCentroControlOK());
			} else {
				DialogoResultadoFirma dialogo = new DialogoResultadoFirma();
				dialogo.show(Constantes.INSTANCIA.asociacionCentroControlError());
			}
		}
		
	}
}