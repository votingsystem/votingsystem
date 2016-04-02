package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.model.Actor;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Query;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
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
    @Inject SubscriptionBean subscriptionBean;
    @Inject TimeStampBean timeStampBean;
    @Inject EventElectionBean eventVSBean;
    /* Executor service for asynchronous processing */
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    private String systemNIF;
    private String contextURL;
    private String accessControlURL;
    private String webSocketURL;
    private String serverName;
    private String timeStampServerURL;
    private Properties props;
    private String emailAdmin;
    private String staticResURL;
    private File serverDir;
    private User systemUser;

    public ConfigVSImpl() {
        try {
            String resourceFile = "ControlCenter.properties";
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
        Query query = dao.getEM().createQuery("select u from User u where u.type =:type")
                .setParameter("type", User.Type.SYSTEM);
        systemUser = dao.getSingleResult(User.class, query);
        if(systemUser == null) {
            systemUser = dao.persist(new User(systemNIF, serverName, User.Type.SYSTEM));
        }
        ContextVS.getInstance().setTimeStampServiceURL(Actor.getTimeStampServiceURL(timeStampServerURL));
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

    @Schedule(dayOfWeek = "*", minute = "0", hour = "0", persistent = false)
    public void checkDates() throws InterruptedException {
        log.info("scheduled - checkDates");
        try {
            List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING);
            Query query = dao.getEM().createQuery("select e from EventElection e where e.state in :inList")
                    .setParameter("inList", inList);
            List<EventVS> eventList = query.getResultList();
            for(EventVS eventVS : eventList) {
                eventVSBean.checkEventVSDates(eventVS);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
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
    public User createIBAN(User user) throws ValidationException { return null;}

    @Override
    public User getSystemUser() {
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
    public ControlCenter getControlCenter() {
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