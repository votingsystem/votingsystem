package org.votingsystem.ejb;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DigestAlgorithm;
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
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.model.*;
import org.votingsystem.throwable.DuplicatedDbItemException;
import org.votingsystem.throwable.SignerValidationException;
import org.votingsystem.throwable.TimeStampValidationException;
import org.votingsystem.throwable.XAdESValidationException;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.xades.CertificateVerifier;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

    @PersistenceContext private EntityManager em;
    @Inject private Config config;
    @Inject private SignerInfoService signerInfoService;

    public byte[] signXAdES(byte[] xmlToSign) throws IOException {
        AbstractSignatureTokenConnection signingToken = config.getSigningToken();
        return org.votingsystem.xades.XAdESSignature.sign(xmlToSign, signingToken,
                new TSPHttpSource(config.getTimestampServiceURL()));
    }

    @TransactionAttribute(REQUIRES_NEW)
    public SignedDocument validateAndSaveXAdES(DSSDocument signedDocument, SignatureParams signatureParams)
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
                xAdESDocument.setIndication(XAdESDocument.Indication.ERROR).setMetaInf(
                        MetaInfDto.FROM_REASON(ex.getMessage()));
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
            xAdESDocument.setIndication(XAdESDocument.Indication.ERROR).setMetaInf(MetaInfDto.FROM_REASON(ex.getMessage()));
            throw new XAdESValidationException(ex.getMessage(), ex, xAdESDocument);
        }
        if (simpleReport.getSignaturesCount() == 0) {
            xAdESDocument.setMetaInf(MetaInfDto.FROM_SIMPLE_REPORT(reports.getXmlSimpleReport()))
                    .setIndication(XAdESDocument.Indication.ERROR_ZERO_SIGNATURES);
            msg = Messages.currentInstance().get("zeroSignaturesErrorMsg");
            log.log(Level.SEVERE, msg);
            log.log(Level.FINEST, reports.getXmlSimpleReport());
            throw new XAdESValidationException(msg, xAdESDocument);
        }
        if (simpleReport.getSignaturesCount() > simpleReport.getValidSignaturesCount()) {
            xAdESDocument.setMetaInf(MetaInfDto.FROM_REASON(reports.getXmlSimpleReport()))
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
                Certificate caCertificate = config.getCACertificate(issuerToken.getCertificate().getSerialNumber().longValue());
                signatureParams.setSigningCert(signingCertToken.getCertificate()).setCertificateCA(caCertificate);
                User signer = signerInfoService.checkSigner(signatureParams);
                switch (signatureParams.getSignerType()) {
                    case ANON_ELECTOR:
                        xAdESDocument.setAnonSigner(signer);
                        if(!signer.isValidElector()) {
                            xAdESDocument.setSignedDocumentType(SignedDocumentType.VOTE_REPEATED);
                            return xAdESDocument;
                        }
                        break;

                }
                //we allow only one timestamp per signature
                List<TimestampToken> timeStampTokens = xAdESSignature.getSignatureTimestamps();
                LocalDateTime selectedTokenDate = null;
                for (TimestampToken timestampToken : timeStampTokens) {
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
                        log.severe("bypassing trusted check of TimeStamp issuer certificate");

                    if(!timestampToken.isMessageImprintDataIntact()) {
                        throw new TimeStampValidationException("Timestamp DOES NOT MATCH the signed data");
                    }
                    if(!timestampToken.isSignatureValid()) {
                        throw new TimeStampValidationException("Timestamp's signature validity: INVALID");
                    }
                    LocalDateTime tokenDate = DateUtils.getLocalDateFromUTCDate(timestampToken.getGenerationTime());
                    log.log(Level.FINEST, "timestampToken: " + timestampToken + " - EncodedSignedDataDigestValue: " +
                            timestampToken.getEncodedSignedDataDigestValue() + " - localDateTime: " + tokenDate);
                    if(selectedTokenDate == null)
                        selectedTokenDate = tokenDate;
                    else if(selectedTokenDate.isAfter(tokenDate))
                        selectedTokenDate = tokenDate;
                }
                switch (signatureParams.getSignerType()) {
                    case ANON_ELECTOR:
                        signatures.add(new Signature(null, xAdESDocument, xAdESSignature.getId(), selectedTokenDate));
                        break;
                    default:
                        signatures.add(new Signature(signer, xAdESDocument, xAdESSignature.getId(), selectedTokenDate));
                }
            }
            xAdESDocument.setSignatures(signatures);
        } catch (SignerValidationException ex) {
            xAdESDocument.setMetaInf(MetaInfDto.FROM_REASON(ex.getMessage()))
                    .setIndication(XAdESDocument.Indication.ERROR_SIGNER);
            throw new XAdESValidationException(ex.getMessage(), xAdESDocument);
        } catch (TimeStampValidationException ex) {
            xAdESDocument.setMetaInf(MetaInfDto.FROM_REASON(ex.getMessage()))
                    .setIndication(XAdESDocument.Indication.ERROR_TIMESTAMP);
            throw new XAdESValidationException(ex.getMessage(), xAdESDocument);
        }
        //InputStream is = new ByteArrayInputStream(reports.getData().getContent("UTF-8"));
        //is = new ByteArrayInputStream(reports.getXmlDetailedReport().getContent("UTF-8"));
        return xAdESDocument;
    }


}