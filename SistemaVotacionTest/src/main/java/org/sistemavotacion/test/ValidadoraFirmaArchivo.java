package org.sistemavotacion.test;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class ValidadoraFirmaArchivo implements Runnable {
    
    private static Logger logger = LoggerFactory.getLogger(ValidadoraFirmaArchivo.class);   
    
    private final BlockingQueue<File> colaArchivos;

    public ValidadoraFirmaArchivo (BlockingQueue<File> colaArchivos) {
        this.colaArchivos = colaArchivos;
    }    
    
    @Override
    public void run() {
        try {
            while (true)
                validarFirma(colaArchivos.take());
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    
    private void validarFirma (File file) {
    }

}