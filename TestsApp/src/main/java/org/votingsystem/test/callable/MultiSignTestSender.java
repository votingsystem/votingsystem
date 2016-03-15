package org.votingsystem.test.callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MultiSignTestSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(MultiSignTestSender.class.getName());

    private String nif;
    private String serverURL = null;

    public MultiSignTestSender(String nif, String serverURL) throws Exception {
        this.nif = nif;
        this.serverURL = serverURL;
    }
    
    @Override public ResponseVS call() throws Exception {
        SignatureService signatureService = SignatureService.load(this.nif);
        CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(getRequest(nif).getBytes());
        ResponseVS responseVS = HttpHelper.getInstance().sendData(
                cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED, serverURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            CMSSignedMessage cmsResponse = CMSSignedMessage.FROM_PEM(responseVS.getMessageBytes());
            log.info("- cmsResponse.isValidSignature(): " + cmsResponse.isValidSignature());
        } else throw new ExceptionVS(responseVS.getMessage());
        return responseVS;
    }

    private String getRequest(String from) throws JsonProcessingException {
        Map map = new HashMap();
        map.put("from", from);
        map.put("UUID", UUID.randomUUID().toString());
        return JSON.getMapper().writeValueAsString(map);
    }

}
