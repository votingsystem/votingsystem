package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="EventVSManifest")
@DiscriminatorValue("EventVSManifest")
//@Indexed
public class EventVSManifest extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;

}
