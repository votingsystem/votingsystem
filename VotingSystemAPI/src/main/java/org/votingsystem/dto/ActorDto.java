package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.Actor;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControl;
import org.votingsystem.model.voting.ControlCenter;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActorDto {

    private Long id;
    private String name;
    private String serverURL;
    private String webSocketURL;
    private String timeStampServerURL;
    private String certChainPEM;
    private String timeStampCertPEM;
    private Date dateCreated;
    private Date date;
    private ActorDto controlCenter;
    private Actor.Type serverType;
    private Actor.State state;

    public ActorDto() {}

    public ActorDto(String serverURL, String name) {
        this.serverURL = serverURL;
        this.name = name;
    }

    public ActorDto(Actor actor) {
        this.setId(actor.getId());
        this.setName(actor.getName());
        this.setServerURL(actor.getServerURL());
        this.setDateCreated(actor.getDateCreated());
        this.setState(actor.getState());
        this.setCertChainPEM(actor.getCertChainPEM());
        this.setServerType(actor.getType());
    }

    @JsonIgnore
    public Actor getActor() throws Exception {
        Actor actor;
        switch (serverType) {
            case CONTROL_CENTER:
                actor = new ControlCenter();
                break;
            case ACCESS_CONTROL:
                actor =  new AccessControl();
                if(controlCenter != null) actor.setControlCenter((ControlCenter) controlCenter.getActor());
                break;
            case CURRENCY:
                actor = new CurrencyServer();
                break;
            default:
                actor = new Actor();
                actor.setType(serverType);
                break;
        }
        actor.setId(id);
        actor.setServerURL(serverURL);
        actor.setWebSocketURL(webSocketURL);
        actor.setName(name);
        actor.setCertChainPEM(certChainPEM);
        actor.setTimeStampServerURL(timeStampServerURL);
        actor.setTimeStampCertPEM(timeStampCertPEM);
        return actor;
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

    public Actor.State getState() {
        return state;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public String getCertChainPEM() {
        return certChainPEM;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getTimeStampServerURL() {
        return timeStampServerURL;
    }

    public void setTimeStampServerURL(String timeStampServerURL) {
        this.timeStampServerURL = timeStampServerURL;
    }

    public void setCertChainPEM(String certChainPEM) {
        this.certChainPEM = certChainPEM;
    }

    public String getTimeStampCertPEM() {
        return timeStampCertPEM;
    }

    public void setTimeStampCertPEM(String timeStampCertPEM) {
        this.timeStampCertPEM = timeStampCertPEM;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public ActorDto getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ActorDto controlCenter) {
        this.controlCenter = controlCenter;
    }

    public Actor.Type getServerType() {
        return serverType;
    }

    public void setServerType(Actor.Type serverType) {
        this.serverType = serverType;
    }

    public void setState(Actor.State state) {
        this.state = state;
    }

    public String getWebSocketURL() {
        return webSocketURL;
    }

    public void setWebSocketURL(String webSocketURL) {
        this.webSocketURL = webSocketURL;
    }

}
