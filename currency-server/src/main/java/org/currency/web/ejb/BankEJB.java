package org.currency.web.ejb;

import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.currency.BankDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.BankInfo;
import org.votingsystem.util.Constants;
import org.votingsystem.util.NifUtils;
import org.iban4j.Iban;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.ejb.SignerInfoService;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BankEJB {

    private static final Logger log = Logger.getLogger(BankEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private BankEJB bankBean;
    @Inject private SignerInfoService signerInfoService;
    @Inject private UserEJB userBean;
    @Inject private TransactionEJB transactionBean;
    @Inject private SignatureService signatureService;
    @Inject private ConfigCurrencyServer config;

    @TransactionAttribute(REQUIRES_NEW)
    public Bank saveBank(SignedDocument signedDocument) throws Exception {
        signedDocument.setSignedDocumentType(SignedDocumentType.BANK_NEW);
        User signer = signedDocument.getFirstSignature().getSigner();
        log.log(Level.FINE, "signer:" + signer.getNumIdAndType());
        BankDto request = signedDocument.getSignedContent(BankDto.class);
        request.validatePublishRequest();
        Iban IBAN = Iban.valueOf(request.getIBAN());
        signerInfoService.checkIfAdmin(signer.getX509Certificate());
        Iterator<X509Certificate> certIterator = PEMUtils.fromPEMToX509CertCollection(
                request.getCertChainPEM().getBytes()).iterator();
        X509Certificate cert = certIterator.next();
        Set<X509Certificate> additionalCerts = new HashSet<>();
        while(certIterator.hasNext()) {
            additionalCerts.add(certIterator.next());
        }
        CertUtils.verifyCertificateChain(cert, additionalCerts);
        Bank bank = Bank.getUser(cert);

        PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                config.getTrustedCertAnchors(), false, Arrays.asList(bank.getX509Certificate()));
        X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        bank.setCertificateCA(config.getCACertificate(certCaResult.getSerialNumber().longValue()));
        log.log(Level.FINE, "bank:" + bank.getNumIdAndType() + " cert issuer: " + certCaResult.getSubjectDN());

        String validatedNIF = NifUtils.validate(bank.getNumId());
        List<Bank> bankList = em.createNamedQuery(User.FIND_USER_BY_NIF).setParameter("nif", validatedNIF).getResultList();
        if(bankList.isEmpty()) {
            bank.setDescription(request.getInfo()).setIBAN(request.getIBAN());
            em.persist(bank);
            em.persist(new BankInfo(bank, IBAN.getBankCode()));
            log.info("Added new bank to database - bank id: " + bank.getId());
        } else {
            Bank bankDB = bankList.iterator().next();
            bankDB.setDescription(request.getInfo()).setCertificateCA(bank.getCertificateCA());
            bankDB.setX509Certificate(bank.getX509Certificate());
            bankDB.setTimeStampToken(bank.getTimeStampToken());
            bank = bankDB;
        }
        signerInfoService.loadCertInfo(bank, null);
        config.createIBAN(bank);
        log.info("Bank id: " + bank.getId() + " - " + cert.getSubjectDN().toString());
        return bank;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void refreshBankInfoData() {
        List<Bank> bankList = em.createQuery("SELECT b FROM Bank b").getResultList();
        for(Bank bank : bankList) {
            List<BankInfo> bankInfoList = em.createNamedQuery(BankInfo.FIND_BY_BANK).setParameter("bank", bank).getResultList();
            BankInfo bankInfo = bankInfoList.iterator().next();
            Iban iban = Iban.valueOf(bank.getIBAN());
            if(bankInfo != null) {
                em.merge(bankInfo.setBankCode(iban.getBankCode()));
            } else em.persist(new BankInfo(bank, iban.getBankCode()));
        }
    }

}