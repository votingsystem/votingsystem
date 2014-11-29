package org.votingsystem.cooin.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.cooin.model.UserVSAccount
import org.votingsystem.cooin.util.IbanVSUtil

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class SubscriptionVSService {

    def grailsApplication
	def messageSource
    def userVSService
    def systemService

    @Transactional public ResponseVS checkUser(UserVS userVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug "$methodName - nif  '${userVS.getNif()}'"
		String msg
        CertificateVS certificate = null;
		if(!userVS.getNif()) throw new ExceptionVS(messageSource.getMessage('userDataWithErrors', null, locale),
                MetaInfMsg.getErrorMsg(methodName, "missingNif"))
		X509Certificate x509Cert = userVS.getCertificate()
		if (!x509Cert) throw new ExceptionVS("Missing certificate!!!", MetaInfMsg.getErrorMsg(methodName, "missingNif"))
		String validatedNIF = org.votingsystem.util.NifUtils.validate(userVS.getNif())
		UserVS userVSDB = UserVS.findByNif(validatedNIF)
        JSONObject deviceData = CertUtils.getCertExtensionData(x509Cert, ContextVS.DEVICEVS_OID)
		if (!userVSDB) {
            userVSDB = userVS.save()
            certificate = setUserCertificate(userVSDB, deviceData);
			log.debug "checkUser ### NEW UserVS '${userVSDB.nif}' CertificateVS id '${certificate.id}'"
            return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVSDB, data:userVSDB)
		} else {
            userVSDB.setCertificateCA(userVS.getCertificateCA())
            userVSDB.setCertificate(userVS.getCertificate())
            userVSDB.setTimeStampToken(userVS.getTimeStampToken())
			setUserCertificate(userVSDB, deviceData);
            return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVSDB)
		}
	}

    @Transactional  CertificateVS setUserCertificate(UserVS userVS, JSONObject deviceData) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug "$methodName - deviceData: ${deviceData}"
        X509Certificate x509Cert = userVS.getCertificate()
        CertificateVS certificate = CertificateVS.findWhere(userVS:userVS, state:CertificateVS.State.OK,
                serialNumber:x509Cert.getSerialNumber()?.longValue(), authorityCertificateVS: userVS.getCertificateCA())
        DeviceVS deviceVS = null;
        if(!certificate){
            certificate = new CertificateVS(userVS:userVS, content:x509Cert?.getEncoded(),
                    state:CertificateVS.State.OK, type:CertificateVS.Type.USER,
                    serialNumber:x509Cert?.getSerialNumber()?.longValue(),
                    authorityCertificateVS:userVS.getCertificateCA(), validFrom:x509Cert?.getNotBefore(),
                    validTo:x509Cert?.getNotAfter()).save();
            userVS.updateCertInfo(x509Cert).save()
            if(deviceData) {
                deviceVS = DeviceVS.findWhere(userVS:userVS, deviceId:deviceData.deviceId)
                if(!deviceVS) {
                    deviceVS = new DeviceVS(userVS:userVS, deviceId:deviceData.deviceId,email:deviceData.email,
                            phone:deviceData.mobilePhone, deviceName:deviceData.deviceName, certificateVS: certificate).save()
                    log.debug "$methodName - new device with id '${deviceVS.id}'"
                } else deviceVS.updateCertInfo(deviceData).save()
            }
            log.debug "$methodName - new certificate with id: '${certificate.id}'"
        } else if(deviceData?.deviceId) deviceVS = DeviceVS.findWhere(deviceId:deviceData.deviceId)
        userVS.setCertificateVS(certificate)
        userVS.setDeviceVS(deviceVS)
        return certificate
    }

    @Transactional
    public ResponseVS deActivateUser(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("$methodName - signer: ${userSigner?.nif}")
        SubscriptionVSRequest request = new SubscriptionVSRequest(messageSMIMEReq.getSMIME()?.getSignedContent())
        GroupVS groupVS = GroupVS.findWhere(name:request.groupvsName, id:Long.valueOf(request.id))
        if(!groupVS) {
            throw new ExceptionVS(messageSource.getMessage('itemNotFoundMsg', [messageJSON.groupvs.id].toArray(),
                    locale), MetaInfMsg.getErrorMsg(methodName, "groupNotFound"))
        }
        if(!groupVS.getRepresentative().nif.equals(request.userVSNIF) && !systemService.isUserAdmin(
                messageSMIMEReq.userVS.nif)) {
            throw new ExceptionVS(messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                TypeVS.COOIN_GROUP_USER_ACTIVATE.toString(), groupVS.name].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivilege"))
        }
        UserVS userToActivate = UserVS.findWhere(nif:request.userVSNIF)
        SubscriptionVS subscription = SubscriptionVS.findWhere(groupVS: groupVS, userVS:userToActivate)

        if(SubscriptionVS.State.CANCELLED == subscription.state) {
            throw new ExceptionVS(messageSource.getMessage('groupUserAlreadyCencelledErrorMsg',
                    [groupVS.name, userSigner.getNif()].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, "groupUserAlreadyCancelled"))
        }
        subscription.setReason(request.reason)
        subscription.setState(SubscriptionVS.State.CANCELLED)
        subscription.dateCancelled = Calendar.getInstance().getTime()
        messageSMIMEReq.setSubscriptionVS(subscription)
        log.debug("deActivateUser OK - userToActivate: ${userToActivate.nif} - group: ${groupVS.name}")

        Map responseMap = [statusCode:ResponseVS.SC_OK, message:messageSource.getMessage('cooinGroupUserdeActivatedMsg',
                [subscription.userVS.nif, subscription.groupVS.name].toArray(), locale)]
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.COOIN_GROUP_USER_DEACTIVATE,
                contentType: ContentTypeVS.JSON, data:responseMap,
                metaInf:MetaInfMsg.getOKMsg(methodName, "subscriptionVS_${subscription.id}"))
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
                messageSMIMEReq.getSMIME()?.getSignedContent())
        GroupVS groupVS = GroupVS.get(request.id)
        if(!groupVS || !request.groupvsName.equals(groupVS.name)) {
            msg = messageSource.getMessage('itemNotFoundMsg', [request.id].toArray(), locale)
            log.error "activateUser - DATA ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, 'groupNotFound'), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(!groupVS.getRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !systemService.isUserAdmin(
                messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                     TypeVS.COOIN_GROUP_USER_ACTIVATE.toString(), groupVS.name].toArray(), locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivilege"))
        }
        UserVS userToActivate = UserVS.findWhere(nif:request.userVSNIF)
        SubscriptionVS subscription = SubscriptionVS.findWhere(groupVS: groupVS, userVS:userToActivate)
        if(!userToActivate || SubscriptionVS.State.ACTIVE == subscription.state) {
            msg = messageSource.getMessage('groupUserNotPendingErrorMsg',
                    [groupVS.name, userSigner.getNif()].toArray(), locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "groupUserNotPending"))
        }
        checkUserVSAccount(userToActivate)
        subscription.setState(SubscriptionVS.State.ACTIVE)
        subscription.dateActivated = Calendar.getInstance().getTime()
        messageSMIMEReq.setSubscriptionVS(subscription)
        log.debug("activateUser OK - userToActivate: ${userToActivate.nif} - group: ${groupVS.name}")

        Map responseMap = [statusCode:ResponseVS.SC_OK, message:messageSource.getMessage('cooinGroupUserActivatedMsg',
                [subscription.userVS.nif, subscription.groupVS.name].toArray(), locale)]
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.COOIN_GROUP_USER_ACTIVATE,
                contentType: ContentTypeVS.JSON, data:responseMap,
                metaInf:MetaInfMsg.getOKMsg(methodName, "subscriptionVS_${subscription.id}"))
    }

    private static class SubscriptionVSRequest {
        String groupvsName, groupvsInfo, userVSName, userVSNIF, reason;
        TypeVS operation;
        Long id;
        public SubscriptionVSRequest(String signedContent) {
            def messageJSON = JSON.parse(signedContent)
            operation = TypeVS.valueOf(messageJSON.operation)
            groupvsName = messageJSON.groupvs.name;
            userVSName = messageJSON.uservs.name
            reason = messageJSON.reason
            id = Long.valueOf(messageJSON.groupvs.id)
            userVSNIF = messageJSON.uservs.NIF
            if(!groupvsName) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsName'");
            if(!userVSName) throw new ValidationExceptionVS(this.getClass(), "missing param 'userVSName'");
            if(!userVSNIF) throw new ValidationExceptionVS(this.getClass(), "missing param 'userVSNIF'");
        }
        public static SubscriptionVSRequest getUserVSActivationRequest(String signedContent) {
            SubscriptionVSRequest result = new SubscriptionVSRequest(signedContent)
            if(TypeVS.COOIN_GROUP_USER_ACTIVATE != result.operation) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'COOIN_GROUP_USER_ACTIVATE' - operation found: " + result.operation.toString())
            return result
        }

        public static SubscriptionVSRequest getUserVSDeActivationRequest(String signedContent) {
            SubscriptionVSRequest result = new SubscriptionVSRequest(signedContent)
            if(TypeVS.COOIN_GROUP_USER_DEACTIVATE != result.operation) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'COOIN_GROUP_USER_ACTIVATE' - operation found: " + result.operation.toString())
            return result
        }
    }

}