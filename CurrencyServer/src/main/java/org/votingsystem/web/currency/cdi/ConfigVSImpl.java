package org.votingsystem.web.currency.cdi;

import org.iban4j.*;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.web.currency.ejb.AuditBean;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

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
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Calendar;
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

    private static final Logger log = Logger.getLogger(ConfigVS.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionBean;
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
    private EnvironmentVS mode;
    private Properties props;
    private String bankCode = null;
    private String  branchCode = null;
    private String emailAdmin = null;
    private String staticResURL = null;
    private File serverDir = null;
    private X509Certificate x509TimeStampServerCert;
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
                    resourceFile = "CurrencyServer_DEVELOPMENT.properties";
                    break;
                case PRODUCTION:
                    resourceFile = "CurrencyServer_PRODUCTION.properties";
                    break;
            }
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
            Query query = dao.getEM().createNamedQuery("findUserByType").setParameter("type", UserVS.Type.SYSTEM);
            systemUser = dao.getSingleResult(UserVS.class, query);
            if(systemUser == null) { //First time run
                dao.persist(new TagVS(TagVS.WILDTAG));
                UserVS userVS = new UserVS(systemNIF, UserVS.Type.SYSTEM, serverName);
                systemUser = dao.persist(userVS);
                createIBAN(systemUser);
                URL res = res = Thread.currentThread().getContextClassLoader().getResource("defaultTags.txt");
                String[] defaultTags = FileUtils.getStringFromInputStream(res.openStream()).split(",");
                for(String tag: defaultTags) {
                    createtagVS(tag.trim());
                }
            }
            new ContextVS(null, null);
            executorService.submit(() -> {
                try {
                    LoggerVS.init(serverDir + "/logs");
                    timeStampBean.init();
                    signatureBean.init();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @PreDestroy private void shutdown() { log.info(" --------- shutdown ---------");}

    //@Schedule(dayOfWeek = "Mon", hour="0")
    public void initWeekPeriod() throws IOException {
        auditBean.checkCurrencyCanceled();
        auditBean.initWeekPeriod(Calendar.getInstance());
    }

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
    public TagVS getTag(String tagName) throws ValidationExceptionVS {
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

    public void setX509TimeStampServerCert(X509Certificate x509TimeStampServerCert) {
        this.x509TimeStampServerCert = x509TimeStampServerCert;
    }

    @Override
    public UserVS createIBAN(UserVS userVS) throws ValidationExceptionVS {
        String accountNumberStr = String.format("%010d", userVS.getId());
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCode).branchCode(branchCode)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        userVS.setIBAN(iban.toString());
        userVS = dao.merge(userVS);
        dao.persist(new CurrencyAccount(userVS, BigDecimal.ZERO,
                java.util.Currency.getInstance("EUR").getCurrencyCode(), getTag(TagVS.WILDTAG)));
        return userVS;
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

    public UserVS getSystemUser() {
        return systemUser;
    }

    public EnvironmentVS getMode() {
        return mode;
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
    public ControlCenterVS getControlCenter() {
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
