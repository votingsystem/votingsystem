package org.sistemavotacion.modelo;

import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Mensaje {

    private static final long serialVersionUID = 1L;


    private Long id;
    private String contenido;
    private String asunto;
    private Usuario usuarioFrom;
    private Usuario usuarioTo;
    private Date dateCreated;
    private Date lastUpdated;

    /**
     * @return the contenido
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    /**
     * @return the usuario
     */
    public Usuario getUsuarioFrom() {
        return usuarioFrom;
    }

    public Usuario getUsuarioTo() {
        return usuarioTo;
    } 
    
    /**
     * @param usuario the usuario to set
     */
    public void setUsuario(Usuario usuarioFrom) {
        this.usuarioFrom = usuarioFrom;
    }

    public void setUsuarioTo(Usuario usuarioTo) {
        this.usuarioTo = usuarioTo;
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

	public void setAsunto(String asunto) {
		this.asunto = asunto;
	}

	public String getAsunto() {
		return asunto;
	}
}
