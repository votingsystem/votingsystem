package org.votingsystem.test.vicket

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketServer
import org.votingsystem.model.VicketTagVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.test.util.SignatureVSService
import org.votingsystem.test.util.TestHelper
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketRequestBatch

import java.security.cert.X509Certificate


Logger logger = TestHelper.init(VicketRequest.class)

SignatureVSService signatureVSService = SignatureVSService.getUserVSSignatureVSService("./certs/Cert_UserVS_07553172H.jks")
UserVS fromUserVS = signatureVSService.getUserVS()

VicketServer vicketServer = TestHelper.loadVicketServer()
if(vicketServer == null) {
    logger.error("vicketServer not found. Program finished")
    return
}
ContextVS.getInstance().setDefaultServer(vicketServer)

BigDecimal transactionAmount = new BigDecimal(10)
String curencyCode = "EUR"
VicketTagVS tag = new VicketTagVS("HIDROGENO")
VicketRequestBatch vicketBatch = new VicketRequestBatch(transactionAmount, transactionAmount, curencyCode, tag,
        ContextVS.getInstance().getVicketServer())

ResponseVS responseVS = null;
try {
    String messageSubject = "TEST_VICKET_REQUEST_DATA_MSG_SUBJECT";

    JSONArray vicketCSRRequest = (JSONArray) JSONSerializer.toJSON(vicketBatch.getVicketCSRList());
    Map<String, Object> mapToSend = new HashMap<String, Object>();
    mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(), vicketCSRRequest.toString().getBytes());
    SMIMEMessage smimeMessage = signatureVSService.getTimestampedSignedMimeMessage(fromUserVS.nif,
            vicketServer.getNameNormalized(), vicketBatch.getRequestDataToSignJSON().toString(), messageSubject)
    mapToSend.put(ContextVS.VICKET_REQUEST_DATA_FILE_NAME + ":" + ContentTypeVS.JSON_SIGNED.getName(),
            smimeMessage.getBytes());
    responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, vicketServer.getVicketRequestServiceURL());
    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
        JSONObject issuedVicketsJSON = JSONSerializer.toJSON(new String(responseVS.getMessageBytes(), "UTF-8"));
        JSONArray transactionsArray = issuedVicketsJSON.getJSONArray("transactionList");
        for(int i = 0; i < transactionsArray.size(); i++) {
            TransactionVS transaction = TransactionVS.parse(transactionsArray.getJSONObject(i));
        }
        JSONArray issuedVicketsArray = issuedVicketsJSON.getJSONArray("issuedVickets");
        logger.debug("VicketRequest - Num IssuedVickets: " + issuedVicketsArray.size());
        if(issuedVicketsArray.size() != vicketBatch.getVicketsMap().values().size()) {
            logger.error("VicketRequest(...) - ERROR - Num vickets requested: " +
                    vicketBatch.getVicketsMap().values().size() + " - num. vickets received: " +
                    issuedVicketsArray.size());
        }
        for(int i = 0; i < issuedVicketsArray.size(); i++) {
            vicketBatch.initVicket(issuedVicketsArray.getString(i));
        }
        //we have al the Vickets initialized, now we can make de transactions
        List<String> vicketTransactionBatch = new ArrayList<String>();
        for(Vicket vicket:vicketBatch.vicketsMap.values()) {
            JSONObject transactionRequest = vicket.getTransactionRequest("toUserName", "ES6778788989450000000012",
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
        JSONArray transactionBatchResponseJSON = JSONArray.fromObject(responseVS.getMessage())
        for(int i = 0; i < transactionBatchResponseJSON.size(); i++) {
            JSONObject receiptData = transactionBatchResponseJSON.get(i)
            String hashCertVS = receiptData.keySet().iterator().next()
            Vicket vicket = vicketBatch.getVicketsMap().get(hashCertVS)
            String receiptStr = new String(Base64.getDecoder().decode(receiptData.getString(hashCertVS).getBytes()), "UTF-8")
            SMIMEMessage smimeReceipt = new SMIMEMessage(new ByteArrayInputStream(receiptStr.getBytes()))
            for(X509Certificate cert : smimeReceipt.getSignersCerts()) {
                CertUtil.verifyCertificate(vicketServer.getTrustAnchors(), false, Arrays.asList(cert));
                logger.debug("Cert validated: " + cert.getSubjectDN().toString());
            }
            String signatureHashCertVS = CertUtil.getHashCertVS(smimeMessage.getCertWithCertExtension(), ContextVS.VICKET_OID);
            if(!signatureHashCertVS.equals(vicket.getHashCertVS())) throw new ExceptionVS ("signatureHashCertVS: " +
                    signatureHashCertVS + " - vicket hashCertVS: " + vicket.getHashCertVS())
        }

    } else {
        logger.error(" --- ERROR --- " + responseVS.getMessage())
    }
} catch(Exception ex) {
    ex.printStackTrace();
}
System.exit(0)