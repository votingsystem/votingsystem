package org.votingsystem.test.cooin

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper

Logger log = TestUtils.init(GroupVS_add.class)

Map requestDataMap = [groupvsInfo:"GroupVS From TESTS Description - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()),
        tags:[], groupvsName:"GroupVS From TESTS - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()),
        operation:'COOIN_GROUP_NEW', UUID:UUID.randomUUID().toString()]

CooinServer cooinServer = TestUtils.fetchCooinServer(ContextVS.getInstance().config.cooinServerURL)
ContextVS.getInstance().setDefaultServer(cooinServer)
SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("00111222V", UserVS.Type.USER)
UserVS fromUserVS = representativeSignatureService.getUserVS()
String messageSubject = "TEST_ADD_GROUPVS";
SMIMEMessage smimeMessage = representativeSignatureService.getSMIMETimeStamped(fromUserVS.nif,
        cooinServer.getName(), JSONSerializer.toJSON(requestDataMap).toString(), messageSubject)
ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
        cooinServer.getSaveGroupVSServiceURL())
log.debug("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage())

System.exit(0)


