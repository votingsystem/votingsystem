package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity @Table(name="VicketSource") @DiscriminatorValue("VicketSource")
public class VicketSource extends UserVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public void beforeInsert() {
        if(getType() == null) setType(Type.VICKET_SOURCE);
    }
}
