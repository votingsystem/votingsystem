package org.votingsystem.model;

import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CommentVS implements Comparable {

    private Long id;
    private Long mensajeMimeId;
    private String contenido;
    private UserVS userVSBase;
    private String nombreUsuario;
    private EventVS eventVS;
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
     * @return the userVSBase
     */
    public UserVS getUserVSBase() {
        return userVSBase;
    }

    /**
     * @param userVSBase the userVSBase to set
     */
    public void setUserVSBase(UserVS userVSBase) {
        this.userVSBase = userVSBase;
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

	public void setEventVSBase(EventVS eventVS) {
		this.eventVS = eventVS;
	}

	public EventVS getEventVSBase() {
		return eventVS;
	}

	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	public String getNombreUsuario() {
		return nombreUsuario;
	}

	public void setMensajeMimeId(Long mensajeMimeId) {
		this.mensajeMimeId = mensajeMimeId;
	}

	public Long getMensajeMimeId() {
		return mensajeMimeId;
	}

	@Override
	public int compareTo(Object comentario) {
		return dateCreated.compareTo(((CommentVS)comentario).dateCreated);
	}

}
