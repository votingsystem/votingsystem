package org.votingsystem.model;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="IncidenceVS")
public class IncidenceVS implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public enum Type {REQUEST_ERROR}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false)
    private Type type;
	
    public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public Type getType() {
		return type;
	}


}
