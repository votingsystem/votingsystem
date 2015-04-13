package org.votingsystem.web.ejb;

import org.votingsystem.dto.currency.SubscriptionVSDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class SubscriptionVSBean {

    private static final Logger log = Logger.getLogger(SubscriptionVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;

    public UserVS checkUser(UserVS userVS) throws ExceptionVS, IOException, CertificateEncodingException {
        log.log(Level.FINE, "nif: " + userVS.getNif());
        CertificateVS certificate = null;
        if(userVS.getNif() == null) throw new ExceptionVS("ERROR - missing Nif");
        X509Certificate x509Cert = userVS.getCertificate();
        if (x509Cert == null) throw new ExceptionVS("Missing certificate!!!");
        userVS.setNif(org.votingsystem.util.NifUtils.validate(userVS.getNif()));
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", userVS.getNif());
        UserVS userVSDB = dao.getSingleResult(UserVS.class, query);
        Map<String, String> deviceData = CertUtils.getCertExtensionData(x509Cert, ContextVS.DEVICEVS_OID);
        if (userVSDB == null) {
            userVSDB = dao.persist(userVS);
            log.log(Level.FINE, "checkUser ### NEW UserVS:" + userVSDB.getNif());
        } else {
            userVSDB.setCertificateCA(userVS.getCertificateCA());
            userVSDB.setCertificate(userVS.getCertificate());
            userVSDB.setTimeStampToken(userVS.getTimeStampToken());
            setUserData(userVSDB, deviceData);
        }
        setUserData(userVSDB, deviceData);
        return userVSDB;
    }

    public void setUserData(UserVS userVS, Map<String, String> deviceData) throws
            CertificateEncodingException, IOException {
        log.log(Level.FINE, " deviceData: " + deviceData);
        X509Certificate x509Cert = userVS.getCertificate();
        Query query = dao.getEM().createNamedQuery("findCertByUserAndStateAndSerialNumberAndCertificateCA")
                .setParameter("userVS", userVS).setParameter("state",CertificateVS.State.OK)
                .setParameter("serialNumber", x509Cert.getSerialNumber().longValue())
                .setParameter("authorityCertificateVS", userVS.getCertificateCA());
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        DeviceVS deviceVS = null;
        if(certificate == null){
            certificate = dao.persist(new CertificateVS(userVS, x509Cert,  CertificateVS.State.OK,
                    CertificateVS.Type.USER,  userVS.getCertificateCA(), x509Cert.getNotBefore(), x509Cert.getNotAfter()));
            if(deviceData != null) {
                query = dao.getEM().createNamedQuery("findDeviceByUserAndDeviceId").setParameter("userVS", userVS)
                        .setParameter("deviceId", deviceData.get("deviceId"));
                deviceVS = dao.getSingleResult(DeviceVS.class, query);
                if(deviceVS == null) {
                    deviceVS = (DeviceVS) dao.persist(new DeviceVS(userVS, deviceData.get("deviceId"), deviceData.get("email"),
                            deviceData.get("mobilePhone"), deviceData.get("deviceName"), certificate));
                    log.log(Level.FINE, "new device with id: " + deviceVS.getId());
                } else dao.getEM().merge(deviceVS.updateCertInfo(deviceData));
            }
            log.log(Level.FINE, "new certificate with id:" + certificate.getId());
        } else if(deviceData != null && deviceData.containsKey("deviceId")) {
            query = dao.getEM().createNamedQuery("findDeviceByDeviceId").setParameter("deviceId", deviceData.get("deviceId"));
            deviceVS = dao.getSingleResult(DeviceVS.class, query);
            if(deviceVS == null) {
                deviceVS = (DeviceVS) dao.persist(new DeviceVS(userVS, deviceData.get("deviceId"), deviceData.get("email"),
                        deviceData.get("mobilePhone"), deviceData.get("deviceName"), certificate));
                log.log(Level.FINE, "new device with id: " + deviceVS.getId());
            }
        }
        userVS.setCertificateVS(certificate);
        userVS.setDeviceVS(deviceVS);
    }

    public DeviceVS checkDevice(String givenname, String surname, String nif, String phone, String email, String deviceId,
                           String deviceType) throws ExceptionVS {
        log.info(format("checkDevice - givenname: {0} - surname: {1} - nif:{2} - phone: {3}" +
                " - email: {4} - deviceId: {5} - deviceType: {6}", givenname, surname, nif, phone,
                email, deviceId, deviceType));
        if(nif == null) throw new ValidationExceptionVS("missing 'nif'");
        if(deviceId == null) throw new ValidationExceptionVS("missing 'deviceId'");
        String validatedNIF = org.votingsystem.util.NifUtils.validate(nif);
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", validatedNIF);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if (userVS == null) userVS = dao.persist(new UserVS(validatedNIF, UserVS.Type.USER, givenname,
                givenname, surname, email, phone));
        query = dao.getEM().createQuery("select d from DeviceVS d where d.deviceId =:deviceId")
                .setParameter("deviceId", deviceId);
        DeviceVS device = dao.getSingleResult(DeviceVS.class, query);
        DeviceVS.Type type = device != null ? device.getType():null;
        if (device == null || (device.getUserVS().getId() != userVS.getId())) device = dao.persist(
                new DeviceVS(userVS, deviceId, email, phone, type));
        else dao.merge(device.setEmail(email).setPhone(phone));
        return device;
    }

    //DeviceVS(UserVS userVS, String deviceId, String email, String phone, Type type)
    public SubscriptionVS deActivateUser(MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        log.log(Level.FINE, "signer: " + signer.getNif());
        SubscriptionVSDto request = messageSMIME.getSignedContent(SubscriptionVSDto.class);
        GroupVS groupVS = dao.find(GroupVS.class, request.getId());
        if(groupVS == null || !request.getGroupvsName().equals(groupVS.getName())) {
            throw new ExceptionVS("group with name: " + request.getGroupvsName() + " and id: " + request.getId() + " not found");
        }
        if(!groupVS.getRepresentative().getNif().equals(request.getUserVSNIF()) && !signatureBean.isUserAdmin(signer.getNif())) {
            throw new ExceptionVS("'userWithoutGroupPrivilegesErrorMsg - groupVS:" + request.getGroupvsName() + " - nif:" +
                    signer.getNif());
        }
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getUserVSNIF());
        UserVS groupUser = dao.getSingleResult(UserVS.class, query);
        if(groupUser == null) throw new ValidationExceptionVS("user unknown - nif:" + request.getUserVSNIF());
        query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("groupVS", groupVS)
                .setParameter("userVS", groupUser);
        SubscriptionVS subscription = dao.getSingleResult(SubscriptionVS.class, query);
        if(subscription == null || SubscriptionVS.State.CANCELED == subscription.getState()) {
            throw new ExceptionVS("groupUserAlreadyCencelledErrorMsg - user nif: " + request.getUserVSNIF() +
                    " - group: " + request.getGroupvsName());
        }
        subscription.setReason(request.getReason());
        subscription.setState(SubscriptionVS.State.CANCELED);
        subscription.setDateCancelled(new Date());
        subscription.setCancellationSMIME(messageSMIME);
        log.info("deActivateUser OK - user nif: " + request.getUserVSNIF() + " - group: " + request.getGroupvsName());
        return subscription;
    }

    public SubscriptionVS activateUser(MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        log.info("signer: " + signer.getNif());
        SubscriptionVSDto request = messageSMIME.getSignedContent(SubscriptionVSDto.class);
        request.validateActivationRequest();
        GroupVS groupVS = dao.find(GroupVS.class, request.getId());
        if(groupVS == null || !request.getGroupvsName().equals(groupVS.getName())) {
            throw new ValidationExceptionVS("Group with id: " + request.getId() + " and name: " + request.getGroupvsName() + " not found");
        }
        if(!groupVS.getRepresentative().getNif().equals(signer.getNif()) && !signatureBean.isUserAdmin(signer.getNif())) {
            throw new ValidationExceptionVS("userWithoutGroupPrivilegesErrorMsg - operation: " +
                    TypeVS.CURRENCY_GROUP_USER_ACTIVATE.toString() + " - nif: " + signer.getNif() + " - group: " +
                    request.getGroupvsName());
        }
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getUserVSNIF());
        UserVS groupUser = dao.getSingleResult(UserVS.class, query);
        if(groupUser == null) throw new ValidationExceptionVS("user unknown - nif:" + request.getUserVSNIF());
        query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("groupVS", groupVS)
                .setParameter("userVS", groupUser);
        SubscriptionVS subscription = dao.getSingleResult(SubscriptionVS.class, query);
        if(subscription == null) throw new ValidationExceptionVS("user:" + request.getUserVSNIF() +
                " has not pending subscription request");
        subscription.setState(SubscriptionVS.State.ACTIVE);
        subscription.setDateActivated(new Date());
        subscription.setActivationSMIME(messageSMIME);
        log.info("activateUser OK - user nif: " + request.getUserVSNIF() + " - group: " + request.getGroupvsName());
        return subscription;
    }
    
}
