package org.votingsystem.ticket.service

import org.votingsystem.model.*
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class SubscriptionVSService {
		
	static transactional = true
    def grailsApplication
	def messageSource
	
	ResponseVS checkUser(UserVS userVS, Locale locale) {
		log.debug "checkUser - userVS.nif  '${userVS.getNif()}'"
		String msg
        CertificateVS certificate = null;
		if(!userVS.getNif()) {
			msg = messageSource.getMessage('userDataWithErrors', null, locale)
			log.error("checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR)
		}
		X509Certificate x509Cert = userVS.getCertificate()
		if (!x509Cert) {
			log.debug("Missing certificate!!!")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:"Missing certificate!!!")
		}
		String validatedNIF = org.votingsystem.util.NifUtils.validate(userVS.getNif())
		if(!validatedNIF) {
			msg = messageSource.getMessage('NIFWithErrorsMsg', [userVS.getNif()].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR)
		}
		UserVS userVSDB = UserVS.findByNif(validatedNIF.toUpperCase())
		if (!userVSDB) {
            userVS.nif = validatedNIF.toUpperCase()
            userVS.type = UserVS.Type.USER
			userVS.save();
            userVSDB = userVS
			log.debug "- checkUser ### NEW USER ${userVSDB.nif} - id '${userVSDB.id}'"
			certificate = new CertificateVS(userVS:userVS, content:x509Cert.getEncoded(),
                    serialNumber:x509Cert.getSerialNumber()?.longValue(), state:CertificateVS.State.OK,
                    type:CertificateVS.Type.USER, authorityCertificate:userVS.getCertificateCA(),
				    validFrom:x509Cert.getNotBefore(), validTo:x509Cert.getNotAfter())
			certificate.save();
			log.debug "- checkUser ### NEW USER CertificateVS id '${certificate.id}'"
		} else {
			certificate = CertificateVS.findWhere(userVS:userVSDB, state:CertificateVS.State.OK)
			if (!certificate?.serialNumber == x509Cert.getSerialNumber()?.longValue()) {
				certificate.state = CertificateVS.State.CANCELLED
				certificate.save()
				log.debug "- checkUser - CANCELLED CertificateVS id '${certificate.id}'"
				certificate = new CertificateVS(userVS:userVSDB, content:x509Cert?.getEncoded(),
                    state:CertificateVS.State.OK, serialNumber:x509Cert?.getSerialNumber()?.longValue(),
                    authorityCertificate:userVS.getCertificateCA(), validFrom:x509Cert?.getNotBefore(),
                    validTo:x509Cert?.getNotAfter())
				certificate.save();
				log.debug "- checkUser - UPDATED CertificateVS id '${certificate.id}'"
			}
            userVSDB.setCertificateCA(userVS.getCertificateCA())
            userVSDB.setCertificate(userVS.getCertificate())
            userVSDB.setTimeStampToken(userVS.getTimeStampToken())
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVSDB, data:certificate)
	}
	
	ResponseVS checkDevice(String givenname, String surname, String nif, String phone, String email,
               String deviceId, Locale locale) {
		log.debug "checkDevice - givenname: ${givenname} - surname: ${surname} - nif:${nif} - phone:${phone} " +
                "- email:${email} - deviceId:${deviceId}"
		if(!nif || !deviceId) {
			log.debug "Missing params"
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
				messageSource.getMessage('requestWithoutData', null, locale))
		}
		String validatedNIF = org.votingsystem.util.NifUtils.validate(nif)
		if(!validatedNIF) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
				messageSource.getMessage('NIFWithErrorsMsg', [nif].toArray(), locale))
		}
		UserVS userVS = UserVS.findWhere(nif:validatedNIF)
		if (!userVS) {
			userVS = new UserVS(nif:validatedNIF, email:email, phone:phone, type:UserVS.Type.USER,
                    name:givenname, firstName:givenname, lastName:surname).save()
		}
		DeviceVS device = DeviceVS.findWhere(deviceId:deviceId)
		if (!device || (device.userVS.id != userVS.id)) device =
                new DeviceVS(userVS:userVS, phone:phone, email:email, deviceId:deviceId).save()
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVS, data:device)
	}

}