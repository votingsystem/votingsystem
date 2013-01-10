package org.sistemavotacion.centrocontrol.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
@Entity
@Table(name="Certificado")
public class Certificado implements Serializable {
	
    public enum Estado {OK, CON_ERRORES, ANULADO, UTILIZADO}

    public enum Tipo {RAIZ_VOTOS, VOTO, USUARIO, AUTORIDAD_CERTIFICADORA}
    
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = LoggerFactory.getLogger(Certificado.class);
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;

    @Column(name="numeroSerie", nullable=false)
    private Long numeroSerie;
   
    @Column(name="contenido", nullable=false)
    @Lob
    private byte[] contenido;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actorConIPId")
    private ActorConIP actorConIP;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoVotacionId")
    private EventoVotacion eventoVotacion;
    
    @Column(name="hashCertificadoVotoBase64")
    private String hashCertificadoVotoBase64;   
    
    @Enumerated(EnumType.STRING)
    @Column(name="tipo", nullable=false)
    private Tipo tipo;

    @Column(name="esRaiz", nullable=false, columnDefinition="BOOLEAN default false")
    private Boolean esRaiz = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name="estado", nullable=false)
    private Estado estado;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23, insertable=true)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23, insertable=true)
    private Date lastUpdated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="validoDesde", length=23, insertable=true)
    private Date validoDesde;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="validoHasta", length=23, insertable=true)
    private Date validoHasta;
    
    @Transient
    private String eventoId;
    
    @Transient
    private String serverURL;
    
    public Certificado () {}
    
    public Certificado (X509Certificate certificate) {
    	String subjectDN = certificate.getSubjectDN().getName();
    	logger.debug("Certificado - subjectDN: " +subjectDN);
    	if(subjectDN.split("OU=eventoId:").length > 1) {
    		setEventoId(subjectDN.split("OU=eventoId:")[1].split(",")[0]);
    	}
    	if(subjectDN.split("CN=controlAccesoURL:").length > 1) {
    		String parte = subjectDN.split("CN=controlAccesoURL:")[1];
    		logger.debug("Certificado - parte: " + parte);
    		if (parte.split(",").length > 1) {
    			serverURL = parte.split(",")[0];
    		} else serverURL = parte;
    	}
    	if (subjectDN.split("OU=hashCertificadoVotoHEX:").length > 1) {
    		String hashCertificadoVotoHEX = subjectDN.split("OU=hashCertificadoVotoHEX:")[1].split(",")[0];
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();     
			hashCertificadoVotoBase64 = new String(
					hexConverter.unmarshal(hashCertificadoVotoHEX));
    	}
    }
    
    
    /**
     * @return the contenido
     */
    public byte[] getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    /**
     * @return the usuario
     */
    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * @param usuario the usuario to set
     */
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * @return the id
     */
    public Long getNumeroSerie() {
        return numeroSerie;
    }

    /**
     * @param id the id to set
     */
    public void setNumeroSerie(Long numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public Boolean getEsRaiz() {
		return esRaiz;
	}

	public void setEsRaiz(Boolean esRaiz) {
		this.esRaiz = esRaiz;
	}

	public String getHashCertificadoVotoBase64() {
		return hashCertificadoVotoBase64;
	}

	public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
		this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	public EventoVotacion getEventoVotacion() {
		return eventoVotacion;
	}

	public void setEventoVotacion(EventoVotacion eventoVotacion) {
		this.eventoVotacion = eventoVotacion;
	}

	public ActorConIP getActorConIP() {
		return actorConIP;
	}

	public void setActorConIP(ActorConIP actorConIP) {
		this.actorConIP = actorConIP;
	}


	public String getEventoId() {
		return eventoId;
	}


	public void setEventoId(String eventoId) {
		this.eventoId = eventoId;
	}


	public String getServerURL() {
		return serverURL;
	}


	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public Date getValidoDesde() {
		return validoDesde;
	}

	public void setValidoDesde(Date validoDesde) {
		this.validoDesde = validoDesde;
	}

	public Date getValidoHasta() {
		return validoHasta;
	}

	public void setValidoHasta(Date validoHasta) {
		this.validoHasta = validoHasta;
	}

}
