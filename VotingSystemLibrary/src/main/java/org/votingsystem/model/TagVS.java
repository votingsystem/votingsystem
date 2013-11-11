package org.votingsystem.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TagVS {

    private Long id;
    private String nombre;
    private Long frecuencia;
    private Date dateCreated;
    private Date lastUpdated;


    private Set<EventTagVS> eventTagVSes = new HashSet<EventTagVS>(0);

    public TagVS() { }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return this.nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<EventTagVS> getEventTagVSes() {
        return this.eventTagVSes;
    }
    
    public void setEventTagVSes(Set<EventTagVS> eventTagVSes) {
        this.eventTagVSes = eventTagVSes;
    }

    public void setFrecuencia(Long frecuencia) {
        this.frecuencia = frecuencia;
    }

    public Long getFrecuencia() {
        return frecuencia;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

}