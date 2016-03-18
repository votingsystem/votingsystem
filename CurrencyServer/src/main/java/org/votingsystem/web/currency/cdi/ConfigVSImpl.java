package org.votingsystem.web.currency.cdi;

import org.iban4j.*;
import org.votingsystem.model.Actor;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.web.currency.ejb.AuditBean;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.Query;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Locale;
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

    private static final Logger log = Logger.getLogger(ConfigVS.class.getName());

    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject SubscriptionBean subscriptionBean;
    @Inject TimeStampBean timeStampBean;
    @Inject AuditBean auditBean;
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    private String systemNIF;
    private String contextURL;
    private String accessControlURL;
    private String webSocketURL;
    private String serverName;
    private String timeStampServerURL;
    private Properties props;
    private String bankCode = null;
    private String  branchCode = null;
    private String emailAdmin = null;
    private String staticResURL = null;
    private File serverDir = null;
    private User systemUser;

    public ConfigVSImpl() {
        try {
            String resourceFile = "CurrencyServer.properties";
            props = new Properties();
            URL res = Thread.currentThread().getContextClassLoader().getResource(resourceFile);
            props.load(res.openStream());
            systemNIF = (String) props.get("vs.systemNIF");
            contextURL = (String) props.get("vs.contextURL");
            emailAdmin = (String) props.get("vs.emailAdmin");
            webSocketURL = (String) props.get("vs.webSocketURL");
            accessControlURL = (String) props.get("vs.accessControlURL");
            serverName = (String) props.get("vs.serverName");
            timeStampServerURL = (String) props.get("vs.timeStampServerURL");
            bankCode = (String) props.get("vs.IBAN_bankCode");
            branchCode = (String) props.get("vs.IBAN_branchCode");
            staticResURL = (String) props.get("vs.staticResourcesURL");
            serverDir =  new File((String) props.get("vs.staticResourcesPath"));
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
        try {
            MessagesVS.setCurrentInstance(Locale.getDefault(), getProperty("vs.bundleBaseName"));
            Query query = dao.getEM().createNamedQuery("findUserByType").setParameter("type", User.Type.SYSTEM);
            systemUser = dao.getSingleResult(User.class, query);
            if(systemUser == null) { //First time run
                dao.persist(new TagVS(TagVS.WILDTAG));
                systemUser = dao.persist(new User(systemNIF, User.Type.SYSTEM, serverName));
                createIBAN(systemUser);
                URL res = Thread.currentThread().getContextClassLoader().getResource("defaultTags.txt");
                String[] defaultTags = FileUtils.getStringFromInputStream(res.openStream()).split(",");
                for(String tag: defaultTags) {
                    createtagVS(tag.trim());
                }
            }
            ContextVS.getInstance().setTimeStampServiceURL(Actor.getTimeStampServiceURL(timeStampServerURL));
            executorService.submit(() -> {
                try {
                    LoggerVS.init(serverDir + "/logs");
                    timeStampBean.init();
                    cmsBean.init();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });

        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @PreDestroy private void shutdown() { log.info(" --------- shutdown ---------");}

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public Properties getProperties() {
        return props;
    }

    @Override
    public File getServerDir() {
        return serverDir;
    }

    @Override
    public TagVS getTag(String tagName) throws ValidationException {
        if(tagName.toLowerCase().equals(MessagesVS.getCurrentInstance().get("wildTagLbl"))) tagName = TagVS.WILDTAG;
        Query query = dao.getEM().createNamedQuery("findTagByName").setParameter("name", tagName.toUpperCase());
        return dao.getSingleResult(TagVS.class, query);
    }

    public TagVS createtagVS(String tagName) {
        TagVS tagVS =  dao.persist(new TagVS(StringUtils.removeAccents(tagName).toUpperCase()));
        //TODO dollar, yuan, yen ...
        dao.persist(new CurrencyAccount(systemUser, BigDecimal.ZERO,
                java.util.Currency.getInstance("EUR").getCurrencyCode(), tagVS));
        return tagVS;
    }

    @Override
    public User createIBAN(User user) throws ValidationException {
        String accountNumberStr = String.format("%010d", user.getId());
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCode).branchCode(branchCode)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        user.setIBAN(iban.toString());
        user = dao.merge(user);
        dao.persist(new CurrencyAccount(user, BigDecimal.ZERO,
                java.util.Currency.getInstance("EUR").getCurrencyCode(), getTag(TagVS.WILDTAG)));
        return user;
    }

    public String getIBAN(Long userId, String bankCodeStr, String branchCodeStr) {
        String accountNumberStr = String.format("%010d", userId);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCodeStr).branchCode(branchCodeStr)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return iban.toString();
    }

    public String validateIBAN(String IBAN) throws IbanFormatException, InvalidCheckDigitException, UnsupportedCountryException {
        IbanUtil.validate(IBAN);
        return IBAN;
    }

    public static String getTempPath() {
        return File.separator + "temp";
    }

    public User getSystemUser() {
        return systemUser;
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

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBranchCode() {
        return branchCode;
    }

    @Override
    public ControlCenter getControlCenter() {
        return null;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getSystemNIF() {
        return systemNIF;
    }

    @Override public void mainServletInitialized() throws Exception { }

    @Override public String getEmailAdmin() {
        return emailAdmin;
    }

}
