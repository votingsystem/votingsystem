package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

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
