package org.currency.web.managed;

import org.votingsystem.model.currency.Bank;

import javax.ejb.AccessTimeout;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Named("banksPage")
@SessionScoped
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class BanksPageBean implements Serializable {

    @PersistenceContext
    private EntityManager em;

    //connect to DB and get customer list
    public List<Bank> getBankList() {
        List<Bank> bankList = em.createQuery("select b from Bank b").getResultList();
        return bankList;
    }
}
