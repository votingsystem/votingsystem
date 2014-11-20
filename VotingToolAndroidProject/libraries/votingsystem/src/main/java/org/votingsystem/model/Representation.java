package org.votingsystem.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Representation implements Serializable{

    private static final long serialVersionUID = 1L;

    public enum State {REPRESENTATIVE, WITH_ANONYMOUS_REPRESENTATION, WITH_PUBLIC_REPRESENTATION,
        WITHOUT_REPRESENTATION;}

    private State state;
    private Date dateTo;
    private Date lastCheckedDate;
    private UserVS representative;

    public Representation() {}

    public Representation(Date lastCheckedDate, State state, UserVS representative, Date dateTo) {
        this.state = state;
        this.dateTo = dateTo;
        this.lastCheckedDate = lastCheckedDate;
        this.representative = representative;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public UserVS getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserVS representative) {
        this.representative = representative;
    }

    public Date getLastCheckedDate() {
        return lastCheckedDate;
    }

    public void setLastCheckedDate(Date lastCheckedDate) {
        this.lastCheckedDate = lastCheckedDate;
    }

}
