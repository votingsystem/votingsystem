package org.votingsystem.cooin.controller

import grails.converters.JSON
import net.sf.json.JSONObject
import org.codehaus.groovy.runtime.StackTraceUtils
import org.iban4j.Iban
import org.springframework.dao.DataAccessException
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.cooin.websocket.SessionVSManager
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils

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
            } else if (params.IBAN) {
                if(params.userType) {
                    UserVS.withTransaction {
                        uservs = UserVS.findWhere(type:UserVS.Type.valueOf(params.userType), IBAN:params.IBAN)
                    }
                } else {
                    CooinAccount.withTransaction {
                        CooinAccount userAccount = CooinAccount.findWhere(IBAN:params.IBAN)
                        if(userAccount) uservs = userAccount.userVS
                        else {
                            Iban iban = Iban.valueOf(params.IBAN);
                            BankVSInfo bankVSInfo
                            BankVSInfo.withTransaction { bankVSInfo = BankVSInfo.findWhere(bankCode:iban.bankCode) }
                            if(bankVSInfo) {
                                msg = message(code: 'ibanFromBankVSClientMsg', args:[params.IBAN, bankVSInfo.bankVS.name])
                                uservs = bankVSInfo.bankVS
                            } else msg = message(code: 'itemNotFoundByIBANMsg', args:[params.IBAN])

                        }
                    }
                }
            }
            String view = null
            if(uservs) {
                if(uservs.instanceOf(GroupVS)) {
                    resultMap = [groupvsMap:groupVSService.getDataMap(GroupVS.get(uservs.id), currentWeekPeriod)]
                    view = '/groupVS/groupvs'
                } else if(uservs.instanceOf(BankVS)) {
                    resultMap = [uservsMap:bankVSService.getDataWithBalancesMap(uservs, currentWeekPeriod),
                                 messageToUser:msg]
                    view = 'userVS'
                } else {
                    resultMap = [uservsMap:userVSService.getDataWithBalancesMap(uservs, currentWeekPeriod)]
                    view = 'userVS'
                }
            } else {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)]
            }
            if(request.contentType?.contains("json")) render resultMap?.values()?.iterator().next() as JSON
            else render(view:view, model: resultMap)

        } else {
            Map sortParamsMap = RequestUtils.getSortParamsMap(params)
            Map.Entry sortParam
            if(!sortParamsMap.isEmpty()) sortParam = sortParamsMap?.entrySet()?.iterator()?.next()
            List<UserVS> userList = null
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
                } else {
                    userList = UserVS.createCriteria().list(max: params.max, offset: params.offset,
                            sort:sortParam?.key, order:sortParam?.value){
                    };
                }
            }
            def resultList = []
            userList.each {userItem ->
                resultList.add(userVSService.getUserVSDataMap(userItem, false))
            }
            def resultMap = [userVSList:resultList, offset: params.offset, max:params.max, totalCount:userList.totalCount]
            render resultMap as JSON
        }
    }

    def search() {
        if(request.contentType?.contains('json')) {
            if(params.searchText) {
                def userList
                def resultList = []
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
                    userList.each {userItem ->
                        resultList.add(userVSService.getUserVSDataMap(userItem, false))
                    }
                    Map resultMap = [userVSList:resultList, offset:params.offset, max:params.max,
                             totalCount:userList.totalCount]
                    render resultMap as JSON
                }

            }
        }
    }

    def searchGroup() {
        if(request.contentType?.contains('json')) {
            if(params.searchText && params.long('groupVSId')) {
                GroupVS groupVS = null
                GroupVS.withTransaction {
                    groupVS = GroupVS.get(params.long('groupVSId'))
                }
                if(!groupVS) {
                    return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                            message: message(code: 'groupNotFoundMsg', args:[params.long('groupVSId')]))]
                }
                def subscriptionList
                SubscriptionVS.State state = SubscriptionVS.State.ACTIVE
                if(params.groupVSState) try {state = SubscriptionVS.State.valueOf(params.groupVSState)} catch(Exception ex) {}
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
                        eq("state", state)
                    }
                }
                def resultList = []
                subscriptionList.each {userSubscriptionItem ->
                    resultList.add(userVSService.getUserVSDataMap(userSubscriptionItem.userVS, false))
                }
                Map resultMap = [userVSList:resultList, offset:params.offset, max:params.max,
                         totalCount:subscriptionList.totalCount]
                render resultMap as JSON
            }
        }
    }

    def searchByDevice() {
        if(params.phone || params.email) {
            DeviceVS deviceVS
            DeviceVS.withTransaction {
                deviceVS = DeviceVS.findByPhoneOrEmail(params.phone, params.email)
            }
            if(deviceVS) {
                render userVSService.getUserVSDataMap(deviceVS.userVS, false) as JSON
                return false
            }
        }
        return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'deviceNotFoundByPhoneEmail', args:[params.phone, params.email]))]
    }

    def bankVSList() {
        List<BankVS> bankVSList = null
        BankVS.withTransaction {
            bankVSList = BankVS.createCriteria().list(max: params.max, offset: params.offset,
                    sort:'dateCreated', order:'desc'){
            };
        }
        def resultList = []
        bankVSList.each {userItem ->
            resultList.add(userVSService.getUserVSDataMap(userItem, false))
        }
        def resultMap = [bankVSList:resultList, offset:params.offset, max:params.max, totalCount:bankVSList.totalCount]
        if(request.contentType?.contains('json')) render resultMap as JSON
        else render(view:'bankVSList', model: [bankVSMap:resultMap])
    }


    /**
     * Service that sends user accounts information
     *
     * @httpMethod [POST]
     * @serviceURL [/userVS/userInfo]
     * @requestContentType [application/pkcs7-signature] Required. JSON signed with the request data..
     * @return JSON with the response
     */
    def userInfoTest() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        SMIMEMessage smimeMessage = messageSMIME.getSMIME()
        JSONObject messageJSON = JSON.parse(smimeMessage.getSignedContent())
        UserVS userVS = messageSMIME.getUserVS()
        if(!messageJSON.NIF.equals(userVS.getNif())) {
            ResponseVS responseVS = new ResponseVS(statusCode:  ResponseVS.SC_ERROR, message: message(
                    code:'nifMisMatchErrorMsg', args: [userVS.getNif(), messageJSON.NIF]), contentType:ContentTypeVS.TEXT)
            return [responseVS:responseVS]
        }
        Calendar calendar = RequestUtils.getCalendar(params);
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar)
        Map responseMap = userVSService.getDataWithBalancesMap(userVS, timePeriod)
        return [responseVS:new ResponseVS(statusCode:  ResponseVS.SC_OK, data:responseMap,
                contentType: ContentTypeVS.JSON, type: TypeVS.COOIN_USER_INFO)]
    }

    /**
     * Service that sends user accounts information
     *
     * @httpMethod [POST]
     * @serviceURL [/userVS/$NIF/$year/$month/$day]
     * @return JSON with the response
     */
    def userInfo() {
        UserVS userVS
        if(params.NIF) UserVS.withTransaction { userVS = UserVS.findWhere(nif:params.NIF)  }
        if(!userVS) return [responseVS:new ResponseVS(statusCode:  ResponseVS.SC_NOT_FOUND,
                message: message(code:'userVSNotFoundByNIF', args: [params.NIF]), contentType: ContentTypeVS.TEXT,
                type: TypeVS.COOIN_USER_INFO)]

        Calendar calendar = RequestUtils.getCalendar(params);
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar)
        Map responseMap = userVSService.getDataWithBalancesMap(userVS, timePeriod)
        return [responseVS:new ResponseVS(statusCode:  ResponseVS.SC_OK, data:responseMap,
                contentType: ContentTypeVS.JSON, type: TypeVS.COOIN_USER_INFO)]
    }

    /**
     * (Disponible sólo para administradores de sistema)
     *
     * Servicio que añade Usuario al sistema
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
        return [responseVS:signatureVSService.addCertificateAuthority("${request.getInputStream()}".getBytes())]*/
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIME = request.messageSMIMEReq
            if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
            return [responseVS:userVSService.saveUser(messageSMIME)]
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
            MessageSMIME messageSMIME = request.messageSMIMEReq
            if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
            return [responseVS:bankVSService.saveBankVS(messageSMIME)]
        }
    }

    def connected() {
        render SessionVSManager.getInstance().getConnectedUsersDataMap() as JSON
        return false
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}