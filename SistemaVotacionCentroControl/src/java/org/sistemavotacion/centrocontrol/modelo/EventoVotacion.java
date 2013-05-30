package org.sistemavotacion.centrocontrol.modelo;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenizerDef;
import org.sistemavotacion.seguridad.CertUtil;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity@Indexed
@Table(name="EventoVotacion")
@AnalyzerDef(name="customAnalyzer",
	charFilters = { @CharFilterDef(factory = HTMLStripCharFilterFactory.class) }, 
	tokenizer =  @TokenizerDef(factory = StandardTokenizerFactory.class)
)
public class EventoVotacion implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public enum Estado {ACTIVO, FINALIZADO, CANCELADO, ACTORES_PENDIENTES_NOTIFICACION, PENDIENTE_COMIENZO,
    	PENDIENTE_DE_FIRMA, BORRADO_DE_SISTEMA}

    public enum CardinalidadOpciones { MULTIPLES, UNA}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    @DocumentId
    private Long id;
    @Column(name="eventoVotacionId",nullable=false)
    private String eventoVotacionId;
    @Enumerated(EnumType.STRING)
    @Column(name="tipo")
    private Tipo tipo;
    @Enumerated(EnumType.STRING)
    @Column(name="estado")
    @Field(index=Index.TOKENIZED, store=Store.YES)    
    private Estado estado;
    @Enumerated(EnumType.STRING)
    @Column(name="cardinalidadOpciones")
    private CardinalidadOpciones cardinalidadOpciones = CardinalidadOpciones.UNA;
    @Column(name="contenido", columnDefinition="TEXT")
    @Analyzer(definition = "customAnalyzer")
    private String contenido; 
    @Column(name="metaInf", columnDefinition="TEXT")
    private String metaInf = "{}"; 
    @Lob @Column(name="cadenaCertificacionControlAcceso")
    private byte[] cadenaCertificacionControlAcceso;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="controlAccesoId")
    private ControlAcceso controlAcceso;   
    @Column(name="asunto", length=1000)
    @Field(index=Index.TOKENIZED, store=Store.YES)
    private String asunto;
    @Column(name="url")
    private String url;
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="evento")
    private Set<MensajeSMIME> mensajeSMIMESet; 
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario; 
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<Certificado> certificados;       
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<OpcionDeEvento> opciones;
    //Owning Entity side of the relationship
    @JoinTable(name="EtiquetaEvento", 
        joinColumns = { 
            @JoinColumn(name = "eventoId", referencedColumnName = "id")
     	}, 
     	inverseJoinColumns = { 
            @JoinColumn(name = "etiquetaId", referencedColumnName = "id")
     	})
    @ManyToMany
    private Set<Etiqueta> etiquetaSet;
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventoVotacion")
    private Set<Voto> votos;  
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaInicio", length=23)
    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    @DateBridge(resolution = Resolution.HOUR)
    private Date fechaInicio;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaFin", length=23)
    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    @DateBridge(resolution = Resolution.HOUR)
    private Date fechaFin;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCancelacion", length=23)
    private Date dateCanceled;

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getContenido () {
        return contenido;
    }

    public void setContenido (String contenido) {
        this.contenido = contenido;
    }

    public String getAsunto () {
        return asunto;
    }

    public void setAsunto (String asunto) {
        this.asunto = asunto;
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

	public void setEtiquetaSet(Set<Etiqueta> etiquetaSet) {
		this.etiquetaSet = etiquetaSet;
	}

	public Set<Etiqueta> getEtiquetaSet() {
		return etiquetaSet;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setCardinalidadOpciones(CardinalidadOpciones cardinalidadOpciones) {
		this.cardinalidadOpciones = cardinalidadOpciones;
	}

	public CardinalidadOpciones getCardinalidadOpciones() {
		return cardinalidadOpciones;
	}

	public void setFechaInicio(Date fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public Date getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaFin(Date fechaFin) {
		this.fechaFin = fechaFin;
	}

	public Date getFechaFin() {
		return fechaFin;
	}

	public void setVotos(Set<Voto> votos) {
		this.votos = votos;
	}

	public Set<Voto> getVotos() {
		return votos;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setEventoVotacionId(String eventoVotacionId) {
		this.eventoVotacionId = eventoVotacionId;
	}

	public String getEventoVotacionId() {
		return eventoVotacionId;
	}
        
	public void setControlAcceso(ControlAcceso controlAcceso) {
		this.controlAcceso = controlAcceso;
	}

	public ControlAcceso getControlAcceso() {
		return controlAcceso;
	}

	public byte[] getCadenaCertificacionControlAcceso() {
		return cadenaCertificacionControlAcceso;
	}

	public void setCadenaCertificacionControlAcceso(
			byte[] cadenaCertificacionFirmasControlAcceso) {
		this.cadenaCertificacionControlAcceso = cadenaCertificacionFirmasControlAcceso;
	}

    /**
     * @return the estado
     */
    public Estado getEstado() {
        return estado;
    }

    /**
     * @param estado the estado to set
     */
    public void setEstado(Estado estado) {
        this.estado = estado;
    }

	public Set<Certificado> getCertificados() {
		return certificados;
	}

	public void setCertificados(Set<Certificado> certificados) {
		this.certificados = certificados;
	}
	
	public Date getDateCanceled() {
		return dateCanceled;
	}

	public void setDateCanceled(Date dateCanceled) {
		this.dateCanceled = dateCanceled;
	}
	
	public X509Certificate getControlAccesoCert() throws Exception {
		if(cadenaCertificacionControlAcceso == null) return null;
		Collection<X509Certificate> controlAccesoCertCollection = 
				CertUtil.fromPEMToX509CertCollection(cadenaCertificacionControlAcceso);
		return controlAccesoCertCollection.iterator().next();		
	}

	public Set<MensajeSMIME> getMensajeSMIMESet() {
		return mensajeSMIMESet;
	}

	public void setMensajeSMIMESet(Set<MensajeSMIME> mensajeSMIMESet) {
		this.mensajeSMIMESet = mensajeSMIMESet;
	}

	public String getMetaInf() {
		return metaInf;
	}

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}
}
