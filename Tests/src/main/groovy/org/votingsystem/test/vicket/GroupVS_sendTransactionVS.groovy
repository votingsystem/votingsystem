package org.votingsystem.test.vicket

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.model.SimulationData
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.test.util.TransactionVSPlan
import org.votingsystem.test.util.TransactionVSUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.vicket.model.TransactionVS

Map simulationDataMap = [groupId:5, serverURL:"http://vickets:8086/Vickets"]
Logger log = TestUtils.init(GroupVS_sendTransactionVS.class, simulationDataMap)

VicketServer vicketServer = TestUtils.fetchVicketServer(ContextVS.getInstance().config.vicketServerURL)
ContextVS.getInstance().setDefaultServer(vicketServer)
TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
        TestUtils.getFileFromResources("groupVS_transactionPlan.json"), vicketServer)

String messageSubject = "TEST_GROUPVS_SEND_TRANSACTIONVS";

for(TransactionVS transactionVS : transactionVSPlan.getGroupVSTransacionList()) {
    UserVS representative = ((GroupVS)transactionVS.fromUserVS).representative
    SignatureService signatureService = SignatureService.getUserVSSignatureService(
            representative.nif, UserVS.Type.GROUP)
    SMIMEMessage smimeMessage = signatureService.getTimestampedSignedMimeMessage(representative.nif,
            vicketServer.getNameNormalized(), JSONSerializer.toJSON(
            TransactionVSUtils.getGroupVSTransactionVS(transactionVS, transactionVS.fromUserVS)).toString(), messageSubject)
    ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
            vicketServer.getTransactionVSServiceURL())
    if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS(responseVS.getMessage())
}

TestUtils.finish()