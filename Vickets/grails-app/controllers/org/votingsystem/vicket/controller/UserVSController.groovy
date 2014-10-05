package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.springframework.dao.DataAccessException
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.UserVSAccount

class UserVSController {

    def transactionVSService
    def groupVSService
    def userVSService
    def bankVSService

    def index() {
        def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
        if (params.long('id') || params.IBAN) {
            UserVS uservs
            Map resultMap
            String msg
            if(params.long('id')) {
                UserVS.withTransaction { uservs = UserVS.get(params.long('id'))  }
                if(!uservs) msg = message(code: 'itemNotFoundMsg', args:[params.long('id')])
            }
            else if (params.IBAN) {
                UserVSAccount.withTransaction {
                    UserVSAccount userAccount = UserVSAccount.findWhere(IBAN:params.IBAN)
                    if(userAccount) uservs = userAccount.userVS
                    else msg = message(code: 'itemNotFoundByIBANMsg', args:[params.IBAN])
                }
            }
            String view = null
            if(uservs) {
                if(uservs instanceof GroupVS) {
                    resultMap = [groupvsMap:groupVSService.getDataMap(uservs, currentWeekPeriod)]
                    view = '/groupVS/groupvs'
                } else if(uservs instanceof BankVS) {
                    resultMap = [uservsMap:bankVSService.getDataWithBalancesMap(uservs, currentWeekPeriod)]
                    view = 'userVS'
                } else {
                    resultMap = [uservsMap:userVSService.getDataWithBalancesMap(uservs, currentWeekPeriod)]
                    view = 'userVS'
                }
            } else {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)]
            }
            if(request.contentType?.contains("json")) {
                render resultMap?.values()?.iterator().next() as JSON
            } else {
                render(view:view, model: resultMap)
            }
        } else {
            Map sortParamsMap = RequestUtils.getSortParamsMap(params)
            Map.Entry sortParam
            if(!sortParamsMap.isEmpty()) sortParam = sortParamsMap?.entrySet()?.iterator()?.next()
            List<UserVS> userList = null
            int totalUsers = 0;
            UserVS.withTransaction {
                if(params.searchText || params.searchFrom || params.searchTo || params.type || params.state) {
                    UserVS.Type userType = null
                    UserVS.State userState = null
                    Date dateFrom = null
                    Date dateTo = null
                    try {userType = UserVS.Type.valueOf(params.type)} catch(Exception ex) {}
                    try {userState = UserVS.State.valueOf(params.state)} catch(Exception ex) {}
                    //searchFrom:2014/04/14 00:00:00, max:100, searchTo
                    if(params.searchFrom) try {dateFrom = DateUtils.getDateFromString(params.searchFrom)} catch(Exception ex) {}
                    if(params.searchTo) try {dateTo = DateUtils.getDateFromString(params.searchTo)} catch(Exception ex) {}

                    userList = UserVS.createCriteria().list(max: params.max, offset: params.offset,
                            sort:sortParam?.key, order:sortParam?.value) {
                        or {
                            if(type) eq("currency", userType)
                            if(userState) eq("state", userState)
                            ilike('name', "%${params.searchText}%")
                            ilike('firstName', "%${params.searchText}%")
                            ilike('lastName', "%${params.searchText}%")
                            ilike('nif', "%${params.searchText}%")
                            ilike('IBAN', "%${params.searchText}%")
                        }
                        and {
                            if(dateFrom && dateTo) {between("dateCreated", dateFrom, dateTo)}
                            else if(dateFrom) {ge("dateCreated", dateFrom)}
                            else if(dateTo) {le("dateCreated", dateTo)}
                        }
                    }
                    totalUsers = userList.totalCount
                } else {
                    userList = UserVS.createCriteria().list(max: params.max, offset: params.offset,
                            sort:sortParam?.key, order:sortParam?.value){
                    };
                    totalUsers = userList.totalCount
                }
            }
            def resultList = []
            userList.each {userItem ->
                resultList.add(userVSService.getUserVSDataMap(userItem, false))
            }
            def resultMap = [userVSList:resultList, queryRecordCount: totalUsers, numTotalTransactions:totalUsers ]
            render resultMap as JSON
        }
    }

    def search() {
        if(request.contentType?.contains('json')) {
            if(params.searchText) {
                def userList
                UserVS.withTransaction {
                    userList = UserVS.createCriteria().list(max: params.max, offset: params.offset) {
                        or {
                            ilike('name', "%${params.searchText}%")
                            ilike('firstName', "%${params.searchText}%")
                            ilike('lastName', "%${params.searchText}%")
                            ilike('nif', "%${params.searchText}%")
                        }
                        and {
                            eq("state", UserVS.State.ACTIVE)
                            eq("type", UserVS.Type.USER)
                        }
                    }
                    def resultList = []
                    userList.each {userItem ->
                        resultList.add(userVSService.getUserVSDataMap(userItem, false))
                    }
                    int totalUsers = userList.totalCount
                    Map resultMap = [userVSList:resultList, queryRecordCount: totalUsers, numTotalTransactions:totalUsers ]
                    render resultMap as JSON
                }

            }
        }
    }

    def searchGroup() {
        if(request.contentType?.contains('json')) {
            if(params.searchText && params.long('groupId')) {
                GroupVS groupVS = null
                GroupVS.withTransaction {
                    groupVS = GroupVS.get(params.long('groupId'))
                }
                if(!groupVS) {
                    return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                            message: message(code: 'groupNotFoundMsg', args:[params.long('groupId')]))]
                }
                def subscriptionList
                SubscriptionVS.withTransaction {
                    subscriptionList = SubscriptionVS.createCriteria().list(max: params.max, offset: params.offset) {
                        eq("groupVS", groupVS)
                        userVS {
                            or {
                                ilike('name', "%${params.searchText}%")
                                ilike('firstName', "%${params.searchText}%")
                                ilike('lastName', "%${params.searchText}%")
                                ilike('nif', "%${params.searchText}%")
                            }
                            and {
                                eq("state", UserVS.State.ACTIVE)
                            }
                        }
                    }
                }
                def resultList = []
                subscriptionList.each {userSubscriptionItem ->
                    resultList.add(userVSService.getUserVSDataMap(userSubscriptionItem.userVS, false))
                }
                int totalUsers = subscriptionList.totalCount
                Map resultMap = [userVSList:resultList, queryRecordCount: totalUsers, numTotalTransactions:totalUsers ]
                render resultMap as JSON
            }
        }
    }

    def bankVSList() {
        if(request.contentType?.contains('json')) {
            List<BankVS> bankVSList = null
            int numTotalBankVSs = 0;
            BankVS.withTransaction {
                bankVSList = BankVS.createCriteria().list(max: params.max, offset: params.offset,
                        sort:'dateCreated', order:'desc'){
                };
                numTotalBankVSs = bankVSList.totalCount
            }
            def resultList = []
            bankVSList.each {userItem ->
                resultList.add(userVSService.getUserVSDataMap(userItem, false))
            }
            def resultMap = [bankVSList:resultList, queryRecordCount: numTotalBankVSs, numTotalBankVSs:numTotalBankVSs ]
            render resultMap as JSON
        }
    }


    /**
     * Servicio que envía información del estado de las cuentas
     *
     * @httpMethod [POST]
     * @serviceURL [/userVS/userInfo]
     * @requestContentType [application/x-pkcs7-signature] Obligatorio.
     *                     documento SMIME firmado con datos del usuario que solicita la información.
     * @responseContentType [application/json;application/pkcs7-mime]. Documento JSON cifrado con datos de
     *                      la cuenta del usuario.
     * @return
     */
    def userInfo() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        SMIMEMessage smimeMessage = messageSMIMEReq.getSmimeMessage()
        def messageJSON = JSON.parse(smimeMessage.getSignedContent())
        UserVS userVS = messageSMIMEReq.getUserVS()
        if(!messageJSON.NIF.equals(userVS.getNif())) {
            ResponseVS responseVS = new ResponseVS(statusCode:  ResponseVS.SC_ERROR, message:  message(code:'nifMisMatchErrorMsg',
                    args: [userVS.getNif(), messageJSON.NIF]), contentType:ContentTypeVS.TEXT)
            return [responseVS:responseVS]
        }
        Calendar calendar = Calendar.getInstance()
        if(params.year && params.month && params.day) {
            calendar.set(Calendar.YEAR, params.int('year'))
            calendar.set(Calendar.MONTH, params.int('month') - 1) //Zero based
            calendar.set(Calendar.DAY_OF_MONTH, params.int('day'))
        }
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar)
        Map responseMap = userVSService.getDataWithBalancesMap(userVS, timePeriod)
        //X509Certificate cert = messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        return [responseVS:new ResponseVS(statusCode:  ResponseVS.SC_OK, data:responseMap,
                contentType: ContentTypeVS.JSON, type: TypeVS.VICKET_USER_INFO)]
    }


    /**
     * (Disponible sólo para administradores de sistema)
     *
     * Servicio que añade Usuario al sistema.<br/>
     *
     * @httpMethod [POST]
     * @param pemCertificate certificado en formato PEM del Usuario que se desea añadir.
     * @return Si todo va bien devuelve un código de estado HTTP 200.
     */
    def save () {
        /*if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        return [responseVS:signatureVSService.addCertificateAuthority(
            "${request.getInputStream()}".getBytes(), request.getLocale())]*/
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            ResponseVS responseVS = userVSService.saveUser(messageSMIMEReq, request.getLocale())
            return [responseVS:responseVS]
        } else render(view:'newUser')
    }

    /**
     * (Disponible sólo para administradores de sistema)
     *
     * Servicio que añade Usuario al sistema.<br/>
     *
     * @httpMethod [POST]
     * @param pemCertificate certificado en formato PEM del Usuario que se desea añadir.
     * @return Si todo va bien devuelve un código de estado HTTP 200.
     */
    def newBankVS() {
        ResponseVS responseVS = null
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            responseVS = bankVSService.saveBankVS(messageSMIMEReq, request.getLocale())
            return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getUserVS()?.getCertificate()]
        }
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error " Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${exception.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        log.error " Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${exception.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: message(code:'paramsErrorMsg'),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }
}