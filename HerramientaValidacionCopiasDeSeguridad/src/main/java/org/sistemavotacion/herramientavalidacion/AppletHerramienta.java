package org.sistemavotacion.herramientavalidacion;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Security;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.UIManager;
import netscape.javascript.JSObject;
import org.apache.log4j.PropertyConfigurator;
import static org.sistemavotacion.Contexto.getString;
import org.sistemavotacion.herramientavalidacion.Operacion;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/HerramientaValidacionCopiasDeSeguridad/blob/master/licencia.txt
*/
public class AppletHerramienta extends JApplet {
    
    private static Logger logger = LoggerFactory.getLogger(AppletHerramienta.class);
    
    public static enum ModoEjecucion {APPLET, APLICACION}
    
    private Frame frame;
    private String locale = "es";
    public static ModoEjecucion modoEjecucion = ModoEjecucion.APPLET;
    
    private static ResourceBundle resourceBundle;
    
    public static AppletHerramienta INSTANCIA;
    public static int MAXIMO_TAMANYO_ARCHIVO_EN_BYTES = 10000000;
    
    public AppletHerramienta() {   
        INSTANCIA = this;
    }
        
    public void init() {
        logger.debug("init");
        
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.debug("ShutdownHook - ShutdownHook - ShutdownHook");
        }});
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AcercaDePanel acercaDePanel = new AcercaDePanel();
                        getContentPane().add(acercaDePanel);           
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public Frame getFrame() {
        return frame;
    }
        
    public void start() {
        logger.debug("start");
        init();
        Frame[] frames = JFrame.getFrames();
        if(frames.length == 0 || frames[0] == null) frame = new javax.swing.JFrame();
        else frame = frames[0];
        Properties props = new Properties();
        try {
            props.load(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("log4jHerramienta.properties"));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        PropertyConfigurator.configure(props);
        if(AppletHerramienta.modoEjecucion == AppletHerramienta.ModoEjecucion.APPLET) {
            if(getParameter("locale") != null) locale = getParameter("locale");
        } 
        resourceBundle = ResourceBundle.getBundle(
                "herramientaValidacionMessages_" + locale);
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if(AppletHerramienta.modoEjecucion != 
                            AppletHerramienta.ModoEjecucion.APPLET) {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }
                    DialogoPrincipal dialogo = new DialogoPrincipal(frame, false);
                    dialogo.setVisible(true);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        Operacion operacion = new Operacion();
        operacion.setTipo(Operacion.Tipo.MENSAJE_HERRAMIENTA_VALIDACION);
        operacion.setMensaje(resourceBundle.getString("appletInicializado"));
        enviarMensajeAplicacion(operacion);
    }

        
    private void enviarMensajeAplicacion(Operacion operacion) {
        if (operacion == null) {
            logger.debug(" - enviarMensajeAplicacion - operación nula");
            return;
        }
        logger.debug(" - enviarMensajeAplicacion - operación: " + operacion.obtenerJSONStr());
        try {
            if(AppletHerramienta.modoEjecucion == AppletHerramienta.ModoEjecucion.APPLET) {
                Object[] args = {operacion.obtenerJSONStr()};
                Object object = JSObject.getWindow(this).call("setClienteFirmaMessage", args);
            } else logger.debug("---------------> enviado mensaje de aplicación");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
        
    public void stop() {
        logger.debug("stop");
    }

    public void destroy() {
        logger.debug("destroy");
    }
    
    public static ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
    
    public static String getString(String key) {
        return resourceBundle.getString(key);
    }
    
    public static String getString(String key, Object... arguments) {
        String pattern = getString(key);
        return MessageFormat.format(pattern, arguments);
    }


    public static void main (String[] args) { 
        logger.info("Arrancando aplicación");
        modoEjecucion = ModoEjecucion.APLICACION;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        AppletHerramienta appletFirma = new AppletHerramienta();
                        appletFirma.start();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
}