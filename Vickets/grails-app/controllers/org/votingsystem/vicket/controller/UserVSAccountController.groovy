package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.dao.DataAccessException
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.vicket.model.UserVSAccount

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class UserVSAccountController {

    def userVSAccountService

    def balance() {
        if(params.long('id')) {
            UserVS userVS = null
            Map resultMap = [:]
            UserVS.withTransaction { userVS = UserVS.get(params.long('id')) }
            if(!userVS) {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            def userAccountsDB
            UserVSAccount.withTransaction { userAccountsDB = UserVSAccount.createCriteria().list(sort:'dateCreated', order:'asc') {
                eq("userVS", userVS)
                eq("state", UserVSAccount.State.ACTIVE)
            }}
            List userAccounts = []
            userAccountsDB.each { it ->
                userAccounts.add(userVSAccountService.getUserVSAccountMap(it))
            }
            resultMap = [name: userVS.name, id:userVS.id, accounts:userAccounts]
            render resultMap as JSON
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error " Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${exception.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: message(code:'paramsErrorMsg'),
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }
}