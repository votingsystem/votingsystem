package org.votingsystem.test.cooin

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.Wallet
import org.votingsystem.cooin.model.CooinRequestBatch

Logger log = TestUtils.init(Cooin_request.class)

SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER)
UserVS fromUserVS = signatureService.getUserVS()

CooinServer cooinServer = TestUtils.fetchCooinServer(ContextVS.getInstance().config.cooinServerURL)
ContextVS.getInstance().setDefaultServer(cooinServer)

BigDecimal totalAmount = new BigDecimal(10)
String curencyCode = "EUR"
TagVS tag = new TagVS("HIDROGENO")
Boolean isTimeLimited = true
CooinRequestBatch cooinBatch = new CooinRequestBatch(totalAmount, totalAmount, curencyCode, tag, isTimeLimited,
        ContextVS.getInstance().getCooinServer())
String messageSubject = "TEST_COOIN_REQUEST_DATA_MSG_SUBJECT";
Map<String, Object> mapToSend = new HashMap<String, Object>();
mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(), cooinBatch.getCooinCSRRequest().toString().getBytes());
SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(fromUserVS.nif,
        cooinServer.getName(), cooinBatch.getRequestDataToSignJSON().toString(), messageSubject)
mapToSend.put(ContextVS.COOIN_REQUEST_DATA_FILE_NAME + ":" + ContentTypeVS.JSON_SIGNED.getName(),
        smimeMessage.getBytes());
ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, cooinServer.getCooinRequestServiceURL());
if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
    JSONObject responseJSON = JSONSerializer.toJSON(new String(responseVS.getMessageBytes(), "UTF-8"))
    cooinBatch.initCooins(responseJSON.getJSONArray("issuedCooins"));
    Wallet.saveCooinsToDir(cooinBatch.getCooinsMap().values(), ContextVS.getInstance().config.walletDir)
} else {
    log.error(" --- ERROR --- " + responseVS.getMessage())
}
System.exit(0)