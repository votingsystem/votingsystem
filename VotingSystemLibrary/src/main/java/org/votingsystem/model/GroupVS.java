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

    public enum State {ACTIVE, PENDING, SUSPENDED, CLOSED}

    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @ManyToOne(fetch=FetchType.LAZY)
    private UserVS groupRepresentative;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<UserVS> userVSSet;

    public Set<UserVS> getUserVSSet() {
        return userVSSet;
    }

    public void setUserVSSet(Set<UserVS> userVSSet) {
        this.userVSSet = userVSSet;
    }

    public UserVS getGroupRepresentative() {
        return groupRepresentative;
    }

    public void setGroupRepresentative(UserVS groupRepresentative) {
        this.groupRepresentative = groupRepresentative;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
