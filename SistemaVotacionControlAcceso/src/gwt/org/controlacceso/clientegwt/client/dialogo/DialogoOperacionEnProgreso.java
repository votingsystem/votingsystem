package org.controlacceso.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.Recursos;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.util.Browser;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoOperacionEnProgreso implements EventoGWTMensajeClienteFirma.Handler, 
	ValueChangeHandler<String>{
	
    private static Logger logger = Logger.getLogger("DialogoOperacionEnProgreso");
	
    private static DialogoOperacionEnProgresoUiBinder uiBinder = GWT.create(DialogoOperacionEnProgresoUiBinder.class);
    
    interface DialogoOperacionEnProgresoUiBinder extends UiBinder<Widget, DialogoOperacionEnProgreso> {}
    
    interface Style extends CssResource {
        String textoChrome();
        String texto();
        String resultMessage();
        String messageLabel();
    }

    @UiField Style style;
    @UiField Label indeterminateLabel;
    @UiField DialogBox dialogBox;
    @UiField VerticalPanel indeterminatePanel;
    @UiField VerticalPanel mainPanel;
    @UiField VerticalPanel textPanel;
    @UiField HorizontalPanel buttonPanel;
    @UiField Image resultImage;
    private HTML textoChrome;
    private HTML textoAdvertencia;
    
    public static DialogoOperacionEnProgreso INSTANCIA;
    
	public DialogoOperacionEnProgreso() {
        uiBinder.createAndBindUi(this);
        History.addValueChangeHandler(this);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
        dialogBox.setText(Constantes.INSTANCIA.dialogoProgresoCaption());
        resultImage.setVisible(false);
        buttonPanel.setVisible(false);
        if(PuntoEntrada.INSTANCIA != null && 
        		!PuntoEntrada.INSTANCIA.isClienteFirmaCargado()) {
            if(Browser.isChrome()) {
            	textoChrome = new HTML(
            			Constantes.INSTANCIA.cargaAppletTextoChrome());
            	textPanel.add(textoChrome);
            } else {
            	textoAdvertencia = new HTML(
            			Constantes.INSTANCIA.cargaAppletTextoAdvertenciaApplet());
            	textPanel.add(textoAdvertencia);
            }
            indeterminatePanel.setVisible(false);
        } 
        INSTANCIA = this;
	}
    

	public void setMessage(String message){
		indeterminateLabel.setText(message);
	}
	
    public void hide() {
    	dialogBox.hide();
    }
    
    public void show() {
    	dialogBox.center();
    	dialogBox.show();
    }
    
    @UiHandler("aceptarButton")
    void handleCerrarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    
    public void showFinishMessage(
    		String caption, String message, Boolean isOK) {
    	HTML htmlMessage = new HTML(message);
    	htmlMessage.setStyleName(style.messageLabel(), true);
    	indeterminatePanel.clear();
    	indeterminatePanel.add(htmlMessage);
    	buttonPanel.setVisible(true);
    	dialogBox.setText(caption);
    	dialogBox.center();
    	if(isOK != null) {
    		if(isOK) {
    			resultImage.setResource(
    					Recursos.INSTANCIA.accept_48x48());
    		} else {
    			resultImage.setResource(
    					Recursos.INSTANCIA.cancel_48x48());
    		}
    		resultImage.setVisible(true);
    	}
    	htmlMessage.setStyleDependentName(style.resultMessage(), true);
    	dialogBox.show();
    }
    
    
    public void showIndeterminate(String msg) {
        mainPanel.remove(textPanel);
        indeterminatePanel.setVisible(true);
        if(msg != null) indeterminateLabel.setText(msg);
    	dialogBox.center();
    	dialogBox.show();
    }

	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case MENSAJE_MONITOR_DESCARGA_APPLET:
	            mainPanel.remove(textPanel);
	            indeterminatePanel.setVisible(true);
	            JsArrayString arrayString;
	            String percentDownloaded;
	            if((arrayString = mensaje.getArgsJsArray()) != null && 
	            		(percentDownloaded = arrayString.get(0)) != null) {
	            	if("100".equals(percentDownloaded)) {
	            		indeterminateLabel.setText(Constantes.INSTANCIA.arrancandoClienteFirmaLabel());
	            	} else {
	            		indeterminateLabel.setText(Constantes.INSTANCIA.porcentajeDescargaLabel(percentDownloaded));
	            	}
	            }
				break;
			case MENSAJE_HERRAMIENTA_VALIDACION:
				dialogBox.hide();
				break;
			default:
				mainPanel.remove(textPanel);
				indeterminateLabel.setVisible(false);
	            indeterminatePanel.setVisible(true);
				break;
		}
		if(MensajeClienteFirmaJso.SC_CANCELADO ==  mensaje.getCodigoEstado()){
			dialogBox.hide();
		}
	}
	
	@Override public void onValueChange(ValueChangeEvent<String> event) {
		dialogBox.hide();
	}
    
}