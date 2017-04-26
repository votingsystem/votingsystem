package org.currency.web.ejb;

import org.votingsystem.ejb.TrustedServicesEJB;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@LocalBean
@Singleton
@Startup
@DependsOn("ConfigCurrencyServer")
public class StartUpEJB {

    private static final Logger log = Logger.getLogger(StartUpEJB.class.getName());

    @EJB
    private BankEJB bankEJB;
    @EJB
    private TrustedServicesEJB trustedServices;


    @PostConstruct
    public void initialize() {
        try {
            log.info("updateBanksInfo");
            bankEJB.updateBanksInfo();
            //Hack to allow local OCSP service initialization
            //Thread.sleep(10000);
            trustedServices.loadTrustedServices();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
