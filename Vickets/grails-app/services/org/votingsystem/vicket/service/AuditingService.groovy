package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.Vicket

@Transactional
class AuditingService {

    private static final CLASS_NAME = AuditingService.class.getSimpleName()

    def grailsApplication
    def transactionVSService
    def grailsLinkGenerator

    //Check that the sum of all issued Vickets match with valid user signed request
    def checkVicketRequest(DateUtils.TimePeriod timePeriod) {
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
                case Vicket.State.REJECTED:
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


    //Backup user transactions for the week of the selected date
    def backupUserTransactionHistory (DateUtils.TimePeriod timePeriod) {
        Calendar weekFromCalendar = Calendar.getInstance();
        weekFromCalendar.setTime(selectedDate)
        weekFromCalendar = DateUtils.getMonday(weekFromCalendar)
        weekFromCalendar.add(Calendar.SECOND, 1)
        Calendar weekToCalendar = weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)
        log.debug("backupUserTransactionHistory - from: ${timePeriod.getDateFrom()} - to :${timePeriod.getDateTo()}")

        String lapsePath = DateUtils.getDirPath(weekFromCalendar.getTime())
        String basePath = "${grailsApplication.config.VotingSystem.backupCopyPath}/userTransactionHistory${lapsePath}"
        File file = new File(basePath)
        file.mkdirs()
        log.debug("basePath: ${file.absolutePath}")


        /*def userAllocations = TransactionVS.createCriteria().scroll {
            eq("type", TransactionVS.Type.USER_ALLOCATION)
            eq("state", TransactionVS.State.OK)
            ge("dateCreated", weekFromCalendar.getTime())
            le("dateCreated", weekToCalendar.getTime())
        }
        File userAllocationReceiptsFile = new File("${basePath}/userAllocationReceipts.json")
        Map userAllocationsMap = [:]
        while(userAllocations.next()) {
            TransactionVS userAllocation = (TransactionVS) userAllocations.get(0);
            MessageSMIME messageSMIME = userAllocation.messageSMIME
            String messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${messageSMIME.id}"
            userAllocationsMap[messageSMIMEURL] = Base64.encode(messageSMIME.content)
        }
        userAllocationReceiptsFile.setBytes("${userAllocationsMap as JSON}".getBytes(ContextVS.UTF_8))

        def users = UserVS.createCriteria().scroll {
            eq("type", UserVS.Type.USER)
            le("dateCreated", weekFromCalendar.getTime())
        }
        while (users.next()) {
            UserVS userVS = (UserVS) users.get(0);
            String userSubPath = StringUtils.getUserDirPath(userVS.nif);
            new File("${basePath}${userSubPath}").mkdirs()
            log.debug("UserVS id: ${userVS.id} - userSubPath: ${userSubPath}")
            Map userTransactionMap = transactionVSService.getUserVSVicketTransactionVSMap(userVS, weekFromCalendar)
            File userTransactionsFile = new File("${basePath}${userSubPath}/transactions.json")
            userTransactionsFile.setBytes("${userTransactionMap as JSON}".getBytes("UTF-8"))
            Map euroDataMap = userTransactionMap?.get(lapsePath)?.get(Currency.getInstance("EUR").getCurrencyCode())

            List transactionList = euroDataMap.transactionList
            transactionList.each {
                Map transactionDataMap = it
                TransactionVS transactionvs = TransactionVS.get(transactionDataMap.id)
                switch(transactionvs.getType()) {
                    case TransactionVS.Type.USER_ALLOCATION_INPUT:
                        break;
                    case TransactionVS.Type.VICKET_REQUEST:
                        File VICKET_REQUEST_File = new File("${basePath}${userSubPath}/VICKET_REQUEST_${transactionvs.id}.json")
                        VICKET_REQUEST_File.setBytes(transactionvs.getMessageSMIME().content);
                        break;
                }
            }
        }*/
    }

}