package org.votingsystem.test

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketServer
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.model.TransactionVS
import org.votingsystem.test.model.VicketBatch
import org.votingsystem.test.util.SignatureVSService
import org.votingsystem.test.util.TestHelper
import org.votingsystem.util.HttpHelper


Logger logger = TestHelper.init(VicketRequest.class)

String keyStorePath = ContextVS.getInstance().getConfig().keyStorePathAuthority
String keyAlias = ContextVS.getInstance().config.signKeysAlias
String password = ContextVS.getInstance().config.signKeysPassword

SignatureVSService signatureVSService = new SignatureVSService(keyStorePath, keyAlias, password)
UserVS fromUserVS = signatureVSService.getUserVS()

VicketServer vicketServer = TestHelper.loadVicketServer()
if(vicketServer == null) {
    logger.error("vicketServer not found. Program finished")
    return
}
ContextVS.getInstance().setDefaultServer(vicketServer)

BigDecimal transactionAmount = new BigDecimal(10)
String curencyCode = "EUR"
VicketBatch vicketBatch = new VicketBatch(transactionAmount, transactionAmount, curencyCode,
        ContextVS.getInstance().getVicketServer())

ResponseVS responseVS = null;
try {
    String messageSubject = "TEST_VICKET_REQUEST_DATA_MSG_SUBJECT";

    Map<String, Object> mapToSend = new HashMap<String, Object>();
    mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(),
            vicketBatch.getVicketCSRRequest().toString().getBytes());
    SMIMEMessage smimeMessage = signatureVSService.getTimestampedSignedMimeMessage(fromUserVS.nif,
            vicketServer.getNameNormalized(), vicketBatch.getRequestJSON().toString(), messageSubject)
    mapToSend.put(ContextVS.VICKET_REQUEST_DATA_FILE_NAME + ":" + ContentTypeVS.JSON_SIGNED.getName(),
            smimeMessage.getBytes());
    responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, vicketServer.getVicketRequestServiceURL());
    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
        JSONObject issuedVicketsJSON = JSONSerializer.toJSON(new String(responseVS.getMessageBytes(), "UTF-8"));
        JSONArray transactionsArray = issuedVicketsJSON.getJSONArray("transactionList");
        for(int i = 0; i < transactionsArray.length(); i++) {
            TransactionVS transaction = TransactionVS.parse(transactionsArray.getJSONObject(i));
        }
        JSONArray issuedVicketsArray = issuedVicketsJSON.getJSONArray("issuedVickets");
        logger.debug("VicketRequest - Num IssuedVickets: " + issuedVicketsArray.size());
        if(issuedVicketsArray.size() != vicketBatch.getVicketsMap().values().size()) {
            logger.error("VicketRequest(...) - ERROR - Num vickets requested: " +
                    vicketBatch.getVicketsMap().values().size() + " - num. vickets received: " +
                    issuedVicketsArray.size());
        }
        for(int i = 0; i < issuedVicketsArray.length(); i++) {
            vicketBatch.initVicket(issuedVicketsArray.getString(i));
        }

    } else {
        logger.error(responseVS.getMessage())
    }
} catch(Exception ex) {
    ex.printStackTrace();
}
System.exit(0)