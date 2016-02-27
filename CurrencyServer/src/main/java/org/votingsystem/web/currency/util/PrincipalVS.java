package org.votingsystem.web.currency.util;

import org.votingsystem.model.UserVS;

import java.security.Principal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PrincipalVS implements Principal {

    private UserVS userVS;

    public PrincipalVS(UserVS userVS) {
        this.userVS = userVS;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PrincipalVS && equals((PrincipalVS) other);
    }

    public boolean equals(PrincipalVS other) {
        return this == other || other != null && userVS.getNif().equals(other.getUserVS().getNif());
    }

    @Override
    public String toString() {
        return userVS.getNif() + " - id: " + userVS.getId();
    }

    @Override
    public int hashCode() {
        return userVS.getNif().hashCode();
    }

    @Override
    public String getName() {
        return userVS.getNif();
    }

    public UserVS getUserVS() {
        return userVS;
    }

}
