package org.votingsystem.currency.web.ejb;

import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.currency.web.http.HttpSessionManager;
import org.votingsystem.currency.web.managed.SocketPushEvent;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
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

    @TransactionAttribute(REQUIRES_NEW)
    public void initBrowserSession(SignedDocument signedDocument) throws Exception {
        //TODO improve request validation
        if(SignedDocumentType.BROWSER_CERTIFICATION_REQUEST_RECEIPT != signedDocument.getSignedDocumentType())
            throw new ValidationException("Expected SignedDocumentType " +
                    SignedDocumentType.BROWSER_CERTIFICATION_REQUEST_RECEIPT + " found: " + signedDocument.getSignedDocumentType());
        if(ChronoUnit.MINUTES.between(signedDocument.getFirstSignature().getSignatureDate(), LocalDateTime.now()) > 0)
            throw new ValidationException("Request expired");

        User idProvider = signedDocument.getFirstSignature().getSigner();

        SessionCertificationDto certificationDto = signedDocument.getSignedContent(SessionCertificationDto.class);

        X509Certificate signerCertificate = PEMUtils.fromPEMToX509Cert(certificationDto.getSignerCertPEM().getBytes());
        User signer = signerInfoService.checkSigner(signerCertificate, User.Type.ID_CARD_USER, null);

        X509Certificate browserCert = PEMUtils.fromPEMToX509Cert(certificationDto.getBrowserCsrSigned().getBytes());
        X509Certificate mobileCert = PEMUtils.fromPEMToX509Cert(certificationDto.getMobileCsrSigned().getBytes());

        Certificate browserCACertificate = signerInfoService.verifyCertificate(browserCert);
        Certificate mobileCACertificate = signerInfoService.verifyCertificate(mobileCert);

        CertExtensionDto certExtension = CertUtils.getCertExtensionData(CertExtensionDto.class, mobileCert, Constants.DEVICE_OID);
        User browserUser = User.FROM_CERT(browserCert, User.Type.BROWSER);

        Certificate browserCertificate = Certificate.SIGNER(signer, browserCert).setType(Certificate.Type.BROWSER_SESSION)
                .setAuthorityCertificate(browserCACertificate).setUUID(browserUser.getNumId());
        Certificate mobileCertificate = Certificate.SIGNER(signer, mobileCert).setType(Certificate.Type.MOBILE_SESSION)
                .setAuthorityCertificate(mobileCACertificate).setUUID(certExtension.getUUID());

        em.persist(mobileCertificate);
        em.persist(browserCertificate);

        HttpSessionManager.getInstance().setUserInSession(browserUser.getNumId(), signer);
        HttpSessionManager.getInstance().setUserInSession(certExtension.getUUID(), signer);

        certificationDto.setMobileUUID(certExtension.getUUID()).setBrowserUUID(browserUser.getNumId())
                .setStatusCode(ResponseDto.SC_OK)
                .setOperation(new OperationTypeDto(CurrencyOperation.BROWSER_CERTIFICATION, config.getEntityId()));
        SocketPushEvent pushEvent = new SocketPushEvent(JSON.getMapper().writeValueAsString(certificationDto),
                SocketPushEvent.Type.TO_USER).setUserUUID(browserUser.getNumId());
        beanManager.fireEvent(pushEvent);
    }
    
}