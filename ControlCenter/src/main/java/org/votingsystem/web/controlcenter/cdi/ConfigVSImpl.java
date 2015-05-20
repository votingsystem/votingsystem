package org.votingsystem.web.controlcenter.cdi;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
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
@Singleton
@Named(value="config")
@Startup
public class ConfigVSImpl implements ConfigVS {

    private static final Logger log = Logger.getLogger(ConfigVSImpl.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionBean;
    @Inject TimeStampBean timeStampBean;
    /* Executor service for asynchronous processing */
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

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

    @PostConstruct
    public void initialize() {
        log.info("initialize");
        Query query = dao.getEM().createQuery("select u from UserVS u where u.type =:type")
                .setParameter("type", UserVS.Type.SYSTEM);
        UserVS systemUser = dao.getSingleResult(UserVS.class, query);
        if(systemUser == null) {
            dao.persist(new UserVS(systemNIF, serverName, UserVS.Type.SYSTEM));
        }
        executorService.submit(() -> {
            try {
                timeStampBean.init();
                signatureBean.init();
                ContextVS.getInstance();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
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

    public String getSystemNIF() {
        return systemNIF;
    }

    @Override public void mainServletInitialized() throws Exception { }

    @Override public String getEmailAdmin() {
        return emailAdmin;
    }

}