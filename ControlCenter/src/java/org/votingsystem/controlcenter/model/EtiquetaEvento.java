package org.votingsystem.controlcenter.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

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
    private EventoVotacion evento;

}


