package org.sistemavotacion.controlacceso.modelo;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.GeneratedValue;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="EtiquetaEvento")
@IdClass(EtiquetaEventoPK.class) 
public class EtiquetaEvento  implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    private Etiqueta etiqueta;

    @Id
    private Evento evento;

}


