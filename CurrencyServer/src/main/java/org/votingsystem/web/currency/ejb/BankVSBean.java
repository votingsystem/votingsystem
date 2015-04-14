package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iban4j.Iban;
import org.votingsystem.model.*;
import org.votingsystem.model.currency.*;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Currency;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BankVSBean {

    private static final Logger log = Logger.getLogger(BankVSBean.class.getSimpleName());

    @PersistenceContext EntityManager em;
    @Inject
    DAOBean dao;
    @Inject IBANBean ibanBean;
    @Inject BankVSBean bankVSBean;
    @Inject UserVSBean userVSBean;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject SystemBean systemBean;
    @Inject
    SignatureBean signatureBean;
    @Inject ConfigVS config;


    private class SaveBankRequest {
        String info, certChainPEM;
        Iban IBAN;
        TypeVS operation;
        public SaveBankRequest(String signedContent) throws ExceptionVS, IOException {
            JsonNode dataJSON = new ObjectMapper().readTree(signedContent);
            if(dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");
            if(dataJSON.get("IBAN") == null) throw new ValidationExceptionVS("missing param 'IBAN'");
            if(dataJSON.get("info") == null) throw new ValidationExceptionVS("missing param 'info'");
            if(dataJSON.get("certChainPEM") == null) throw new ValidationExceptionVS("missing param 'certChainPEM'");
            operation = TypeVS.valueOf(dataJSON.get("operation").asText());
            if(TypeVS.BANKVS_NEW != operation) throw new ValidationExceptionVS(
                    "Operation expected: 'BANKVS_NEW' - operation found: " + operation.toString());
            
            IBAN = Iban.valueOf(dataJSON.get("IBAN").asText());
            info = dataJSON.get("info").asText();
            certChainPEM = dataJSON.get("certChainPEM").asText();
        }
    }

    public BankVS saveBankVS(MessageSMIME messageSMIMEReq) throws Exception {
        UserVS signer = messageSMIMEReq.getUserVS();
        log.log(Level.FINE, "signer:" + signer.getNif());
        SaveBankRequest request = new SaveBankRequest(messageSMIMEReq.getSMIME().getSignedContent());
        if(!signatureBean.isUserAdmin(signer.getNif())) {
            throw new ValidationExceptionVS("operation: " + request.operation.toString() +
                    " - userWithoutPrivilegesErrorMsg - nif: " + signer.getNif());
        }
        Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(request.certChainPEM.getBytes());
        X509Certificate x509Certificate = certChain.iterator().next();
        BankVS bankVS = BankVS.getUserVS(x509Certificate);
        signatureBean.verifyUserCertificate(bankVS);
        String validatedNIF = org.votingsystem.util.NifUtils.validate(bankVS.getNif());
        Query query = em.createNamedQuery("findUserByNIF").setParameter("nif", validatedNIF);
        Object bankVSDB = dao.getSingleResult(Object.class, query);
        if(bankVSDB instanceof UserVS) throw new ExceptionVS("The BankVS: " + ((UserVS)bankVSDB).getId() +
                " has the same NIF:" + validatedNIF);
        if(bankVSDB == null) {
            bankVSDB = dao.persist(bankVS.setDescription(request.info).setIBAN(request.IBAN.toString()));
            dao.persist(new BankVSInfo((BankVS) bankVSDB, request.IBAN.getBankCode()));
            log.info("NEW bankVS id: " + ((BankVS) bankVSDB).getId());
        } else {
            ((BankVS)bankVSDB).setDescription(request.info).setCertificateCA(bankVS.getCertificateCA());
            ((BankVS)bankVSDB).setCertificate(bankVS.getCertificate());
            ((BankVS)bankVSDB).setTimeStampToken(bankVS.getTimeStampToken());
        }
        bankVS = (BankVS) bankVSDB;
        subscriptionVSBean.setUserData(bankVS, null);
        bankVS.setIBAN(ibanBean.getIBAN(bankVS.getId()));
        dao.persist(new CurrencyAccount(bankVS, BigDecimal.ZERO,Currency.getInstance("EUR").getCurrencyCode(),
                config.getTag(TagVS.WILDTAG)));
        log.info("added new bank - id: " + bankVS.getId() + " - " + x509Certificate.getSubjectDN().toString());
        return bankVS;
    }

    public Map getDataWithBalancesMap(BankVS bankVS, DateUtils.TimePeriod timePeriod) throws Exception {
        Map resultMap = new HashMap<>();
        resultMap.put("timePeriod", timePeriod.getMap());
        resultMap.put("userVS", userVSBean.getUserVSDataMap(bankVS, false));
        Map transactionListWithBalances = transactionVSBean.getTransactionListWithBalances(
                transactionVSBean.getTransactionFromList(bankVS, timePeriod), TransactionVS.Source.FROM);
        resultMap.put("transactionFromList", transactionListWithBalances.get("transactionList"));
        resultMap.put("balancesFrom", transactionListWithBalances.get("balances"));
        return resultMap;
    }

    public void refreshBankInfoData() {
        List<BankVS> bankVSList = dao.findAll(BankVS.class);
        for(BankVS bankVS : bankVSList) {
            Query query = em.createNamedQuery("findBankVSInfoByBank").setParameter("bankVS", bankVS);
            BankVSInfo bankVSInfo = dao.getSingleResult(BankVSInfo.class, query);
            Iban iban = Iban.valueOf(bankVS.getIBAN());
            if(bankVSInfo != null) {
                em.merge(bankVSInfo.setBankCode(iban.getBankCode()));
            } else dao.persist(new BankVSInfo(bankVS, iban.getBankCode()));
        }
    }

}
