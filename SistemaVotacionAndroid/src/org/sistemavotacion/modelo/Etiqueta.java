package org.sistemavotacion.modelo;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class Etiqueta {

    private Long id;
    private String nombre;
    private Long frecuencia;
    private Date dateCreated;
    private Date lastUpdated;


    private Set<EventoEtiqueta> eventoEtiquetas = new HashSet<EventoEtiqueta>(0);

    public Etiqueta() { }
   
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

    public Set<EventoEtiqueta> getEventoEtiquetas() {
        return this.eventoEtiquetas;
    }
    
    public void setEventoEtiquetas(Set<EventoEtiqueta> eventoEtiquetas) {
        this.eventoEtiquetas = eventoEtiquetas;
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