package org.votingsystem.model;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class GroupVSUserVS_PK implements Serializable {

    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "user", referencedColumnName = "ID")
    private UserVS userVS;

    @ManyToOne
    @JoinColumn(name = "group", referencedColumnName = "ID")
    private GroupVS groupVS;

}
