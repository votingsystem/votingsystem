package org.votingsystem.util;

import org.votingsystem.model.UserVS;

import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMECheck {

    private UserVS signer;
    private UserVS anonymousSigner;
    private Set<UserVS> signers;

    public SMIMECheck() {}

    public UserVS getSigner() {
        return signer;
    }

    public void setSigner(UserVS signer) {
        this.signer = signer;
        addSigner(signer);
    }

    public UserVS getAnonymousSigner() {
        return anonymousSigner;
    }

    public void setAnonymousSigner(UserVS anonymousSigner) {
        this.anonymousSigner = anonymousSigner;
        addSigner(anonymousSigner);
    }

    public Set<UserVS> getSigners() {
        return signers;
    }

    public void setSigners(Set<UserVS> signers) {
        this.signers = signers;
    }

    public void addSigner(UserVS signer) {
        if(signers == null) signers = new HashSet<>();
        signers.add(signer);
    }
}
