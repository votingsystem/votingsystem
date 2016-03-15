package org.votingsystem.web.ejb;

import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.currency.SubscriptionDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Group;
import org.votingsystem.model.currency.Subscription;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class SubscriptionBean {

    private static final Logger log = Logger.getLogger(SubscriptionBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;

    public User checkUser(User user) throws Exception {
        log.log(Level.FINE, "nif: " + user.getNif());
        if(user.getNif() == null) throw new ExceptionVS("ERROR - missing Nif");
        X509Certificate x509Cert = user.getCertificate();
        if (x509Cert == null) throw new ExceptionVS("Missing certificate!!!");
        user.setNif(org.votingsystem.util.NifUtils.validate(user.getNif()));
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", user.getNif());
        User userDB = dao.getSingleResult(User.class, query);
        CertExtensionDto deviceData = CertUtils.getCertExtensionData(CertExtensionDto.class, x509Cert,
                ContextVS.DEVICE_OID);
        if (userDB == null) {
            userDB = dao.persist(user);
            config.createIBAN(user);
            log.log(Level.INFO, "checkUser ### NEW User: " + userDB.getNif());
        } else {
            userDB.setCertificateCA(user.getCertificateCA());
            userDB.setCertificate(user.getCertificate());
            userDB.setTimeStampToken(user.getTimeStampToken());
        }
        setUserData(userDB, deviceData);
        return userDB;
    }

    public void setUserData(User user, CertExtensionDto deviceData) throws
            CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException {
        log.log(Level.FINE, " deviceData: " + deviceData);
        X509Certificate x509Cert = user.getCertificate();
        Query query = dao.getEM().createQuery("SELECT c FROM CertificateVS c WHERE c.user =:user and c.state =:state " +
                "and c.serialNumber =:serialNumber and c.authorityCertificateVS =:authorityCertificateVS")
                .setParameter("user", user).setParameter("state",CertificateVS.State.OK)
                .setParameter("serialNumber", x509Cert.getSerialNumber().longValue())
                .setParameter("authorityCertificateVS", user.getCertificateCA());
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        Device device = null;
        if(certificate == null){
            certificate = dao.persist(CertificateVS.USER(user, x509Cert));
            if(deviceData != null) {
                query = dao.getEM().createNamedQuery("findDeviceByUserAndDeviceId").setParameter("user", user)
                        .setParameter("deviceId", deviceData.getDeviceId());
                device = dao.getSingleResult(Device.class, query);
                if(device == null) {
                    device = dao.persist(new Device(user, deviceData.getDeviceId(), deviceData.getEmail(),
                            deviceData.getMobilePhone(), deviceData.getDeviceName(), certificate));
                    log.log(Level.FINE, "new device with id: " + device.getId());
                } else dao.getEM().merge(device.updateCertInfo(deviceData));
            }
            log.log(Level.FINE, "new certificate with id:" + certificate.getId());
        } else if(deviceData != null && deviceData.getDeviceId() != null) {
            query = dao.getEM().createQuery("SELECT d FROM Device d WHERE d.deviceId =:deviceId and d.certificateVS =:certificate")
                    .setParameter("deviceId", deviceData.getDeviceId())
                    .setParameter("certificate", certificate);
            device = dao.getSingleResult(Device.class, query);
            if(device == null) {
                device = dao.persist(new Device(user, deviceData.getDeviceId(), deviceData.getEmail(),
                        deviceData.getMobilePhone(), deviceData.getDeviceName(), certificate));
                log.log(Level.FINE, "new device with id: " + device.getId());
            }
        }
        user.setCertificateVS(certificate);
        user.setDevice(device);
    }

    @Transactional
    public Device checkDeviceFromCSR(DeviceDto dto) throws ExceptionVS {
        log.info(format("checkDevice - givenname: {0} - surname: {1} - nif:{2} - phone: {3}" +
                " - email: {4} - deviceId: {5} - deviceType: {6}", dto.getFirstName(), dto.getLastName(), dto.getNIF(),
                dto.getPhone(), dto.getEmail(), dto.getDeviceId(), dto.getDeviceType()));
        if(dto.getNIF() == null) throw new ValidationExceptionVS("missing 'nif'");
        if(dto.getDeviceId() == null) throw new ValidationExceptionVS("missing 'deviceId'");
        String validatedNIF = org.votingsystem.util.NifUtils.validate(dto.getNIF());
        Query query = dao.getEM().createQuery("select u from User u where u.nif =:nif").setParameter("nif", validatedNIF);
        User user = dao.getSingleResult(User.class, query);
        if (user == null) user = dao.persist(new User(validatedNIF, User.Type.USER, null,
                dto.getFirstName(), dto.getLastName(), dto.getEmail(), dto.getPhone()));
        Device device = new Device(user, dto.getDeviceId(), dto.getEmail(), dto.getPhone(),
                dto.getDeviceType());
        device.setState(Device.State.PENDING);
        return dao.persist(device);
    }

    public Subscription deActivateUser(CMSMessage cmsMessage) throws Exception {
        User signer = cmsMessage.getUser();
        log.log(Level.FINE, "signer: " + signer.getNif());
        SubscriptionDto request = cmsMessage.getSignedContent(SubscriptionDto.class);
        Group group = dao.find(Group.class, request.getGroupId());
        if(group == null || !request.getGroupName().equals(group.getName())) {
            throw new ExceptionVS("group with name: " + request.getGroupName() + " and id: " + request.getId() + " not found");
        }
        if(!group.getRepresentative().getNif().equals(request.getUserNIF()) && !cmsBean.isAdmin(signer.getNif())) {
            throw new ExceptionVS("'userWithoutGroupPrivilegesErrorMsg - group:" + request.getGroupName() + " - nif:" +
                    signer.getNif());
        }
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getUserNIF());
        User groupUser = dao.getSingleResult(User.class, query);
        if(groupUser == null) throw new ValidationExceptionVS("user unknown - nif:" + request.getUserNIF());
        query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("group", group)
                .setParameter("user", groupUser);
        Subscription subscription = dao.getSingleResult(Subscription.class, query);
        if(subscription == null || Subscription.State.CANCELED == subscription.getState()) {
            throw new ExceptionVS("groupUserAlreadyCencelledErrorMsg - user nif: " + request.getUserNIF() +
                    " - group: " + request.getGroupName());
        }
        subscription.setReason(request.getReason());
        subscription.setState(Subscription.State.CANCELED);
        subscription.setDateCancelled(new Date());
        subscription.setCancellationCMS(cmsMessage);
        log.info("deActivateUser OK - user nif: " + request.getUserNIF() + " - group: " + request.getGroupName());
        return subscription;
    }

    public Subscription activateUser(CMSMessage cmsMessage) throws Exception {
        User signer = cmsMessage.getUser();
        log.info("signer: " + signer.getNif());
        SubscriptionDto request = cmsMessage.getSignedContent(SubscriptionDto.class);
        request.validateActivationRequest();
        Group group = dao.find(Group.class, request.getGroupId());
        if(group == null || !request.getGroupName().equals(group.getName())) {
            throw new ValidationExceptionVS("Group with id: " + request.getId() + " and name: " + request.getGroupName() + " not found");
        }
        if(!group.getRepresentative().getNif().equals(signer.getNif()) && !cmsBean.isAdmin(signer.getNif())) {
            throw new ValidationExceptionVS("userWithoutGroupPrivilegesErrorMsg - operation: " +
                    TypeVS.CURRENCY_GROUP_USER_ACTIVATE.toString() + " - nif: " + signer.getNif() + " - group: " +
                    request.getGroupName());
        }
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getUserNIF());
        User groupUser = dao.getSingleResult(User.class, query);
        if(groupUser == null) throw new ValidationExceptionVS("user unknown - nif:" + request.getUserNIF());
        query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("group", group)
                .setParameter("user", groupUser);
        Subscription subscription = dao.getSingleResult(Subscription.class, query);
        if(subscription == null) throw new ValidationExceptionVS("user:" + request.getUserNIF() +
                " has not pending subscription request");
        subscription.setState(Subscription.State.ACTIVE);
        subscription.setDateActivated(new Date());
        subscription.setActivationCMS(cmsMessage);
        log.info("activateUser OK - user nif: " + request.getUserNIF() + " - group: " + request.getGroupName());
        return subscription;
    }
    
}
