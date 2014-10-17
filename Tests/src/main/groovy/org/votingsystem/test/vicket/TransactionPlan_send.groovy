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
        TestUtils.getFileFromResources("transactionPlan.json"), vicketServer)
Map bankVSCurrencyResultMap = transactionVSPlan.runBankVSTransactions("TEST_BANKVS_SEND_TRANSACTIONVS")
Map groupVSCurrencyResultMap = transactionVSPlan.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS")
TestUtils.finish("bankVSCurrencyResultMap: " + bankVSCurrencyResultMap + "\n groupVSCurrencyResultMap: " +
        groupVSCurrencyResultMap);