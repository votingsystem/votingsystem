package org.votingsystem.applet.callable;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.votingsystem.applet.util.HttpHelper;
import org.votingsystem.model.ResponseVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class InfoGetter implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(InfoGetter.class);
 
    private String urlDocument;
    private String contentType = null;
    private Integer id;
    
    public InfoGetter(Integer id, String urlDocument, String contentType) {
        this.id = id;
        this.urlDocument = urlDocument;
        this.contentType = contentType;
    }

    @Override public ResponseVS call() { 
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
        try {
            responseVS = HttpHelper.INSTANCE.
                    getData(urlDocument, contentType);
        } catch(Exception ex){
            logger.error(ex.getMessage(), ex);
            responseVS.appendErrorMessage(ex.getMessage());
        } finally {
            return responseVS;
        }
    }
    
}