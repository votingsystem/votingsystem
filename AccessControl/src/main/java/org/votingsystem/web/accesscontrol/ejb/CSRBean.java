package org.votingsystem.web.accesscontrol.ejb;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.CertValidationDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.UserRequestCsr;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.CsrResponse;
import org.votingsystem.util.crypto.KeyStoreInfo;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class CSRBean {

    private static Logger log = Logger.getLogger(CSRBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;
    @Inject
    SubscriptionBean subscriptionBean;

    public Device signCertUser(CMSMessage cmsMessage) throws Exception {
        User user = cmsMessage.getUser();
        CertValidationDto certValidationDto = cmsMessage.getSignedContent(CertValidationDto.class);
        if(!cmsBean.isAdmin(user.getNif()) && !user.getNif().equals(certValidationDto.getNif()))
            throw new ExceptionVS("operation: signCertUser - userWithoutPrivilegesERROR - user nif: " + user.getNif());
        String validatedNif = NifUtils.validate(certValidationDto.getNif());
        Query query = dao.getEM().createQuery("select d from Device d where d.deviceId =:deviceId and d.user.nif =:nif " +
                "and d.state =:state").setParameter("state", Device.State.PENDING)
                .setParameter("deviceId", certValidationDto.getDeviceId()).setParameter("nif", validatedNif);
        Device device = dao.getSingleResult(Device.class, query);
        if(device == null) throw new ExceptionVS("deviceNotFoundErrorMsg - deviceId: " + certValidationDto.getDeviceId() +
                " - nif: " + validatedNif);
        query = dao.getEM().createQuery("select u from UserRequestCsr u where u.user =:user and u.state =:state")
                .setParameter("user", device.getUser()).setParameter("state", UserRequestCsr.State.PENDING);
        UserRequestCsr csrRequest = dao.getSingleResult(UserRequestCsr.class, query);
        if (csrRequest == null) throw new ExceptionVS("userRequestCsrMissingErrorMsg - validatedNif: " + validatedNif +
            " - deviceId: " + certValidationDto.getDeviceId());
        query = dao.getEM().createQuery("select d from Device d where d.deviceId =:deviceId and d.user.nif =:nif " +
                "and d.state =:state").setParameter("state", Device.State.OK)
                .setParameter("deviceId", certValidationDto.getDeviceId()).setParameter("nif", validatedNif);
        List<Device> oldDeviceList = dao.findAll(Device.class);
        if(!oldDeviceList.isEmpty()) {
            for(Device oldDevice :  oldDeviceList) {
                dao.merge(oldDevice.setState(Device.State.CANCELED));
            }
        }
        X509Certificate issuedCert = signCertUser(csrRequest);
        Certificate certificate = dao.persist(Certificate.USER(device.getUser(), issuedCert));
        dao.merge(device.getUser().updateCertInfo(issuedCert));
        dao.merge(device.setCertificate(certificate).setState(Device.State.OK).updateCertInfo(issuedCert));
        dao.merge(csrRequest.setCertificate(certificate).setActivationCMS(cmsMessage));
        return device;
    }

    public X509Certificate signCertUser(UserRequestCsr csrRequest) throws Exception {
        Date validFrom = new Date();
        Date validTo = DateUtils.addDays(validFrom, 365).getTime(); //one year
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrRequest.getContent());
        X509Certificate issuedCert = cmsBean.signCSR(csr, null, validFrom, validTo);
        csrRequest.setSerialNumber(issuedCert.getSerialNumber().longValue());
        dao.persist(csrRequest.setState(UserRequestCsr.State.OK));
        Certificate certificate = dao.persist(Certificate.USER(csrRequest.getUser(), issuedCert));
        log.info("signCertUser - issued new Certificate id: " + certificate.getId() + " for UserRequestCsr: " +
                csrRequest.getId());
        return issuedCert;
    }


    public CsrResponse signCertVote (byte[] csrPEMBytes, EventElection eventVS) throws Exception {
        CsrResponse csrResponse = validateCSRVote(csrPEMBytes, eventVS);
        KeyStore keyStore = eventVS.getKeyStore();
        //TODO ==== vote keystore -- this is for developement
        KeyStoreInfo keyStoreInfo = cmsBean.getKeyStoreInfo(keyStore.getBytes(), keyStore.getKeyAlias());
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, keyStoreInfo.getPrivateKeySigner(),
                keyStoreInfo.getCertSigner(), eventVS.getDateBegin(), eventVS.getDateFinish());
        dao.persist(Certificate.VOTE(csrResponse.getHashCertVSBase64(), null, issuedCert));
        csrResponse.setIssuedCert(PEMUtils.getPEMEncoded(issuedCert));
        return csrResponse;
    }

    public CsrResponse signRepresentativeCertVote (byte[] csrPEMBytes, EventElection eventVS,
                          User representative) throws Exception {
        CsrResponse csrResponse = validateCSRVote(csrPEMBytes, eventVS);
        KeyStore keyStore = eventVS.getKeyStore();
        //TODO ==== vote keystore -- this is for developement
        KeyStoreInfo keyStoreInfo = cmsBean.getKeyStoreInfo(keyStore.getBytes(), keyStore.getKeyAlias());
        String representativeURL = config.getContextURL() + "/rest/representative/id/" + representative.getId();
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        Attribute attribute = new Attribute(new ASN1ObjectIdentifier(ContextVS.REPRESENTATIVE_VOTE_OID),
                new DERSet(new DERUTF8String(representativeURL)));
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, keyStoreInfo.getPrivateKeySigner(),
                keyStoreInfo.getCertSigner(), eventVS.getDateBegin(), eventVS.getDateFinish(), attribute);
        Certificate certificate = Certificate.VOTE(csrResponse.getHashCertVSBase64(), representative, issuedCert);
        certificate.setUser(representative);
        dao.persist(certificate);
        csrResponse.setIssuedCert(PEMUtils.getPEMEncoded(issuedCert));
        return csrResponse;
    }

    private CsrResponse validateCSRVote(byte[] csrPEMBytes, EventElection eventVS) throws Exception{
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        VoteCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(VoteCertExtensionDto.class, csr,
                ContextVS.VOTE_OID);
        if (certExtensionDto.getEventId().longValue() != eventVS.getId().longValue()) {
            throw new ExceptionVS("validateCSRVote - expected eventId: " + eventVS.getId() + " - found: " +
                    certExtensionDto.getEventId());
        }
        if (!config.getContextURL().equals(certExtensionDto.getAccessControlURL())) {
            throw new ExceptionVS("accessControlURLError - expected: " + config.getContextURL() +
                    " - found: " + certExtensionDto.getAccessControlURL());
        }
        return new CsrResponse(CertUtils.getPublicKey(csr), null, certExtensionDto.getHashCertVS());
    }


    public X509Certificate signAnonymousDelegationCert (byte[] csrPEMBytes) throws Exception {
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        CertificationRequestInfo info = csr.toASN1Structure().getCertificationRequestInfo();
        AnonymousDelegationCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(AnonymousDelegationCertExtensionDto.class,
                csr, ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID );
        if(certExtensionDto == null) throw new ValidationException("missing 'AnonymousDelegationCertExtensionDto'");
        String serverURL = config.getContextURL();
        String accessControlURL = StringUtils.checkURL(certExtensionDto.getAccessControlURL());
        if (!serverURL.equals(accessControlURL)) throw new ValidationException("accessControlURLError - expected: " +
                serverURL + " - found: " + accessControlURL);
        Date certValidFrom = DateUtils.getMonday(Calendar.getInstance()).getTime();
        Calendar calendarValidTo = Calendar.getInstance();
        calendarValidTo.add(Calendar.DATE, 1);//cert valid for one day
        X509Certificate issuedCert = cmsBean.signCSR(csr, null, certValidFrom, calendarValidTo.getTime());
        Certificate certificate = Certificate.ANONYMOUS_REPRESENTATIVE_DELEGATION(
                certExtensionDto.getHashCertVS(), issuedCert);
        dao.persist(certificate);
        return issuedCert;
    }

    /*  C=ES, ST=State or Province, L=locality name, O=organization name, OU=org unit, CN=common name,
    emailAddress=user@votingsystem.org, SERIALNUMBER=1234, SN=surname, GN=given name, GN=name given */
    public UserRequestCsr saveUserCSR(byte[] csrPEMBytes) throws Exception {
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        User user = User.getUser(csr.getSubject());
        CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class,
                csr, ContextVS.DEVICE_OID);
        DeviceDto deviceDto = new DeviceDto(user, certExtensionDto);
        Device device = subscriptionBean.checkDeviceFromCSR(deviceDto);
        Query query = dao.getEM().createQuery("select r from UserRequestCsr r where r.device.deviceId =:device and " +
                "r.user.nif =:user and r.state =:state").setParameter("device", device.getDeviceId())
                .setParameter("user", user.getNif()).setParameter("state", UserRequestCsr.State.PENDING);
        List<UserRequestCsr> previousRequestList = query.getResultList();
        for(UserRequestCsr prevRequest: previousRequestList) {
            dao.merge(prevRequest.setState(UserRequestCsr.State.CANCELED));
        }
        UserRequestCsr requestCSR = dao.persist(new UserRequestCsr(UserRequestCsr.State.PENDING, csrPEMBytes, device));
        log.info("requestCSR id:" + requestCSR.getId() + " - cert subject: " + csr.getSubject());
        return requestCSR;
    }

}
