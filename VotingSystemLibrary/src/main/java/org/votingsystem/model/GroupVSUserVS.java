package org.votingsystem.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="GroupVSUserVS")
@IdClass(GroupVSUserVS_PK.class)
public class GroupVSUserVS  implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id private GroupVS groupVS;

    @Id private UserVS userVS;

}

