package org.votingsystem.test.vicket


import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketServer
import org.votingsystem.model.VicketTagVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureVSService
import org.votingsystem.test.util.TestHelper
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.WalletUtils
import org.votingsystem.vicket.model.VicketRequestBatch


Logger logger = TestHelper.init(VicketRequest.class)

SignatureVSService signatureVSService = SignatureVSService.getUserVSSignatureVSService("./certs/Cert_UserVS_07553172H.jks")
UserVS fromUserVS = signatureVSService.getUserVS()

VicketServer vicketServer = TestHelper.loadVicketServer()
if(vicketServer == null) throw new ExceptionVS("Vicket server not found")
ContextVS.getInstance().setDefaultServer(vicketServer)

BigDecimal totalAmount = new BigDecimal(10)
String curencyCode = "EUR"
VicketTagVS tag = new VicketTagVS("HIDROGENO")
VicketRequestBatch vicketBatch = new VicketRequestBatch(totalAmount, totalAmount, curencyCode, tag,
        ContextVS.getInstance().getVicketServer())
String messageSubject = "TEST_VICKET_REQUEST_DATA_MSG_SUBJECT";
Map<String, Object> mapToSend = new HashMap<String, Object>();
mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(), vicketBatch.getVicketCSRRequest().toString().getBytes());
SMIMEMessage smimeMessage = signatureVSService.getTimestampedSignedMimeMessage(fromUserVS.nif,
        vicketServer.getNameNormalized(), vicketBatch.getRequestDataToSignJSON().toString(), messageSubject)
mapToSend.put(ContextVS.VICKET_REQUEST_DATA_FILE_NAME + ":" + ContentTypeVS.JSON_SIGNED.getName(),
        smimeMessage.getBytes());
ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, vicketServer.getVicketRequestServiceURL());
if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
    vicketBatch.initVickets(JSONSerializer.toJSON(new String(responseVS.getMessageBytes(), "UTF-8")));
    WalletUtils.saveVicketsToWallet(vicketBatch.getVicketsMap().values(), ContextVS.getInstance().config.walletDir)
} else {
    logger.error(" --- ERROR --- " + responseVS.getMessage())
}
System.exit(0)