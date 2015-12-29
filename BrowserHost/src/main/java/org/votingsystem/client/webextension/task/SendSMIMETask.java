package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.dialog.CertNotFoundDialog;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SendSMIMETask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(SendSMIMETask.class.getSimpleName());

    private String[] headers;
    private char[] password;
    private OperationVS operationVS;
    private String documentToSign;

    public SendSMIMETask(OperationVS operationVS, String documentToSign, char[] password, String... headers) throws Exception {
        this.operationVS = operationVS;
        this.headers = headers;
        this.password = password;
        this.documentToSign = documentToSign;
    }

    @Override protected ResponseVS call() throws Exception {
        ResponseVS responseVS = null;
        try {
            updateProgress(1, 10);
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, operationVS.getReceiverName(),
                    documentToSign, password, operationVS.getSignedMessageSubject());
            updateProgress(3, 10);
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    operationVS.getServiceURL(), headers);
            updateProgress(10, 10);
            operationVS.processResult(responseVS);
        }catch (KeyStoreExceptionVS ex) {
            CertNotFoundDialog.showDialog();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            operationVS.processResult(ResponseVS.ERROR(ex.getMessage()));
        }
        return responseVS;
    }
}
