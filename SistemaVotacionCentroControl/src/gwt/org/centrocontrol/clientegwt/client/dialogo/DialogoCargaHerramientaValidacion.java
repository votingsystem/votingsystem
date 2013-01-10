package org.centrocontrol.clientegwt.client.dialogo;

import java.util.logging.Logger;

import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.PuntoEntrada;
import org.centrocontrol.clientegwt.client.evento.BusEventos;
import org.centrocontrol.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.centrocontrol.clientegwt.client.util.Browser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoCargaHerramientaValidacion implements EventoGWTMensajeClienteFirma.Handler{
	
    private static Logger logger = Logger.getLogger("DialogoCargaHerramientaValidacion");
	
    private static DialogoCargaHerramientaValidacionUiBinder uiBinder = GWT.create(DialogoCargaHerramientaValidacionUiBinder.class);
    
    interface DialogoCargaHerramientaValidacionUiBinder extends UiBinder<Widget, DialogoCargaHerramientaValidacion> {}
    
    interface EditorStyle extends CssResource {
        String textoChrome();
        String texto();
    }

    @UiField Label indeterminateLabel;
    @UiField DialogBox dialogBox;
    @UiField VerticalPanel indeterminatePanel;
    @UiField VerticalPanel mainPanel;
    @UiField VerticalPanel textPanel;
    @UiField PushButton aceptarButton;
    private HTML textoChrome;
    private HTML textoAdvertencia;
    
    public static DialogoCargaHerramientaValidacion INSTANCIA;
    
	public DialogoCargaHerramientaValidacion() {
        uiBinder.createAndBindUi(this);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
        dialogBox.setText(Constantes.INSTANCIA.dialogoProgresoCaption());
        if(!PuntoEntrada.INSTANCIA.isClienteFirmaCargado()) {
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
    void handleAceptarButton(ClickEvent e) {
		dialogBox.hide();
    }

	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case MENSAJE_MONITOR_DESCARGA_APPLET:
	            aceptarButton.setVisible(false);
				mainPanel.remove(textPanel);
		    	dialogBox.center();
		    	dialogBox.show();
	            indeterminatePanel.setVisible(true);
	            JsArrayString arrayString;
	            String percentDownloaded;
	            if((arrayString = mensaje.getArgsJsArray()) != null && 
	            		(percentDownloaded = arrayString.get(0)) != null) {
	            	if("100".equals(percentDownloaded)) {
	            		indeterminateLabel.setText(Constantes.INSTANCIA.arrancandoHerramientaValidacionLabel());
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
		
	}



    
}