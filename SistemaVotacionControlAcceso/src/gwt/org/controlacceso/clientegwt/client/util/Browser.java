package org.controlacceso.clientegwt.client.util;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class Browser {
	
	private static Logger logger = Logger.getLogger("Browser");

	public static boolean isChrome () {
		return (getUserAgent().indexOf("chrome") > - 1);
	}
	
	public static boolean isAndroid () {
		return (getUserAgent().indexOf("android") > - 1);
	}

	public static boolean isFirefox () {
		return (getUserAgent().indexOf("firefox") > - 1);
	}
	
	public static boolean isPC () {
		return !isAndroid ();
	}
	
	public static native String getUserAgent() /*-{
		return navigator.userAgent.toLowerCase();
	}-*/;
	
	public static void ejecutarOperacionClienteFirma(MensajeClienteFirmaJso mensajeClienteFirma) {
		if(mensajeClienteFirma == null) return;
		mensajeClienteFirma.setUrlTimeStampServer(ServerPaths.getUrlTimeStampServer());
		if(isAndroid()) {
			//to avoid URI too large
			mensajeClienteFirma.getEvento().setContenido(null);
			String encodedMsg = getEncodedString(mensajeClienteFirma.toJSONString());
			String url = ServerPaths.getUrlClienteAndroid() + "?browserToken=" 
	    			+ History.getToken() + "&serverURL=" + ServerPaths.getApplicationPath() 
	    			+ "&msg=" + encodedMsg;
	    	ServerPaths.redirect(url);
		} else {
			logger.info("ejecutarOperacionClienteFirma - mensajeClienteFirma: " + mensajeClienteFirma.toJSONString());
			PuntoEntrada.INSTANCIA.cargarClienteFirma();
			PuntoEntrada.INSTANCIA.setMensajeClienteFirmaPendiente(mensajeClienteFirma);	
		}
	}
	
	private static native String ejecutarNativeOperacionClienteFirma(
			String idFrameClienteFirma, String operacionJSONStr) /*-{
	    var iFrame =  $doc.getElementById(idFrameClienteFirma);
	    var content = (iFrame.contentWindow || iFrame.contentDocument);
	    var document
	    if (content.document) document = content.document;
	    var clienteFirma = document.getElementById('clienteFirma');
	    return clienteFirma.ejecutarOperacion(operacionJSONStr)
	}-*/;

	public static native String getMessageFromIFrame(String iFrameId)/*-{
	    var iFrame =  $doc.getElementById(iFrameId);
	    var content = (iFrame.contentWindow || iFrame.contentDocument);
	    var document
	    if (content.document) document = content.document;
		var mensaje = document.getElementById('mensaje');
	    return mensaje.innerHTML	
	}-*/;

    /**
     * Evaluate scripts in an HTML string. Will eval both <script src=""></script>
     * and <script>javascript here</scripts>.
     * 
     * http://snippets.dzone.com/posts/show/7052
     *
     * @param element a new HTML(text).getElement()
     */
    public static native void evalScripts(Element element) /*-{
        var scripts = element.getElementsByTagName("script");
    
        for (i=0; i < scripts.length; i++) {
            // if src, eval it, otherwise eval the body
            if (scripts[i].hasAttribute("src")) {
                var src = scripts[i].getAttribute("src");
                var script = $doc.createElement('script');
                script.setAttribute("src", src);
                $doc.getElementsByTagName('body')[0].appendChild(script);
            } else {
                $wnd.eval(scripts[i].innerHTML);
            }
        }
    }-*/;
    
    
    public static native String getEncodedString(String str) /*-{
			return encodeURIComponent(str);
	}-*/;
    
    public static native String getDecodedString(String str) /*-{
			return decodeURIComponent(str);
	}-*/;

    
    
}