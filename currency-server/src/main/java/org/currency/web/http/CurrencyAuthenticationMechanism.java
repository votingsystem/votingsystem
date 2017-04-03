package org.currency.web.http;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.votingsystem.model.User;
import org.currency.web.util.AuthRole;
import org.votingsystem.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyAuthenticationMechanism implements AuthenticationMechanism {

    private static final Logger log = Logger.getLogger(CurrencyAuthenticationMechanism.class.getName());

    private final String mechanismName;

    public CurrencyAuthenticationMechanism(String mechanismName) {
        this.mechanismName = mechanismName;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest request = servletRequestContext.getOriginalRequest();
        User user = (User) request.getSession().getAttribute(Constants.USER_KEY);
        if(user == null)
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        Account account =  new AccountImpl(new CurrencyPrincipal(user), AuthRole.USER_ROLES);
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
        private CurrencyPrincipal principal;

        public AccountImpl(CurrencyPrincipal principal, Set<String> roles) {
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
            return new CurrencyAuthenticationMechanism(mechanismName);
        }
    }

}
