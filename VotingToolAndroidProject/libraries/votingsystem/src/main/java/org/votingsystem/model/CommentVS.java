package org.votingsystem.model;

import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CommentVS implements Comparable {

    private Long id;
    private Long messageSMIMEId;
    private String content;
    private UserVS userVS;
    private EventVS eventVS;
    private Date dateCreated;
    private Date lastUpdated;


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

	public void setEventVS(EventVS eventVS) {
		this.eventVS = eventVS;
	}

	public EventVS getEventVS() {
		return eventVS;
	}

    public Long getMessageSMIMEId() {
        return messageSMIMEId;
    }

    public void setMessageSMIMEId(Long messageSMIMEId) {
        this.messageSMIMEId = messageSMIMEId;
    }

    @Override public int compareTo(Object comment) {
        return dateCreated.compareTo(((CommentVS)comment).dateCreated);
    }

}
