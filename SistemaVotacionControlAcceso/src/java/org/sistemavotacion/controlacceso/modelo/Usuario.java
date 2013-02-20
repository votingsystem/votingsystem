package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.cert.X509Certificate;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="Usuario")
public class Usuario implements Serializable {
	
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = LoggerFactory.getLogger(Usuario.class);

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    
    @Column(name="nif", nullable=false)
    private String nif;

    @Column(name="nombre" )
    private String nombre;
    
    @Column(name="primerApellido" )
    private String primerApellido;
    
    @Column(name="pais" )
    private String pais;
    
    @Column(name="telefono" )
    private String telefono;
    
    @Column(name="email" )
    private String email;

    @Column(name="cn")
    private String cn;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;
    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="usuario")
    private Set<Evento> eventos = new HashSet<Evento>(0);
    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="usuario")
    private Set<Certificado> certificados = new HashSet<Certificado>(0);
    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="usuario")
    private Set<Dispositivo> dispositivo = new HashSet<Dispositivo>(0);
    
    @Transient
    private X509Certificate certificate;
    
    @Transient
    private Certificado certificadoCA;

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
    	logger.debug("getUsuario: " +subjectDN);
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

    public void setCertificados(Set<Certificado> certificados) {
        this.certificados = certificados;
    }

    public Set<Certificado> getCertificados() {
        return certificados;
    }

    public void setEventos(Set<Evento> eventos) {
        this.eventos = eventos;
    }

    public Set<Evento> getEventos() {
        return eventos;
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

	public Set<Dispositivo> getDispositivo() {
		return dispositivo;
	}

	public void setDispositivo(Set<Dispositivo> dispositivo) {
		this.dispositivo = dispositivo;
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
	
}