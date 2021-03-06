package org.currency.web.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@LocalBean
@Singleton
@Startup
@DependsOn("ConfigEJB")
public class StartUpEJB {

    private static final Logger log = Logger.getLogger(StartUpEJB.class.getName());

    @EJB private BankEJB bankEJB;

    @PostConstruct
    public void initialize() {
        try {
            bankEJB.updateBanksInfo();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
