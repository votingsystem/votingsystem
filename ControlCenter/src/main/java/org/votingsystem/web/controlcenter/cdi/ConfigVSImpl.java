package org.votingsystem.web.controlcenter.cdi;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Named(value="config")
@Startup
public class ConfigVSImpl implements ConfigVS {

    private static final Logger log = Logger.getLogger(ConfigVSImpl.class.getName());

    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject SubscriptionVSBean subscriptionBean;
    @Inject TimeStampBean timeStampBean;
    /* Executor service for asynchronous processing */
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    private String systemNIF;
    private String contextURL;
    private String accessControlURL;
    private String webSocketURL;
    private String serverName;
    private String timeStampServerURL;
    private EnvironmentVS mode;
    private Properties props;
    private String emailAdmin;
    private String staticResURL;
    private File serverDir;
    private UserVS systemUser;

    public ConfigVSImpl() {
        try {
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
            URL res = Thread.currentThread().getContextClassLoader().getResource(resourceFile);
            props.load(res.openStream());
            systemNIF = (String) props.get("vs.systemNIF");
            contextURL = (String) props.get("vs.contextURL");
            emailAdmin = (String) props.get("vs.emailAdmin");
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
        systemUser = dao.getSingleResult(UserVS.class, query);
        if(systemUser == null) {
            systemUser = dao.persist(new UserVS(systemNIF, serverName, UserVS.Type.SYSTEM));
        }
        new ContextVS(null, null);
        executorService.submit(() -> {
            try {
                timeStampBean.init();
                cmsBean.init();
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

    @Override
    public String getStaticResURL() {
        return staticResURL;
    }

    @Override
    public UserVS createIBAN(UserVS userVS) throws ValidationExceptionVS { return null;}

    @Override
    public UserVS getSystemUser() {
        return systemUser;
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