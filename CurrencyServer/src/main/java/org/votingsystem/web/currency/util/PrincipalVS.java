package org.votingsystem.web.currency.util;

import org.votingsystem.model.User;

import java.security.Principal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PrincipalVS implements Principal {

    private User user;

    public PrincipalVS(User user) {
        this.user = user;
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

}
