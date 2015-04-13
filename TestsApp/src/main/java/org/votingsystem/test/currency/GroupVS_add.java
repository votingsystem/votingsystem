package org.votingsystem.test.currency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;

import java.util.*;
import java.util.logging.Logger;


public class GroupVS_add {
    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(GroupVS_add.class);
        Map requestDataMap = new HashMap<>();
        requestDataMap.put("groupvsInfo", "GroupVS From TESTS Description - " + DateUtils.getDayWeekDateStr(new Date()));
        requestDataMap.put("tags", new ArrayList<>());
        requestDataMap.put("groupvsName", "GroupVS From TESTS - " + DateUtils.getDayWeekDateStr(new Date()));
        requestDataMap.put("operation","CURRENCY_GROUP_NEW");
        requestDataMap.put("UUID", UUID.randomUUID().toString());
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("00111222V", UserVS.Type.USER);
        UserVS fromUserVS = representativeSignatureService.getUserVS();
        String messageSubject = "TEST_ADD_GROUPVS";
        String contentStr = new ObjectMapper().writeValueAsString(requestDataMap);;
        SMIMEMessage smimeMessage = representativeSignatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                currencyServer.getName(), contentStr, messageSubject);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                currencyServer.getSaveGroupVSServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }

}




