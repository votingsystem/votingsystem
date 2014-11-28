package org.votingsystem.test.vicket

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.WalletUtils
import org.votingsystem.vicket.model.VicketRequestBatch

Logger log = TestUtils.init(Vicket_request.class)

SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER)
UserVS fromUserVS = signatureService.getUserVS()

VicketServer vicketServer = TestUtils.fetchVicketServer(ContextVS.getInstance().config.vicketServerURL)
ContextVS.getInstance().setDefaultServer(vicketServer)

BigDecimal totalAmount = new BigDecimal(10)
String curencyCode = "EUR"
TagVS tag = new TagVS("HIDROGENO")
Boolean isTimeLimited = true
VicketRequestBatch vicketBatch = new VicketRequestBatch(totalAmount, totalAmount, curencyCode, tag, isTimeLimited,
        ContextVS.getInstance().getVicketServer())
String messageSubject = "TEST_VICKET_REQUEST_DATA_MSG_SUBJECT";
Map<String, Object> mapToSend = new HashMap<String, Object>();
mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(), vicketBatch.getVicketCSRRequest().toString().getBytes());
SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(fromUserVS.nif,
        vicketServer.getNameNormalized(), vicketBatch.getRequestDataToSignJSON().toString(), messageSubject)
mapToSend.put(ContextVS.VICKET_REQUEST_DATA_FILE_NAME + ":" + ContentTypeVS.JSON_SIGNED.getName(),
        smimeMessage.getBytes());
ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, vicketServer.getVicketRequestServiceURL());
if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
    JSONObject responseJSON = JSONSerializer.toJSON(new String(responseVS.getMessageBytes(), "UTF-8"))
    vicketBatch.initVickets(responseJSON.getJSONArray("issuedVickets"));
    WalletUtils.saveVicketsToDir(vicketBatch.getVicketsMap().values(), ContextVS.getInstance().config.walletDir)
} else {
    log.error(" --- ERROR --- " + responseVS.getMessage())
}
System.exit(0)