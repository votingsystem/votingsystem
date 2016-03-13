package org.votingsystem.test.currency;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.BankVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.*;

import java.util.Date;
import java.util.logging.Logger;

public class PublishBankVS {

    private static Logger log =  Logger.getLogger(PublishBankVS.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        String info = "Voting System Test Bank - " + DateUtils.getDayWeekDateStr(new Date(), "HH:mm:ss");
        String certChainPEM = new String(ContextVS.getInstance().getResourceBytes(
                "./certs/Cert_BANKVS_03455543T.pem"),"UTF-8");
        BankVSDto bankVSDto = BankVSDto.NEW("ES1877777777450000000050", info, certChainPEM);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService(
                "Currency_07553172H", UserVS.Type.USER);
        CMSSignedMessage cmsMessage = superUserSignatureService.addSignatureWithTimeStampToUnsignedAttributes(
                JSON.getMapper().writeValueAsString(bankVSDto));
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                currencyServer.getSaveBankServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }

}






