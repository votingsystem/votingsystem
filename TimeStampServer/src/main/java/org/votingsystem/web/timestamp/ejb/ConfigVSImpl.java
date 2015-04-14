package org.votingsystem.web.timestamp.ejb;

import org.votingsystem.util.EnvironmentVS;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Startup
public class ConfigVSImpl {

    private static final Logger log = Logger.getLogger(ConfigVSImpl.class.getSimpleName());

    private String contextURL;
    private EnvironmentVS mode;
    private Properties props;

    public ConfigVSImpl() {
        try {
            String resourceFile = null;
            //String fileName = System.getProperty("jboss.server.config.dir") + "/my.properties";
            log.info("environment: " + System.getProperty("vs.environment"));
            if(System.getProperty("vs.environment") != null) {
                mode = EnvironmentVS.valueOf(System.getProperty("vs.environment"));
            } else mode = EnvironmentVS.DEVELOPMENT;
            switch (mode) {
                case DEVELOPMENT:
                    resourceFile = "TimeStampServer_DEVELOPMENT.properties";
                    break;
                case PRODUCTION:
                    resourceFile = "TimeStampServer_PRODUCTION.properties";
                    break;
            }
            props = new Properties();
            URL res = Thread.currentThread().getContextClassLoader().getResource(resourceFile);
            props.load(res.openStream());
            contextURL = (String) props.get("vs.contextURL");
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public EnvironmentVS getMode() {
        return mode;
    }

    public String getContextURL() {
        return contextURL;
    }

}
