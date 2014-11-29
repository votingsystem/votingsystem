package org.votingsystem.test.cooin

import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CooinServer
import org.votingsystem.test.util.TestUtils
import org.votingsystem.test.util.TransactionVSPlan

Logger log = TestUtils.init(BankVS_sendTransactionVS.class, [:])

CooinServer cooinServer = TestUtils.fetchCooinServer(ContextVS.getInstance().config.cooinServerURL)
ContextVS.getInstance().setDefaultServer(cooinServer)

TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
        TestUtils.getFileFromResources("transactionsPlan/bankVS.json"), cooinServer)
Map currencyResultMap = transactionVSPlan.runTransactionsVS("TEST_BANKVS_SEND_TRANSACTIONVS")

log.debug("Transaction report:" + transactionVSPlan.getReport())

TestUtils.finish("currencyResultMap: " + currencyResultMap);