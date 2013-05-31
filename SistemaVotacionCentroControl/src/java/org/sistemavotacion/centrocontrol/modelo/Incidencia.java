package org.sistemavotacion.centrocontrol.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="Incidencia")
public class Incidencia implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public enum Tipo {SOLICITUD_CON_ERRORES, ANULACION_SESION_VOTO_OK, ANULACION_SESION_VOTO_ERROR}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="tipo", nullable=false)
    private Tipo tipo;  
    

	
    public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}
	public Tipo getTipo() {
		return tipo;
	}


}
