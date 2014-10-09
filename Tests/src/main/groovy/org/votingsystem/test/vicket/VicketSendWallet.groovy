package org.votingsystem.test.vicket


import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VicketServer
import org.votingsystem.test.util.TestHelper
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.vicket.model.VicketTransactionBatch


Logger logger = TestHelper.init(VicketSendWallet.class)
VicketServer vicketServer = TestHelper.loadVicketServer()
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
        "First Vicket Transaction", false, vicketServer.getTimeStampServiceURL())
responseVS = HttpHelper.getInstance().sendData(transactionBatch.getTransactionVSRequest().toString().getBytes(),
        ContentTypeVS.JSON, vicketServer.getVicketTransactionServiceURL());
logger.debug("Vicket Transaction result: " + responseVS.getStatusCode())
if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
transactionBatch.validateTransactionVSResponse(responseVS.getMessage(), vicketServer.getTrustAnchors());



