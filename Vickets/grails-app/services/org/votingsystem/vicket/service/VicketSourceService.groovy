package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.model.VicketSource
import org.votingsystem.util.DateUtils

@Transactional
class VicketSourceService {

    def userVSService
    def transactionVSService

    public Map getDetailedDataMap(VicketSource vicketSource, DateUtils.TimePeriod timePeriod) {
        Map resultMap = userVSService.getUserVSDataMap(vicketSource)

        def transactionFromListJSON = []
        transactionVSService.getTransactionFromList(vicketSource, timePeriod).each { transaction ->
            transactionFromListJSON.add(transactionVSService.getTransactionMap(transaction))
        }

        resultMap.transactionFromList = transactionFromListJSON
        return resultMap
    }

}
