package org.votingsystem.vicket.controller

import grails.converters.JSON
import net.sf.json.JSONObject
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.DERObject
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.springframework.dao.DataAccessException
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.vicket.util.ApplicationContextHolder
import org.votingsystem.util.DateUtils

import java.security.cert.X509Certificate

class UserVSController {
	
	def subscriptionVSService
    def transactionVSService
    def groupVSService
    def userVSService

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
                        resultList.add(userVSService.getUserVSDataMap(userItem))
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
                    resultList.add(userVSService.getUserVSDataMap(userSubscriptionItem.userVS))
                }
                int totalUsers = subscriptionList.totalCount
                Map resultMap = [userVSList:resultList, queryRecordCount: totalUsers, numTotalTransactions:totalUsers ]
                render resultMap as JSON
            }
        }
    }

    def index() {
        if (params.long('id')) {
            UserVS uservs
            Map resultMap
            UserVS.withTransaction { uservs = UserVS.get(params.long('id'))  }
            String view = null
            if(uservs) {
                if(uservs instanceof GroupVS) {
                    resultMap = [groupvsMap:groupVSService.getGroupVSDataMap(uservs)]
                    view = '/groupVS/group'
                } else if(uservs instanceof VicketSource) {
                    resultMap = [uservsMap:userVSService.getVicketSourceDataMap(uservs)]
                    view = 'uservs'
                } else {
                    resultMap = [uservsMap:userVSService.getUserVSDataMap(uservs)]
                    view = 'uservs'
                }
            }
            else {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            if(request.contentType?.contains("json")) {
                render resultMap as JSON
            } else {
                render(view:view, model: resultMap)
            }
        } else {
            Map sortParamsMap = org.votingsystem.groovy.util.StringUtils.getSortParamsMap(params)
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
                resultList.add(userVSService.getUserVSDataMap(userItem))
            }
            def resultMap = [userVSList:resultList, queryRecordCount: totalUsers, numTotalTransactions:totalUsers ]
            render resultMap as JSON
        }
    }

    def vicketSourceList() {
        if(request.contentType?.contains('json')) {
            List<VicketSource> vicketSourceList = null
            int numTotalVicketSources = 0;
            VicketSource.withTransaction {
                vicketSourceList = VicketSource.createCriteria().list(max: params.max, offset: params.offset,
                        sort:'dateCreated', order:'desc'){
                };
                numTotalVicketSources = vicketSourceList.totalCount
            }
            def resultList = []
            vicketSourceList.each {userItem ->
                resultList.add(userVSService.getUserVSDataMap(userItem))
            }
            def resultMap = [vicketSourceList:resultList, queryRecordCount: numTotalVicketSources, numTotalVicketSources:numTotalVicketSources ]
            render resultMap as JSON
        }
    }


    /**
     * Servicio que envía información del estado de las cuentas
     *
     * @httpMethod [POST]
     * @serviceURL [/userVS/userInfo]
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio.
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
        SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
        def messageJSON = JSON.parse(smimeMessage.getSignedContent())
        UserVS userVS = messageSMIMEReq.getUserVS()
        if(!messageJSON.NIF.equals(userVS.getNif())) {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR, message(code:'nifMisMatchErrorMsg',
                    args: [userVS.getNif(), messageJSON.NIF]))
            responseVS.setContentType(ContentTypeVS.TEXT)
            return [responseVS:responseVS]
        }

        Calendar calendar = Calendar.getInstance()
        if(params.year && params.month && params.day) {
            calendar.set(Calendar.YEAR, params.int('year'))
            calendar.set(Calendar.MONTH, params.int('month') - 1) //Zero based
            calendar.set(Calendar.DAY_OF_MONTH, params.int('day'))
        } else calendar = DateUtils.getMonday(calendar)

        Map responseMap = transactionVSService.getUserInfoMap(userVS, calendar)
        ResponseVS responseVS = new ResponseVS(statusCode:  ResponseVS.SC_OK, data:responseMap)
        responseVS.setContentType(ContentTypeVS.JSON)
        X509Certificate cert = messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate

        responseVS.setType(TypeVS.VICKET_USER_INFO)
        return [responseVS:responseVS]
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
        /*if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
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
    def newVicketSource() {
        ResponseVS responseVS = null
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            responseVS = userVSService.saveVicketSource(messageSMIMEReq, request.getLocale())
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