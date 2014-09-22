package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.model.BankVS
import org.votingsystem.util.DateUtils

@Transactional
class BankVSService {

    private static final CLASS_NAME = BankVSService.class.getSimpleName()

    def userVSService
    def transactionVSService

    public Map getDetailedDataMap(BankVS bankVS, DateUtils.TimePeriod timePeriod) {
        Map resultMap = userVSService.getUserVSDataMap(bankVS, false)
        def transactionFromListJSON = []
        transactionVSService.getTransactionFromList(bankVS, timePeriod).each { transaction ->
            transactionFromListJSON.add(transactionVSService.getTransactionMap(transaction))
        }
        resultMap.transactionFromList = transactionFromListJSON
        return resultMap
    }

    public Map getDetailedDataMapWithBalances(BankVS bankVS, DateUtils.TimePeriod timePeriod) {
        Map resultMap = userVSService.getUserVSDataMap(bankVS, false)
        Map transactionsWithBalancesMap = transactionVSService.getTransactionFromListWithBalances(bankVS, timePeriod)
        resultMap.transactionFromList = transactionsWithBalancesMap.transactionFromList
        resultMap.balancesFrom = transactionsWithBalancesMap.balancesFrom
        return resultMap
    }

}
