package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.ActorVS;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActorVSDto {

    private Long id;
    private String name;
    private String serverURL;
    private String certChainPEM;
    private String state;
    private Date dateCreated;

    public ActorVSDto() {}

    public ActorVSDto(ActorVS actorVS) {
        this.id = actorVS.getId();
        this.name = actorVS.getName();
        this.serverURL = actorVS.getServerURL();
        this.dateCreated = actorVS.getDateCreated();
        this.state = actorVS.getState().toString();
        this.certChainPEM = actorVS.getCertChainPEM();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getServerURL() {
        return serverURL;
    }

    public String getState() {
        return state;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public String getCertChainPEM() {
        return certChainPEM;
    }
}
