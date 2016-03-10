package org.votingsystem.web.accesscontrol.ejb;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.CertValidationDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.UserRequestCsrVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.CsrResponse;
import org.votingsystem.util.crypto.KeyStoreInfo;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
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
    @Inject SubscriptionVSBean subscriptionVSBean;

    public DeviceVS signCertUserVS(CMSMessage cmsMessage) throws Exception {
        UserVS userVS = cmsMessage.getUserVS();
        CertValidationDto certValidationDto = cmsMessage.getSignedContent(CertValidationDto.class);
        if(!cmsBean.isAdmin(userVS.getNif()) && !userVS.getNif().equals(certValidationDto.getNif()))
            throw new ExceptionVS("operation: signCertUserVS - userWithoutPrivilegesERROR - userVS nif: " + userVS.getNif());
        String validatedNif = NifUtils.validate(certValidationDto.getNif());
        Query query = dao.getEM().createQuery("select d from DeviceVS d where d.deviceId =:deviceId and d.userVS.nif =:nif " +
                "and d.state =:state").setParameter("state", DeviceVS.State.PENDING)
                .setParameter("deviceId", certValidationDto.getDeviceId()).setParameter("nif", validatedNif);
        DeviceVS deviceVS = dao.getSingleResult(DeviceVS.class, query);
        if(deviceVS == null) throw new ExceptionVS("deviceNotFoundErrorMsg - deviceId: " + certValidationDto.getDeviceId() +
                " - nif: " + validatedNif);
        query = dao.getEM().createQuery("select u from UserRequestCsrVS u where u.userVS =:userVS and u.state =:state")
                .setParameter("userVS", deviceVS.getUserVS()).setParameter("state", UserRequestCsrVS.State.PENDING);
        UserRequestCsrVS csrRequest = dao.getSingleResult(UserRequestCsrVS.class, query);
        if (csrRequest == null) throw new ExceptionVS("userRequestCsrMissingErrorMsg - validatedNif: " + validatedNif +
            " - deviceId: " + certValidationDto.getDeviceId());
        query = dao.getEM().createQuery("select d from DeviceVS d where d.deviceId =:deviceId and d.userVS.nif =:nif " +
                "and d.state =:state").setParameter("state", DeviceVS.State.OK)
                .setParameter("deviceId", certValidationDto.getDeviceId()).setParameter("nif", validatedNif);
        List<DeviceVS> oldDeviceList = dao.findAll(DeviceVS.class);
        if(!oldDeviceList.isEmpty()) {
            for(DeviceVS device :  oldDeviceList) {
                dao.merge(device.setState(DeviceVS.State.CANCELED));
            }
        }
        X509Certificate issuedCert = signCertUserVS(csrRequest);
        CertificateVS certificate = dao.persist(CertificateVS.USER(deviceVS.getUserVS(), issuedCert));
        dao.merge(deviceVS.getUserVS().updateCertInfo(issuedCert));
        dao.merge(deviceVS.setCertificateVS(certificate).setState(DeviceVS.State.OK).updateCertInfo(issuedCert));
        dao.merge(csrRequest.setCertificateVS(certificate).setActivationCMS(cmsMessage));
        return deviceVS;
    }

    public X509Certificate signCertUserVS(UserRequestCsrVS csrRequest) throws Exception {
        Date validFrom = new Date();
        Date validTo = DateUtils.addDays(validFrom, 365).getTime(); //one year
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrRequest.getContent());
        X509Certificate issuedCert = cmsBean.signCSR(csr, null, validFrom, validTo);
        csrRequest.setSerialNumber(issuedCert.getSerialNumber().longValue());
        dao.persist(csrRequest.setState(UserRequestCsrVS.State.OK));
        CertificateVS certificate = dao.persist(CertificateVS.USER(csrRequest.getUserVS(), issuedCert));
        log.info("signCertUserVS - issued new CertificateVS id: " + certificate.getId() + " for UserRequestCsrVS: " +
                csrRequest.getId());
        return issuedCert;
    }


    public CsrResponse signCertVote (byte[] csrPEMBytes, EventElection eventVS) throws Exception {
        CsrResponse csrResponse = validateCSRVote(csrPEMBytes, eventVS);
        KeyStoreVS keyStoreVS = eventVS.getKeyStoreVS();
        //TODO ==== vote keystore -- this is for developement
        KeyStoreInfo keyStoreInfo = cmsBean.getKeyStoreInfo(keyStoreVS.getBytes(), keyStoreVS.getKeyAlias());
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, keyStoreInfo.getPrivateKeySigner(),
                keyStoreInfo.getCertSigner(), eventVS.getDateBegin(), eventVS.getDateFinish());
        dao.persist(CertificateVS.VOTE(csrResponse.getHashCertVSBase64(), null, issuedCert));
        csrResponse.setIssuedCert(PEMUtils.getPEMEncoded(issuedCert));
        return csrResponse;
    }

    public CsrResponse signRepresentativeCertVote (byte[] csrPEMBytes, EventElection eventVS,
                          UserVS representative) throws Exception {
        CsrResponse csrResponse = validateCSRVote(csrPEMBytes, eventVS);
        KeyStoreVS keyStoreVS = eventVS.getKeyStoreVS();
        //TODO ==== vote keystore -- this is for developement
        KeyStoreInfo keyStoreInfo = cmsBean.getKeyStoreInfo(keyStoreVS.getBytes(), keyStoreVS.getKeyAlias());
        String representativeURL = config.getContextURL() + "/rest/representative/id/" + representative.getId();
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        Attribute attribute = new Attribute(new ASN1ObjectIdentifier(ContextVS.REPRESENTATIVE_VOTE_OID),
                new DERSet(new DERUTF8String(representativeURL)));
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, keyStoreInfo.getPrivateKeySigner(),
                keyStoreInfo.getCertSigner(), eventVS.getDateBegin(), eventVS.getDateFinish(), attribute);
        CertificateVS certificate = CertificateVS.VOTE(csrResponse.getHashCertVSBase64(), representative, issuedCert);
        certificate.setUserVS(representative);
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
        if(certExtensionDto == null) throw new ValidationExceptionVS("missing 'AnonymousDelegationCertExtensionDto'");
        String serverURL = config.getContextURL();
        String accessControlURL = StringUtils.checkURL(certExtensionDto.getAccessControlURL());
        if (!serverURL.equals(accessControlURL)) throw new ValidationExceptionVS("accessControlURLError - expected: " +
                serverURL + " - found: " + accessControlURL);
        Date certValidFrom = DateUtils.getMonday(Calendar.getInstance()).getTime();
        Calendar calendarValidTo = Calendar.getInstance();
        calendarValidTo.add(Calendar.DATE, 1);//cert valid for one day
        X509Certificate issuedCert = cmsBean.signCSR(csr, null, certValidFrom, calendarValidTo.getTime());
        CertificateVS certificate = CertificateVS.ANONYMOUS_REPRESENTATIVE_DELEGATION(
                certExtensionDto.getHashCertVS(), issuedCert);
        dao.persist(certificate);
        return issuedCert;
    }

    /*  C=ES, ST=State or Province, L=locality name, O=organization name, OU=org unit, CN=common name,
    emailAddress=user@votingsystem.org, SERIALNUMBER=1234, SN=surname, GN=given name, GN=name given */
    public UserRequestCsrVS saveUserCSR(byte[] csrPEMBytes) throws Exception {
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        CertificationRequestInfo info = csr.toASN1Structure().getCertificationRequestInfo();
        String subjectDN = info.getSubject().toString();
        UserVS userVS = UserVS.getUserVS(subjectDN);
        CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class,
                csr, ContextVS.DEVICEVS_OID);
        DeviceVSDto deviceVSDto = new DeviceVSDto(userVS, certExtensionDto);
        DeviceVS deviceVS = subscriptionVSBean.checkDeviceFromCSR(deviceVSDto);
        Query query = dao.getEM().createQuery("select r from UserRequestCsrVS r where r.deviceVS.deviceId =:device and " +
                "r.userVS.nif =:user and r.state =:state").setParameter("device", deviceVS.getDeviceId())
                .setParameter("user", userVS.getNif()).setParameter("state", UserRequestCsrVS.State.PENDING);
        List<UserRequestCsrVS> previousRequestList = query.getResultList();
        for(UserRequestCsrVS prevRequest: previousRequestList) {
            dao.merge(prevRequest.setState(UserRequestCsrVS.State.CANCELED));
        }
        UserRequestCsrVS requestCSR = dao.persist(new UserRequestCsrVS(UserRequestCsrVS.State.PENDING, csrPEMBytes,
                deviceVS));
        log.info("requestCSR id:" + requestCSR.getId() + " - cert subject: " + subjectDN);
        return requestCSR;
    }

}
