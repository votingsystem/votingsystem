package org.votingsystem.web.timestamp.ejb;

import org.votingsystem.model.TagVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.web.cdi.ConfigVS;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.File;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Startup
public class ConfigVSImpl implements ConfigVS {

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

    @Override
    public String getTimeStampServerURL() {
        return null;
    }

    @Override
    public String getSystemNIF() {
        return null;
    }

    @Override
    public String getEmailAdmin() {
        return null;
    }

    @Override
    public TagVS getTag(String tagName) {
        return null;
    }

    @Override
    public void setX509TimeStampServerCert(X509Certificate x509Cert) {

    }

    @Override
    public String getServerName() {
        return null;
    }

    public EnvironmentVS getMode() {
        return mode;
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public File getServerDir() {
        return null;
    }

    public String getContextURL() {
        return contextURL;
    }

    @Override
    public String getWebSocketURL() {
        return null;
    }

    @Override
    public String getWebURL() {
        return null;
    }

    @Override
    public String getIBAN(Long userId) { return null;}

    @Override
    public String getRestURL() {
        return null;
    }

    @Override
    public String getStaticResURL() {
        return null;
    }

}
