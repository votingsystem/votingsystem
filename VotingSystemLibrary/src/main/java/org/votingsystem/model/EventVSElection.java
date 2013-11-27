package org.votingsystem.model;

import org.hibernate.search.annotations.Indexed;
import org.votingsystem.signature.util.CertUtil;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Indexed
@Entity @Table(name="EventVSElection") @DiscriminatorValue("EventVSElection")
public class EventVSElection extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;


    @Lob @Column(name="certChainControlCenter") private byte[] certChainControlCenter;
    @Lob @Column(name="certChainAccessControl") private byte[] certChainAccessControl;

    @Transient private Type type = Type.ELECTION;

    @Override public Type getType() { return type; }

    @Override public void setType(Type type) { this.type = type; }


	public byte[] getCertChainControlCenter() {
		return certChainControlCenter;
	}

	public void setCertChainControlCenter(
			byte[] certChainControlCenter) {
		this.certChainControlCenter = certChainControlCenter;
	}
	
	public FieldEventVS checkOptionId(Long opcionId) {
		if(opcionId == null) return null;
		for(FieldEventVS opcion: getFieldsEventVS()) {
			if(opcionId.longValue() == opcion.getId().longValue()) return opcion;
		}
		return null;
	}
	
	public X509Certificate getControlCenterCert() throws Exception {
		if(certChainControlCenter == null) return null;
		Collection<X509Certificate> controlCenterCertCollection = 
				CertUtil.fromPEMToX509CertCollection(certChainControlCenter);
		return controlCenterCertCollection.iterator().next();		
	}

    public X509Certificate getAccessControlCert() throws Exception {
        if(certChainAccessControl == null) return null;
        Collection<X509Certificate> accessControlCertCollection =
                CertUtil.fromPEMToX509CertCollection(certChainAccessControl);
        return accessControlCertCollection.iterator().next();
    }

    public byte[] getCertChainAccessControl() {
        return certChainAccessControl;
    }

    public void setCertChainAccessControl(byte[] certChainAccessControl) {
        this.certChainAccessControl = certChainAccessControl;
    }


}
