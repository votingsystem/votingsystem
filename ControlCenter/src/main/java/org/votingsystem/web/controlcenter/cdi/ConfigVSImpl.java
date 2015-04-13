package org.votingsystem.web.controlcenter.cdi;

import org.votingsystem.model.TagVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Query;
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
@ApplicationScoped
@Named(value="config")
public class ConfigVSImpl implements ConfigVS {

    private static final Logger log = Logger.getLogger(ConfigVSImpl.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionBean;

    public static final String RESOURCE_PATH= "/resources/bower_components";
    public static final String WEB_PATH= "/jsf";
    public static final String REST_PATH= "/rest";


    private String systemNIF;
    private String contextURL;
    private String accessControlURL;
    private String webSocketURL;
    private String resourceURL;
    private String restURL;
    private String webURL;
    private String serverName;
    private String timeStampServerURL;
    private EnvironmentVS mode;
    private Properties props;
    private String bankCode = null;
    private String  branchCode = null;
    private String emailAdmin = null;
    private String staticResURL = null;
    private File serverDir = null;
    private TagVS wildTag;
    private X509Certificate x509TimeStampServerCert;

    public ConfigVSImpl() {
        try {
            URL res = Thread.currentThread().getContextClassLoader().getResource("META-INF/logging.properties");
            LogManager.getLogManager().readConfiguration(res.openStream());

            String resourceFile = null;
            log.info("environment: " + System.getProperty("vs.environment"));
            if(System.getProperty("vs.environment") != null) {
                mode = EnvironmentVS.valueOf(System.getProperty("vs.environment"));
            } else mode = EnvironmentVS.DEVELOPMENT;
            switch (mode) {
                case DEVELOPMENT:
                    resourceFile = "ControlCenter_DEVELOPMENT.properties";
                    break;
                case PRODUCTION:
                    resourceFile = "ControlCenter_PRODUCTION.properties";
                    break;
            }
            props = new Properties();
            res = Thread.currentThread().getContextClassLoader().getResource(resourceFile);
            props.load(res.openStream());
            systemNIF = (String) props.get("vs.systemNIF");
            contextURL = (String) props.get("vs.contextURL");
            emailAdmin = (String) props.get("vs.emailAdmin");
            resourceURL = contextURL + RESOURCE_PATH;
            restURL = contextURL + REST_PATH;
            webURL = contextURL + WEB_PATH;
            accessControlURL = (String) props.get("vs.accessControlURL");
            serverName = (String) props.get("vs.serverName");
            timeStampServerURL = (String) props.get("vs.timeStampServerURL");
            staticResURL = (String) props.get("vs.staticResourcesURL");
            serverDir = new File((String) props.get("vs.staticResourcesPath"));
            serverDir.mkdirs();
            log.info("serverDir: " + serverDir.getAbsolutePath());
            new File((String) props.get("vs.staticResourcesPath") + "/backup");
            new File((String) props.get("vs.staticResourcesPath") + "/error").mkdirs();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public TagVS getTag(String tagName) {
        Query query = dao.getEM().createNamedQuery("findTagByName").setParameter("name", tagName);
        return dao.getSingleResult(TagVS.class, query);
    }

    public void setX509TimeStampServerCert(X509Certificate x509TimeStampServerCert) {
        this.x509TimeStampServerCert = x509TimeStampServerCert;
    }

    public EnvironmentVS getMode() {
        return mode;
    }

    @Override
    public Properties getProperties() {
        return props;
    }

    @Override
    public File getServerDir() {
        return serverDir;
    }

    public String getContextURL() {
        return contextURL;
    }

    public String getServerName() {
        return serverName;
    }

    public String getTimeStampServerURL() {
        return timeStampServerURL;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public String getWebSocketURL() {
        return webSocketURL;
    }

    public TagVS getWildTag() {
        return wildTag;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public String getRestURL() {
        return restURL;
    }

    @Override
    public String getStaticResURL() {
        return staticResURL;
    }

    public String getWebURL() {
        return webURL;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getSystemNIF() {
        return systemNIF;
    }

    @Override
    public String getEmailAdmin() {
        return emailAdmin;
    }

}