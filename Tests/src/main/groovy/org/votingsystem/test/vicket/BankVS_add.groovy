package org.votingsystem.test.vicket

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper

Logger logger = TestUtils.init(Vicket_request.class)

Map requestDataMap = [info:"Voting System Test Bank - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()),
        certChainPEM:new String(ContextVS.getInstance().getResourceBytes("./certs/Cert_BankVS_03455543T.pem"),"UTF-8"),
        IBAN:"ES1877777777450000000050", operation:"BANKVS_NEW", UUID:UUID.randomUUID().toString()]

VicketServer vicketServer = TestUtils.fetchVicketServer(ContextVS.getInstance().config.vicketServerURL)
ContextVS.getInstance().setDefaultServer(vicketServer)
SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService("./certs/Cert_UserVS_07553172H.jks")
UserVS fromUserVS = superUserSignatureService.getUserVS()

String messageSubject = "TEST_ADD_BANKVS";
SMIMEMessage smimeMessage = superUserSignatureService.getTimestampedSignedMimeMessage(fromUserVS.nif,
        vicketServer.getNameNormalized(), JSONSerializer.toJSON(requestDataMap).toString(), messageSubject)
ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
        vicketServer.getSaveBankServiceURL())
logger.debug("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage())

System.exit(0)


