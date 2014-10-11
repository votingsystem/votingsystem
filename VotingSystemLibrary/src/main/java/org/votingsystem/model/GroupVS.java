package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity @Table(name="GroupVS") @DiscriminatorValue("GroupVS")
public class GroupVS extends UserVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public void beforeInsert() {
        setType(Type.GROUP);
    }

}
