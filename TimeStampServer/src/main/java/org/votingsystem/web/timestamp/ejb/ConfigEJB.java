package org.votingsystem.web.timestamp.ejb;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Named;
import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Named(value="config")
@Startup
public class ConfigEJB implements ConfigVS {

    private static final Logger log = Logger.getLogger(ConfigEJB.class.getName());

    private String contextURL;
    private Properties props;

    public ConfigEJB() {
        try {
            String resourceFile = "TimeStampServer.properties";
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
    public User createIBAN(User user) throws ValidationException { return null;}

    @Override
    public User getSystemUser() {
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
    public ControlCenter getControlCenter() {
        return null;
    }

    @Override
    public String getStaticResURL() {
        return null;
    }

    @Override public void mainServletInitialized() throws Exception { }

}
