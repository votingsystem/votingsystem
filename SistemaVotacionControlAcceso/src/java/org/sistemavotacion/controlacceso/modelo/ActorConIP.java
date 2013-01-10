package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.security.cert.X509Certificate;
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
@Entity
@Table(name="ActorConIP")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="tipoDeActor",
    discriminatorType=DiscriminatorType.STRING
)
@DiscriminatorValue("ActorConIP")
public class ActorConIP implements Serializable {

    public static final long serialVersionUID = 1L;
    
    public enum Estado {SUSPENDIDO, ACTIVO, INACTIVO}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    
    @Column(name="serverURL")
    private String serverURL;
    
    @Column(name="nombre")
    private String nombre;    
    
    @Enumerated(EnumType.STRING)
    @Column(name="estado")
    private Estado estado;
   
    @Transient
    private Tipo tipoServidor;
    
    @Transient
    private X509Certificate certificadoX509;
    
    @Transient
    private Certificado certificado;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23, insertable=true)
    public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23, insertable=true)
    public Date lastUpdated;

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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

    public void setServerURL(String serverURL) {
            this.serverURL = serverURL;
    }

    public String getServerURL() {
            return serverURL;
    }
    

    /**
     * @return the certificadoX509
     */
    public X509Certificate getCertificadoX509() {
        return certificadoX509;
    }

    /**
     * @param certificadoX509 the certificadoX509 to set
     */
    public void setCertificadoX509(X509Certificate certificadoX509) {
        this.certificadoX509 = certificadoX509;
    }

    /**
     * @return the certificado
     */
    public Certificado getCertificado() {
        return certificado;
    }

    /**
     * @param certificado the certificado to set
     */
    public void setCertificado(Certificado certificado) {
        this.certificado = certificado;
    }    
    
    /**
     * @return the tipoServidor
     */
    public Tipo getTipoServidor() {
        return tipoServidor;
    }

    /**
     * @param tipoServidor the tipoServidor to set
     */
    public void setTipoServidor(Tipo tipoServidor) {
        this.tipoServidor = tipoServidor;
    }
}
