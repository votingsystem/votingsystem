package org.votingsystem.currency.web.http;

import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;

import java.security.Principal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyPrincipal implements Principal {

    private User user;
    private SignedDocument signedDocument;

    public CurrencyPrincipal(User user) {
        this.user = user;
    }

    public CurrencyPrincipal(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CurrencyPrincipal && equals((CurrencyPrincipal) other);
    }

    public boolean equals(CurrencyPrincipal other) {
        return this == other || other != null && user.getNumIdAndType().equals(other.getUser().getNumIdAndType());
    }

    @Override
    public String toString() {
        return user.getNumIdAndType() + " - id: " + user.getId();
    }

    @Override
    public int hashCode() {
        return user.getNumIdAndType().hashCode();
    }

    @Override
    public String getName() {
        return user.getNumIdAndType();
    }

    public User getUser() {
        return user;
    }


    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
    }
}
