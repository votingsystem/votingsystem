package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public enum OperationType {

    @JsonProperty("GET_METADATA")
    GET_METADATA("/api/metadata"),
    //service that generates QR codes
    @JsonProperty("GET_QR")
    GET_QR("/api/qr"),
    //service that provides the information related to QR codes
    @JsonProperty("GET_QR_INFO")
    GET_QR_INFO("/api/qr/info"),

    @JsonProperty("ADMIN_OPERATION_PROCESS")
    ADMIN_OPERATION_PROCESS("/api/operation/process"),
    @JsonProperty("VALIDATE_SIGNED_DOCUMENT")
    VALIDATE_SIGNED_DOCUMENT("/api/signature/validate"),

    //TimeStamp server
    @JsonProperty("TIMESTAMP_REQUEST")
    TIMESTAMP_REQUEST("/api/timestamp"),
    @JsonProperty("TIMESTAMP_REQUEST_DISCRETE")
    TIMESTAMP_REQUEST_DISCRETE("/api/timestamp/discrete"),


    //voting service provider
    @JsonProperty("FETCH_ELECTIONS")
    FETCH_ELECTIONS("/api/election"),
    @JsonProperty("PUBLISH_ELECTION")
    PUBLISH_ELECTION("/api/election/save"),
    @JsonProperty("FETCH_ELECTION")
    FETCH_ELECTION("/api/election/uuid"),
    @JsonProperty("SEND_VOTE")
    SEND_VOTE("/api/vote"),


    //id provider
    //Service that generates the QR code with the operation details
    @JsonProperty("INIT_AUTHENTICATION")
    INIT_AUTHENTICATION("/api/auth/initAuthentication"),
    @JsonProperty("VALIDATE_IDENTITY")
    VALIDATE_IDENTITY("/api/auth/validate"),
    //Service that generates the QR code with the election details
    @JsonProperty("ELECTION_INIT_AUTHENTICATION")
    ELECTION_INIT_AUTHENTICATION("/api/election/initAuthentication"),
    @JsonProperty("ANON_VOTE_CERT_REQUEST")
    ANON_VOTE_CERT_REQUEST("/election/validateIdentity"),

    @JsonProperty("CANCEL_VOTE")
    CANCEL_VOTE(null),
    @JsonProperty("CONNECT")
    CONNECT(null),
    @JsonProperty("DISCONNECT")
    DISCONNECT(null),
    @JsonProperty("BACKUP")
    BACKUP(null),

    SIGN_PDF("/api/pdf/processSigned"),

    @JsonProperty("BACKUP_REQUEST")
    BACKUP_REQUEST(null),

    @JsonProperty("ELECTION_CANCELLATION")
    ELECTION_CANCELLATION(null),

    @JsonProperty("MESSAGE_INFO")
    MESSAGE_INFO(null),
    @JsonProperty("MESSAGE_INFO_RESPONSE")
    MESSAGE_INFO_RESPONSE(null),
    @JsonProperty("USER_INFO")
    USER_INFO(null),

    @JsonProperty("MESSAGEVS")
    MESSAGEVS(null),
    @JsonProperty("MSG_TO_DEVICE")
    MSG_TO_DEVICE(null),
    @JsonProperty("MESSAGEVS_FROM_VS")
    MESSAGEVS_FROM_VS(null),

    @JsonProperty("LISTEN_TRANSACTIONS")
    LISTEN_TRANSACTIONS(null),
    @JsonProperty("INIT_SESSION")
    INIT_SESSION(null),
    @JsonProperty("CLOSE_SESSION")
    CLOSE_SESSION(null),
    @JsonProperty("INIT_SIGNED_SESSION")
    INIT_SIGNED_SESSION(null),
    @JsonProperty("INIT_REMOTE_SIGNED_SESSION")
    INIT_REMOTE_SIGNED_SESSION(null),

    @JsonProperty("CERT_USER_CHECK")
    CERT_USER_CHECK(null),
    @JsonProperty("CERT_USER_NEW")
    CERT_USER_NEW(null),
    @JsonProperty("CERT_ENTITY_NEW")
    CERT_ENTITY_NEW(null),
    @JsonProperty("CERT_EDIT")
    CERT_EDIT(null);


    private String url;

    OperationType(String url) {
        this.url = url;
    }

    public String getUrl(String entityId) {
        return entityId + url;
    }

}
