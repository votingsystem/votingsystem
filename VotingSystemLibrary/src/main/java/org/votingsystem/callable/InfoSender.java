package org.votingsystem.callable;

import org.apache.log4j.Logger;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class InfoSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(InfoSender.class);

    private Integer id = null;
    private String urlToSendDocument;
    private Object data;
    private String documentContentType = null;
    private List<String> headerNameList = new ArrayList<String>();

    public InfoSender(Integer id, Object data, String documentContentType, String urlToSendDocument,
                      String... headerNames) {
        if(headerNames != null) {
            for(String headerName: headerNames) {
                headerNameList.add(headerName);
            }
        }
        this.id = id;
        this.data = data;
        this.documentContentType = documentContentType;
        this.urlToSendDocument = urlToSendDocument;
    }
    
    public InfoSender setDocumentToSend(Object data,
            String documentContentType) {
        this.data = data;
        this.documentContentType = documentContentType;
        return this;
    }
    
    @Override public ResponseVS call() { 
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        try {
            if(data instanceof File) {
                responseVS = HttpHelper.getInstance().sendFile((File)data, documentContentType, urlToSendDocument,
                        headerNameList.toArray(new String[headerNameList.size()]));
            } else if(data instanceof byte[]) {
                responseVS = HttpHelper.getInstance().sendData((byte[])data, documentContentType, urlToSendDocument,
                        headerNameList.toArray(new String[headerNameList.size()]));
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            responseVS.appendMessage(ex.getMessage());
        } finally { return responseVS; }
    }

}
