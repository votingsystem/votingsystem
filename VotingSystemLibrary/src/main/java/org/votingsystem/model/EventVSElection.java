package org.votingsystem.model;

import org.votingsystem.signature.util.CertUtil;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
//@Indexed
@Entity @Table(name="EventVSElection") @DiscriminatorValue("EventVSElection")
public class EventVSElection extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public FieldEventVS checkOptionId(Long opcionId) {
        if(opcionId == null) return null;
        for(FieldEventVS opcion: getFieldsEventVS()) {
            if(opcionId.longValue() == opcion.getId().longValue()) return opcion;
        }
        return null;
    }

}
