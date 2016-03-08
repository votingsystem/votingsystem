package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SendCMSTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(SendCMSTask.class.getName());

    private String[] headers;
    private char[] password;
    private OperationVS operationVS;
    private String documentToSign;
    private String message;

    public SendCMSTask(OperationVS operationVS, String documentToSign, String message, char[] password,
                       String... headers) throws Exception {
        this.operationVS = operationVS;
        this.headers = headers;
        this.password = password;
        this.documentToSign = documentToSign;
        this.message = message;
    }

    @Override protected ResponseVS call() throws Exception {
        ResponseVS responseVS = null;
        try {
            updateMessage(message);
            updateProgress(1, 10);
            CMSSignedMessage cmsMessage = BrowserSessionService.getCMS(null, operationVS.getReceiverName(),
                    documentToSign, password, operationVS.getSignedMessageSubject());
            updateProgress(3, 10);
            responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                    operationVS.getServiceURL(), headers);
            updateProgress(10, 10);
        } catch (KeyStoreExceptionVS ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = ResponseVS.ERROR(ex.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = ResponseVS.ERROR(ex.getMessage());
        } finally {
            operationVS.processResult(responseVS);
        }
        return responseVS;
    }
}
