package org.votingsystem.vicket.controller

import grails.converters.JSON
import net.sf.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.DateUtils

import java.security.cert.X509Certificate

class UserVSController {
	
	def subscriptionVSService
    def transactionVSService
    def groupVSService
    def userVSService

    def search() { }

    def index() {
        if (params.long('id')) {
            def result
            Map resultMap
            UserVS.withTransaction {
                result = UserVS.get(params.long('id'))
            }
            String view = null
            if(result) {
                if(result instanceof GroupVS) {
                    resultMap = [groupvsMap:groupVSService.getGroupVSDataMap(result)]
                    view = '/groupVS/group'
                } else {
                    resultMap = [uservsMap:userVSService.getUserVSDataMap(result)]
                    view = 'user'
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
        String result = new JSONObject(responseMap).toString()
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK, result)
        responseVS.setContentType(ContentTypeVS.JSON_ENCRYPTED)
        responseVS.setType(TypeVS.VICKET_USER_INFO)
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }


	/**
	 * Servicio que sirve para añadir usuarios de pruebas.
	 * SOLO DISPONIBLES EN ENTORNOS DE DESARROLLO.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/userVS]
	 * @param [userCert] Certificado de usuario en formato PEM
	 * 
	 * @requestContentType [application/x-x509-ca-cert]
	 * 
	 */
	def save() {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		String pemCert = "${request.getInputStream()}"
		Collection<X509Certificate> userCertCollection = CertUtil.fromPEMToX509CertCollection(pemCert.getBytes())
		X509Certificate userCert = userCertCollection?.toArray()[0]
		if(userCert) {
			UserVS userVS = UserVS.getUserVS(userCert);
			ResponseVS responseVS = subscriptionVSService.checkUser(userVS, request.locale)
			responseVS.userVS.type = UserVS.Type.USER
			responseVS.userVS.representative = null
			responseVS.userVS.representativeMessage = null
			responseVS.userVS.representativeRegisterDate = null
			responseVS.userVS.metaInf = null

            Calendar weekFromCalendar = Calendar.getInstance();
            weekFromCalendar = DateUtils.getMonday(weekFromCalendar)
            responseVS.userVS.setDateCreated(weekFromCalendar.getTime())


			UserVS.withTransaction { responseVS.userVS.save() }
            return [responseVS : responseVS]
		} else return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code:"nullCertificateErrorMsg"))]
	}
	
	
	/**
	 *
	 * Servicio que sirve para prepaparar la base de usuarios
	 * antes de lanzar simulaciones.
	 * SOLO DISPONIBLES EN ENTORNOS DE DESARROLLO.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/user/prepareUserBaseData]
	 *
	 */
	def prepareUserBaseData() {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		def usersVS = UserVS.findAll()
        usersVS.each { user ->
			user.type = UserVS.Type.USER
			user.representative = null
			user.representativeMessage = null
			user.representativeRegisterDate = null
			user.metaInf = null
			def repDocsFromUser
			RepresentationDocumentVS.withTransaction {
				repDocsFromUser = RepresentationDocumentVS.findAllWhere(userVS:user)
				repDocsFromUser.each { repDocFromUser ->
					repDocFromUser.state = RepresentationDocumentVS.State.CANCELLED
					repDocFromUser.dateCanceled = Calendar.getInstance().getTime()
					repDocFromUser.save()
				}
			}
			String userId = String.format('%05d', user.id)
			render "prepareUserBaseData - user: ${userId} of ${usersVS.size()} - ${repDocsFromUser.size()} representations<br/>"
			log.info("prepareUserBaseData - user: ${userId} of ${usersVS.size()} - ${repDocsFromUser.size()} representations");
		}
		response.status = ResponseVS.SC_OK
		render "OK"
	}

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
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.VICKET_ERROR, reason:exception.getMessage())]
    }
	
}