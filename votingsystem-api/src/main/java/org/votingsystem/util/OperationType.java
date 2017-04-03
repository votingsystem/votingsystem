package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public enum OperationType implements SystemOperation {

    @JsonProperty("ADMIN_OPERATION_PROCESS")
    ADMIN_OPERATION_PROCESS("/api/operation/process"),
    @JsonProperty("ANON_VOTE_CERT_REQUEST")
    ANON_VOTE_CERT_REQUEST("/election/validateIdentity"),
    @JsonProperty("BACKUP")
    BACKUP(null),
    @JsonProperty("BACKUP_REQUEST")
    BACKUP_REQUEST(null),
    @JsonProperty("CANCEL_VOTE")
    CANCEL_VOTE(null),
    @JsonProperty("CERT_USER_CHECK")
    CERT_USER_CHECK(null),
    @JsonProperty("CERT_USER_NEW")
    CERT_USER_NEW(null),
    @JsonProperty("CERT_ENTITY_NEW")
    CERT_ENTITY_NEW(null),
    @JsonProperty("CERT_EDIT")
    CERT_EDIT(null),
    @JsonProperty("CLOSE_SESSION")
    CLOSE_SESSION(null),
    @JsonProperty("CONNECT")
    CONNECT(null),
    @JsonProperty("DISCONNECT")
    DISCONNECT(null),
    @JsonProperty("ELECTION_CANCELLATION")
    ELECTION_CANCELLATION(null),
    //Service that generates the QR code with the election details
    @JsonProperty("ELECTION_INIT_AUTHENTICATION")
    ELECTION_INIT_AUTHENTICATION("/api/election/initAuthentication"),
    @JsonProperty("GENERATE_BROWSER_CERTIFICATE")
    GENERATE_BROWSER_CERTIFICATE(null),
    @JsonProperty("GET_METADATA")
    GET_METADATA("/api/metadata"),
    @JsonProperty("FETCH_ELECTION")
    FETCH_ELECTION("/api/election/uuid"),
    //voting service provider
    @JsonProperty("FETCH_ELECTIONS")
    FETCH_ELECTIONS("/api/election"),
    //service that generates QR codes
    @JsonProperty("GET_QR")
    GET_QR("/api/qr"),
    //service that provides the information related to QR codes
    @JsonProperty("GET_QR_INFO")
    GET_QR_INFO("/api/qr/info"),
    //id provider
    //Service that generates the QR code with the operation details
    @JsonProperty("INIT_AUTHENTICATION")
    INIT_AUTHENTICATION("/api/auth/initAuthentication"),
    @JsonProperty("INIT_SESSION")
    INIT_SESSION(null),

    @JsonProperty("MESSAGE_INFO")
    MESSAGE_INFO(null),
    @JsonProperty("MESSAGE_INFO_RESPONSE")
    MESSAGE_INFO_RESPONSE(null),

    @JsonProperty("PUBLISH_ELECTION")
    PUBLISH_ELECTION("/api/election/save"),
    @JsonProperty("SEND_VOTE")
    SEND_VOTE("/api/vote"),
    SIGN_PDF("/api/pdf/processSigned"),
    //TimeStamp server
    @JsonProperty("TIMESTAMP_REQUEST")
    TIMESTAMP_REQUEST("/api/timestamp"),
    @JsonProperty("TIMESTAMP_REQUEST_DISCRETE")
    TIMESTAMP_REQUEST_DISCRETE("/api/timestamp/discrete"),
    @JsonProperty("REGISTER")
    REGISTER("/api/user/register"),
    @JsonProperty("USER_INFO")
    USER_INFO(null),
    @JsonProperty("LISTEN_TRANSACTIONS")
    LISTEN_TRANSACTIONS(null),
    @JsonProperty("MESSAGE")
    MESSAGEVS(null),
    @JsonProperty("MSG_TO_DEVICE")
    MSG_TO_DEVICE(null),

    @JsonProperty("VALIDATE_SIGNED_DOCUMENT")
    VALIDATE_SIGNED_DOCUMENT("/api/signature/validate"),
    @JsonProperty("VALIDATE_IDENTITY")
    VALIDATE_IDENTITY("/api/auth/validate"),
    @JsonProperty("VOTE_REPOSITORY")
    VOTE_REPOSITORY("/api/vote/repository");

    private String url;

    OperationType(String url) {
        this.url = url;
    }

    public String getUrl(String entityId) {
        return entityId + url;
    }

}
