package org.votingsystem.dto;

import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.Signature;

import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CMSDto {

    private Signature firstSignature;
    private Signature anonymousSignature;
    private Set<Signature> signatures;
    private CMSDocument cmsDocument;

    public CMSDto() {}


    public void addSignature(Signature signature) {
        if(signatures == null) signatures = new HashSet<>();
        signatures.add(signature);
    }

    public CMSDocument getCmsDocument() {
        return cmsDocument;
    }

    public CMSDto setCmsDocument(CMSDocument cmsDocument) {
        this.cmsDocument = cmsDocument;
        return this;
    }

    public Signature getFirstSignature() {
        return firstSignature;
    }

    public CMSDto setFirstSignature(Signature firstSignature) {
        this.firstSignature = firstSignature;
        return this;
    }

    public Signature getAnonymousSignature() {
        return anonymousSignature;
    }

    public CMSDto setAnonymousSignature(Signature anonymousSignature) {
        this.anonymousSignature = anonymousSignature;
        return this;
    }

    public Set<Signature> getSignatures() {
        return signatures;
    }

    public CMSDto setSignatures(Set<Signature> signatures) {
        this.signatures = signatures;
        return this;
    }

}
