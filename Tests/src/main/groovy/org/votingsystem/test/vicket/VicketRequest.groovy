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
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.ObjectUtils
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketRequestBatch

import java.security.cert.X509Certificate


Logger logger = TestHelper.init(VicketRequest.class)

SignatureVSService signatureVSService = SignatureVSService.getUserVSSignatureVSService("./certs/Cert_UserVS_07553172H.jks")
UserVS fromUserVS = signatureVSService.getUserVS()

VicketServer vicketServer = TestHelper.loadVicketServer()
if(vicketServer == null) throw new ExceptionVS("vicketServer not found. Program finished")
ContextVS.getInstance().setDefaultServer(vicketServer)

BigDecimal transactionAmount = new BigDecimal(10)
String curencyCode = "EUR"
VicketTagVS tag = new VicketTagVS("HIDROGENO")
VicketRequestBatch vicketBatch = new VicketRequestBatch(transactionAmount, transactionAmount, curencyCode, tag,
        ContextVS.getInstance().getVicketServer())
String messageSubject = "TEST_VICKET_REQUEST_DATA_MSG_SUBJECT";

JSONArray vicketCSRRequest = (JSONArray) JSONSerializer.toJSON(vicketBatch.getVicketCSRList());
Map<String, Object> mapToSend = new HashMap<String, Object>();
mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(), vicketCSRRequest.toString().getBytes());
SMIMEMessage smimeMessage = signatureVSService.getTimestampedSignedMimeMessage(fromUserVS.nif,
        vicketServer.getNameNormalized(), vicketBatch.getRequestDataToSignJSON().toString(), messageSubject)
mapToSend.put(ContextVS.VICKET_REQUEST_DATA_FILE_NAME + ":" + ContentTypeVS.JSON_SIGNED.getName(),
        smimeMessage.getBytes());
ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, vicketServer.getVicketRequestServiceURL());
if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
    JSONObject issuedVicketsJSON = JSONSerializer.toJSON(new String(responseVS.getMessageBytes(), "UTF-8"));
    JSONArray transactionsArray = issuedVicketsJSON.getJSONArray("transactionList");
    for(int i = 0; i < transactionsArray.size(); i++) {
        TransactionVS transaction = TransactionVS.parse(transactionsArray.getJSONObject(i));
    }
    JSONArray issuedVicketsArray = issuedVicketsJSON.getJSONArray("issuedVickets");
    logger.debug("VicketRequest - Num IssuedVickets: " + issuedVicketsArray.size());
    if(issuedVicketsArray.size() != vicketBatch.getVicketsMap().values().size()) {
        logger.error("VicketRequest(...) - ERROR - Num vickets requested: " + vicketBatch.getVicketsMap().values().size() +
                " - num. vickets received: " + issuedVicketsArray.size());
    }
    for(int i = 0; i < issuedVicketsArray.size(); i++) {
        Vicket vicket = vicketBatch.initVicket(issuedVicketsArray.getString(i));
        byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
        String walletDir =   ContextVS.getInstance().config.walletDir
        new File(walletDir).mkdirs();
        String vicketPath = walletDir + UUID.randomUUID().toString() + ".servs";
        File vicketFile = FileUtils.copyStreamToFile(new ByteArrayInputStream(vicketSerialized), new File(vicketPath))
        Vicket vicketDeSerializedFromFile = ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(vicketFile));
        logger.debug("Stored vicket: " + vicketFile.getAbsolutePath())
    }
} else {
    logger.error(" --- ERROR --- " + responseVS.getMessage())
}
System.exit(0)