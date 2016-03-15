package org.votingsystem.test.misc;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CheckTransactionFromUserToUser {

    private static Logger log =  Logger.getLogger(CheckTransactionFromUserToUser.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CMSSignedMessage cmsMessage = new CMSSignedMessage(ContextVS.getInstance().getResourceBytes("2222.p7s"));
        TransactionDto dto = cmsMessage.getSignedContent(TransactionDto.class);
        CMSSignedMessage receipt = CMSSignedMessage.FROM_PEM(dto.getCmsMessagePEM());
        log.info("receipt.getSignedContent: " + receipt.getSignedContent());
        System.exit(0);
    }

}
