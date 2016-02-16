package org.votingsystem.test.misc;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;

import java.util.Base64;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CheckTransactionFromUserToUser {

    private static Logger log =  Logger.getLogger(CheckTransactionFromUserToUser.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SMIMEMessage smimeMessage = new SMIMEMessage(ContextVS.getInstance().getResourceBytes("2222.p7s"));
        TransactionVSDto dto = smimeMessage.getSignedContent(TransactionVSDto.class);
        SMIMEMessage receipt = new SMIMEMessage(Base64.getDecoder().decode(dto.getMessageSMIME()));
        log.info("receipt.getSignedContent: " + receipt.getSignedContent());
        System.exit(0);
    }

}
