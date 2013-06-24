package org.sistemavotacion.callable;

import java.io.File;
import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class InfoSender implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(InfoSender.class);

    private Integer id = null;
    private String urlToSendDocument;
    private Object documentoEnviado;
    private String documentContentType = null;

    public InfoSender(Integer id, Object documentoEnviado, 
            String documentContentType, String urlToSendDocument) {
        this.id = id;
        this.documentoEnviado = documentoEnviado;
        this.documentContentType = documentContentType;
        this.urlToSendDocument = urlToSendDocument;
    }
    
    public InfoSender setDocumentoEnviado(Object documentoEnviado,
            String documentContentType) {
        this.documentoEnviado = documentoEnviado;
        this.documentContentType = documentContentType;
        return this;
    }
    
    @Override public Respuesta call() { 
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
        try {
            if(documentoEnviado instanceof File) {
                respuesta = Contexto.INSTANCE.getHttpHelper().sendFile((File)documentoEnviado, 
                    documentContentType, urlToSendDocument);
            } else if(documentoEnviado instanceof byte[]) {
                respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                    (byte[])documentoEnviado, documentContentType, urlToSendDocument);
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        } finally {
            respuesta.setId(id);
            return respuesta;
        }
    }

}
