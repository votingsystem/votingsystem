package org.votingsystem.client.webextension.util;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.service.InboxService;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.webextension.BrowserHost.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDecryptTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(InboxDecryptTask.class.getSimpleName());

    PrivateKey privateKey;
    private InboxMessage timeLimitedInboxMessage;

    public InboxDecryptTask(PrivateKey privateKey, InboxMessage timeLimitedInboxMessage) {
        this.privateKey = privateKey;
        this.timeLimitedInboxMessage = timeLimitedInboxMessage;
    }

    @Override protected ResponseVS call() throws Exception {
        updateMessage(ContextVS.getMessage("decryptingMessagesMsg"));
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
            log.log(Level.SEVERE, ex.getMessage(), ex);
            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return null;
    }
}