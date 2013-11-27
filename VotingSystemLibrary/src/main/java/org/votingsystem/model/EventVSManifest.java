package org.votingsystem.model;

import org.hibernate.search.annotations.Indexed;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="EventVSManifest")
@DiscriminatorValue("EventVSManifest")
@Indexed
public class EventVSManifest extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;


    private Type type = Type.MANIFEST;

    public Type getType() { return type; }

    public void setType(Type type) { this.type = type; }

}
