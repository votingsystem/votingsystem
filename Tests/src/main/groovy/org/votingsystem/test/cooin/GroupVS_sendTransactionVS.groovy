package org.votingsystem.test.cooin

import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CooinServer
import org.votingsystem.test.util.TestUtils
import org.votingsystem.test.util.TransactionVSPlan

Map simulationDataMap = [groupId:5, serverURL:"http://cooins:8086/Cooins"]
Logger log = TestUtils.init(GroupVS_sendTransactionVS.class, simulationDataMap)

CooinServer cooinServer = TestUtils.fetchCooinServer(ContextVS.getInstance().config.cooinServerURL)
ContextVS.getInstance().setDefaultServer(cooinServer)
TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
        TestUtils.getFileFromResources("transactionsPlan/groupVS.json"), cooinServer)

Map currencyResultMap = transactionVSPlan.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS")

TestUtils.finish("currencyResultMap: " + currencyResultMap)