package org.controlacceso.clientegwt.client.representatives;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.PuntoEntradaEditor;
import org.controlacceso.clientegwt.client.dialogo.ConfirmacionListener;
import org.controlacceso.clientegwt.client.dialogo.DialogoConfirmacion;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.dialogo.NifDialog;
import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class RepresentativeConfigPanel extends Composite implements ConfirmacionListener {

    private static Logger logger = Logger.getLogger("RepresentativeConfigPanel");

    @UiField PushButton removeRepresentativeButton;
    @UiField PushButton newRepresentativeButton;
    private DialogoOperacionEnProgreso dialogoProgreso;
    
	private static final int EDIT_REPRESENTATIVE_DIALOG = 0;
	private static final int UNSUBSCRIBE_REPRESENTATIVE = 1;

    private static RepresentativeConfigPanelUiBinder uiBinder = 
    		GWT.create(RepresentativeConfigPanelUiBinder.class);

    interface RepresentativeConfigPanelUiBinder extends 
    	UiBinder<VerticalPanel, RepresentativeConfigPanel> { }

    public RepresentativeConfigPanel() {
    	initWidget(uiBinder.createAndBindUi(this));
    }

    @UiHandler("newRepresentativeButton")
    void onClickNewRepresentativeButton(ClickEvent e) {
    	History.newItem(HistoryToken.NEW_REPRESENTATIVE.toString());
    }
    
    @UiHandler("editRepresentativeButton")
    void onClickEditRepresentativeButton(ClickEvent e) {
    	NifDialog nifDialog = new NifDialog(EDIT_REPRESENTATIVE_DIALOG, this, 
    			Constantes.INSTANCIA.editRepresentativeCaption(),
    			Constantes.INSTANCIA.editRepresentativeMsg());
    	nifDialog.show();
    }
    
    @UiHandler("removeRepresentativeButton")
    void onClickRemoveRepresentativeButton(ClickEvent e) {
    	DialogoConfirmacion dialogoConfirmacion = new DialogoConfirmacion(
    			UNSUBSCRIBE_REPRESENTATIVE, this);
    	dialogoConfirmacion.show(
    			Constantes.INSTANCIA.unsubscribeRepresentativeCaption(), 
    			Constantes.INSTANCIA.unsubscribeRepresentativeMsg());
    }

    private void showErrorDialog (String caption, String message) {
    	ResultDialog resultDialog = new ResultDialog();
		resultDialog.show(caption, message,Boolean.FALSE);  
    }

	@Override
	public void confirmed(Integer id, Object param) {
		logger.info(" - confirmed + id: " + id + " - param: " + param);
		MensajeClienteFirmaJso mensajeClienteFirma = null;
		JSONObject contenidoFirma = null;
		switch(id) {
			case EDIT_REPRESENTATIVE_DIALOG:
				logger.info(" - Lanzando comprobación de representante " + param);
				String representativeNif = (String)param;
				RequestHelper.doGet(ServerPaths.getRepresentativeByNif(representativeNif), 
						new RepresentativeCheckRequestCallback());
				break;
			case UNSUBSCRIBE_REPRESENTATIVE:
				logger.info("confirmed UNSUBSCRIBE_REPRESENTATIVE");
		    	mensajeClienteFirma = MensajeClienteFirmaJso.create();
		    	mensajeClienteFirma.setCodigoEstado(MensajeClienteFirmaJso.SC_PROCESANDO);
		    	mensajeClienteFirma.setOperacion(MensajeClienteFirmaJso.Operacion.
		    			REPRESENTATIVE_UNSUBSCRIBE_REQUEST.toString());
		    	contenidoFirma = new JSONObject();
		    	contenidoFirma.put("operation", new JSONString(MensajeClienteFirmaJso.Operacion.
		    			REPRESENTATIVE_UNSUBSCRIBE_REQUEST.toString()));
		    	mensajeClienteFirma.setContenidoFirma(contenidoFirma.getJavaScriptObject());
		    	mensajeClienteFirma.setUrlEnvioDocumento(
		    			ServerPaths.getUrlUnsubscribeRepresentative());
		    	if(PuntoEntrada.INSTANCIA != null &&
		    			PuntoEntrada.INSTANCIA.servidor != null) {
		    		mensajeClienteFirma.setNombreDestinatarioFirma(
		    				PuntoEntrada.INSTANCIA.servidor.getNombre());
		    	}
		    	mensajeClienteFirma.setAsuntoMensajeFirmado(
		    			Constantes.INSTANCIA.unsubscribeRepresentativeSubject());	
		    	mensajeClienteFirma.setRespuestaConRecibo(true);
		    	mensajeClienteFirma.setUrlTimeStampServer(ServerPaths.getUrlTimeStampServer());
				break;
		}
		if(mensajeClienteFirma != null) {
			if(!Browser.isAndroid()) setAppletOperationMode(true);
			Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
		}
	}
	
	private void setAppletOperationMode(boolean publicando) {
		if(publicando) {
			if(PuntoEntradaEditor.INSTANCIA != null && 
					PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
				Browser.showProgressDialog(Constantes.INSTANCIA.publishingDocument());
			} else {
				dialogoProgreso = new DialogoOperacionEnProgreso();
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

    public class RepresentativeCheckRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	showErrorDialog(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
			ResultDialog resultDialog = new ResultDialog();
            if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("response.getText(): " + response.getText());
            	JSONValue jsonValue = JSONParser.parseLenient(response.getText());
            	JSONObject jsonObj = jsonValue.isObject();
            	Double representativeId = jsonObj.get("representativeId").isNumber().doubleValue();
	        	History.newItem(HistoryToken.EDIT_REPRESENTATIVE.toString()
	        			+ "&representativeId=" +  representativeId.intValue());
            } else {
            	logger.info("ERROR: " +response.getStatusCode() 
            			+ " - message: " + response.getText());
            	if(response.getStatusCode() == 0) {//Magic Number!!! -> network problem
            		resultDialog.show(Constantes.INSTANCIA.errorLbl() , 
            				Constantes.INSTANCIA.networkERROR(), Boolean.FALSE);
            	} else {
            		resultDialog.show(Constantes.INSTANCIA.errorLbl(), 
            				response.getText(), Boolean.FALSE);
            	} 
            }
        }

    }

}