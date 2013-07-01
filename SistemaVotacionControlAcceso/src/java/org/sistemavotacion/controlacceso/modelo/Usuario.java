package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.bouncycastle.tsp.TimeStampToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="Usuario")
@DiscriminatorValue("Usuario")
public class Usuario implements Serializable {
	
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = LoggerFactory.getLogger(Usuario.class);
    
	 public enum Type {USER, REPRESENTATIVE, USER_WITH_CANCELLED_REPRESENTATIVE, EX_REPRESENTATIVE}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
	@Enumerated(EnumType.STRING)
	@Column(name="type", nullable=false)
	private Type type;
    @Column(name="nif", nullable=false)
    private String nif;

    @Column(name="nombre" )
    private String nombre;
    
    @Column(name="metaInf", columnDefinition="TEXT")
    private String metaInf = "{\"numRepresentations\"=1}"; 
    
    @Column(name="primerApellido" )
    private String primerApellido;
    
    @Column(name="representativeMessageId" )
    private MensajeSMIME representativeMessage;
    
    @Column(name="info", columnDefinition="TEXT")
    private String info; 
    
    @Column(name="pais" )
    private String pais;
    
    @Column(name="telefono" )
    private String telefono;
    
    @Column(name="email" )
    private String email;

    @Column(name="cn")
    private String cn;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="representativeId")
    private Usuario representative;  
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="representativeRegisterDate", length=23)
    private Date representativeRegisterDate;
    
    @Transient
    private transient X509Certificate certificate;
    
    @Transient
    private transient Certificado certificadoCA;
    
    @Transient
    private transient TimeStampToken timeStampToken;

   /**
     * @return the id
     */
    public String getNif() {
        return nif;
    }

    /**
     * @param id the id to set
     */
    public void setNif(String nif) {
        this.nif = nif;
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
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre the nombre to set
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public static Usuario getUsuario (X509Certificate certificate) {
    	Usuario usuario = new Usuario();
    	usuario.setCertificate(certificate);
    	String subjectDN = certificate.getSubjectDN().getName();
    	if (subjectDN.contains("C="))
    		usuario.setPais(subjectDN.split("C=")[1].split(",")[0]);
    	if (subjectDN.contains("SERIALNUMBER="))
    		usuario.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
    	if (subjectDN.contains("SURNAME="))
    		usuario.setPrimerApellido(subjectDN.split("SURNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("GIVENNAME="))
    		usuario.setNombre(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("CN="))
    		usuario.setCn(subjectDN.split("CN=")[1]);
		if(subjectDN.split("OU=email:").length > 1) {
			usuario.setEmail(subjectDN.split("OU=email:")[1].split(",")[0]);
		}
		if(subjectDN.split("CN=nif:").length > 1) {
			String nif = subjectDN.split("CN=nif:")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
			usuario.setNif(nif);
		}
		if (subjectDN.split("OU=telefono:").length > 1) {
			usuario.setTelefono(subjectDN.split("OU=telefono:")[1].split(",")[0]);
		}
    	return usuario;
    }
    
    public String getDescription () {
    	String result = "";
    	if (pais != null) result.concat(" - Pais: " + pais);
    	if (nif != null) result.concat(" - Nif: " + nif);
    	if (primerApellido != null) result.concat(" - Apellido: " + primerApellido);
    	if (nombre != null) result.concat(" - Nombre: " + nombre);
    	if (cn != null) result.concat(" - CN: " + cn);
    	return result;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getPais() {
        return pais;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getCn() {
        return cn;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public void beforeInsert(){
		if(nif != null) this.nif = nif.toUpperCase();
	}

	public Certificado getCertificadoCA() {
		return certificadoCA;
	}

	public void setCertificadoCA(Certificado certificadoCA) {
		this.certificadoCA = certificadoCA;
	}

	public Usuario getRepresentative() {
		return representative;
	}

	public void setRepresentative(Usuario representative) {
		this.representative = representative;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public MensajeSMIME getRepresentativeMessage() {
		return representativeMessage;
	}

	public void setRepresentativeMessage(MensajeSMIME representativeMessage) {
		this.representativeMessage = representativeMessage;
	}

	public TimeStampToken getTimeStampToken() {
		return timeStampToken;
	}

	public void setTimeStampToken(TimeStampToken timeStampToken) {
		this.timeStampToken = timeStampToken;
	}

	public String getMetaInf() {
		return metaInf;
	}

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}

	public Date getRepresentativeRegisterDate() {
		return representativeRegisterDate;
	}

	public void setRepresentativeRegisterDate(Date representativeRegisterDate) {
		this.representativeRegisterDate = representativeRegisterDate;
	}

}