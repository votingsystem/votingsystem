package org.votingsystem.web.currency.util;

import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import org.votingsystem.web.currency.filter.AuthenticationMechanismVS;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class StartupExtension implements ServletExtension {

    private static final Logger log = Logger.getLogger(StartupExtension.class.getName());

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        log.info("handleDeployment");
        deploymentInfo.addAuthenticationMechanism("CURRENCY_SERVER_LOGIN", new AuthenticationMechanismVS.Factory());
        Map<String, AuthenticationMechanismFactory> map = deploymentInfo.getAuthenticationMechanisms();
        map.keySet().stream().forEach(m -> log.info("--- AuthenticationMechanism: " + m));
    }

}
