package org.votingsystem.web.cdi;

import org.votingsystem.model.TagVS;
import org.votingsystem.util.EnvironmentVS;

import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ConfigVS {

    public String getTimeStampServerURL();

    public String getSystemNIF();

    public String getProperty(String key);

    public TagVS getTag(String tagName);

    public void setX509TimeStampServerCert(X509Certificate x509Cert);

    public String getServerName();

    public String getContextURL();

    public String getWebSocketURL();

    public String getWebURL();

    public String getRestURL();

    public EnvironmentVS getMode();

    public Properties getProperties();

}
