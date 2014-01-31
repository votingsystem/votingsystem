package org.votingsystem.ticket.service

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64
import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TicketVS
import org.votingsystem.model.ticket.TransactionVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.StringUtils

@Transactional
class AuditingService {

    def grailsApplication
    def transactionVSService
    def grailsLinkGenerator

    //Check that the sum of all issued Tickets match with valid user signed request
    def checkTicketRequest(Date selectedDate) {
        Calendar weekFromCalendar = Calendar.getInstance();
        weekFromCalendar.setTime(selectedDate)
        weekFromCalendar = DateUtils.getMonday(weekFromCalendar)
        Calendar weekToCalendar = weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)
        weekFromCalendar.add(Calendar.SECOND, -1)
        //weekFromCalendar.add(Calendar.SECOND, 1)
        weekToCalendar.add(Calendar.SECOND, 1)
        def tickets = TicketVS.createCriteria().scroll {
            ge("validFrom", weekFromCalendar.getTime())
            le("validTo", weekToCalendar.getTime())
        }
        int numTicketsCancelled = 0
        int numTickets = 0
        BigDecimal amountIssued = new BigDecimal(0)
        //euro
        while(tickets.next()) {
            TicketVS ticket = (TicketVS) tickets.get(0);
            switch(ticket.getState()) {
                case TicketVS.State.LAPSED:
                    break;
                case TicketVS.State.CANCELLED:
                    break;
                case TicketVS.State.EXPENDED:
                    break;
                case TicketVS.State.OK:
                    break;
                case TicketVS.State.REJECTED:
                    break;
            }
            amountIssued = amountIssued.add(ticket.amount)
        }

        def ticketRequests = TransactionVS.createCriteria().scroll {
            eq("type", TransactionVS.Type.TICKET_REQUEST)
            ge("dateCreated", weekFromCalendar.getTime())
            le("dateCreated", weekToCalendar.getTime())
        }
        BigDecimal amountRequested = new BigDecimal(0)
        while(ticketRequests.next()) {
            //user signed requests
            TransactionVS ticketTransaction = (TransactionVS) ticketRequests.get(0);
            amountRequested = amountRequested.add(ticketTransaction.amount)
        }
        log.debug("checkTicketRequest - amountIssued: ${amountIssued.toPlainString()} " +
                "- amountRequested: ${amountRequested.toPlainString()}")
    }


    //Backup user transactions for the week of the selected date
    //Users
    def backupUserTransactionHistory (Date selectedDate) {
        Calendar weekFromCalendar = Calendar.getInstance();
        weekFromCalendar.setTime(selectedDate)
        weekFromCalendar = DateUtils.getMonday(weekFromCalendar)
        weekFromCalendar.add(Calendar.SECOND, 1)
        Calendar weekToCalendar = weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)
        log.debug("backupUserTransactionHistory - selectedDate: ${selectedDate} - weekFrom :${weekFromCalendar.getTime()} " +
                "- weekTo :${weekToCalendar.getTime()}")

        String lapsePath = DateUtils.getDirPath(weekFromCalendar.getTime())
        String basePath = "${grailsApplication.config.VotingSystem.backupCopyPath}/userTransactionHistory${lapsePath}"
        File file = new File(basePath)
        file.mkdirs()
        log.debug("basePath: ${file.absolutePath}")
        //Save user USER_ALLOCATION transactions

        def userAllocations = TransactionVS.createCriteria().scroll {
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
            Map userTransactionMap = transactionVSService.getUserInfoMap(userVS, weekFromCalendar)
            File userTransactionsFile = new File("${basePath}${userSubPath}/transactions.json")
            userTransactionsFile.setBytes("${userTransactionMap as JSON}".getBytes("UTF-8"))
            Map euroDataMap = userTransactionMap?.get(lapsePath)?.get(CurrencyVS.Euro.toString())

            List transactionList = euroDataMap.transactionList
            transactionList.each {
                Map transactionDataMap = it
                TransactionVS transactionvs = TransactionVS.get(transactionDataMap.id)
                switch(transactionvs.getType()) {
                    case TransactionVS.Type.USER_ALLOCATION_INPUT:
                        break;
                    case TransactionVS.Type.TICKET_REQUEST:
                        File TICKET_REQUEST_File = new File("${basePath}${userSubPath}/TICKET_REQUEST_${transactionvs.id}.json")
                        TICKET_REQUEST_File.setBytes(transactionvs.getMessageSMIME().content);
                        break;
                }
            }
        }
    }


}
