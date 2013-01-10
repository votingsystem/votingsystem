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
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DialogoOperacionEnProgreso implements EventoGWTMensajeClienteFirma.Handler{
	
    private static Logger logger = Logger.getLogger("DialogoOperacionEnProgreso");
	
    private static DialogoOperacionEnProgresoUiBinder uiBinder = GWT.create(DialogoOperacionEnProgresoUiBinder.class);
    
    interface DialogoOperacionEnProgresoUiBinder extends UiBinder<Widget, DialogoOperacionEnProgreso> {}
    
    interface EditorStyle extends CssResource {
        String textoChrome();
        String texto();
    }

    @UiField Label indeterminateLabel;
    @UiField DialogBox dialogBox;
    @UiField VerticalPanel indeterminatePanel;
    @UiField VerticalPanel mainPanel;
    @UiField VerticalPanel textPanel;
    private HTML textoChrome;
    private HTML textoAdvertencia;
    
    public static DialogoOperacionEnProgreso INSTANCIA;
    
	public DialogoOperacionEnProgreso() {
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
		
	}
    
}