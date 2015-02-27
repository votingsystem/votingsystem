package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.iban4j.CountryCode
import org.iban4j.Iban
import org.springframework.dao.DataAccessException
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.model.ResponseVS

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class IBANController {

	def grailsApplication;
    def transactionVSService

    def index() {
        Iban iban = Iban.valueOf(params.IBANCode);
        List result = []
        if(iban.getBankCode().equals(grailsApplication.config.vs.IBAN_bankCode) &&
                iban.getBranchCode().equals(grailsApplication.config.vs.IBAN_branchCode)) {
            log.debug "External IBAN"
            TransactionVS.withTransaction {
                def transactionList = TransactionVS.createCriteria().list(max: params.max, offset: params.offset) {
                    createAlias("fromUserVS", "fromUserVS")
                    eq("fromUserVS.IBAN", iban.toString())
                }
                transactionList.each {transaction ->
                    result.add(transactionVSService.getTransactionMap(transaction))
                }
            }
        } else {
            log.debug "VotingSystem IBAN"
            TransactionVS.withTransaction {
                def transactionList = TransactionVS.createCriteria().list(max: params.max, offset: params.offset) {
                    eq("fromUserIBAN", iban.toString())
                }
                transactionList.each {transaction ->
                    result.add(transactionVSService.getTransactionMap(transaction))
                }
            }
        }
        render result as JSON
    }

    def testExternal() {
        String accountNumberStr = String.format("%010d", params.long('id'));
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode("7777").branchCode("7777")
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        render iban.toString();
        return false
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}