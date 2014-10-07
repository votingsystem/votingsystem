package org.votingsystem.test.vicket

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VicketServer
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.test.util.TestHelper
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.ObjectUtils
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.Vicket

import java.security.cert.X509Certificate

Logger logger = TestHelper.init(VicketSendFromDisk.class)
VicketServer vicketServer = TestHelper.loadVicketServer()
ContextVS.getInstance().setDefaultServer(vicketServer)

String vicketDeSerializedFilePath = "/home/jgzornoza/temp/vickets/2014/oct/07/1d62077e-9e95-4aff-9053-d99ea5a1bd4f.servs"


File vicketDeSerializedFile = new File(vicketDeSerializedFilePath)
byte[] vicketDeSerializedBytes = FileUtils.getBytesFromFile(vicketDeSerializedFile)
Vicket vicketDeSerializedFromFile = ObjectUtils.deSerializeObject(vicketDeSerializedBytes);

logger.debug("vicketDeSerializedFromFile: " + vicketDeSerializedFromFile.certificationRequest)
//we have al the Vickets initialized, now we can make de transactions
List<Vicket> vicketList = Arrays.asList(vicketDeSerializedFromFile)
List<String> vicketTransactionBatch = new ArrayList<String>();
for(Vicket vicket:vicketList) {
    JSONObject transactionRequest = vicket.getTransaction("toUserName", "ES0878788989450000000007",
            "First Vicket Transaction", false)

    smimeMessage = vicket.getCertificationRequest().genMimeMessage(vicket.getHashCertVS(),
            StringUtils.getNormalized(vicket.getToUserName()),
            transactionRequest.toString(), vicket.getSubject(), null);
    MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, vicketServer.getTimeStampServiceURL())
    responseVS = timeStamper.call();
    if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
        logger.error(responseVS.getMessage());
        System.exit(0)
    }
    smimeMessage = timeStamper.getSmimeMessage();
    String smimeMessageStr = new String(org.bouncycastle.util.encoders.Base64.encode(smimeMessage.getBytes()), "UTF-8")
    vicketTransactionBatch.add(smimeMessageStr)
}
JSONArray transactionBatchJSON = JSONSerializer.toJSON(vicketTransactionBatch)
responseVS = HttpHelper.getInstance().sendData(transactionBatchJSON.toString().getBytes(),
        ContentTypeVS.JSON, vicketServer.getVicketTransactionServiceURL());
logger.debug("Vicket Transaction result: " + responseVS.getStatusCode())
if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
    logger.debug(responseVS.getMessage())
    System.exit(0)
}


JSONArray transactionBatchResponseJSON = JSONArray.fromObject(responseVS.getMessage())
for(int i = 0; i < transactionBatchResponseJSON.size(); i++) {
    JSONObject receiptData = transactionBatchResponseJSON.get(i)
    String hashCertVS = receiptData.keySet().iterator().next()

    String receiptStr = new String(Base64.getDecoder().decode(receiptData.getString(hashCertVS).getBytes()), "UTF-8")
    SMIMEMessage smimeReceipt = new SMIMEMessage(new ByteArrayInputStream(receiptStr.getBytes()))
    for(X509Certificate cert : smimeReceipt.getSignersCerts()) {
        CertUtil.verifyCertificate(vicketServer.getTrustAnchors(), false, Arrays.asList(cert));
        logger.debug("Cert validated: " + cert.getSubjectDN().toString());
    }
    String signatureHashCertVS = CertUtil.getHashCertVS(smimeMessage.getCertWithCertExtension(), ContextVS.VICKET_OID);
    if(!signatureHashCertVS.equals(vicketDeSerializedFromFile.getHashCertVS())) throw new ExceptionVS ("signatureHashCertVS: " +
            signatureHashCertVS + " - vicket hashCertVS: " + vicketDeSerializedFromFile.getHashCertVS())
}

