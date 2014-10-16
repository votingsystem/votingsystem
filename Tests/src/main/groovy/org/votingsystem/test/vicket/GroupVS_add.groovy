package org.votingsystem.test.vicket

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureVSService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper

Logger logger = TestUtils.init(Vicket_request.class)

Map requestDataMap = [groupvsInfo:"GroupVS From TESTS Description - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()),
        tags:[], groupvsName:"GroupVS From TESTS - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()),
        operation:'VICKET_GROUP_NEW', UUID:UUID.randomUUID().toString()]

VicketServer vicketServer = TestUtils.fetchVicketServer(ContextVS.getInstance().config.vicketServerURL)
ContextVS.getInstance().setDefaultServer(vicketServer)
SignatureVSService representativeSignatureService = SignatureVSService.getUserVSSignatureVSService("./certs/Cert_UserVS_00111222V.jks")
UserVS fromUserVS = representativeSignatureService.getUserVS()
String messageSubject = "TEST_ADD_GROUPVS";
SMIMEMessage smimeMessage = representativeSignatureService.getTimestampedSignedMimeMessage(fromUserVS.nif,
        vicketServer.getNameNormalized(), JSONSerializer.toJSON(requestDataMap).toString(), messageSubject)
ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
        vicketServer.getSaveGroupVSServiceURL())
logger.debug("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage())

System.exit(0)


