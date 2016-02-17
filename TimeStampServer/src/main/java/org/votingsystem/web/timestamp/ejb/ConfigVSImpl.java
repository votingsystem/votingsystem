package org.votingsystem.web.timestamp.ejb;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Named;
import java.io.File;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Named(value="config")
@Startup
public class ConfigVSImpl implements ConfigVS {

    private static final Logger log = Logger.getLogger(ConfigVSImpl.class.getName());

    private String contextURL;
    private EnvironmentVS mode;
    private Properties props;

    public ConfigVSImpl() {
        try {
            String resourceFile = null;
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
            ContextVS.getInstance();
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
    public UserVS createIBAN(UserVS userVS) throws ValidationExceptionVS { return null;}

    @Override
    public UserVS getSystemUser() {
        return null;
    }

    @Override
    public String validateIBAN(String IBAN) throws Exception {
        return null;
    }

    @Override
    public String getBankCode() {
        return null;
    }

    @Override
    public String getBranchCode() {
        return null;
    }

    @Override
    public ControlCenterVS getControlCenter() {
        return null;
    }

    @Override
    public String getStaticResURL() {
        return null;
    }

    @Override public void mainServletInitialized() throws Exception { }

}
