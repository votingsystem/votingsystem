package org.votingsystem.web.cdi;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.EnvironmentVS;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ConfigVS {

    public String getTimeStampServerURL();

    public String getSystemNIF();

    public String getEmailAdmin();

    public String getProperty(String key);

    public TagVS getTag(String tagName) throws ValidationExceptionVS;

    public void setX509TimeStampServerCert(X509Certificate x509Cert);

    public void mainServletInitialized() throws Exception;

    public String getServerName();

    public String getContextURL();

    public String getWebSocketURL();

    public String getWebURL();

    public String getRestURL();

    public String getStaticResURL();

    public EnvironmentVS getMode();

    public Properties getProperties();

    public File getServerDir();

    public UserVS createIBAN(UserVS userVS) throws ValidationExceptionVS;

    public UserVS getSystemUser();

}
