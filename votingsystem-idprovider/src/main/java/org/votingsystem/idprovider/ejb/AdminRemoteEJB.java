package org.votingsystem.idprovider.ejb;


import org.votingsystem.crypto.KeyStoreUtils;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.UserCSRRequest;
import org.votingsystem.service.EJBAdminRemoteIdProvider;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.NifUtils;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@Remote(EJBAdminRemoteIdProvider.class)
public class AdminRemoteEJB implements EJBAdminRemoteIdProvider {

    private static final Logger log = Logger.getLogger(AdminRemoteEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @EJB
    private CertIssuerEJB certIssuer;

    @Override
    public void generateBackup(String electionUUID) throws Exception {
        log.info("generateBackup: " + electionUUID);
    }

    @Override
    public byte[] generateUserKeyStore(String givenname, String surname, String nif, char[] password) throws Exception {
        java.security.KeyStore keyStore = certIssuer.generateUserKeyStore(givenname, surname, nif, password);
        return KeyStoreUtils.toByteArray(keyStore, password);
    }

    @Override
    public byte[] generateSystemEntityKeyStore(User.Type type, String givenName, String keyAlias, char[] password) throws Exception {
        log.info("type: " + type + " - givenName: " + givenName);
        java.security.KeyStore keyStore = null;
        switch(type) {
            case ENTITY:
                keyStore = certIssuer.generateSystemEntityKeyStore(givenName, keyAlias, password);;
                break;
            case TIMESTAMP_SERVER:
                keyStore = certIssuer.generateTimeStampServerKeyStore(givenName, keyAlias, password);
                break;
        }
        return KeyStoreUtils.toByteArray(keyStore, password);
    }

    @Override
    public String validateCSR(String nif, String deviceId) throws Exception {
        log.info("nif: " + nif + " - deviceId: " + deviceId);
        List<Device> deviceList = em.createQuery("select d from Device d where d.deviceId =:deviceId")
                .setParameter("deviceId", deviceId).getResultList();
        if(deviceList.isEmpty())
            throw new ValidationException("Device not found - deviceId: " + deviceId);
        String validatedNIF = NifUtils.validate(nif);
        List<User> userList = em.createQuery("select u from User u where u.numId =:numId")
                .setParameter("numId", validatedNIF).getResultList();
        if(userList.isEmpty())
            throw new ValidationException("User not found - nif: " + validatedNIF);
        List<UserCSRRequest> userCSRList = em.createQuery("select csr from UserCSRRequest csr where csr.device=:device and " +
                "csr.user =:user and csr.state=:state").setParameter("device", deviceList.iterator().next())
                .setParameter("user", userList.iterator().next())
                .setParameter("state", UserCSRRequest.State.OK).getResultList();
        if(userCSRList.isEmpty()) throw new ValidationException("UserCSRRequest not found for nif: " + validatedNIF +
                " - and deviceId: " + deviceId);
        X509Certificate issuedCert = certIssuer.signUserCert(userCSRList.iterator().next());
        return "issued cert:" + issuedCert.getSerialNumber().longValue() + "- subjectDN: " + issuedCert.getSubjectDN();
    }

}