package org.currency.web.servlet;

import org.votingsystem.ejb.TrustedServicesEJB;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppServletContextListener implements ServletContextListener{

    private static final Logger log = Logger.getLogger(AppServletContextListener.class.getName());


    /* Executor service for asynchronous processing */
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    @EJB
    private TrustedServicesEJB trustedServices;

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        log.info("ContextPath: " + contextEvent.getServletContext().getContextPath());
        try {
            //bankEJB.updateBanksInfo();
            //Hack to allow local OCSP service initialization in development
            executorService.submit(() -> {
                try {
                    //Hack to allow local OCSP service initialization
                    Thread.sleep(10000);
                    trustedServices.loadTrustedServices();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });


        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
        log.info("--------- shutdown --------- ServerInfo: " + contextEvent.getServletContext().getServerInfo());
    }

}