package org.currency.web.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import org.iban4j.Iban;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.currency.BankDto;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.BankInfo;
import org.votingsystem.xml.XML;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
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
    @Inject private SignatureServiceEJB signatureService;
    @Inject private ConfigCurrencyServer config;

    @TransactionAttribute(REQUIRES_NEW)
    public void updateBanksInfo() throws Exception {
        File banksFile = new File(config.getApplicationDirPath() + "/sec/banks.xml");
        List<BankDto> bankList = new XML().getMapper().readValue(banksFile,  new TypeReference<List<BankDto>>() {});
        for(BankDto bankDto : bankList) {
            bankDto.validatePublishRequest(config.getEntityId());
            Iban IBAN = Iban.valueOf(bankDto.getIBAN());
            List<BankInfo> bankInfoList = em.createQuery("SELECT b FROM BankInfo b WHERE b.bankCode =:bankCode and b.countryCode=:countryCode")
                    .setParameter("bankCode", IBAN.getBankCode())
                    .setParameter("countryCode", IBAN.getCountryCode()).getResultList();
            BankInfo bankInfo = null;
            X509Certificate x509BankCert = PEMUtils.fromPEMToX509Cert(bankDto.getX509Certificate().trim().getBytes());
            String x509BankCertUUID = CertificateUtils.getHash(x509BankCert);
            Certificate bankCertificate = null;
            if(bankInfoList.isEmpty()) {
                Bank bank = Bank.FROM_CERT(Bank.class, x509BankCert, User.Type.BANK);
                bank.setDescription(bankDto.getInfo()).setEntityId(bankDto.getEntityId()).setUUID(UUID.randomUUID().toString());
                bank.setCertificateCA(signerInfoService.verifyCertificate(x509BankCert));
                signerInfoService.checkSigner(bank, User.Type.BANK, bankDto.getEntityId());
                bankInfo = new BankInfo(bank, IBAN.getBankCode(), IBAN.getCountryCode());
                em.persist(bankInfo);
                config.createIBAN(bank);
                log.info("Added new bank to database - bank UUID: " + bank.getUUID());
            } else {
                bankInfo = bankInfoList.iterator().next();
                List<Certificate> certificates = em.createQuery(
                        "select c from Certificate c where c.state=:state and c.signer=:bank")
                        .setParameter("state", Certificate.State.OK)
                        .setParameter("bank", bankInfo.getBank()).getResultList();
                bankCertificate = certificates.iterator().next();
                if(!bankCertificate.getUUID().equals(x509BankCertUUID)) {
                    Certificate certificateCA = signerInfoService.verifyCertificate(x509BankCert);
                    Certificate newBankCertificate = Certificate.SIGNER(bankInfo.getBank(), x509BankCert)
                            .setAuthorityCertificate(certificateCA);
                    bankCertificate.setState(Certificate.State.RENEWED);
                    em.persist(newBankCertificate);
                    log.info("Renewed bank certificate from " + x509BankCert.getSubjectDN() +
                            " - new certificate id: " + newBankCertificate.getId());
                }
            }
            log.info("Bank UUID: " + bankInfo.getBank().getUUID() + " - " + x509BankCert.getSubjectDN().toString());
        }
    }


}