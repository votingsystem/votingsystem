package org.votingsystem.currency.web.ejb;

import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.crypto.cms.CMSGenerator;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.crypto.cms.CMSUtils;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.OperationCheckerDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.Signature;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class CmsEJB {

    private static Logger log = Logger.getLogger(CmsEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private SignatureService signatureService;
    @Inject private SignerInfoService signerInfoService;

    private X509Certificate serverCert;
    private PrivateKey privateKey;


    @PostConstruct
    public void initialize() {
        try {
            Properties properties = new Properties();
            File propertiesFile = new File(config.getApplicationDirPath() + "/sec/keystore.properties");
            properties.load(new FileInputStream(propertiesFile));

            String issuerKeyStoreFileName = (String) properties.get("issuerKeyStoreFileName");
            String issuerKeyPassword = (String) properties.get("issuerKeyPassword");

            KeyStore keyStoreCertIssuer = KeyStore.getInstance("JKS");
            keyStoreCertIssuer.load(new FileInputStream(config.getApplicationDirPath() + "/sec/" + issuerKeyStoreFileName),
                    issuerKeyPassword.toCharArray());
            String keyAlias = keyStoreCertIssuer.aliases().nextElement();
            serverCert = (X509Certificate) keyStoreCertIssuer.getCertificate(keyAlias);
            privateKey = (PrivateKey) keyStoreCertIssuer.getKey(keyAlias, issuerKeyPassword.toCharArray());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public CMSDto validateCMS(CMSSignedMessage cmsSignedMessage, SignedDocumentType documentType) throws Exception {
        if (cmsSignedMessage.isValidSignature() != null) {
            List<CMSDocument> documentList = em.createNamedQuery(CMSDocument.FIND_BY_MESSAGE_DIGEST)
                    .setParameter("messageDigest", cmsSignedMessage.getContentDigestStr()).getResultList();
            if(!documentList.isEmpty())
                throw new ValidationException(Messages.currentInstance().get("cmsDigestRepeatedErrorMsg",
                        cmsSignedMessage.getContentDigestStr()));
            CMSDto cmsDto = validateSignersCerts(cmsSignedMessage);


            OperationCheckerDto operationDto = cmsSignedMessage.getSignedContent(OperationCheckerDto.class);
            log.info("operation: " + operationDto.getOperation().getType());

            CMSDocument cmsDocument = new CMSDocument(cmsSignedMessage);
            cmsDocument.setIndication(SignedDocument.Indication.TOTAL_PASSED).setSignedDocumentType(SignedDocumentType.SIGNED_DOCUMENT);
            em.persist(cmsDocument);

            for(Signature signature : cmsDto.getSignatures()) {
                em.persist(signature.setDocument(cmsDocument));
                log.info("signature: " + signature.getId());
            }
            cmsDto.setCmsDocument(cmsDocument);
            return cmsDto;
        } else throw new ValidationException("invalid CMSDocument");
    }

    public CMSDto validateSignersCerts(CMSSignedMessage signedMessage) throws Exception {
        if(signedMessage.getSignatures().isEmpty())
            throw new ValidationException("document without signatures");
        String signerNumId = signedMessage.getFirstSignature().getSigner().getNumId();
        CMSDto cmsDto = new CMSDto();
        for(Signature signature: signedMessage.getSignatures()) {
            User signer = signature.getSigner();
            if(signer.getTimeStampToken() != null)
                validateToken(signer.getTimeStampToken());
            else log.info("signature without timestamp - signer: " + signer.getX509Certificate().getSubjectDN());
            signerInfoService.verifyUserCertificate(signer);
            signature.setSigner(signerInfoService.checkSigner(signer.getX509Certificate(), User.Type.USER, null));
            if(signer.isAnonymousUser()) {
                log.log(Level.FINE, "anonymous signer: " + signer.getX509Certificate().getSubjectDN());
                cmsDto.setFirstSignature(signature).setAnonymousSignature(signature);
            } else {
                if(signer.getNumId().equals(signerNumId))
                    cmsDto.setFirstSignature(signature);
                cmsDto.addSignature(signature);
            }
        }
        return cmsDto;
    }


    public void validateToken(TimeStampToken timeStampToken) throws TSPException {
        log.info("===== TODO timeStampToken: " + timeStampToken);
        /*if(timeStampSignerInfoVerifier == null) throw new ExceptionVS("TimeStamp service not initialized");
        X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
        tsToken.validate(timeStampSignerInfoVerifier);*/
    }

    public CMSSignedMessage signDataWithTimeStamp(byte[] contentToSign) throws Exception {
        CMSGenerator cmsGenerator = new CMSGenerator(privateKey, new X509Certificate[]{serverCert}, Constants.SIGNATURE_ALGORITHM);
        TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(cmsGenerator.getSignatureMechanism(), contentToSign,
                config.getTimestampServiceURL());
        return cmsGenerator.signDataWithTimeStamp(contentToSign, timeStampToken);
    }

    public CMSSignedMessage signData(byte[] contentToSign) throws Exception {
        CMSGenerator cmsGenerator = new CMSGenerator(privateKey, new X509Certificate[]{serverCert}, Constants.SIGNATURE_ALGORITHM);
        return cmsGenerator.signData(contentToSign);
    }

    public CMSSignedMessage addSignature (final CMSSignedMessage cmsMessage) throws Exception {
        CMSGenerator cmsGenerator = new CMSGenerator(privateKey, new X509Certificate[]{serverCert}, Constants.SIGNATURE_ALGORITHM);
        return new CMSSignedMessage(cmsGenerator.addSignature(cmsMessage));
    }

    public byte[] decryptCMS (byte[] encryptedMessageBytes) throws Exception {
        Encryptor encryptor = new Encryptor(serverCert, privateKey);
        return encryptor.decryptCMS(encryptedMessageBytes);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        Encryptor encryptor = new Encryptor(serverCert, privateKey);
        return encryptor.encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, PublicKey publicKey) throws Exception {
        Encryptor encryptor = new Encryptor(serverCert, privateKey);
        return encryptor.encryptToCMS(dataToEncrypt, publicKey);
    }

}
