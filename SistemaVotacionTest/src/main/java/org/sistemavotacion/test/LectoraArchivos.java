package org.sistemavotacion.test;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class LectoraArchivos implements Runnable {
    
    private static Logger logger = LoggerFactory.getLogger(LectoraArchivos.class);    
    
    private final BlockingQueue<File> colaArchivos;
    private final File archivoRaiz;
    private final FileFilter fileFilter;
    
    public LectoraArchivos (BlockingQueue<File> colaArchivos, File raiz) {
        this.colaArchivos = colaArchivos;
        archivoRaiz = raiz;
        fileFilter = new FileFilter() {
            public boolean accept(File file) {return true;}
        };
    }
    
    @Override
    public void run() {
        try {
            leerArchivos();
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void leerArchivos () throws InterruptedException {

        File[] archivos = archivoRaiz.listFiles(fileFilter);
        if (archivos != null) {
            for (File archivo : archivos) {
                if (archivo.isDirectory()) leerArchivos();
                else if (!validado(archivo)) colaArchivos.put(archivo);
            }
            
        }
    }
    
    private boolean validado (File archivo) {
        return false;
    }
    
}
