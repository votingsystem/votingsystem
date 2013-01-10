package org.sistemavotacion;

import java.awt.Frame;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.UIManager;
import netscape.javascript.*;
import static org.sistemavotacion.Contexto.*;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.VotacionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class AppletFirma extends JApplet {
    
    private static Logger logger = LoggerFactory.getLogger(AppletFirma.class);

    
    public static enum ModoEjecucion {APPLET, APLICACION}
    
    private Frame frame;
    private Timer recolectorOperaciones;
    private AtomicBoolean cancelado = new AtomicBoolean(false);
    private Operacion operacionEnCurso;
    public static String locale = "es";
    public static ModoEjecucion modoEjecucion = ModoEjecucion.APPLET;
    
    private AppletFirma INSTANCIA;
    
    public AppletFirma() {
        INSTANCIA = this;
    }
        
    public void init() {
        logger.debug("init");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.debug("ShutdownHook - ShutdownHook - ShutdownHook");
                finalizar();
        }});
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            Contexto.inicializar();
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AcercaDePanel acercaDePanel = new AcercaDePanel();
                        getContentPane().add(acercaDePanel);
                        getContentPane().repaint();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
        
    public void start() {
        logger.debug("start");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        Frame[] frames = JFrame.getFrames();
        if(frames.length == 0 || frames[0] == null) frame = new javax.swing.JFrame();
        else frame = frames[0];
        if(AppletFirma.modoEjecucion == AppletFirma.ModoEjecucion.APPLET) {
            lanzarTimer();
            if(getParameter("locale") != null) locale = getParameter("locale");
        } 
        Operacion operacion = new Operacion(Operacion.SC_PROCESANDO, 
                Operacion.Tipo.MENSAJE_APPLET, getString("appletInicializado"));
        enviarMensajeAplicacion(operacion);
    }

    public void stop() {
        logger.debug("stop");
        finalizar();
    }

    public void destroy() {
        logger.debug("destroy");
        finalizar();
    }

    public Operacion getOperacionEnCurso() {
        return operacionEnCurso;
    }
    
    private void lanzarTimer() {
        recolectorOperaciones =  new Timer(true);
        final JSObject jsObject = JSObject.getWindow(this);
        recolectorOperaciones.scheduleAtFixedRate(
            new TimerTask(){
                public void run() { 
                    //logger.debug("Comprobando operaciones pendientes");
                    Object object = jsObject.call("obtenerOperacion", null);
                    if(object != null) {
                        ejecutarOperacion(object.toString());
                    } else {
                        //logger.debug("Testeando JSObject - respuesta nula");
                    }
                }
            }, 0, 4000);
    }
    
        
    public void finalizar() {
        logger.debug("finalizar");
        if(cancelado.get()) {
            logger.debug("Gestor previamente finalizado");
            return;
        }
        recolectorOperaciones.cancel();
        Operacion operacion = new Operacion();
        operacion.setTipo(Operacion.Tipo.MENSAJE_CIERRE_APPLET);
        enviarMensajeAplicacion(operacion);
        cancelado.set(true);
    }
    
    private void enviarMensajeAplicacion(Operacion operacion) {
        if (operacion == null) {
            logger.debug(" - enviarMensajeAplicacion - operación nula");
            return;
        }
        logger.debug(" - enviarMensajeAplicacion - operación: " + operacion.obtenerJSONStr());
        try {
            if(AppletFirma.modoEjecucion == AppletFirma.ModoEjecucion.APPLET) {
                Object[] args = {operacion.obtenerJSONStr()};
                Object object = JSObject.getWindow(this).call("setClienteFirmaMessage", args);
            } else logger.debug("---------------> enviado mensaje de aplicación");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    public void responderCliente(int codigoEstado, String mensaje) {
        logger.debug("responderCliente - codigoEstado: " + codigoEstado + 
                    " - mensaje: " + mensaje);
        if(operacionEnCurso == null) {
            logger.debug("responderCliente ---> No hay ninguna operación en curso");
            return;
        }
        operacionEnCurso.setCodigoEstado(codigoEstado);
        operacionEnCurso.setMensaje(mensaje);
        enviarMensajeAplicacion(operacionEnCurso);
        operacionEnCurso = null;
    }
    
    public void cancelarOperacion () {
        if(operacionEnCurso == null) return;
        else if(operacionEnCurso.getCodigoEstado() == Operacion.SC_PROCESANDO) {
            operacionEnCurso.setMensaje(getString("operacionCancelada"));
            operacionEnCurso.setCodigoEstado(Operacion.SC_CANCELADO);
            enviarMensajeAplicacion(operacionEnCurso);
            operacionEnCurso = null;  
        }  
    }
        
    public void ejecutarOperacion(String operacionJSONStr) {
        logger.debug("ejecutarOperacion: " + operacionJSONStr);
        if(operacionJSONStr == null || "".equals(operacionJSONStr)) return;
        this.operacionEnCurso = Operacion.parse(operacionJSONStr);
        String errorValidacion = operacionEnCurso.getErrorValidacion();
        if(errorValidacion != null) {
            logger.info("ejecutarOperacion - errorValidacion: " + errorValidacion);
            responderCliente(Operacion.SC_ERROR_PETICION, errorValidacion);
            return;
        }
        switch(operacionEnCurso.getTipo()) {
            case GUARDAR_RECIBO_VOTO:
                Operacion resultado = VotacionHelper.guardarRecibo(
                        operacionEnCurso.getArgs()[0], frame);
                responderCliente(resultado.getCodigoEstado(), resultado.getMensaje());
                break;
            case ENVIO_VOTO_SMIME:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        VotacionDialog votacionDialog = new VotacionDialog(
                                    frame, true, INSTANCIA);
                        votacionDialog.setVisible(true);
                    }
                });    
                break;
            case PUBLICACION_MANIFIESTO_PDF:
            case FIRMA_MANIFIESTO_PDF:
            case PUBLICACION_RECLAMACION_SMIME:
            case FIRMA_RECLAMACION_SMIME:
            case PUBLICACION_VOTACION_SMIME:
            case CANCELAR_EVENTO:
            case ASOCIAR_CENTRO_CONTROL_SMIME:
            case ANULAR_SOLICITUD_ACCESO:
            case ANULAR_VOTO: 
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        FirmaDialog firmaDialog = new FirmaDialog(frame, true, INSTANCIA);
                    }
                });
                break;
            case SOLICITUD_COPIA_SEGURIDAD:
                FirmaDialog firmaDialog = new FirmaDialog(frame, true, INSTANCIA);
                byte[] bytesPDF = null; 
                try {
                    File file = PdfFormHelper.obtenerSolicitudCopia(
                            operacionEnCurso.getEvento().getEventoId().toString(),
                            operacionEnCurso.getEvento().getAsunto(), 
                            operacionEnCurso.getEmailSolicitante());
                    bytesPDF = FileUtils.getBytesFromFile(file);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                firmaDialog.inicializarSinDescargarPDF(bytesPDF);
                break;
            default:
                logger.debug("Operación desconocida: " + operacionEnCurso.getTipo().toString());
        }
    }
    
    public static void main (String[] args) { 
        logger.info("Arrancando aplicación");
        modoEjecucion = ModoEjecucion.APLICACION;
        Operacion ope = new Operacion();
        String[] argus = {"hola"};
        ope.setArgs(argus);
        logger.info("ope: " + ope.obtenerJSONStr());
        try {
            Contexto.inicializar();
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AppletFirma appletFirma = new AppletFirma();
                        appletFirma.start();
                        File jsonFile = File.createTempFile("operacion", ".json");
                        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("testFiles/votingOperation.json"), jsonFile);
                        appletFirma.ejecutarOperacion(FileUtils.getStringFromFile(jsonFile));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
            logger.debug("-- finalizando aplicación --- ");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
}