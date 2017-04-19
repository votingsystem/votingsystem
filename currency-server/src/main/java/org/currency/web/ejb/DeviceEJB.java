package org.currency.web.ejb;

import org.currency.web.http.HttpSessionManager;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpSession;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;


@Stateless
public class DeviceEJB {

    private static Logger log = Logger.getLogger(DeviceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private SignerInfoService signerInfoService;
    @Inject private BeanManager beanManager;
    @Inject private CmsEJB cmsBean;

    @TransactionAttribute(REQUIRES_NEW)
    public SessionCertification sessionCertification(CMSSignedMessage cmsSignedMessage) throws Exception {
        SignedDocument signedDocument = cmsBean.validateCMS(cmsSignedMessage, null).getCmsDocument();
        em.merge(signedDocument.setSignedDocumentType(SignedDocumentType.SESSION_CERTIFICATION_RECEIPT));
        if(ChronoUnit.MINUTES.between(signedDocument.getFirstSignature().getSignatureDate(), LocalDateTime.now()) > 0)
            throw new ValidationException("Request expired");

        User idProvider = signedDocument.getFirstSignature().getSigner();
        SessionCertificationDto certificationDto = cmsSignedMessage.getSignedContent(SessionCertificationDto.class);
        //TODO improve request validation
        if(CurrencyOperation.GET_SESSION_CERTIFICATION != certificationDto.getOperation().getValue()) {
            signedDocument.setIndication(SignedDocument.Indication.VALIDATION_ERROR);
            throw new ValidationException("Expected SignedDocumentType " +
                    SignedDocumentType.SESSION_CERTIFICATION_RECEIPT + " found: " + signedDocument.getSignedDocumentType());
        }

        X509Certificate signerCertificate = PEMUtils.fromPEMToX509Cert(certificationDto.getSignerCertPEM().getBytes());
        User signer = signerInfoService.checkSigner(signerCertificate, User.Type.ID_CARD_USER, null);

        X509Certificate browserCert = PEMUtils.fromPEMToX509Cert(certificationDto.getBrowserCertificate().getBytes());
        X509Certificate mobileCert = PEMUtils.fromPEMToX509Cert(certificationDto.getMobileCertificate().getBytes());

        Certificate browserCACertificate = signerInfoService.verifyCertificate(browserCert);
        Certificate mobileCACertificate = signerInfoService.verifyCertificate(mobileCert);

        //User browserUser = User.FROM_CERT(browserCert, User.Type.BROWSER);
        Certificate browserCertificate = Certificate.SIGNER(signer, browserCert).setType(Certificate.Type.BROWSER_SESSION)
                .setAuthorityCertificate(browserCACertificate).setSignedDocument(signedDocument)
                .setUUID(certificationDto.getBrowserUUID());
        Certificate mobileCertificate = Certificate.SIGNER(signer, mobileCert).setType(Certificate.Type.MOBILE_SESSION)
                .setAuthorityCertificate(mobileCACertificate).setSignedDocument(signedDocument)
                .setUUID(certificationDto.getMobileUUID());

        em.persist(mobileCertificate);
        em.persist(browserCertificate);
        SessionCertification result = new SessionCertification(signer, mobileCertificate, browserCertificate, signedDocument);
        em.persist(result);
        return result;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void initDeviceSession(CMSDocument signedDocument, HttpSession httpSession) throws Exception {
        OperationDto operation = JSON.getMapper().readValue(signedDocument.getCMS().getSignedContentStr(), OperationDto.class);
        operation.getOperation().validate(CurrencyOperation.INIT_DEVICE_SESSION, config.getEntityId());
        signedDocument.setSignedDocumentType(SignedDocumentType.DEVICE_SESSION);
        em.merge(signedDocument);
        String previousSessionUUID = (String) httpSession.getAttribute(Constants.SESSION_UUID);
        HttpSessionManager.getInstance().updateSession(previousSessionUUID, operation.getSessionUUID(),
                operation.getHttpSessionId(), signedDocument.getFirstSignature().getSigner());
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void closeDeviceSession(CMSDocument signedDocument, HttpSession httpSession) throws Exception {
        OperationDto operation = JSON.getMapper().readValue(signedDocument.getCMS().getSignedContentStr(), OperationDto.class);
        operation.getOperation().validate(CurrencyOperation.CLOSE_SESSION, config.getEntityId());
        signedDocument.setSignedDocumentType(SignedDocumentType.CLOSE_SESSION);
        em.merge(signedDocument);
        httpSession.invalidate();

        em.merge(signedDocument.getSignatures().iterator().next().getSignerCertificate().setState(
                Certificate.State.SESSION_CLOSED));
    }
}