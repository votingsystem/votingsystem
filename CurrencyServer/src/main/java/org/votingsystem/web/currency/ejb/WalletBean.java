package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.util.WalletVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class WalletBean {

    @Inject ConfigVS config;
    @Inject
    DAOBean dao;
    @PersistenceContext private EntityManager em;

    public WalletVS getWalletVS(String userIBAN, TagVS tag, String currencyCode) throws ExceptionVS {
        List accountList = new ArrayList<>();
        Query query = em.createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("userIBAN", userIBAN).setParameter("currencyCode", currencyCode)
                .setParameter("tag", config.getTag(TagVS.WILDTAG)).setParameter("state", CurrencyAccount.State.ACTIVE);
        CurrencyAccount wildTagAccount = dao.getSingleResult(CurrencyAccount.class, query);
        if(wildTagAccount != null) accountList.add(wildTagAccount);
        if(tag != null && !tag.getName().equals(TagVS.WILDTAG)) {
            query = em.createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                    .setParameter("userIBAN", userIBAN).setParameter("currencyCode", currencyCode)
                    .setParameter("tag",tag).setParameter("state", CurrencyAccount.State.ACTIVE);
            CurrencyAccount tagAccount = dao.getSingleResult(CurrencyAccount.class, query);
            if(tagAccount != null) accountList.add(tagAccount);
        }
        if(accountList.isEmpty()) throw new ExceptionVS(
                "no accounts for IBAN:" + userIBAN + " - " + tag.getName() + " - " + currencyCode);
        else return new WalletVS(accountList, currencyCode);
    }

    public Map<CurrencyAccount, BigDecimal> getAccountMovementsForTransaction(String fromUserIBAN,
               TagVS tag, BigDecimal amount, String currencyCode) throws Exception {
        WalletVS transactionWallet = getWalletVS(fromUserIBAN, tag, currencyCode);
        return transactionWallet.getAccountMovementsForTransaction(tag, amount, currencyCode);
    }

}