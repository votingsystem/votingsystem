package org.votingsystem.vicket.util;

import groovy.util.ConfigObject;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ApplicationVS;
import org.votingsystem.vicket.model.TransactionVS;
import org.votingsystem.vicket.service.TransactionVSService;
import javax.servlet.ServletContext;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ApplicationContextHolder implements ApplicationContextAware, ApplicationVS {

    private static Logger log = Logger.getLogger(ApplicationContextHolder.class);

    private ApplicationContext ctx;
    private static ApplicationContextHolder instance;
   
    private ApplicationContextHolder() {}

    public void setApplicationContext(ApplicationContext applicationContext) {
        ctx = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
    return getInstance().ctx;
    }

    public static ApplicationContextHolder getInstance() {
        if (instance == null) {
            log.debug("init instance");
            instance = new ApplicationContextHolder();
        }
        return instance;
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }


    static ConfigObject getConfig() {
        return getGrailsApplication().getConfig();
    }

    static ServletContext getServletContext() {
        return (ServletContext) getBean("servletContext");
    }

    static GrailsPluginManager getPluginManager() {
        return (GrailsPluginManager)getBean("pluginManager");
    }

    public static GrailsApplication getGrailsApplication() {
        return (GrailsApplication) getBean("grailsApplication");
    }

    public static String getMessage(String key, String... args) {
        String msg = null;
        try {
            msg = ((MessageSource)getBean("messageSource")).getMessage(key, args, getLocale());
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return msg;
    }

    public void updateBalances(TransactionVS transactionVS) {
        ((TransactionVSService)getBean("transactionVSService")).updateBalances(transactionVS);
    }

    public void alert(ResponseVS responseVS) {
        ((TransactionVSService) ApplicationContextHolder.getBean("transactionVSService")).alert(responseVS);
    }

}