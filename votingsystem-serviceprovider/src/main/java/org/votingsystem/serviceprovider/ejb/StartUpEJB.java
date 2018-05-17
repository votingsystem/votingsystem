package org.votingsystem.serviceprovider.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.logging.Logger;

@LocalBean
@Singleton
@Startup
@DependsOn("ConfigEJB")
public class StartUpEJB {

    private static final Logger log = Logger.getLogger(StartUpEJB.class.getName());

    @PostConstruct
    public void initialize() { }

}