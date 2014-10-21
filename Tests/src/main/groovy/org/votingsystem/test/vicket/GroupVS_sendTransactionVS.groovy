package org.votingsystem.test.vicket

import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.VicketServer
import org.votingsystem.test.util.TestUtils
import org.votingsystem.test.util.TransactionVSPlan

Map simulationDataMap = [groupId:5, serverURL:"http://vickets:8086/Vickets"]
Logger log = TestUtils.init(GroupVS_sendTransactionVS.class, simulationDataMap)

VicketServer vicketServer = TestUtils.fetchVicketServer(ContextVS.getInstance().config.vicketServerURL)
ContextVS.getInstance().setDefaultServer(vicketServer)
TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
        TestUtils.getFileFromResources("transactionsPlan/groupVS.json"), vicketServer)

Map currencyResultMap = transactionVSPlan.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS")

TestUtils.finish("currencyResultMap: " + currencyResultMap)