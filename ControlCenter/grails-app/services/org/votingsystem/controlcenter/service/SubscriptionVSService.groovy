package org.votingsystem.controlcenter.service

import grails.converters.JSON
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils
import org.votingsystem.model.ActorVS
import org.votingsystem.model.AccessControlVS
import org.votingsystem.model.ResponseVS
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class SubscriptionVSService {
		
	static transactional = true
	
	def messageSource
    def grailsApplication
	
	ResponseVS checkUser(UserVS userVS, Locale locale) {
		log.debug " --- checkUser - userVS '${userVS.nif}'"
		String msg
		if(!userVS?.nif) {
			msg = messageSource.getMessage('userDataWithErrors', null, locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR)
		}
		String nifValidado = org.votingsystem.util.NifUtils.validate(userVS.nif)
		if(!nifValidado) {
			msg = messageSource.getMessage('NIFWithErrorsMsg', [userVS.nif].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR)
		}
		userVS.nif = nifValidado.toUpperCase()
		X509Certificate certificateUsu = userVS.getCertificate()
		def userVSDB = UserVS.findWhere(nif:userVS.nif)
		if (!userVSDB) {
            userVS.type = UserVS.Type.USER
			userVSDB = userVS.save();
			log.debug "- checkUser - NEW USER -> ${userVS.nif} - id '${userVSDB.id}'"
			if (userVS.getCertificate()) {
				CertificateVS certificate = new CertificateVS(userVS:userVS,
					content:userVS.getCertificate()?.getEncoded(),
					serialNumber:userVS.getCertificate()?.getSerialNumber()?.longValue(),
					state:CertificateVS.State.OK, type:CertificateVS.Type.USER,
					validFrom:userVS.getCertificate()?.getNotBefore(),
					validTo:userVS.getCertificate()?.getNotAfter())
				certificate.save();
				log.debug "- checkUser - NEW USER CERT -> id '${certificate.id}'"
			}
		} else {
			CertificateVS certificate = CertificateVS.findWhere(userVS:userVSDB, state:CertificateVS.State.OK)
			if (!certificate?.serialNumber == certificateUsu.getSerialNumber()?.longValue()) {
				certificate.state = CertificateVS.State.CANCELLED
				certificate.save()
				log.debug "- checkUser - CANCELLED user cert id '${certificate.id}'"
				certificate = new CertificateVS(userVS:userVSDB,
					content:certificateUsu?.getEncoded(), state:CertificateVS.State.OK,
					serialNumber:certificateUsu?.getSerialNumber()?.longValue(),
					certificateAutoridad:userVS.getCertificateCA(),
					validFrom:userVS.getCertificate()?.getNotBefore(),
					validTo:userVS.getCertificate()?.getNotAfter())
				certificate.save();
				log.debug "- checkUser - UPDATED user cert -> id '${certificate.id}'"
			}
            userVSDB.setCertificateCA(userVS.getCertificateCA())
            userVSDB.setCertificate(userVS.getCertificate())
            userVSDB.setTimeStampToken(userVS.getTimeStampToken())
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVSDB)
	}

	AccessControlVS checkAccessControl(String serverURL) {
		log.debug "--- checkAccessControlVS - serverURL:${serverURL}"
		serverURL = StringUtils.checkURL(serverURL)
		AccessControlVS accessControl = AccessControlVS.findWhere(serverURL:serverURL)
        if(accessControl) return accessControl
		else {
			ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(serverURL), null)
			if (ResponseVS.SC_OK == responseVS.statusCode) {
				try {
					accessControl = new AccessControlVS(ActorVS.populate(JSON.parse(responseVS.message)))
					accessControl.save()
				} catch(Exception ex) {
					log.error(ex.getMessage(), ex)
				}
			} else return null
		}
	}
	
}