package org.votingsystem.web.util;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.throwable.ValidationExceptionVS;
import java.io.File;
import java.util.Properties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ConfigVS  {

    public String getTimeStampServerURL();

    public String getSystemNIF();

    public String getEmailAdmin();

    public String getProperty(String key);

    public TagVS getTag(String tagName) throws ValidationExceptionVS;

    public void mainServletInitialized() throws Exception;

    public String getServerName();

    public String getContextURL();

    public String getWebSocketURL();

    public String getStaticResURL();

    public Properties getProperties();

    public File getServerDir();

    public User createIBAN(User user) throws ValidationExceptionVS;

    public User getSystemUser();

    public String validateIBAN(String IBAN) throws Exception;

    public String getBankCode();

    public String getBranchCode();

    public ControlCenter getControlCenter();

}
