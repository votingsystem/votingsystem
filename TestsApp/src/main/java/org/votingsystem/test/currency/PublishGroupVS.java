package org.votingsystem.test.currency;

import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.*;

import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;


public class PublishGroupVS {

    private static Logger log =  Logger.getLogger(PublishGroupVS.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        GroupVSDto groupVSDto = new GroupVSDto();
        String description = "GroupVS From TESTS Description - " + DateUtils.getDayWeekDateStr(new Date(), "HH:mm:ss");
        groupVSDto.setDescription(Base64.getEncoder().encodeToString(description.getBytes()));
        groupVSDto.setTags(new HashSet<>());
        groupVSDto.setName("GroupVS From TESTS - " + DateUtils.getDayWeekDateStr(new Date(), "HH:mm:ss"));
        groupVSDto.setOperation(TypeVS.CURRENCY_GROUP_NEW);
        groupVSDto.setUUID(UUID.randomUUID().toString());
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("00111222V", UserVS.Type.USER);
        UserVS fromUserVS = representativeSignatureService.getUserVS();
        String messageSubject = "TEST_ADD_GROUPVS";
        SMIMEMessage smimeMessage = representativeSignatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                currencyServer.getName(), JSON.getMapper().writeValueAsString(groupVSDto), messageSubject);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                currencyServer.getSaveGroupVSServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }

}




