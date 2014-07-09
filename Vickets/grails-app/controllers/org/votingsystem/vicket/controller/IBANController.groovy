package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.iban4j.CountryCode
import org.iban4j.Iban
import org.springframework.dao.DataAccessException
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.vicket.model.TransactionVS

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
        if(iban.getBankCode().equals(grailsApplication.config.VotingSystem.IBAN_bankCode) &&
                iban.getBranchCode().equals(grailsApplication.config.VotingSystem.IBAN_branchCode)) {
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
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error " Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${rootCause.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: metaInf,
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error " Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${exception.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: message(code:'paramsErrorMsg'),
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }
}