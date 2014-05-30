package org.votingsystem.model;

import javax.persistence.*;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity @Table(name="GroupVS") @DiscriminatorValue("GroupVS")
public class GroupVS extends UserVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="groupRepresentative") private UserVS groupRepresentative;

    public UserVS getGroupRepresentative() {
        return groupRepresentative;
    }

    public void setGroupRepresentative(UserVS groupRepresentative) {
        this.groupRepresentative = groupRepresentative;
    }

    public void beforeInsert() {
        if(getType() == null) setType(Type.GROUP);
    }

}
