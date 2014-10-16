package org.votingsystem.test.vicket

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VicketServer
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.vicket.model.VicketTransactionBatch


Logger logger = TestUtils.init(Vicket_sendFromWallet.class)

VicketServer vicketServer = TestUtils.fetchVicketServer(ContextVS.getInstance().config.vicketServerURL)
ContextVS.getInstance().setDefaultServer(vicketServer)

File walletFir = new File(ContextVS.getInstance().config.walletDir)
File[] vicketFiles = walletFir.listFiles(new FilenameFilter() {
    public boolean accept(File dir, String fileName) { return !fileName.startsWith("EXPENDED_"); }
});
if(vicketFiles.length == 0) throw new ExceptionVS(" --- Wallet empty ---")
//we have al the Vickets initialized, now we can make de transactions
VicketTransactionBatch transactionBatch = new VicketTransactionBatch()
transactionBatch.addVicket(vicketFiles[0])
transactionBatch.initTransactionVSRequest("toUserName", "ES0878788989450000000007",
        "First Vicket Transaction", true, vicketServer.getTimeStampServiceURL())
responseVS = HttpHelper.getInstance().sendData(transactionBatch.getTransactionVSRequest().toString().getBytes(),
        ContentTypeVS.JSON, vicketServer.getVicketTransactionServiceURL());
logger.debug("Vicket Transaction result: " + responseVS.getStatusCode())
if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
JSONObject responseJSON = JSONSerializer.toJSON(responseVS.getMessage());
logger.debug("Transaction result - statusCode: $responseJSON.statusCode - message:$responseJSON.message")
transactionBatch.validateTransactionVSResponse(responseJSON.getJSONArray("receiptList"), vicketServer.getTrustAnchors());
System.exit(0);



