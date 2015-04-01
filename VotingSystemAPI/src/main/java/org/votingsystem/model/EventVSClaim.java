package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
//@Indexed
@Entity @Table(name="EventVSClaim") @DiscriminatorValue("EventVSClaim")
public class EventVSClaim extends EventVS implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public EventVSClaim() {}

    public EventVSClaim(UserVS userVS, String subject, String content, Boolean backupAvailable,
                        Cardinality cardinality, Date dateFinish) {
        super(userVS, subject, content, backupAvailable, cardinality, dateFinish);
    }

}
