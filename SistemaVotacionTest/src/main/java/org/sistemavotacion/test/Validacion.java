package org.sistemavotacion.test;


import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class Validacion {
    
    private static Logger logger = LoggerFactory.getLogger(Validacion.class);
    
    public static String DIRECTORIO_RAIZ = ContextoPruebas.APPDIR + "temp/archivoConfirma" ;
    public static final int TAMANYO_COLA = 1000;
    
    private static Executor validacionExecutor = Executors.newCachedThreadPool();
  
    public static void main( String[] args ) throws Exception{ 
        BlockingQueue<File> queue = new LinkedBlockingQueue<File>(TAMANYO_COLA);
        File dirRaiz = new File(DIRECTORIO_RAIZ);
        validacionExecutor.execute(new LectoraArchivos(queue, dirRaiz));
        validacionExecutor.execute(new ValidadoraFirmaArchivo(queue));
    }
    

}
