package org.votingsystem.ejb;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.TimestampToken;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.validation.reports.SimpleReport;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.xades.validation.XAdESSignature;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.OperationCheckerDto;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.model.*;
import org.votingsystem.throwable.*;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.xades.CertificateVerifier;
import org.votingsystem.xml.XML;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * Class that manages most of the logic related to 'new signature', 'open signature' and 'cancel signature' requests
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class SignatureServiceEJB implements SignatureService {

    private static final Logger log = Logger.getLogger(SignatureServiceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private Config config;
    @Inject private SignerInfoService signerInfoService;

    public SignedDocument signXAdESAndSave(byte[] xmlToSign, SignatureParams signatureParams) throws SignatureException {
        try {
            byte[] signedDocumentBytes = org.votingsystem.xades.XAdESSignature.sign(xmlToSign, config.getSigningToken(),
                    new TSPHttpSource(config.getTimestampServiceURL()));
            DSSDocument signedDocument = new InMemoryDocument(signedDocumentBytes);
            String documentDigest = signedDocument.getDigest(DigestAlgorithm.MD5);
            XAdESDocument xadesDocument = new XAdESDocument(signedDocument, signatureParams.getSignedDocumentType(),
                    documentDigest);
            xadesDocument.setIndication(SignedDocument.Indication.LOCAL_SIGNATURE);
            em.persist(xadesDocument);
            return xadesDocument;
        } catch (Exception ex) {
            throw new SignatureException(ex.getMessage(), ex);
        }
    }

    public byte[] signXAdES(byte[] xmlToSign) throws SignatureException {
        try {
            AbstractSignatureTokenConnection signingToken = config.getSigningToken();
            return org.votingsystem.xades.XAdESSignature.sign(xmlToSign, signingToken,
                    new TSPHttpSource(config.getTimestampServiceURL()));
        } catch (Exception ex) {
            throw new SignatureException(ex.getMessage(), ex);
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public SignedDocument validateXAdESAndSave(DSSDocument signedDocument, SignatureParams signatureParams)
            throws XAdESValidationException, DuplicatedDbItemException {
        try {
            XAdESDocument xAdESDocument = validateXAdES(signedDocument, signatureParams);
            List<XAdESDocument> signedDocuments = em.createNamedQuery(SignedDocument.FIND_BY_MESSAGE_DIGEST)
                    .setParameter("messageDigest", xAdESDocument.getMessageDigest()).getResultList();
            if(!signedDocuments.isEmpty())
                throw new DuplicatedDbItemException("SignedDocument already stored in database");
            em.persist(xAdESDocument);
            return xAdESDocument;
        } catch (XAdESValidationException ex) {
            if(ex.getXAdESDocument() != null) {
                em.persist(ex.getXAdESDocument());
                log.log(Level.SEVERE, "XAdESDocument id: " + ex.getXAdESDocument().getId() +" - " + ex.getMessage(),ex);
            }
            throw ex;
        }
    }

    @Override
    public SignedDocument validateXAdESAndSave(byte[] httpRequestBytes) throws XAdESValidationException,
            DuplicatedDbItemException, IOException {
        OperationCheckerDto checkerDto = XML.getMapper().readValue(httpRequestBytes, OperationCheckerDto.class);
        if(checkerDto.getOperation() == null)
            throw new WebApplicationException("Signed document without operation info");
        SignatureParams signatureParams = null;
        if(checkerDto.getOperation().isCurrencyOperation()) {
            switch ((CurrencyOperation)checkerDto.getOperation().getType()) {
                case SESSION_CERTIFICATION:
                    signatureParams = new SignatureParams(checkerDto.getOperation().getEntityId(),
                            User.Type.IDENTITY_SERVER, SignedDocumentType.SESSION_CERTIFICATION_RECEIPT)
                            .setWithTimeStampValidation(true);
                    break;
                default:
                    signatureParams = new SignatureParams(checkerDto.getOperation().getEntityId(),
                            User.Type.ID_CARD_USER, SignedDocumentType.SIGNED_DOCUMENT).setWithTimeStampValidation(true);
                    break;
            }
        } else if(checkerDto.getOperation().isOperationType()) {
            signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                    SignedDocumentType.SIGNED_DOCUMENT).setWithTimeStampValidation(true);
        }
        return validateXAdESAndSave(new InMemoryDocument(httpRequestBytes), signatureParams);
    }

    public XAdESDocument validateXAdES(final DSSDocument signedDocument, final SignatureParams signatureParams)
            throws XAdESValidationException {
        //to avoid duplicated documents in database
        String documentDigest = signedDocument.getDigest(DigestAlgorithm.MD5);
        XAdESDocument xAdESDocument = null;
        SignedDocumentValidator validator = null;
        try {
            xAdESDocument = new XAdESDocument(signedDocument, signatureParams.getSignedDocumentType(), documentDigest);
            validator = SignedDocumentValidator.fromDocument(signedDocument);
        } catch (Exception ex) {
            if (xAdESDocument != null) {
                xAdESDocument.setIndication(XAdESDocument.Indication.ERROR).setMetaInf(new MetaInfDto(ex.getMessage(), null));
            }
            throw new XAdESValidationException(ex.getMessage(), ex, xAdESDocument);
        }
        SimpleReport simpleReport = null;
        String msg = null;
        Reports reports = null;
        try {
            validator.setCertificateVerifier(CertificateVerifier.create(config.getTrustedCertSource()));
            //if we set validationLevel to ValidationLevel.TIMESTAMPS we get exceptions (it doesn't seem that this is the
            // recommended way o check this)
            //validator.setValidationLevel(ValidationLevel.TIMESTAMPS);
            reports = validator.validateDocument();
            simpleReport = reports.getSimpleReport();
        } catch (Exception ex) {
            xAdESDocument.setIndication(XAdESDocument.Indication.ERROR).setMetaInf(new MetaInfDto(ex.getMessage(), null));
            throw new XAdESValidationException(ex.getMessage(), ex, xAdESDocument);
        }
        if (simpleReport.getSignaturesCount() == 0) {
            xAdESDocument.setMetaInf(new MetaInfDto(reports.getXmlSimpleReport(), null))
                    .setIndication(XAdESDocument.Indication.ERROR_ZERO_SIGNATURES);
            msg = Messages.currentInstance().get("zeroSignaturesErrorMsg");
            log.log(Level.SEVERE, msg);
            log.log(Level.FINEST, reports.getXmlSimpleReport());
            throw new XAdESValidationException(msg, xAdESDocument);
        }
        if (simpleReport.getSignaturesCount() > simpleReport.getValidSignaturesCount()) {
            xAdESDocument.setMetaInf(new MetaInfDto(reports.getXmlSimpleReport(), null))
                    .setIndication(XAdESDocument.Indication.ERROR_SIGNATURES_COUNT);
            msg = Messages.currentInstance().get("signatureCountMismatchErrorMsg",
                    simpleReport.getSignaturesCount(), simpleReport.getValidSignaturesCount());
            log.log(Level.SEVERE, msg);
            log.log(Level.FINEST, reports.getXmlSimpleReport());
            throw new XAdESValidationException(msg, xAdESDocument);
        }
        List<AdvancedSignature> signatureList = validator.getSignatures();
        xAdESDocument.setIndication(XAdESDocument.Indication.TOTAL_PASSED);
        try {
            Set<Signature> signatures = new HashSet<>();
            for (AdvancedSignature advancedSignature : signatureList) {
                XAdESSignature xAdESSignature = (XAdESSignature) advancedSignature;
                List<CertificateToken> certList = xAdESSignature.getCertificates();
                CertificateToken signingCertToken = xAdESSignature.getCandidatesForSigningCertificate()
                        .getTheBestCandidate().getCertificateToken();
                CertificateToken issuerToken = signingCertToken.getIssuerToken();
                if(issuerToken == null) {
                    for(CertificateToken certificateToken : certList) {
                        if(signingCertToken.isSignedBy(certificateToken))
                            issuerToken = certificateToken;
                    }
                }
                User signer = signerInfoService.checkSigner(signingCertToken.getCertificate(),
                        signatureParams.getSignerType(), signatureParams.getEntityId());
                signatureParams.setSigningCert(signingCertToken.getCertificate()).setCertificateCA(signer.getCertificateCA());
                switch (signatureParams.getSignerType()) {
                    case ANON_ELECTOR:
                        xAdESDocument.setAnonSigner(signer);
                        if(!signer.isValidElector()) {
                            xAdESDocument.setSignedDocumentType(SignedDocumentType.VOTE_REPEATED);
                            return xAdESDocument;
                        }
                        break;
                }
                TimestampToken selectedTimestampToken = null;
                //we allow only one timestamp per signature
                for (TimestampToken timestampToken : xAdESSignature.getSignatureTimestamps()) {
                    X509Certificate tokenIssuerCert = timestampToken.getIssuerToken().getCertificate();
                    if(signatureParams.isWithTimeStampValidation()) {
                        try {
                            boolean isTimeStampTokenValidAuthority = false;
                            for(X509Certificate tstAuthority:config.getTrustedTimeStampServers()) {
                                if(Arrays.equals(tstAuthority.getEncoded(), tokenIssuerCert.getEncoded()))
                                    isTimeStampTokenValidAuthority = true;
                            }
                            if(!isTimeStampTokenValidAuthority) throw new TimeStampValidationException(
                                    "Token issuer not in trusted list. Token issuer: " + tokenIssuerCert.getSubjectDN());
                        } catch (TimeStampValidationException ex) {
                            throw ex;
                        } catch (Exception ex) {
                            throw new TimeStampValidationException("Timestamp cert validation error", ex);
                        }
                    } else
                        log.severe("by-passing trusted check of TimeStamp issuer certificate");

                    if(!timestampToken.isMessageImprintDataIntact()) {
                        throw new TimeStampValidationException("Timestamp DOES NOT MATCH the signed data");
                    }
                    if(!timestampToken.isSignatureValid()) {
                        throw new TimeStampValidationException("Timestamp's signature validity: INVALID");
                    }
                    if(selectedTimestampToken == null)
                        selectedTimestampToken = timestampToken;
                    else if(selectedTimestampToken.getGenerationTime().after(timestampToken.getGenerationTime()))
                        selectedTimestampToken = timestampToken;
                }
                LocalDateTime selectedTokenDate = DateUtils.getLocalDateFromUTCDate(selectedTimestampToken.getGenerationTime());
                log.log(Level.FINEST, "timestampToken: " + selectedTimestampToken + " - EncodedSignedDataDigestValue: " +
                        selectedTimestampToken.getEncodedSignedDataDigestValue() + " - localDateTime: " + selectedTokenDate);
                X509Certificate signingCert = xAdESSignature.getCandidatesForSigningCertificate()
                        .getTheBestCandidate().getCertificateToken().getCertificate();
                switch (signatureParams.getSignerType()) {
                    case ANON_ELECTOR:
                        signatures.add(new Signature(null, signer.getCertificate(), signer.getCertificateCA(),
                                signingCert, xAdESDocument, xAdESSignature.getId(), selectedTokenDate));
                        break;
                    default:
                        signatures.add(new Signature(signer, signer.getCertificate(), signer.getCertificateCA(),
                                signingCert, xAdESDocument, xAdESSignature.getId(), selectedTokenDate));
                }
            }
            xAdESDocument.setSignatures(signatures);
        } catch (CertificateValidationException | SignerValidationException ex) {
            xAdESDocument.setMetaInf(new MetaInfDto(ex.getMessage(), null))
                    .setIndication(XAdESDocument.Indication.ERROR_SIGNER);
            throw new XAdESValidationException(ex.getMessage(), xAdESDocument);
        } catch (TimeStampValidationException ex) {
            xAdESDocument.setMetaInf(new MetaInfDto(ex.getMessage(), null))
                    .setIndication(XAdESDocument.Indication.ERROR_TIMESTAMP);
            throw new XAdESValidationException(ex.getMessage(), xAdESDocument);
        }
        return xAdESDocument;
    }

}