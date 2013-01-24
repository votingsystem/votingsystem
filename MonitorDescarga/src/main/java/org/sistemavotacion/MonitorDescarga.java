package org.sistemavotacion;

import java.applet.AppletStub;
import java.awt.Container;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jnlp.DownloadServiceListener;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

/**
 *
 * @author jgzornoza
 * http://docs.oracle.com/javase/tutorial/deployment/applet/customProgressIndicatorForApplet.html
 */
public class MonitorDescarga implements DownloadServiceListener {
    
    
    private static Logger logger =   java.util.logging.Logger.getLogger(
                    MonitorDescarga.class.getName());
    
    Container surfaceContainer = null;
    AppletStub appletStub = null;
    JProgressBar progressBar = null;
    JLabel statusLabel = null;
    boolean uiCreated = false;
    int porcentajeDescargado = -1;

    final URLStreamHandler streamHandler = new URLStreamHandler() {

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return null;
        }

    };
    
    public MonitorDescarga(Object surface) {
       logger.info("MonitorDescarga - surface");
       init(surface, null);
    }

    public MonitorDescarga(Object surface, Object stub) {
        logger.info("MonitorDescarga - surface stub");
        init(surface, stub);
    }
    
    public void init() {
        logger.info("init");
    }
    
    public void init(Object surface, Object stub) {
        logger.info("init - surface, stub");
        surfaceContainer = (Container) surface;
        appletStub = (AppletStub) stub;
        enviarMensajeAplicacion(getMensajeJSON("Inicializando descarga", 
                new Integer(0).toString()));
    }
    
    public String obtenerEstado() {
        logger.info("obtenerEstado");
        return "";
    }   
    
    public void enviarMensajeAplicacion(String mensaje) {
        logger.info(" - enviarMensajeAplicacion - mensaje: " + mensaje);
        if (mensaje == null) return;
        try {
           /* appletStub.getAppletContext().showDocument (new URL(null, 
                    "javascript:setAppletFirmaMessage('" + mensajeEnviado + "');", 
                    streamHandler));*/
           appletStub.getAppletContext().showDocument (new URL(
                   "javascript:setClienteFirmaMessage('" + mensaje + "');"));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    

    public void progress(URL paramURL, String paramString,
            long readSoFar, long total, int overallPercents) {
        /*logger.info("validating - readSoFar: " + readSoFar + " - total: " + total 
                + " - overallPercents: " + overallPercents);
        if(paramURL != null) logger.info(" - paramURL: " + paramURL.toString());
        if(paramString != null) logger.info(" - paramString: " + paramString);*/
    }

    public void validating(URL paramURL, String paramString, long entry,
            long total, int overallPercent) {
        if(paramURL != null) logger.info(" - paramURL: " + paramURL.toString());
        if(paramString != null) logger.info(" - paramString: " + paramString);
        updateProgressUI(overallPercent);
    }

    public void upgradingArchive(URL paramURL, String paramString, 
            int patchPercent, int overallPercent) {
        /*logger.info("upgradingArchive - patchPercent: " + patchPercent 
                + " - overallPercent: " + overallPercent);
        if(paramURL != null) logger.info(" - paramURL: " + paramURL.toString());
        if(paramString != null) logger.info(" - paramString: " + paramString);
        updateProgressUI(overallPercent);*/
    }

    public void downloadFailed(URL paramURL, String paramString) {
        logger.info("downloadFailed");
        /*if(paramURL != null) logger.info(" - paramURL: " + paramURL.toString());
        if(paramString != null) logger.info(" - paramString: " + paramString);
        enviarMensajeAplicacion("descarga applet fallida");*/
    }
    
    private void updateProgressUI(int overallPercent) {
        logger.info("updateProgressUI - overallPercent: " + overallPercent);
        String mensaje = "Descargando applet";
        if(100 == overallPercent) mensaje = "Descarga applet completa";
        if(porcentajeDescargado != overallPercent) {
            porcentajeDescargado = overallPercent;
            enviarMensajeAplicacion(getMensajeJSON(
                mensaje, new Integer(overallPercent).toString()));
        }  
    }
    
    private static String getMensajeJSON(String mensaje, String arg) {
        return "{\"codigoEstado\":700, \"operacion\":\"MENSAJE_MONITOR_DESCARGA_APPLET\", \"mensaje\":\""
                 + mensaje + "\", \"args\":[\"" + arg + "\"]}";
    }
    
    public static void main(String[] args)  {
        logger.info("AppletDescargasApplet DescargasAppletDescargas");
    }
}
