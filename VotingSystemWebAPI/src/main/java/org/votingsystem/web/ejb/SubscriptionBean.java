package org.votingsystem.web.ejb;

import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContextVS;
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
        X509Certificate x509Cert = user.getX509Certificate();
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
            userDB.setX509Certificate(user.getX509Certificate());
            userDB.setTimeStampToken(user.getTimeStampToken());
        }
        setUserData(userDB, deviceData);
        return userDB;
    }

    public void setUserData(User user, CertExtensionDto deviceData) throws
            CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException {
        log.log(Level.FINE, " deviceData: " + deviceData);
        X509Certificate x509Cert = user.getX509Certificate();
        Query query = dao.getEM().createQuery("SELECT c FROM Certificate c WHERE c.user =:user and c.state =:state " +
                "and c.serialNumber =:serialNumber and c.authorityCertificate =:authorityCertificate")
                .setParameter("user", user).setParameter("state", Certificate.State.OK)
                .setParameter("serialNumber", x509Cert.getSerialNumber().longValue())
                .setParameter("authorityCertificate", user.getCertificateCA());
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        Device device = null;
        if(certificate == null){
            certificate = dao.persist(Certificate.USER(user, x509Cert));
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
            query = dao.getEM().createQuery("SELECT d FROM Device d WHERE d.deviceId =:deviceId and d.certificate =:certificate")
                    .setParameter("deviceId", deviceData.getDeviceId())
                    .setParameter("certificate", certificate);
            device = dao.getSingleResult(Device.class, query);
            if(device == null) {
                device = dao.persist(new Device(user, deviceData.getDeviceId(), deviceData.getEmail(),
                        deviceData.getMobilePhone(), deviceData.getDeviceName(), certificate));
                log.log(Level.FINE, "new device with id: " + device.getId());
            }
        }
        user.setCertificate(certificate);
        user.setDevice(device);
    }

    @Transactional
    public Device checkDeviceFromCSR(DeviceDto dto) throws ExceptionVS {
        log.info(format("checkDevice - givenname: {0} - surname: {1} - nif:{2} - phone: {3}" +
                " - email: {4} - deviceId: {5} - deviceType: {6}", dto.getFirstName(), dto.getLastName(), dto.getNIF(),
                dto.getPhone(), dto.getEmail(), dto.getDeviceId(), dto.getDeviceType()));
        if(dto.getNIF() == null) throw new ValidationException("missing 'nif'");
        if(dto.getDeviceId() == null) throw new ValidationException("missing 'deviceId'");
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
    
}
