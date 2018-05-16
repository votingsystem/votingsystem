package org.votingsystem.serviceprovider.ejb;

import org.votingsystem.ejb.TrustedServicesEJB;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.logging.Logger;

@LocalBean
@Singleton
@Startup

public class StartUpEJB {

    private static final Logger log = Logger.getLogger(StartUpEJB.class.getName());


    @EJB private TrustedServicesEJB trustedServices;




}
