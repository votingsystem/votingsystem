package org.votingsystem.controlcenter.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.mail.MessagingException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* Clase para tratar los messagemados recibidos en el servidor
*/
@Entity
@Table(name="MessageSMIME")
public class MessageSMIME implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false)
    private TypeVS type;
    @Column(name="contenido")
    @Lob
    private byte[] contenido;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoId")
    private EventoVotacion evento;
    
    @OneToOne(mappedBy="messageSMIME")
    private Voto voto;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="smimePadreId")
    private MessageSMIME smimePadre;
    
    @Column(name="valido", nullable=false, columnDefinition="BOOLEAN default false")
    private Boolean valido;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23, insertable=true)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23, insertable=true)
    private Date lastUpdated;
    
    
    @Column(name="motivo", columnDefinition="TEXT") 
    private String motivo;
    
    //To avoid repeated messages
    @Column(name="base64ContentDigest") 
    private String base64ContentDigest;
    
    @Transient
    private transient SMIMEMessageWrapper smimeMessage;
    
    @Transient
    private Set<Usuario> signers;
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
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
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

	public void setType(TypeVS type) {
		this.type = type;
	}

	public TypeVS getType() {
		return type;
	}

	public void setValido(Boolean valido) {
		this.valido = valido;
	}

	public Boolean getValido() {
		return valido;
	}

	public Voto getVoto() {
		return voto;
	}

	public void setVoto(Voto voto) {
		this.voto = voto;
	}
	
	public MessageSMIME getSmimePadre() {
		return smimePadre;
	}

	public void setSmimePadre(MessageSMIME smimePadre) {
		this.smimePadre = smimePadre;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public String getBase64ContentDigest() {
		return base64ContentDigest;
	}

	public void setBase64ContentDigest(String base64ContentDigest) {
		this.base64ContentDigest = base64ContentDigest;
	}

	public Set<Usuario> getSigners() {
		return signers;
	}

	public void setSigners(Set<Usuario> signers) {
		this.signers = signers;
	}

	public SMIMEMessageWrapper getSmimeMessage() throws Exception {
		if(smimeMessage == null && contenido != null) {
			smimeMessage = new SMIMEMessageWrapper(
				new ByteArrayInputStream(contenido));
		}
		return smimeMessage;
	}

	public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) throws IOException, MessagingException {
		this.smimeMessage = smimeMessage;
		this.contenido = smimeMessage.getBytes();
	}

	public EventoVotacion getEvento() {
		return evento;
	}

	public void setEvento(EventoVotacion evento) {
		this.evento = evento;
	}

}
