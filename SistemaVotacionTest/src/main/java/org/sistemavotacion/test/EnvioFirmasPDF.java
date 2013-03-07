package org.sistemavotacion.test;

import java.io.File;
import java.security.cert.PKIXParameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.test.modelo.InfoFirma;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 * Para enviar firmas modificar los datos de ContextoPruebas para que coincidan con
 * una firma publicada
 */
public class EnvioFirmasPDF {
    
    private static Logger logger = LoggerFactory.getLogger(EnvioFirmasPDF.class);
        
        
    static long comienzo;
    private static final ExecutorService firmasExecutor = Executors.newFixedThreadPool(5);
    
    private static final ExecutorService envioFirmasExecutor = Executors.newFixedThreadPool(20);
    private static CompletionService<InfoFirma> firmasCompletionService =
            new ExecutorCompletionService<InfoFirma>(envioFirmasExecutor);
    
    private static Evento evento;
    private static PKIXParameters params;
    
    private static AtomicLong numeroUsuarios = new AtomicLong(1000);
    private static AtomicLong firmasEnviadas = new AtomicLong();
    private static AtomicLong firmasValidas = new AtomicLong();
    private static AtomicLong firmasConError = new AtomicLong();
    
    public static void main( String[] args ) throws Exception{ 
        comienzo = System.currentTimeMillis();
        Contexto.inicializar();
        if (args.length > 0) {
            numeroUsuarios.set(Long.valueOf(args[0]));
            logger.debug("### Se lanzarán: '" + numeroUsuarios.get() + "' firmas");
        } else logger.debug("### Sin argumentos - se lanzarán: '" + numeroUsuarios.get() + "' firmas");
        logger.info("Arrancando recogida de firmas");
        final Evento documento = new Evento();
        firmasExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    lanzarFirmas(documento);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }
    
    public static void inicializarAutoridadCertificadora() {}
    
    
    public static void lanzarFirmas (Evento documento) throws Exception {
        logger.info("Lanzado hilo de firmas");
        params = ContextoPruebas.INSTANCIA.getSessionPKIXParameters();
        for (int numUsu = 0; numUsu < numeroUsuarios.get(); numUsu++) {
            InfoFirma infoFirma = new InfoFirma(documento, String.valueOf(numUsu));
            firmasCompletionService.submit(new LanzadoraFirma(infoFirma));
            firmasEnviadas.getAndIncrement();
        }        
        for (int v = 0; v < numeroUsuarios.get(); v++) {
            Future<InfoFirma> f = firmasCompletionService.take();
            InfoFirma infoFirma = f.get();
            if (200 == infoFirma.getRespuesta().getCodigoEstado() &&
                    infoFirma.getRespuesta().isValidSignature()) {
                String rutaRecibo = ContextoPruebas.getUserDirPath(infoFirma.getFrom()) +
                        infoFirma.getRespuesta().getArchivo().getName();
                FileUtils.copyFileToFile(infoFirma.getRespuesta().getArchivo(), 
                        new File(rutaRecibo));
                firmasValidas.getAndIncrement();
            } else {
                firmasConError.getAndIncrement();
                logger.error("FIRMA CON ERROR - Usuario: " + infoFirma.getFrom()
                        + " - Codigo respuesta de Servidor: " + infoFirma.getRespuesta().getCodigoEstado()
                        + " - Mensaje de Servidor: " + infoFirma.getRespuesta().getMensaje());
                if (infoFirma.getRespuesta().getArchivo() != null) {
                    String rutaRecibo = ContextoPruebas.getUserDirPath(infoFirma.getFrom()) +
                        infoFirma.getRespuesta().getArchivo().getName() + "_ERROR";
                    FileUtils.copyFileToFile(infoFirma.getRespuesta().getArchivo(), 
                        new File(rutaRecibo));
                }
            }   
        }
        long delta = System.currentTimeMillis() - comienzo; 
        logger.info("Delta: " + delta + " - Duracion: " + 
                DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(delta));
        logger.info("Número usuarios: " + numeroUsuarios.get());
        logger.info("Firmas enviadas: " + firmasEnviadas.get());
        logger.info("Firmas validas: " + firmasValidas.get());
        logger.info("Firmas con error: " + firmasConError.get());
        firmasExecutor.shutdown();
        envioFirmasExecutor.shutdown();
    }

   public static String obtenerFirmaParaEventoJSON(Evento evento) {
        logger.debug("obtenerFirmaParaEventoJSON");
        Map map = new HashMap();
        Map controlAccesoMap = new HashMap();
        controlAccesoMap.put("serverURL", evento.getControlAcceso().getServerURL());
        controlAccesoMap.put("nombre", evento.getControlAcceso().getNombre());
        map.put("controlAcceso", controlAccesoMap);
        map.put("eventoId", evento.getEventoId());
        map.put("asunto", evento.getAsunto());
        map.put("contenido", evento.getContenido());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON( map );
        if (evento.getCampos() != null) {
            List<OpcionEvento> opciones = evento.getOpciones();
            JSONArray jsonArray = new JSONArray();
            for (OpcionEvento opcion : opciones) {
                Map campoMap = new HashMap();
                campoMap.put("id", opcion.getId());
                campoMap.put("contenido", opcion.getContenido());
                campoMap.put("valor", opcion.getValor());
                JSONObject camposJSON = (JSONObject) JSONSerializer.toJSON(campoMap);
                jsonArray.element(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        return jsonObject.toString();
    }

    /**
     * @return the evento
     */
    public static Evento getEvento() {
        return evento;
    }

    /**
     * @param aEvento the evento to set
     */
    public static void setEvento(Evento aEvento) {
        evento = aEvento;
    }

    /**
     * @return the params
     */
    public static PKIXParameters getParams() {
        return params;
    }

    /**
     * @param aParams the params to set
     */
    public static void setParams(PKIXParameters aParams) {
        params = aParams;
    }

    
}
