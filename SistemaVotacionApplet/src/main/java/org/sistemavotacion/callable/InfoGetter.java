package org.sistemavotacion.callable;

import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class InfoGetter implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(InfoGetter.class);
 
    private String urlDocument;
    private String contentType = null;
    private Integer id;
    
    public InfoGetter(Integer id, String urlDocument, String contentType) {
        this.id = id;
        this.urlDocument = urlDocument;
        this.contentType = contentType;
    }

    @Override public Respuesta call() { 
        Respuesta respuesta = new Respuesta(Respuesta.SC_OK);
        try {
            respuesta = Contexto.INSTANCE.getHttpHelper().
                    getData(urlDocument, contentType);
        } catch(Exception ex){
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        } finally {
            respuesta.setId(id);
            return respuesta;
        }
    }
    
}