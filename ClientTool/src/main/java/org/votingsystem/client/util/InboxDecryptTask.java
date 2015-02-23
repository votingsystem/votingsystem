package org.votingsystem.client.util;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.model.ResponseVS;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDecryptTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(InboxDecryptTask.class);

    PrivateKey privateKey;
    private InboxMessage timeLimitedInboxMessage;

    public InboxDecryptTask(PrivateKey privateKey, InboxMessage timeLimitedInboxMessage) {
        this.privateKey = privateKey;
        this.timeLimitedInboxMessage = timeLimitedInboxMessage;
    }

    @Override protected ResponseVS call() throws Exception {
        List<InboxMessage> messageList = null;
        if(timeLimitedInboxMessage == null) messageList = InboxService.getInstance().getEncryptedMessageList();
        else messageList = Arrays.asList(timeLimitedInboxMessage);
        try {
            int i = 0;
            for(InboxMessage inboxMessage : messageList) {
                updateProgress(i++, messageList.size());
                if(inboxMessage.isEncrypted() && privateKey != null) {
                    inboxMessage.decryptMessage(privateKey);
                }
            }
            InboxService.getInstance().updateDecryptedMessages(messageList);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return null;
    }
}