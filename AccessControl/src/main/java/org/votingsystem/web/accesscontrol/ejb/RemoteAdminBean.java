package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.Actor;
import org.votingsystem.model.Device;
import org.votingsystem.model.KeyStoreVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.UserRequestCsr;
import org.votingsystem.service.EJBRemoteAdminAccessControl;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.crypto.KeyStoreUtil;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

@Stateless
@Remote(EJBRemoteAdminAccessControl.class)
public class RemoteAdminBean implements EJBRemoteAdminAccessControl {

    private static final Logger log = Logger.getLogger(RemoteAdminBean.class.getName());

    @Inject EventElectionBean eventElectionBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CSRBean csrBean;
    @Inject CMSBean cmsBean;

    @Override
    public void generateBackup(Long eventId) throws Exception {
        log.info("generateBackup: " + eventId);
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        EventElection eventElection = dao.find(EventElection.class, eventId);
        if(eventElection == null) throw new ValidationExceptionVS("ERROR - EventElection not found - eventId: " + eventId);
        eventElectionBean.generateBackup(eventElection);
    }

    @Override
    public byte[] generateUserKeyStore(String givenName, String surname, String nif, char[] password) throws Exception {
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        KeyStore keyStore = cmsBean.generateKeysStore(givenName, surname, nif, password);
        return KeyStoreUtil.getBytes(keyStore, password);
    }

    @Override
    public byte[] generateServerKeyStore(Actor.Type type, String givenName, String keyAlias, String nif,
                                         char[] password, KeyStoreVS keyStoreVS) throws Exception {
        log.info("generateKeyStore - type: " + type + " - nif: " + nif);
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        KeyStore keyStore = null;
        switch(type) {
            case SERVER:
                keyStore = generateServerKeyStore(givenName, keyAlias, nif, password, keyStoreVS);
            case TIMESTAMP_SERVER:
                keyStore = generateTimeStampKeyStore(givenName, keyAlias, nif, password, keyStoreVS);
                break;
        }
        return KeyStoreUtil.getBytes(keyStore, password);
    }

    public KeyStore generateServerKeyStore(String givenName, String keyAlias, String nif, char[] password,
                  KeyStoreVS rootKeyStoreVS) throws Exception {
        log.info("generateServerKeyStore - nif: " + nif);
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(rootKeyStoreVS.getBytes()), rootKeyStoreVS.getPassword().toCharArray());
        X509Certificate rootCertSigner = (X509Certificate) keyStore.getCertificate(rootKeyStoreVS.getKeyAlias());
        PrivateKey rootPrivateKey = (PrivateKey)keyStore.getKey(rootKeyStoreVS.getKeyAlias(),
                rootKeyStoreVS.getPassword().toCharArray());
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(rootCertSigner,
                rootPrivateKey, rootKeyStoreVS.getKeyAlias());
        String testUserDN = format("GIVENNAME={0}, SERIALNUMBER={1}", givenName, nif);
        return KeyStoreUtil.createUserKeyStore(validFrom.getTime(),
                (validTo.getTime() - validFrom.getTime()), password, keyAlias, rootCAPrivateCredential, testUserDN);
    }

    public KeyStore generateTimeStampKeyStore(String givenName, String keyAlias, String nif, char[] password,
                          KeyStoreVS rootKeyStoreVS) throws Exception {
        log.info("generateTimeStampKeyStore - nif: " + nif);
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(rootKeyStoreVS.getBytes()), rootKeyStoreVS.getPassword().toCharArray());
        X509Certificate rootCertSigner = (X509Certificate) keyStore.getCertificate(rootKeyStoreVS.getKeyAlias());
        PrivateKey rootPrivateKey = (PrivateKey)keyStore.getKey(rootKeyStoreVS.getKeyAlias(),
                rootKeyStoreVS.getPassword().toCharArray());
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(rootCertSigner,
                rootPrivateKey, rootKeyStoreVS.getKeyAlias());
        String testUserDN = format("GIVENNAME={0}, SERIALNUMBER={1}", givenName, nif);
        return KeyStoreUtil.createTimeStampingKeyStore(validFrom.getTime(),
                (validTo.getTime() - validFrom.getTime()), password, keyAlias, rootCAPrivateCredential, testUserDN);
    }

    @Override
    public String validateCSR(String nif, String deviceId) throws Exception {
        log.info("validateCSR - nif: " + nif + " - deviceId: " + deviceId);
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        Query query = dao.getEM().createQuery("select d from Device d where d.deviceId =:deviceId").setParameter("deviceId", deviceId);
        Device device = dao.getSingleResult(Device.class, query);
        if(device == null) throw new ExceptionVS("Device not found - deviceId: " + deviceId);
        String validatedNIF = NifUtils.validate(nif);
        query = dao.getEM().createQuery("select u from User u where u.nif =:nif").setParameter("nif", validatedNIF);
        User user = dao.getSingleResult(User.class, query);
        if(user == null) throw new ExceptionVS("User not found - nif: " + validatedNIF);
        query = dao.getEM().createQuery("select csr from UserRequestCsr csr where csr.device=:device and " +
                "csr.user =:user and csr.state=:state").setParameter("device", device).setParameter("user", user)
                .setParameter("state", UserRequestCsr.State.OK);
        UserRequestCsr csrRequest = dao.getSingleResult(UserRequestCsr.class, query);
        if(csrRequest == null) throw new ExceptionVS("UserRequestCsr not found for nif: " + validatedNIF +
                " - and deviceId: " + deviceId);
        X509Certificate issuedCert = csrBean.signCertUser(csrRequest);
        return "issued cert:" + issuedCert.getSerialNumber().longValue() + "- subjectDN: " + issuedCert.getSubjectDN();
    }

}
