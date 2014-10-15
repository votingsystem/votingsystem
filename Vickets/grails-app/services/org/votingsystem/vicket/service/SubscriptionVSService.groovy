package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.IbanVSUtil

import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class SubscriptionVSService {

	static transactional = false

    private static final CLASS_NAME = SubscriptionVSService.class.getSimpleName()

    def grailsApplication
	def messageSource
    def userVSService
    def systemService

    @Transactional public ResponseVS checkUser(UserVS userVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug "checkUser - userVS.nif  '${userVS.getNif()}'"
		String msg
        CertificateVS certificate = null;
		if(!userVS.getNif()) {
			msg = messageSource.getMessage('userDataWithErrors', null, LocaleContextHolder.locale)
			log.error("checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR,
                metaInf: MetaInfMsg.getErrorMsg(methodName, "missingNif"))
		}
		X509Certificate x509Cert = userVS.getCertificate()
		if (!x509Cert) {
			log.debug("Missing certificate!!!")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:"Missing certificate!!!",
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "missingCert"))
		}
		String validatedNIF = org.votingsystem.util.NifUtils.validate(userVS.getNif())
		UserVS userVSDB = UserVS.findByNif(validatedNIF.toUpperCase())
        JSONObject deviceData = CertUtils.getCertExtensionData(x509Cert, ContextVS.DEVICEVS_OID)
        boolean isNewUser = false
		if (!userVSDB) {
            userVSDB = userVS.save()
			certificate = saveUserCertificate(userVS, deviceData);
            isNewUser = true
			log.debug "checkUser ### NEW UserVS '${userVSDB.nif}' CertificateVS id '${certificate.id}'"
		} else {
            userVSDB.setCertificateCA(userVS.getCertificateCA())
            userVSDB.setCertificate(userVS.getCertificate())
            userVSDB.setTimeStampToken(userVS.getTimeStampToken())
			certificate = saveUserCertificate(userVSDB, deviceData);
		}
        userVS.setCertificateVS(certificate)
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVSDB, data:[isNewUser:isNewUser,certificateVS:certificate])
	}

    @Transactional
    public CertificateVS saveUserCertificate(UserVS userVS, JSONObject deviceData) {
        log.debug "saveUserCertificate - deviceData: ${deviceData}"
        X509Certificate x509Cert = userVS.getCertificate()
        CertificateVS certificate = CertificateVS.findWhere(userVS:userVS, state:CertificateVS.State.OK,
                serialNumber:x509Cert.getSerialNumber()?.longValue(), authorityCertificateVS: userVS.getCertificateCA())
        if(certificate) return certificate
        else{
            certificate = new CertificateVS(userVS:userVS, content:x509Cert?.getEncoded(),
                    state:CertificateVS.State.OK, type:CertificateVS.Type.USER,
                    serialNumber:x509Cert?.getSerialNumber()?.longValue(),
                    authorityCertificateVS:userVS.getCertificateCA(), validFrom:x509Cert?.getNotBefore(),
                    validTo:x509Cert?.getNotAfter()).save();
            if(deviceData) {
                DeviceVS deviceVS = DeviceVS.findWhere(userVS:userVS, deviceId:deviceData.deviceId)
                if(!deviceVS) {
                    deviceVS = new DeviceVS(userVS:userVS, deviceId:deviceData.deviceId,email:deviceData.email,
                            phone:deviceData.mobilePhone, deviceName:deviceData.deviceName, certificateVS: certificate).save()
                    log.debug "saveUserCertificate - certificate id: '${certificate.id}' - new device with id '${deviceVS.id}'"
                }
            } else log.debug "saveUserCertificate - certificate id: '${certificate.id}'"
            return certificate
        }
    }

    public ResponseVS checkDevice(String givenname, String surname, String nif, String phone, String email,
               String deviceId) {
		log.debug "checkDevice - givenname: ${givenname} - surname: ${surname} - nif:${nif} - phone:${phone} " +
                "- email:${email} - deviceId:${deviceId}"
		if(!nif || !deviceId) {
			log.debug "Missing params"
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
				messageSource.getMessage('requestWithoutData', null, LocaleContextHolder.locale))
		}
		String validatedNIF = org.votingsystem.util.NifUtils.validate(nif)
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

    @Transactional
    public ResponseVS deActivateUser(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("deActivateUser - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        log.debug("deActivateUser - documentStr: ${documentStr}")
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvs.name || !messageJSON.groupvs.id ||
                !messageJSON.uservs.name || !messageJSON.uservs.NIF ||
                (TypeVS.VICKET_GROUP_USER_DEACTIVATE != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, LocaleContextHolder.locale)
            log.error "deActivateUser - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "params"), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.findWhere(name:messageJSON.groupvs.name.trim(), id:Long.valueOf(messageJSON.groupvs.id))
        if(!groupVS) {
            msg = messageSource.getMessage('itemNotFoundMsg', [messageJSON.groupvs.id].toArray(), LocaleContextHolder.locale)
            log.error "deActivateUser - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "groupNotFound"), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }

        if(!groupVS.getRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !systemService.isUserAdmin(
                messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                 TypeVS.VICKET_GROUP_USER_ACTIVATE.toString(), groupVS.name].toArray(), LocaleContextHolder.locale)
            log.error "deActivateUser - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivilege"))
        }
        UserVS userToActivate = UserVS.findWhere(nif:messageJSON.uservs.NIF)
        SubscriptionVS subscription = SubscriptionVS.findWhere(groupVS: groupVS, userVS:userToActivate)

        if(SubscriptionVS.State.CANCELLED == subscription.state) {
            msg = messageSource.getMessage('groupUserAlreadyCencelledErrorMsg',
                    [groupVS.name, userSigner.getNif()].toArray(), LocaleContextHolder.locale)
            log.error "deActivateUser - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "groupUserAlreadyCancelled"))
        }
        subscription.setReason(messageJSON.reason)
        subscription.setState(SubscriptionVS.State.CANCELLED)
        subscription.dateCancelled = Calendar.getInstance().getTime()
        messageSMIMEReq.setSubscriptionVS(subscription)
        log.debug("deActivateUser OK - userToActivate: ${userToActivate.nif} - group: ${groupVS.name}")
        return new ResponseVS(type:TypeVS.VICKET_GROUP_USER_DEACTIVATE, message:msg, statusCode:ResponseVS.SC_OK,
                metaInf:MetaInfMsg.getOKMsg(methodName, "subscriptionVS_${subscription.id}"), data:subscription)
        return responseVS
    }

    public UserVSAccount checkUserVSAccount(UserVS userVS){
        UserVSAccount userAccount = UserVSAccount.findWhere(userVS:userVS)
        if(!userAccount) {
            userVS.setIBAN(IbanVSUtil.getInstance().getIBAN(userVS.id))
            new UserVSAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:userVS,
                    balance:BigDecimal.ZERO, IBAN:userVS.getIBAN(), tag:systemService.getWildTag()).save()
        }
        return userAccount
    }

    @Transactional
    public ResponseVS activateUser(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("activateUser - signer: ${userSigner?.nif}")
        String msg = null
        SubscriptionVSRequest request = SubscriptionVSRequest.getUserVSActivationRequest(
                messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        GroupVS groupVS = GroupVS.get(request.id)
        if(!groupVS || !request.groupvsName.equals(groupVS.name)) {
            msg = messageSource.getMessage('itemNotFoundMsg', [request.id].toArray(), LocaleContextHolder.locale)
            log.error "activateUser - DATA ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, 'groupNotFound'), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(!groupVS.getRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !systemService.isUserAdmin(
                messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                     TypeVS.VICKET_GROUP_USER_ACTIVATE.toString(), groupVS.name].toArray(), LocaleContextHolder.locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivilege"))
        }
        UserVS userToActivate = UserVS.findWhere(nif:request.userVSNIF)
        SubscriptionVS subscription = SubscriptionVS.findWhere(groupVS: groupVS, userVS:userToActivate)
        if(!userToActivate || SubscriptionVS.State.ACTIVE == subscription.state) {
            msg = messageSource.getMessage('groupUserNotPendingErrorMsg',
                    [groupVS.name, userSigner.getNif()].toArray(), LocaleContextHolder.locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "groupUserNotPending"))
        }
        checkUserVSAccount(userToActivate)
        subscription.setState(SubscriptionVS.State.ACTIVE)
        subscription.dateActivated = Calendar.getInstance().getTime()
        messageSMIMEReq.setSubscriptionVS(subscription)
        log.debug("activateUser OK - userToActivate: ${userToActivate.nif} - group: ${groupVS.name}")
        return new ResponseVS(type:TypeVS.VICKET_GROUP_USER_ACTIVATE, message:msg, statusCode:ResponseVS.SC_OK,
                metaInf:MetaInfMsg.getOKMsg(methodName, "subscriptionVS_${subscription.id}"), data:subscription)
    }

    private static class SubscriptionVSRequest {
        String groupvsName, groupvsInfo, userVSName, userVSNIF;
        TypeVS operation;
        Long id;
        public SubscriptionVSRequest() {}
        public static SubscriptionVSRequest getUserVSActivationRequest(String signedContent) {
            SubscriptionVSRequest result = new SubscriptionVSRequest()
            def messageJSON = JSON.parse(signedContent)
            if(TypeVS.VICKET_GROUP_USER_ACTIVATE != TypeVS.valueOf(messageJSON.operation)) throw ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'VICKET_GROUP_USER_ACTIVATE' - operation found: " + messageJSON.operation)
            result.groupvsName = messageJSON.groupvs.name;
            result.userVSName = messageJSON.uservs.name
            result.id = Long.valueOf(messageJSON.groupvs.id)
            result.userVSNIF = messageJSON.uservs.NIF
            if(!result.groupvsName) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsName'");
            if(!result.userVSName) throw new ValidationExceptionVS(this.getClass(), "missing param 'userVSName'");
            if(!result.userVSNIF) throw new ValidationExceptionVS(this.getClass(), "missing param 'userVSNIF'");
            return result
        }
    }


}