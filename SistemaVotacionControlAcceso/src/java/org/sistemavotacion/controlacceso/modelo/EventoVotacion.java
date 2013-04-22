package org.sistemavotacion.controlacceso.modelo;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
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
import org.sistemavotacion.seguridad.CertUtil;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
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
    private Set<Certificado> certificados = new HashSet<Certificado>(0);    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<OpcionDeEvento> opciones = new HashSet<OpcionDeEvento>(0);
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<SolicitudAcceso> solicitudesAcceso = new HashSet<SolicitudAcceso>(0);
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<SolicitudCSRVoto> solicitudesCSR = new HashSet<SolicitudCSRVoto>(0);
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<Voto> votos = new HashSet<Voto>(0);
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

    public void setSolicitudesAcceso(Set<SolicitudAcceso> SolicitudesAcceso) {
        this.solicitudesAcceso = SolicitudesAcceso;
    }

    public Set<SolicitudAcceso> getSolicitudesAcceso() {
        return solicitudesAcceso;
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

	public Set<SolicitudCSRVoto> getSolicitudesCSR() {
		return solicitudesCSR;
	}

	public void setSolicitudesCSR(
			Set<SolicitudCSRVoto> solicitudesCSR) {
		this.solicitudesCSR = solicitudesCSR;
	}

	public byte[] getCadenaCertificacionCentroControl() {
		return cadenaCertificacionCentroControl;
	}

	public void setCadenaCertificacionCentroControl(
			byte[] cadenaCertificacionCentroControl) {
		this.cadenaCertificacionCentroControl = cadenaCertificacionCentroControl;
	}

	public Set<Certificado> getCertificados() {
		return certificados;
	}

	public void setCertificados(Set<Certificado> certificados) {
		this.certificados = certificados;
	}

	public Set<Voto> getVotos() {
		return votos;
	}

	public void setVotos(Set<Voto> votos) {
		this.votos = votos;
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
