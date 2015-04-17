package org.votingsystem.web.currency.ejb;

import org.iban4j.Iban;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.BankVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.BankVSInfo;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BankVSBean {

    private static final Logger log = Logger.getLogger(BankVSBean.class.getName());


    @Inject DAOBean dao;
    @Inject BankVSBean bankVSBean;
    @Inject UserVSBean userVSBean;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject SystemBean systemBean;
    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;

    public BankVS saveBankVS(MessageSMIME messageSMIMEReq) throws Exception {
        UserVS signer = messageSMIMEReq.getUserVS();
        log.log(Level.FINE, "signer:" + signer.getNif());
        BankVSDto request = messageSMIMEReq.getSignedContent(BankVSDto.class);
        request.validatePublishRequest();
        Iban IBAN = Iban.valueOf(request.getIBAN());
        if(!signatureBean.isUserAdmin(signer.getNif())) {
            throw new ValidationExceptionVS("operation: " + request.getOperation() +
                    " - userWithoutPrivilegesErrorMsg - nif: " + signer.getNif());
        }
        Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(request.getCertChainPEM().getBytes());
        X509Certificate x509Certificate = certChain.iterator().next();
        BankVS bankVS = BankVS.getUserVS(x509Certificate);
        signatureBean.verifyUserCertificate(bankVS);
        String validatedNIF = org.votingsystem.util.NifUtils.validate(bankVS.getNif());
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", validatedNIF);
        Object bankVSDB = dao.getSingleResult(Object.class, query);
        if(bankVSDB instanceof UserVS) throw new ExceptionVS("The BankVS: " + ((UserVS)bankVSDB).getId() +
                " has the same NIF:" + validatedNIF);
        if(bankVSDB == null) {
            bankVSDB = dao.persist(bankVS.setDescription(request.getInfo()).setIBAN(request.getIBAN()));
            dao.persist(new BankVSInfo((BankVS) bankVSDB, IBAN.getBankCode()));
            log.info("NEW bankVS id: " + ((BankVS) bankVSDB).getId());
        } else {
            ((BankVS)bankVSDB).setDescription(request.getInfo()).setCertificateCA(bankVS.getCertificateCA());
            ((BankVS)bankVSDB).setCertificate(bankVS.getCertificate());
            ((BankVS)bankVSDB).setTimeStampToken(bankVS.getTimeStampToken());
        }
        bankVS = (BankVS) bankVSDB;
        subscriptionVSBean.setUserData(bankVS, null);
        bankVS.setIBAN(config.getIBAN(bankVS.getId()));
        dao.persist(new CurrencyAccount(bankVS, BigDecimal.ZERO,Currency.getInstance("EUR").getCurrencyCode(),
                config.getTag(TagVS.WILDTAG)));
        log.info("added new bank - id: " + bankVS.getId() + " - " + x509Certificate.getSubjectDN().toString());
        return bankVS;
    }

    public BalancesDto getBalancesDto(BankVS bankVS, TimePeriod timePeriod) throws Exception {
        BalancesDto balancesDto = transactionVSBean.getBalancesDto(
                transactionVSBean.getTransactionFromList(bankVS, timePeriod), TransactionVS.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUserVS(userVSBean.getUserVSDto(bankVS, false));
        return balancesDto;
    }

    public void refreshBankInfoData() {
        List<BankVS> bankVSList = dao.findAll(BankVS.class);
        for(BankVS bankVS : bankVSList) {
            Query query = dao.getEM().createNamedQuery("findBankVSInfoByBank").setParameter("bankVS", bankVS);
            BankVSInfo bankVSInfo = dao.getSingleResult(BankVSInfo.class, query);
            Iban iban = Iban.valueOf(bankVS.getIBAN());
            if(bankVSInfo != null) {
                dao.getEM().merge(bankVSInfo.setBankCode(iban.getBankCode()));
            } else dao.persist(new BankVSInfo(bankVS, iban.getBankCode()));
        }
    }

}
