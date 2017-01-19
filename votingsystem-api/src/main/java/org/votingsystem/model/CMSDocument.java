package org.votingsystem.model;

import org.votingsystem.crypto.cms.CMSSignedMessage;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@DiscriminatorValue("CMS_DOCUMENT")
public class CMSDocument extends SignedDocument implements Serializable {

    private static Logger log = Logger.getLogger(CMSDocument.class.getName());

    private static final long serialVersionUID = 1L;

    @Transient private transient CMSSignedMessage cmsMessage;

    public CMSDocument() {}

    public CMSDocument(CMSSignedMessage cmsMessage) throws Exception {
        setCMS(cmsMessage);
    }

    public CMSSignedMessage getCMS() throws Exception {
		if(cmsMessage == null && getBody() != null)
		    cmsMessage = CMSSignedMessage.FROM_PEM(getBody().getBytes());
		return cmsMessage;
	}

	public CMSDocument setCMS(CMSSignedMessage cmsMessage) throws Exception {
		this.cmsMessage = cmsMessage;
		setBody(new String(cmsMessage.toPEM()));
		setMessageDigest(cmsMessage.getContentDigestStr());
        return this;
	}

    public CMSSignedMessage getCmsMessage() {
        return cmsMessage;
    }

    public CMSDocument setCmsMessage(CMSSignedMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
        return this;
    }

}