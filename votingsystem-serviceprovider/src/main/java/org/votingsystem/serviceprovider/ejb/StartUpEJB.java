package org.votingsystem.serviceprovider.ejb;

import org.votingsystem.ejb.TrustedServicesEJB;

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


    @EJB private TrustedServicesEJB trustedServices;

    @PostConstruct
    public void initialize() {
        try {
            trustedServices.loadTrustedServices();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }


}