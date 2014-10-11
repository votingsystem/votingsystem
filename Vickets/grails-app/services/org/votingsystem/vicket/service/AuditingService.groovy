package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.Vicket

import java.text.SimpleDateFormat

@Transactional
class AuditingService {

    private static final CLASS_NAME = AuditingService.class.getSimpleName()

    def grailsApplication
    def transactionVSService
    def grailsLinkGenerator

    //Check that the sum of all issued Vickets match with valid user signed request
    def checkVicketRequest(DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        def vickets = Vicket.createCriteria().scroll {
            ge("validFrom", timePeriod.getDateFrom())
            le("validTo", timePeriod.getDateTo())
        }
        int numVicketsCancelled = 0
        int numVickets = 0
        BigDecimal amountIssued = BigDecimal.ZERO
        //euro
        while(vickets.next()) {
            Vicket vicket = (Vicket) vickets.get(0);
            switch(vicket.getState()) {
                case Vicket.State.LAPSED:
                    break;
                case Vicket.State.CANCELLED:
                    break;
                case Vicket.State.EXPENDED:
                    break;
                case Vicket.State.OK:
                    break;

            }
            amountIssued = amountIssued.add(vicket.amount)
        }

        def vicketRequests = TransactionVS.createCriteria().scroll {
            eq("type", TransactionVS.Type.VICKET_REQUEST)
            between('dateCreated', timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        BigDecimal amountRequested = BigDecimal.ZERO
        while(vicketRequests.next()) {
            //user signed requests
            TransactionVS vicketTransaction = (TransactionVS) vicketRequests.get(0);
            amountRequested = amountRequested.add(vicketTransaction.amount)
        }
        log.debug("checkVicketRequest - amountIssued: ${amountIssued.toPlainString()} " +
                "- amountRequested: ${amountRequested.toPlainString()}")
    }


    //Backup user transactions for timePeriod
    public String backupUserVSTransactionVSList (UserVS userVS, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("$methodName - from: ${timePeriod.getDateFrom()} - to :${timePeriod.getDateTo()} - for user")
        String lapsePath = DateUtils.getDirPath(timePeriod.getDateFrom())
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        lapsePath = "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
        File backupDir = new File("${grailsApplication.config.VotingSystem.backupCopyPath}/userTransactionHistory${lapsePath}")
        backupDir.mkdirs()
        log.debug("$methodName - backupDir.absolutePath: ${backupDir.absolutePath}")
        //Expenses
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('fromUserVS', userVS)
            isNull("transactionParent")
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        def transactionFromList = []
        Map<String, Map> balancesMap = [:]
        transactionList.each { transaction ->
            if(balancesMap[transaction.currencyCode]) {
                Map<String, BigDecimal> currencyMap = balancesMap[transaction.currencyCode]
                if(currencyMap[transaction.tag.name]) {
                    currencyMap[transaction.tag.name] = ((BigDecimal) currencyMap[transaction.tag.name]).add(transaction.amount)
                } else currencyMap[(transaction.tag.name)] = transaction.amount
            } else {
                Map<String, BigDecimal> currencyMap = [(transaction.tag.name):transaction.amount]
                balancesMap[(transaction.currencyCode)] = currencyMap
            }
            Map transactionVSMap = transactionVSService.getTransactionMap(transaction)
            MessageSMIME messageSMIME = transaction.messageSMIME
            //String messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${messageSMIME.id}"
            //transactionVSMap[messageSMIMEURL] = Base64.getUrlEncoder().encodeToString(messageSMIME.content)
            transactionVSMap.messageSMIME = Base64.getUrlEncoder().encodeToString(messageSMIME.content)
            transactionFromList.add(transactionVSMap)
        }
        Map resultMap = [transactionFromList:transactionFromList, balancesFrom:balancesMap]
        //incomes
        transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('toUserVS', userVS)
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        def transactionToList = []
        transactionList.each { transaction ->
            if(balancesMap[transaction.currencyCode]) {
                Map<String, BigDecimal> currencyMap = balancesMap[transaction.currencyCode]
                if(currencyMap[transaction.tag.name]) {
                    currencyMap[transaction.tag.name] = ((BigDecimal) currencyMap[transaction.tag.name]).add(transaction.amount)
                } else currencyMap[(transaction.tag.name)] = transaction.amount
            } else {
                Map<String, BigDecimal> currencyMap = [(transaction.tag.name):transaction.amount]
                balancesMap[(transaction.currencyCode)] = currencyMap
            }
            Map transactionVSMap = transactionVSService.getTransactionMap(transaction)
            MessageSMIME messageSMIME = transaction.messageSMIME
            transactionVSMap.messageSMIME = Base64.getUrlEncoder().encodeToString(messageSMIME.content)
            transactionToList.add(transactionVSMap)
        }
        resultMap.transactionToList = transactionToList
        resultMap.balancesTo = balancesMap
        File userHistoryFile = new File("$backupDir.absolutePath/${userVS.nif}.json")
        userHistoryFile.setBytes("${resultMap as JSON}".getBytes("UTF-8"))
        log.debug("$methodName - saved userHistoryFile: ${userHistoryFile.absolutePath}")
        return userHistoryFile.absolutePath
    }

}