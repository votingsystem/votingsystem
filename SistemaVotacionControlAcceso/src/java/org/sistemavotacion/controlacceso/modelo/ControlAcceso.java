package org.sistemavotacion.controlacceso.modelo;

import java.io.Serializable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="ControlAcceso")
@DiscriminatorValue("ControlAcceso")
public class ControlAcceso extends ActorConIP implements Serializable {

    public static final long serialVersionUID = 1L;


}
