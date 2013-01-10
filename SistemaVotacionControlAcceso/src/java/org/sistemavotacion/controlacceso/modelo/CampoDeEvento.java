package org.sistemavotacion.controlacceso.modelo;

import java.io.Serializable;
import static javax.persistence.GenerationType.IDENTITY;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="CampoDeEvento")
public class CampoDeEvento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoId", nullable=false)
    private Evento evento;

    @Column(name="contenido", nullable=false)
    private String contenido;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="campoDeEvento")
    private Set<ValorCampoDeEvento> valores = new HashSet<ValorCampoDeEvento>(0);
    
    public CampoDeEvento () {}

    /**
     * @return the contenido
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }


    public void setValores(Set<ValorCampoDeEvento> valores) {
            this.valores = valores;
    }

    public Set<ValorCampoDeEvento> getValores() {
            return valores;
    }

}
