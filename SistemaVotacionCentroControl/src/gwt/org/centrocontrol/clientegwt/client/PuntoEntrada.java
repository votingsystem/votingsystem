package org.centrocontrol.clientegwt.client;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.centrocontrol.clientegwt.client.dialogo.DialogoCargaClienteAndroid;
import org.centrocontrol.clientegwt.client.dialogo.ErrorDialog;
import org.centrocontrol.clientegwt.client.evento.BusEventos;
import org.centrocontrol.clientegwt.client.evento.EventoGWTMensajeAplicacion;
import org.centrocontrol.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.centrocontrol.clientegwt.client.modelo.ActorConIPJso;
import org.centrocontrol.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.centrocontrol.clientegwt.client.panel.PanelCentral;
import org.centrocontrol.clientegwt.client.panel.PanelEncabezado;
import org.centrocontrol.clientegwt.client.util.Browser;
import org.centrocontrol.clientegwt.client.util.RequestHelper;
import org.centrocontrol.clientegwt.client.util.ServerPaths;
import org.centrocontrol.clientegwt.client.util.StringUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.NamedFrame;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.centrocontrol.clientegwt.client.modelo.Respuesta;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PuntoEntrada implements EntryPoint {

    private static Logger logger = Logger.getLogger("PuntoEntrada");
	
    public static enum EstadoClienteFirma {INHABILITADA, DESCARGANDO, CARGADO}
    
    private static PuntoEntradaBinder uiBinder = GWT.create(PuntoEntradaBinder.class);
    
    interface PuntoEntradaBinder extends UiBinder<VerticalPanel, PuntoEntrada> { }
    
    @UiField VerticalPanel appPanel;
    @UiField PanelEncabezado panelEncabezado;
    @UiField PanelCentral panelCentral;
	

    public ActorConIPJso servidor;
    public static PuntoEntrada INSTANCIA;
    private boolean clienteFirmaCargado = false;
    private boolean androidClientLoaded = false;
    private NamedFrame clienteFirmaFrame;
    private EstadoClienteFirma estadoAppetFirma;
    private MensajeClienteFirmaJso mensajeClienteFirmaPendiente; 
	
    public void onModuleLoad() {
    	INSTANCIA = this;
    	logger.log(Level.INFO, "Browser.getUserAgent(): " + Browser.getUserAgent());
    	appPanel = uiBinder.createAndBindUi(this);
    	RootPanel.get("ui").add(appPanel);
    	//RootLayoutPanel.get().add(appPanel);
    	RequestHelper.doGet(ServerPaths.getInfoServidorPath(), 
    			new ServerRequestInfoCallback());
        String token = History.getToken();
        if (token.length() == 0) {
        	History.newItem(HistoryToken.VOTACIONES.toString());
        } else {
        	History.newItem(token);
        }
        initSetClienteFirmaMessageJS(this);
        initObtenerOperacionJS(this);
        History.fireCurrentHistoryState();
        clienteFirmaFrame = new NamedFrame(Constantes.ID_FRAME_APPLET);
        logger.info("--- isJavaAvailable: " + isJavaAvailable());
        if(!isJavaAvailable() && Browser.isPC()) {
        	ErrorDialog errorDialog = new ErrorDialog();
        	errorDialog.show(Constantes.INSTANCIA.captionNavegadorSinJava(), 
        	Constantes.INSTANCIA.mensajeNavegadorSinJava());
        }
        //is webkit web session? 
        if(Browser.isAndroid() && Window.Location.getParameter("androidClientLoaded") != null &&
        		"true".equals(Window.Location.getParameter("androidClientLoaded"))) {
        	logger.info("androidClientLoaded");
        	androidClientLoaded = true;
        }
        if (Browser.isAndroid() && Window.Location.getParameter("androidClientLoaded") != null &&
    		"false".equals(Window.Location.getParameter("androidClientLoaded"))) {
        	DialogoCargaClienteAndroid dialogoCarga = new DialogoCargaClienteAndroid();
        	dialogoCarga.show(ServerPaths.getUrlClienteAndroid() + 
        			"?androidClientLoaded=false" + StringUtils.getEncodedToken(token), null);
        }
        logger.info("queryString: " +  Window.Location.getQueryString());
    }
    
    
    private class ServerRequestInfoCallback implements RequestCallback {

        @Override
    	public void onError(Request request, Throwable exception) {
        	new ErrorDialog().show(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());
        	logger.log(Level.SEVERE, exception.getMessage(), exception);
    	}

        @Override
    	public void onResponseReceived(Request request, Response response) {
			logger.log(Level.INFO, "StatusCode: " + response.getStatusCode() + 
					", Response Text: " + response.getText());
			if (response.getStatusCode() == Response.SC_OK) {
				servidor = ActorConIPJso.create(response.getText());
                EventoGWTMensajeAplicacion mensaje = new EventoGWTMensajeAplicacion(
                		servidor, HistoryToken.OBTENIDA_INFO_SERVIDOR);
                BusEventos.fireEvent(mensaje);
    		} else if (response.getStatusCode() == Response.SC_UNAUTHORIZED) {}
    		else if(response.getStatusCode() == 0) { //Magic Number!!! -> network problem
        		showErrorDialog (Constantes.INSTANCIA.errorLbl() , 
        				Constantes.INSTANCIA.networkERROR());
        	} else showErrorDialog (String.valueOf(
        			response.getStatusCode()), response.getText());
    		}
    	}


	private void showErrorDialog (String text, String body) {
		ErrorDialog errorDialog = new ErrorDialog();
		errorDialog.show(text, body);	
	}
    public boolean isClienteFirmaCargado () {
    	return clienteFirmaCargado;
    }
    
    public boolean isAndroidClientLoaded() {
    	return androidClientLoaded;
    }
    
    public void cargarClienteFirma () {
    	if(clienteFirmaCargado) return;
    	if(clienteFirmaFrame != null) RootPanel.get("uiBody").remove(clienteFirmaFrame);
    	
        clienteFirmaFrame.setSize("0px", "0px");
        clienteFirmaFrame.setUrl(ServerPaths.getUrlFrameClienteFirma());
        RootPanel.get("uiBody").add(clienteFirmaFrame);
        DOM.setElementAttribute(clienteFirmaFrame.getElement(), "id", 
        		clienteFirmaFrame.getName());
    }
    
    /**
     * Detecta si java es > 1.6
     * */
    public static native boolean isJavaAvailable() /*-{
    	return ($wnd.deployJava.versionCheck('1.6') || 
    		$wnd.deployJava.versionCheck('1.7'));
    }-*/;


    private native void initSetClienteFirmaMessageJS (PuntoEntrada pe) /*-{
	    $wnd.setClienteFirmaMessage = function (messageClienteFirma) {
	        pe.@org.centrocontrol.clientegwt.client.PuntoEntrada::setClienteFirmaMessage(Ljava/lang/String;)(messageClienteFirma);
	    };
 	}-*/;
    
    private native void initObtenerOperacionJS (PuntoEntrada pe) /*-{
    	$wnd.obtenerOperacion = function () {
        	return pe.@org.centrocontrol.clientegwt.client.PuntoEntrada::obtenerOperacion()();
    	};
	}-*/;
    
    
    public synchronized void setMensajeClienteFirmaPendiente(MensajeClienteFirmaJso mensaje) {
    	 this.mensajeClienteFirmaPendiente = mensaje;
    }
    
    public synchronized MensajeClienteFirmaJso getMensajeClienteFirmaPendiente() {
    	return this.mensajeClienteFirmaPendiente;
    }
    
    public String obtenerOperacion() {
    	logger.info(" - obtenerOperacion");
    	if (getMensajeClienteFirmaPendiente() == null) return null;
    	else {
    		String respuesta = getMensajeClienteFirmaPendiente().toJSONString();
    		setMensajeClienteFirmaPendiente(null);
    		return respuesta;
    	}
    }
    
    public String setClienteFirmaMessage(String message) {
    	logger.log(Level.INFO, "setClienteFirmaMessage - message: " + message);
    	String resultado = "Respuesta de la aplicación gwt";
    	try {
    		MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.getMensajeClienteFirma(message);
    		if(mensajeClienteFirma.getOperacion() == null) {
    			logger.log(Level.SEVERE, "setClienteFirmaMessage - mensaje operación nulo");
    			return null;
    		}
    		BusEventos.fireEvent(new EventoGWTMensajeClienteFirma(mensajeClienteFirma));
    		switch(mensajeClienteFirma.getOperacionEnumValue()) {
	    		case MENSAJE_CIERRE_APPLET:
	    			if(clienteFirmaFrame != null) RootPanel.get("uiBody").remove(clienteFirmaFrame);
	    			clienteFirmaFrame = null;
	    			clienteFirmaCargado = false;
	    			break;
	    		case PUBLICACION_MANIFIESTO_PDF:
	    			break;
	    		case ASOCIAR_CENTRO_CONTROL_SMIME:
	    			if(Respuesta.SC_OK== mensajeClienteFirma.getCodigoEstado()) {
	    		    	RequestHelper.doGet(ServerPaths.getInfoServidorPath(), 
	    		    			new ServerRequestInfoCallback());
	    			}
	    			break;
	    		default:
	    			logger.info("setClienteFirmaMessage - operación desconocida: " + 
	    					mensajeClienteFirma.getOperacionEnumValue().toString());
	    			break;
    		}
    	} catch (Exception exception) {
    		logger.log(Level.SEVERE, exception.getMessage(), exception);
    	}
    	return resultado;
    }
    
}