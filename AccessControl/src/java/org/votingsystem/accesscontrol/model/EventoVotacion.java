package org.votingsystem.accesscontrol.model;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.search.annotations.Indexed;
import org.votingsystem.signature.util.CertUtil;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="EventoVotacion")
@DiscriminatorValue("EventoVotacion")
@Indexed
public class EventoVotacion extends Evento implements Serializable {

    private static final long serialVersionUID = 1L;


    @Lob @Column(name="cadenaCertificacionCentroControl")
    private byte[] cadenaCertificacionCentroControl;
    @Enumerated(EnumType.STRING)
    @Column(name="cardinalidadOpciones")
    private Cardinalidad cardinalidadOpciones = Cardinalidad.UNA; 
  
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<OpcionDeEvento> opciones;

    /**
     * @return the opciones
     */
    public Set<OpcionDeEvento> getOpciones() {
        return opciones;
    }

    /**
     * @param opciones the opciones to set
     */
    public void setOpciones(Set<OpcionDeEvento> opciones) {
        this.opciones = opciones;
    }


    /**
     * @return the cardinalidadOpciones
     */
    public Cardinalidad getCardinalidadOpciones() {
        return cardinalidadOpciones;
    }

    /**
     * @param cardinalidadOpciones the cardinalidadOpciones to set
     */
    public void setCardinalidadOpciones(Cardinalidad cardinalidadOpciones) {
        this.cardinalidadOpciones = cardinalidadOpciones;
    }


	public byte[] getCadenaCertificacionCentroControl() {
		return cadenaCertificacionCentroControl;
	}

	public void setCadenaCertificacionCentroControl(
			byte[] cadenaCertificacionCentroControl) {
		this.cadenaCertificacionCentroControl = cadenaCertificacionCentroControl;
	}
	
	public OpcionDeEvento comprobarOpcionId(Long opcionId) {
		if(opcionId == null) return null;
		for(OpcionDeEvento opcion: opciones) {
			if(opcionId.longValue() == opcion.getId().longValue()) return opcion;
		}
		return null;
	}
	
	public X509Certificate getControlCenterCert() throws Exception {
		if(cadenaCertificacionCentroControl == null) return null;
		Collection<X509Certificate> controlCenterCertCollection = 
				CertUtil.fromPEMToX509CertCollection(cadenaCertificacionCentroControl);
		return controlCenterCertCollection.iterator().next();		
	}
	
}
