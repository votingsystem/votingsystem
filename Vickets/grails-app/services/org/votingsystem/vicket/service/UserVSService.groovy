package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketSource
import org.votingsystem.vicket.util.MetaInfMsg
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.vicket.util.IbanVSUtil
import org.votingsystem.util.NifUtils

import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UserVSService {
	
	static transactional = false

    def signatureVSService
	def grailsApplication
    def grailsLinkGenerator
    def messageSource
    def subscriptionVSService
    private UserVS systemUser

    public synchronized Map init() throws Exception {
        log.debug("init")
        systemUser = UserVS.findWhere(type:UserVS.Type.SYSTEM)
        if(!systemUser) {
            systemUser = new UserVS(nif:grailsApplication.config.VotingSystem.systemNIF, type:UserVS.Type.SYSTEM,
                    name:grailsApplication.config.VotingSystem.serverName).save()
            systemUser.setIBAN(IbanVSUtil.getInstance().getIBAN(systemUser.id))
            systemUser.save()
        }
        return [systemUser:systemUser]
    }

    public UserVS getSystemUser() {
        if(!systemUser) systemUser = init().systemUser
        return systemUser;
    }

    public ResponseVS saveVicketSource(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("${methodName} - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.info || !messageJSON.certChainPEM || !messageJSON.IBAN ||
                (TypeVS.VICKET_SOURCE_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - PARAMS ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, reason:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        try {
            IbanVSUtil.validate(messageJSON.IBAN);
        } catch(Exception ex) {
            msg = messageSource.getMessage('IBANCodeErrorMsg', [messageJSON.IBAN].toArray(), locale)
            log.error("${msg} - ${ex.getMessage()}", ex)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: msg,
                    metaInf:MetaInfMsg.getExceptionMsg(methodName, ex, "IBAN_code"), type:TypeVS.ERROR)
        }
        if(!isUserAdmin(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                    TypeVS.VICKET_SOURCE_NEW.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }

        Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes());

        log.debug("======== ${certChain}")

        responseVS = signatureVSService.validateCertificates(new ArrayList(certChain))
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        X509Certificate x509Certificate = certChain.iterator().next();

        //{info:getEditor_editorDivData(),certChainPEM:$("#pemCert").val(), operation:Operation.VICKET_SOURCE_NEW}

        VicketSource vicketSource = UserVS.getVicketSource(x509Certificate)
        String validatedNIF = org.votingsystem.util.NifUtils.validate(vicketSource.getNif())
        if(!validatedNIF) {
            msg = messageSource.getMessage('NIFWithErrorsMsg', [vicketSource.getNif()].toArray(), locale)
            log.error("checkUser - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR,
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "nif"))
        }

        def vicketSourceDB = UserVS.findWhere(nif:validatedNIF.toUpperCase())

        if(!vicketSourceDB || !(vicketSourceDB instanceof VicketSource)) {
            vicketSource.description = messageJSON.info
            vicketSourceDB.setIBAN(messageJSON.IBAN)
            vicketSource.save()
            vicketSourceDB = vicketSource
            log.debug("${methodName} - NEW vicketSource.id: '${vicketSourceDB.id}'")
        } else {
            log.debug("${methodName} - updating vicketSource.id: '${vicketSourceDB.id}'")
            vicketSourceDB.description = messageJSON.info
            vicketSourceDB.setCertificateCA(vicketSource.getCertificateCA())
            vicketSourceDB.setCertificate(vicketSource.getCertificate())
            vicketSourceDB.setTimeStampToken(vicketSource.getTimeStampToken())
        }
        CertificateVS certificateVS = subscriptionVSService.saveUserCertificate(vicketSourceDB, null)

        vicketSourceDB.save()
        msg = messageSource.getMessage('newVicketSourceOKMsg', [x509Certificate.subjectDN].toArray(), locale)
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "vicketSource_${vicketSourceDB.id}_certificateVS_${certificateVS.id}")
        String vicketSourceURL = "${grailsLinkGenerator.link(controller:"userVS", absolute:true)}/${vicketSourceDB.id}"
        log.debug("${metaInfMsg}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_SOURCE_NEW, message:msg, metaInf:metaInfMsg,
            data:[message:msg, URL:vicketSourceURL, statusCode:ResponseVS.SC_OK], contentType:ContentTypeVS.JSON)
    }

    /*
     * Método para poder añadir usuarios a partir de un certificado en formato PEM
     */
    @Transactional
    public ResponseVS saveUser(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        /*if(grails.util.Environment.PRODUCTION  ==  grails.util.Environment.current) {
            log.debug(" ### ADDING CERTS NOT ALLOWED IN PRODUCTION ENVIRONMENTS ###")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message: messageSource.getMessage('serviceDevelopmentModeMsg', null, locale))
        }*/
        ResponseVS responseVS = null;
        UserVS userSigner = messageSMIMEReq.getUserVS()
        String msg
        if(!isUserAdmin(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                     TypeVS.CERT_CA_NEW.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }

        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        if (!messageJSON.info || !messageJSON.certChainPEM ||
                (TypeVS.CERT_USER_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName}- ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName, "params"),
                    reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes());
        UserVS newUser = UserVS.getUserVS(certChain.iterator().next())

        responseVS = signatureVSService.verifyUserCertificate(newUser)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        responseVS = subscriptionVSService.checkUser(newUser, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        String userURL = "${grailsLinkGenerator.link(controller:"userVS", absolute:true)}/${responseVS.getUserVS().id}"

        if(!responseVS.data.isNewUser) {
            msg = messageSource.getMessage('certUserNewErrorMsg', [responseVS.getUserVS().getNif()].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.CERT_USER_NEW, contentType:
                    ContentTypeVS.JSON, data:[message:msg, URL:userURL, statusCode:ResponseVS.SC_ERROR], message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userVS_${responseVS.getUserVS().id}"))
        }

        responseVS.getUserVS().setState(UserVS.State.ACTIVE)
        responseVS.getUserVS().setReason(messageJSON.info)
        responseVS.getUserVS().save()
        msg = messageSource.getMessage('certUserNewMsg', [responseVS.getUserVS().getNif()].toArray(), locale)

        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.CERT_USER_NEW, contentType: ContentTypeVS.JSON,
                data:[message:msg, URL:userURL, statusCode:ResponseVS.SC_OK], message:msg,
                metaInf:MetaInfMsg.getOKMsg(methodName, "userVS_${responseVS.getUserVS().id}"))
    }

	public Map getUserVS(Date fromDate){
		def usersVS
		UserVS.withTransaction {
			usersVS = UserVS.createCriteria().list(offset: 0) {
				gt("dateCreated", fromDate)
			}
		}
		return [totalNumUsu:usersVS?usersVS.getTotalCount():0]
	}

    @Transactional
    public Map getUserVSDataMap(UserVS userVS){
        String name = userVS.name
        def certificateList = []
        def certificates = CertificateVS.findAllWhere(userVS:userVS, state:CertificateVS.State.OK)
        certificates.each {certItem ->
            X509Certificate x509Cert = certItem.getX509Cert()
            certificateList.add([serialNumber:"${certItem.serialNumber}",
                 pemCert:new String(CertUtil.getPEMEncoded (x509Cert), "UTF-8")])
        }

        if(!userVS.name) name = "${userVS.firstName} ${userVS.lastName}"
        return [id:userVS?.id, nif:userVS?.nif, firstName: userVS.firstName, lastName: userVS.lastName, name:name,
                IBAN:userVS.IBAN, state:userVS.state.toString(), type:userVS.type.toString(), reason:userVS.reason,
                description:userVS.description, certificateList:certificateList]
    }


    public Map getSubscriptionVSDataMap(SubscriptionVS subscriptionVS){
        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
             dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
             uservs:[id:subscriptionVS.userVS.id, NIF:subscriptionVS.userVS.nif,
                   name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
             groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

    public Map getSubscriptionVSDetailedDataMap(SubscriptionVS subscriptionVS){
        String subscriptionMessageURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${subscriptionVS.subscriptionSMIME.id}"
        def adminMessages = []
        SubscriptionVS.withTransaction {
            subscriptionVS.adminMessageSMIMESet.each {adminMessage ->
                adminMessages.add("${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${adminMessage.id}")
            }
        }

        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
                dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
                messageURL:subscriptionMessageURL,adminMessages:adminMessages,
                uservs:[id:subscriptionVS.userVS.id, NIF:subscriptionVS.userVS.nif,
                      name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
                groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

	boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        boolean result = grailsApplication.config.VotingSystem.adminsDNI.contains(nif)
        if(result) log.debug("isUserAdmin - nif: ${nif}")
		return result
	}

}

