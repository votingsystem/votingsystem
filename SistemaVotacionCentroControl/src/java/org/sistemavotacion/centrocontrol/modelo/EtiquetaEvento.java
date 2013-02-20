package org.sistemavotacion.centrocontrol.modelo;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
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


