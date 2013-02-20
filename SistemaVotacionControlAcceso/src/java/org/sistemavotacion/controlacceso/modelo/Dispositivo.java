package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="Dispositivo")
public class Dispositivo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;
    
    @Column(name="deviceId" )
    private String deviceId;
    
    @Column(name="email" )
    private String email;
    
    @Column(name="telefono" )
    private String telefono;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
}
