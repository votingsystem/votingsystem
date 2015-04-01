package org.votingsystem.test.currency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class BankVS_add {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(BankVS_add.class);
        Map requestDataMap = new HashMap<>();
        requestDataMap.put("info","Voting System Test Bank - " + DateUtils.getDayWeekDateStr(new Date()));
        requestDataMap.put("certChainPEM",new String(ContextVS.getInstance().getResourceBytes(
                "./certs/Cert_BANKVS_03455543T.pem"),"UTF-8"));
        requestDataMap.put("IBAN","ES1877777777450000000050");
        requestDataMap.put("operation","BANKVS_NEW");
        requestDataMap.put("UUID", UUID.randomUUID().toString());

        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        UserVS fromUserVS = superUserSignatureService.getUserVS();

        String messageSubject = "TEST_ADD_BANKVS";
        String contentStr = new ObjectMapper().writeValueAsString(requestDataMap);
        SMIMEMessage smimeMessage = superUserSignatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                currencyServer.getName(), contentStr, messageSubject);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                currencyServer.getSaveBankServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }

}






