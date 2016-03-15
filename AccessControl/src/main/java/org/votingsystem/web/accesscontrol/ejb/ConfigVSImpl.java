package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.AccessControl;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
import java.security.cert.X509Certificate;
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
    @Inject
    SubscriptionBean subscriptionBean;
    @Inject EventElectionBean eventElectionBean;
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
    private Properties props;
    private String emailAdmin;
    private String staticResURL;
    private File serverDir;
    private User systemUser;
    private ControlCenter controlCenter;

    public ConfigVSImpl() {
        try {
            String resourceFile = "AccessControl.properties";
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
            new File((String) props.get("vs.staticResourcesPath") + "/backup").mkdirs();
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
        query = dao.getEM().createQuery("select a from Actor a where a.serverURL =:serverURL")
                .setParameter("serverURL", contextURL);
        AccessControl actor = dao.getSingleResult(AccessControl.class, query);
        if(actor == null) {
            actor = new AccessControl();
            actor.setServerURL(contextURL);
            actor.setState(Actor.State.OK).setName(serverName);
            dao.persist(actor);
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

    @Schedule(dayOfWeek = "*")
    public void generateElectionBackups() throws Exception {
        log.info("scheduled - generateElectionBackups");
        eventElectionBean.generateBackups();
    }

    @PreDestroy
    private void shutdown() { log.info(" --------- shutdown ---------");}

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public TagVS getTag(String tagName) {
        Query query = dao.getEM().createNamedQuery("findTagByName").setParameter("name", tagName);
        return dao.getSingleResult(TagVS.class, query);
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

    public String getSystemNIF() {
        return systemNIF;
    }

    @Override public String getEmailAdmin() {
        return emailAdmin;
    }

    public ControlCenter getControlCenter() {
        return controlCenter;
    }

    public void checkControlCenter(String serverURL) throws Exception {
        try {
            log.info("checkControlCenter - serverURL:" + serverURL);
            Certificate controlCenterCert = null;
            serverURL = StringUtils.checkURL(serverURL);
            Query query = dao.getEM().createQuery("select c from ControlCenter c where c.serverURL =:serverURL")
                    .setParameter("serverURL", serverURL);
            ControlCenter controlCenterDB = dao.getSingleResult(ControlCenter.class, query);
            if(controlCenterDB != null) {
                query = dao.getEM().createQuery("select c from Certificate c where c.actor =:actor " +
                        "and c.state =:state").setParameter("actor", controlCenterDB)
                        .setParameter("state", Certificate.State.OK);
                controlCenterCert = dao.getSingleResult(Certificate.class, query);
                if(controlCenterCert != null) return ;
            }
            ResponseVS responseVS = HttpHelper.getInstance().getData(Actor.getServerInfoURL(serverURL), ContentType.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Actor actor = ((ActorDto)responseVS.getMessage(ActorDto.class)).getActor();
                if (Actor.Type.CONTROL_CENTER != actor.getType()) throw new ExceptionVS(
                        "ERROR - actorNotControlCenterMsg serverURL: " + serverURL);
                if(!actor.getServerURL().equals(serverURL)) throw new ExceptionVS(
                        "ERROR - serverURLMismatch expected URL: " + serverURL + " - found: " + actor.getServerURL());
                X509Certificate x509Cert = PEMUtils.fromPEMToX509CertCollection(
                        actor.getCertChainPEM().getBytes()).iterator().next();
                cmsBean.verifyCertificate(x509Cert);
                if(controlCenterDB == null) {
                    controlCenterDB = dao.persist((ControlCenter) new ControlCenter(actor).setX509Certificate(
                            x509Cert).setState(Actor.State.OK));
                }
                controlCenterDB.setCertChainPEM(actor.getCertChainPEM());
                controlCenterCert = Certificate.ACTOR(controlCenterDB, x509Cert);
                controlCenterCert.setCertChainPEM(actor.getCertChainPEM().getBytes());
                dao.persist(controlCenterCert);
                controlCenter = controlCenterDB;
                return;
            }
            log.log(Level.SEVERE, "ERROR fetching ControlCenter - serverURL: " + serverURL + " - retry");
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void mainServletInitialized() throws Exception{
        log.info("mainServletInitialized - initControlCenter");
        try {
            Query query = dao.getEM().createQuery("select c from ControlCenter c where c.state =:state")
                    .setParameter("state", Actor.State.OK);
            controlCenter = dao.getSingleResult(ControlCenter.class, query);
            if(controlCenter == null) {
                executorService.submit(() -> {
                    try {
                        checkControlCenter(getProperty("vs.controlCenterURL"));
                    } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
                });
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}
