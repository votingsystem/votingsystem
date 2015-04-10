package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.CurrencyServer;
import org.votingsystem.util.EnvironmentVS;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActorVSDto {

    private Long id;
    private String name;
    private String serverURL;
    private String webSocketURL;
    private String voteVSInfoURL;
    private String timeStampServerURL;
    private String certChainPEM;
    private String timeStampCertPEM;
    private Date dateCreated;
    private Date date;
    private ActorVSDto controlCenter;
    private ActorVS.Type serverType;
    private ActorVS.State state;
    private EnvironmentVS environmentMode;

    public ActorVSDto() {}

    public ActorVSDto(String serverURL, String name) {
        this.serverURL = serverURL;
        this.name = name;
    }

    public ActorVSDto(ActorVS actorVS) {
        this.setId(actorVS.getId());
        this.setName(actorVS.getName());
        this.setServerURL(actorVS.getServerURL());
        this.setDateCreated(actorVS.getDateCreated());
        this.setState(actorVS.getState());
        this.setCertChainPEM(actorVS.getCertChainPEM());
        this.setServerType(actorVS.getType());
    }

    @JsonIgnore
    public ActorVS getActorVS() throws Exception {
        ActorVS actorVS;
        switch (serverType) {
            case CONTROL_CENTER:
                actorVS = new ControlCenterVS();
                break;
            case ACCESS_CONTROL:
                actorVS =  new AccessControlVS();
                actorVS.setControlCenter((ControlCenterVS) controlCenter.getActorVS());
                break;
            case CURRENCY:
                actorVS = new CurrencyServer();
                break;
            default:
                actorVS = new ActorVS();
                actorVS.setType(serverType);
                break;
        }
        actorVS.setId(id);
        actorVS.setEnvironmentVS(environmentMode);
        actorVS.setServerURL(serverURL);
        actorVS.setWebSocketURL(webSocketURL);
        actorVS.setName(name);
        actorVS.setVoteVSInfoURL(voteVSInfoURL);
        actorVS.setCertChainPEM(certChainPEM);
        actorVS.setTimeStampServerURL(timeStampServerURL);
        actorVS.setTimeStampCertPEM(timeStampCertPEM);
        return actorVS;
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

    public ActorVS.State getState() {
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

    public ActorVSDto getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ActorVSDto controlCenter) {
        this.controlCenter = controlCenter;
    }

    public ActorVS.Type getServerType() {
        return serverType;
    }

    public void setServerType(ActorVS.Type serverType) {
        this.serverType = serverType;
    }

    public void setState(ActorVS.State state) {
        this.state = state;
    }

    public EnvironmentVS getEnvironmentMode() {
        return environmentMode;
    }

    public void setEnvironmentMode(EnvironmentVS environmentMode) {
        this.environmentMode = environmentMode;
    }

    public String getWebSocketURL() {
        return webSocketURL;
    }

    public void setWebSocketURL(String webSocketURL) {
        this.webSocketURL = webSocketURL;
    }

    public String getVoteVSInfoURL() {
        return voteVSInfoURL;
    }

    public void setVoteVSInfoURL(String voteVSInfoURL) {
        this.voteVSInfoURL = voteVSInfoURL;
    }
}
