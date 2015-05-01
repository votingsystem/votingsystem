package org.votingsystem.web.accesscontrol.ejb;

import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.CertValidationDto;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.model.voting.UserRequestCsrVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CsrResponse;
import org.votingsystem.signature.util.KeyStoreInfo;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class CSRBean {

    private static Logger log = Logger.getLogger(CSRBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionVSBean;

    public DeviceVS signCertUserVS(MessageSMIME messageSMIME) throws Exception {
        UserVS userVS = messageSMIME.getUserVS();
        CertValidationDto certValidationDto = messageSMIME.getSignedContent(CertValidationDto.class);
        if(!signatureBean.isAdmin(userVS.getNif()) && !userVS.getNif().equals(certValidationDto.getNif()))
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
        DeviceVS oldDevice = dao.getSingleResult(DeviceVS.class, query);
        if(oldDevice != null) dao.merge(oldDevice.setState(DeviceVS.State.CANCELED));
        X509Certificate issuedCert = signCertUserVS(csrRequest);
        CertificateVS certificate = dao.persist(CertificateVS.USER(deviceVS.getUserVS(), issuedCert));
        dao.merge(deviceVS.getUserVS().updateCertInfo(issuedCert));
        dao.merge(deviceVS.setCertificateVS(certificate).setState(DeviceVS.State.OK).updateCertInfo(issuedCert));
        dao.merge(csrRequest.setCertificateVS(certificate).setActivationSMIME(messageSMIME));
        return deviceVS;
    }

    public X509Certificate signCertUserVS(UserRequestCsrVS csrRequest) throws Exception {
        Date validFrom = new Date();
        Date validTo = DateUtils.addDays(validFrom, 365).getTime(); //one year
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrRequest.getContent());
        X509Certificate issuedCert = signatureBean.signCSR(csr, null, validFrom, validTo, null);
        csrRequest.setSerialNumber(issuedCert.getSerialNumber().longValue());
        dao.persist(csrRequest.setState(UserRequestCsrVS.State.OK));
        CertificateVS certificate = dao.persist(CertificateVS.USER(csrRequest.getUserVS(), issuedCert));
        log.info("signCertUserVS - issued new CertificateVS id: " + certificate.getId() + " for UserRequestCsrVS: " +
                csrRequest.getId());
        return issuedCert;
    }


    public CsrResponse signCertVoteVS (byte[] csrPEMBytes, EventVSElection eventVS) throws Exception {
        CsrResponse csrResponse = validateCSRVote(csrPEMBytes, eventVS);
        KeyStoreVS keyStoreVS = eventVS.getKeyStoreVS();
        //TODO ==== vote keystore -- this is for developement
        KeyStoreInfo keyStoreInfo = signatureBean.getKeyStoreInfo(keyStoreVS.getBytes(), keyStoreVS.getKeyAlias());
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, keyStoreInfo.getPrivateKeySigner(),
                keyStoreInfo.getCertSigner(), eventVS.getDateBegin(), eventVS.getDateFinish());
        CertificateVS certificate = dao.persist(CertificateVS.VOTE(csrResponse.getHashCertVSBase64(), null, issuedCert));
        csrResponse.setIssuedCert(CertUtils.getPEMEncoded(issuedCert));
        return csrResponse;
    }

    public CsrResponse signRepresentativeCertVoteVS (byte[] csrPEMBytes, EventVSElection eventVS,
                          UserVS representative) throws Exception {
        CsrResponse csrResponse = validateCSRVote(csrPEMBytes, eventVS);
        KeyStoreVS keyStoreVS = eventVS.getKeyStoreVS();
        //TODO ==== vote keystore -- this is for developement
        KeyStoreInfo keyStoreInfo = signatureBean.getKeyStoreInfo(keyStoreVS.getBytes(), keyStoreVS.getKeyAlias());
        String representativeURL = config.getRestURL() + "/representative/id/" + representative.getId();
        DERTaggedObject representativeExtension = new DERTaggedObject(
                ContextVS.REPRESENTATIVE_VOTE_TAG, new DERUTF8String(representativeURL));
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, keyStoreInfo.getPrivateKeySigner(),
                keyStoreInfo.getCertSigner(), eventVS.getDateBegin(), eventVS.getDateFinish(), representativeExtension);
        CertificateVS certificate = CertificateVS.VOTE(csrResponse.getHashCertVSBase64(), representative, issuedCert);
        certificate.setUserVS(representative);
        dao.persist(certificate);
        csrResponse.setIssuedCert(CertUtils.getPEMEncoded(issuedCert));
        return csrResponse;
    }


    private CsrResponse validateCSRVote(byte[] csrPEMBytes, EventVSElection eventVS) throws Exception{
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects();
        VoteCertExtensionDto certExtensionDto = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.VOTE_TAG:
                    certExtensionDto = JSON.getMapper().readValue(
                            ((DERUTF8String)attribute.getObject()).getString(), VoteCertExtensionDto.class);
                    break;
            }
        }
        if (certExtensionDto.getEventId().longValue() != eventVS.getId().longValue()) {
            throw new ExceptionVS("validateCSRVote - expected eventId: " + eventVS.getId() + " - found: " +
                    certExtensionDto.getEventId());
        }
        if (!config.getContextURL().equals(certExtensionDto.getAccessControlURL())) {
            throw new ExceptionVS("accessControlURLError - expected: " + config.getContextURL() +
                    " - found: " + certExtensionDto.getAccessControlURL());
        }
        return new CsrResponse(csr.getPublicKey(), null, certExtensionDto.getHashCertVS());
    }


    public X509Certificate signAnonymousDelegationCert (byte[] csrPEMBytes) throws Exception {
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects();
        AnonymousDelegationCertExtensionDto certExtensionDto = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG:
                    String certAttributeMapStr = ((DERUTF8String)attribute.getObject()).getString();
                    certExtensionDto = JSON.getMapper().readValue(certAttributeMapStr,
                            AnonymousDelegationCertExtensionDto.class);
                    break;
            }
        }
        if(certExtensionDto == null) throw new ValidationExceptionVS("missing 'AnonymousDelegationCertExtensionDto'");
        String serverURL = config.getContextURL();
        String accessControlURL = StringUtils.checkURL(certExtensionDto.getAccessControlURL());
        if (!serverURL.equals(accessControlURL)) throw new ValidationExceptionVS("accessControlURLError - expected: " +
                serverURL + " - found: " + accessControlURL);
        Date certValidFrom = DateUtils.getMonday(Calendar.getInstance()).getTime();
        Calendar calendarValidTo = Calendar.getInstance();
        calendarValidTo.add(Calendar.DATE, 1);//cert valid for one day
        X509Certificate issuedCert = signatureBean.signCSR(csr, null, certValidFrom, calendarValidTo.getTime());
        CertificateVS certificate = CertificateVS.ANONYMOUS_REPRESENTATIVE_DELEGATION(
                certExtensionDto.getHashCertVS(), issuedCert);
        dao.persist(certificate);
        return issuedCert;
        /*byte[] issuedCertPEMBytes = CertUtils.getPEMEncoded(issuedCert);
        Map data = [requestPublicKey:csr.getPublicKey()]
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ANONYMOUS_SELECTION_CERT_REQUEST,
                contentType: ContentTypeVS.TEXT_STREAM, data:data, message:"certificateVS_${certificate.id}",
                messageBytes:issuedCertPEMBytes)*/
    }


    /*  C=ES, ST=State or Province, L=locality name, O=organization name, OU=org unit, CN=common name,
    emailAddress=user@votingsystem.org, SERIALNUMBER=1234, SN=surname, GN=given name, GN=name given */
    public UserRequestCsrVS saveUserCSR(byte[] csrPEMBytes) throws Exception {
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        String subjectDN = info.getSubject().toString();
        UserVS userVS = UserVS.getUserVS(subjectDN);
        Enumeration csrAttributes = info.getAttributes().getObjects();
        CertExtensionDto certExtensionDto = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.DEVICEVS_TAG:
                    String deviceData = ((DERUTF8String)attribute.getObject()).getString();
                    certExtensionDto = JSON.getMapper().readValue(deviceData, CertExtensionDto.class);
                    break;
            }
        }
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
        //return new ResponseVS(statusCode:ResponseVS.SC_OK, message:requestCSR.id)
    }

}
