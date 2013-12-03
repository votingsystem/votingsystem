package org.votingsystem.model;

import org.hibernate.search.annotations.Indexed;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Indexed
@Entity @Table(name="EventVSClaim") @DiscriminatorValue("EventVSClaim")
public class EventVSClaim extends EventVS implements Serializable {
    
    private static final long serialVersionUID = 1L;

}
