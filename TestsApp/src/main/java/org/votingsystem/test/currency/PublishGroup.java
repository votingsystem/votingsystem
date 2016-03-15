package org.votingsystem.test.currency;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.GroupDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.*;

import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;


public class PublishGroup {

    private static Logger log =  Logger.getLogger(PublishGroup.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        GroupDto groupDto = new GroupDto();
        String description = "Group From TESTS Description - " + DateUtils.getDayWeekDateStr(new Date(), "HH:mm:ss");
        groupDto.setDescription(Base64.getEncoder().encodeToString(description.getBytes()));
        groupDto.setTags(new HashSet<>());
        groupDto.setName("Group From TESTS - " + DateUtils.getDayWeekDateStr(new Date(), "HH:mm:ss"));
        groupDto.setOperation(TypeVS.CURRENCY_GROUP_NEW);
        groupDto.setUUID(UUID.randomUUID().toString());
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        SignatureService representativeSignatureService = SignatureService.getUserSignatureService(
                "Currency_07553172H", User.Type.USER);
        CMSSignedMessage cmsMessage = representativeSignatureService.signDataWithTimeStamp(
                JSON.getMapper().writeValueAsBytes(groupDto));
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                currencyServer.getSaveGroupServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }

}




