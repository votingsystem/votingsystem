package org.votingsystem.vicket.util;

import groovy.util.ConfigObject;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.votingsystem.model.EnvironmentVS;

import javax.servlet.ServletContext;
import java.util.Locale;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    private static Logger logger = Logger.getLogger(ApplicationContextHolder.class);

    private ApplicationContext ctx;
    private EnvironmentVS environment;
    private static ApplicationContextHolder instance;
    private static Locale locale = new Locale("es");
   
    private ApplicationContextHolder() {}

    public void setApplicationContext(ApplicationContext applicationContext) {
    ctx = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
    return getInstance().ctx;
    }

    public static void setLocale(Locale newLocale) {
    locale = newLocale;
    }

    public static ApplicationContextHolder getInstance() {
        if (instance == null) {
            logger.debug("init instance");
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
            msg = ((MessageSource)getBean("messageSource")).getMessage(key, args, locale);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return msg;
    }

    public static EnvironmentVS getEnvironment() {
        if(getInstance().environment == null) {
            String environmentStr = (String) ((GrailsApplication)getBean("grailsApplication")).getConfig().
                    getProperty("VotingSystemEnvironment");
            if(environmentStr == null || environmentStr.trim().isEmpty()) {
                getInstance().environment = EnvironmentVS.valueOf(grails.util.Environment.getCurrent().
                        getName().toUpperCase());
            } else getInstance().environment = EnvironmentVS.valueOf(environmentStr);
        }
        return getInstance().environment;
    }

}