package org.votingsystem.web.currency.filter;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PrincipalVS implements Principal {

    public static final String USER_KEY = "USER_KEY";

    public static final Set<String> ADMIN_ROLES =  Collections.unmodifiableSet(new HashSet<>(Arrays.asList("USER", "ADMIN")));
    public static final Set<String> USER_ROLES =  Collections.unmodifiableSet(new HashSet<>(Arrays.asList("USER")));

    private User user;
    private CMSMessage cmsMessage;

    public PrincipalVS(User user) {
        this.user = user;
    }

    public PrincipalVS(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PrincipalVS && equals((PrincipalVS) other);
    }

    public boolean equals(PrincipalVS other) {
        return this == other || other != null && user.getNif().equals(other.getUser().getNif());
    }

    @Override
    public String toString() {
        return user.getNif() + " - id: " + user.getId();
    }

    @Override
    public int hashCode() {
        return user.getNif().hashCode();
    }

    @Override
    public String getName() {
        return user.getNif();
    }

    public User getUser() {
        return user;
    }

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }
}
