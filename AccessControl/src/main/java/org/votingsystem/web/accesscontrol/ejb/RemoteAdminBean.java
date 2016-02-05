package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.ActorVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.KeyStoreVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.model.voting.UserRequestCsrVS;
import org.votingsystem.service.EJBRemoteAdminAccessControl;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
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

    @Inject EventVSElectionBean eventVSElectionBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CSRBean csrBean;
    @Inject SignatureBean signatureBean;

    @Override
    public void generateBackup(Long eventId) throws Exception {
        log.info("generateBackup: " + eventId);
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        EventVSElection eventVSElection = dao.find(EventVSElection.class, eventId);
        if(eventVSElection == null) throw new ValidationExceptionVS("ERROR - EventVSElection not found - eventId: " + eventId);
        eventVSElectionBean.generateBackup(eventVSElection);
    }

    @Override
    public byte[] generateUserKeyStore(String givenName, String surname, String nif, char[] password) throws Exception {
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        KeyStore keyStore = signatureBean.generateKeysStore(givenName, surname, nif, password);
        return KeyStoreUtil.getBytes(keyStore, password);
    }

    @Override
    public byte[] generateServerKeyStore(ActorVS.Type type, String givenName, String keyAlias, String nif,
               char[] password,  KeyStoreVS keyStoreVS) throws Exception {
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
        if(config.getMode() != EnvironmentVS.DEVELOPMENT) {
            throw new ExceptionVS("service available only in mode DEVELOPMENT - actual mode: " + config.getMode());
        }
        Query query = dao.getEM().createQuery("select d from DeviceVS d where d.deviceId =:deviceId").setParameter("deviceId", deviceId);
        DeviceVS device = dao.getSingleResult(DeviceVS.class, query);
        if(device == null) throw new ExceptionVS("DeviceVS not found - deviceId: " + deviceId);
        String validatedNIF = NifUtils.validate(nif);
        query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", validatedNIF);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) throw new ExceptionVS("UserVS not found - nif: " + validatedNIF);
        query = dao.getEM().createQuery("select csr from UserRequestCsrVS csr where csr.deviceVS=:deviceVS and " +
                "csr.userVS =:userVS and csr.state=:state").setParameter("deviceVS", device).setParameter("userVS", userVS)
                .setParameter("state", UserRequestCsrVS.State.OK);
        UserRequestCsrVS csrRequest = dao.getSingleResult(UserRequestCsrVS.class, query);
        if(csrRequest == null) throw new ExceptionVS("UserRequestCsrVS not found for nif: " + validatedNIF +
                " - and deviceId: " + deviceId);
        X509Certificate issuedCert = csrBean.signCertUserVS(csrRequest);
        return "issued cert:" + issuedCert.getSerialNumber().longValue() + "- subjectDN: " + issuedCert.getSubjectDN();
    }

}
