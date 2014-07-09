package org.votingsystem.model;

import javax.persistence.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity @Table(name="GroupVS") @DiscriminatorValue("GroupVS")
public class GroupVS extends UserVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public void beforeInsert() {
        if(getType() == null) setType(Type.GROUP);
    }

}
