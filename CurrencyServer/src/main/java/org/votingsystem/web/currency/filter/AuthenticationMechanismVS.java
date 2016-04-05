package org.votingsystem.web.currency.filter;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.votingsystem.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AuthenticationMechanismVS implements AuthenticationMechanism {

    private static final Logger log = Logger.getLogger(AuthenticationMechanismVS.class.getName());

    private final String mechanismName;

    public AuthenticationMechanismVS(String mechanismName) {
        this.mechanismName = mechanismName;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        //log.info("authenticate");
        ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest request = servletRequestContext.getOriginalRequest();
        User user = (User) request.getSession().getAttribute(PrincipalVS.USER_KEY);
        if(user == null) return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        Account account =  new AccountImpl(new PrincipalVS(user), PrincipalVS.USER_ROLES);
        securityContext.authenticationComplete(account, mechanismName, true);
        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange httpServerExchange, SecurityContext securityContext) {
        return new ChallengeResult(true, HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean isLocalhost(final HttpServletRequest req) {
        return req.getLocalAddr().equals(req.getRemoteAddr());
    }

    public static final class AccountImpl implements Account {

        private Set<String> roles;
        private PrincipalVS principal;

        public AccountImpl(PrincipalVS principal, Set<String> roles) {
            this.principal = principal;
            this.roles = roles;
        }

        @Override public Principal getPrincipal() {
            return principal;
        }

        @Override
        public Set<String> getRoles() {
            return roles;
        }
    }

    public static final class Factory implements AuthenticationMechanismFactory {
        @Override
        public AuthenticationMechanism create(String mechanismName, FormParserFactory formParserFactory, Map<String, String> properties) {
            return new AuthenticationMechanismVS(mechanismName);
        }
    }

}
