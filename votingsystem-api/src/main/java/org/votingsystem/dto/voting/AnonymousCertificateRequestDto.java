package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.model.voting.AnonVoteCertRequest;
import org.votingsystem.model.voting.Election;
import org.votingsystem.util.OperationType;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "IdentityRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnonymousCertificateRequestDto {

    private OperationType operation = OperationType.ANON_VOTE_CERT_REQUEST;
    private String eventURL;
    private Long eventId;
    private String hashAccessRequestBase64;
    private String UUID;

    @JsonIgnore private Election election;
    @JsonIgnore private AnonVoteCertRequest accessRequest;

    public AnonymousCertificateRequestDto() {}

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public AnonVoteCertRequest getAccessRequest() {
        return accessRequest;
    }

    public void setAccessRequest(AnonVoteCertRequest accessRequest) {
        this.accessRequest = accessRequest;
    }

}
