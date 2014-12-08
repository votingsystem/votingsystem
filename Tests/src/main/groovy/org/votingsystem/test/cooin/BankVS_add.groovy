package org.votingsystem.test.cooin

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper

Logger log = TestUtils.init(Cooin_request.class)

Map requestDataMap = [info:"Voting System Test Bank - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()),
        certChainPEM:new String(ContextVS.getInstance().getResourceBytes("./certs/Cert_BANKVS_03455543T.pem"),"UTF-8"),
        IBAN:"ES1877777777450000000050", operation:"BANKVS_NEW", UUID:UUID.randomUUID().toString()]

CooinServer cooinServer = TestUtils.fetchCooinServer(ContextVS.getInstance().config.cooinServerURL)
ContextVS.getInstance().setDefaultServer(cooinServer)
SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER)
UserVS fromUserVS = superUserSignatureService.getUserVS()

String messageSubject = "TEST_ADD_BANKVS";
SMIMEMessage smimeMessage = superUserSignatureService.getSMIMETimeStamped(fromUserVS.nif,
        cooinServer.getNameNormalized(), JSONSerializer.toJSON(requestDataMap).toString(), messageSubject)
ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
        cooinServer.getSaveBankServiceURL())
log.debug("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage())

System.exit(0)


