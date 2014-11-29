package org.votingsystem.test.cooin

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.CooinServer
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.cooin.model.CooinTransactionBatch


Logger log = TestUtils.init(Cooin_sendFromWallet.class)

CooinServer cooinServer = TestUtils.fetchCooinServer(ContextVS.getInstance().config.cooinServerURL)
ContextVS.getInstance().setDefaultServer(cooinServer)

File walletFir = new File(ContextVS.getInstance().config.walletDir)
File[] cooinFiles = walletFir.listFiles(new FilenameFilter() {
    public boolean accept(File dir, String fileName) { return !fileName.startsWith("EXPENDED_"); }
});
if(!cooinFiles || cooinFiles.length == 0) throw new ExceptionVS(" --- Empty wallet ---")
//we have al the Cooins initialized, now we can make de transactions
CooinTransactionBatch transactionBatch = new CooinTransactionBatch()
transactionBatch.addCooin(cooinFiles[0])
transactionBatch.initTransactionVSRequest("toUserName", "ES0878788989450000000007",
        "First Cooin Transaction", true, cooinServer.getTimeStampServiceURL())
responseVS = HttpHelper.getInstance().sendData(transactionBatch.getTransactionVSRequest().toString().getBytes(),
        ContentTypeVS.JSON, cooinServer.getCooinTransactionServiceURL());
log.debug("Cooin Transaction result: " + responseVS.getStatusCode())
if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
JSONObject responseJSON = JSONSerializer.toJSON(responseVS.getMessage());
log.debug("Transaction result - statusCode: $responseJSON.statusCode - message:$responseJSON.message")
transactionBatch.validateTransactionVSResponse(responseJSON.getJSONArray("receiptList"), cooinServer.getTrustAnchors());
System.exit(0);



