package org.votingsystem.model;

import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CommentVS implements Comparable {

    private Long id;
    private Long mensajeMimeId;
    private String content;
    private UserVS userVS;
    private String userVSName;
    private EventVS eventVS;
    private Date dateCreated;
    private Date lastUpdated;

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the userVS
     */
    public UserVS getUserVS() {
        return userVS;
    }

    /**
     * @param userVS the userVS to set
     */
    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
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

	public void setEventVS(EventVS eventVS) {
		this.eventVS = eventVS;
	}

	public EventVS getEventVS() {
		return eventVS;
	}

	public void setUserName(String userVSName) {
		this.userVSName = userVSName;
	}

	public String getUserName() {
		return userVSName;
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
