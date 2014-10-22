package org.votingsystem.test.vicket

import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.VicketServer
import org.votingsystem.test.util.TestUtils
import org.votingsystem.test.util.TransactionVSPlan

Logger log = TestUtils.init(BankVS_sendTransactionVS.class, [:])

VicketServer vicketServer = TestUtils.fetchVicketServer(ContextVS.getInstance().config.vicketServerURL)
ContextVS.getInstance().setDefaultServer(vicketServer)

TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
        TestUtils.getFileFromResources("transactionsPlan/bankVS.json"), vicketServer)
Map currencyResultMap = transactionVSPlan.runTransactionsVS("TEST_BANKVS_SEND_TRANSACTIONVS")

log.debug("Transaction report:" + transactionVSPlan.getReport())

TestUtils.finish("currencyResultMap: " + currencyResultMap);