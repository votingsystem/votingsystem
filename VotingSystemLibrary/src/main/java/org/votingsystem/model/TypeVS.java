package org.votingsystem.model;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public enum TypeVS {

    ALERT,
	CONTROL_CENTER_VALIDATED_VOTE,
    ACCESS_CONTROL_VALIDATED_VOTE,
	   
    REQUEST_WITH_ERRORS,
    REQUEST_WITHOUT_FILE,
    EVENT_WITH_ERRORS,
    OK,
    EXCEPTION,
    FORMAT_DATE,
    TEST,
    SIGNATURE_ERROR,
    ERROR,
    CANCELLED,
    USER_ERROR,
    RECEIPT,
    RECEIPT_VIEW,
    VOTING_EVENT,
    VOTEVS,
    VOTE_ERROR,
    CANCEL_VOTE,
    CANCEL_VOTE_ERROR,
    CONTROL_CENTER_ASSOCIATION,
    CONNECT,
    DISCONNECT,
    BACKUP,
    MANIFEST_EVENT,
    MANIFEST_EVENT_ERROR,
    CLAIM_EVENT_SIGN,
    CLAIM_EVENT, 
    CLAIM_EVENT_ERROR,
    CLAIM_EVENT_SIGNATURE_ERROR,
    VOTE_RECEIPT_ERROR,
    RECEIPT_ERROR,
    VOTE_RECEIPT,
    INDEX_REQUEST,
    SELECT_IMAGE,
    SIGNAL_VS,
    INDEX_REQUEST_ERROR,
    ACCESS_REQUEST_ERROR,
    SMIME_CLAIM_SIGNATURE,
    SEND_SMIME_VOTE,
	REPRESENTATIVE_SELECTION,

    ANONYMOUS_REPRESENTATIVE_REQUEST,
    ANONYMOUS_REPRESENTATIVE_SELECTION,
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED,

    BACKUP_REQUEST, 
    MANIFEST_PUBLISHING, 
    MANIFEST_SIGN, 
    CLAIM_PUBLISHING,
    VOTING_PUBLISHING,
    ACCESS_REQUEST,
    ACCESS_REQUEST_CANCELLATION,
    EVENT_CANCELLATION,
    KEYSTORE_SELECT,
    SAVE_SMIME,
    SAVE_SMIME_ANONYMOUS_DELEGATION,
    OPEN_SMIME,
    OPEN_SMIME_FROM_URL,
    OPEN_VICKET,
    NEW_REPRESENTATIVE,
    REPRESENTATIVE_VOTING_HISTORY_REQUEST,
    REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR,
    REPRESENTATIVE_REVOKE_ERROR,
    REPRESENTATIVE_ACCREDITATIONS_REQUEST,
    REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR,
    REPRESENTATIVE_DATA_ERROR,
    REPRESENTATIVE_DATA_CANCELLED,
    REPRESENTATIVE_DATA,
    REPRESENTATIVE_REVOKE,
    REPRESENTATIVE_STATE,
    TERMINATED,

    MESSAGEVS,
    MESSAGEVS_GET,
    MESSAGEVS_EDIT,
    MESSAGEVS_SIGN,
    MESSAGEVS_SIGN_RESPONSE,
    MESSAGEVS_TO_DEVICE,
    MESSAGEVS_FROM_DEVICE,
    MESSAGEVS_DECRYPT,

    LISTEN_TRANSACTIONS,
    INIT_VALIDATED_SESSION,

    CERT_USER_NEW,
    CERT_CA_NEW,
    CERT_EDIT,

    FROM_BANKVS,
    FROM_GROUP_TO_MEMBER,
    FROM_GROUP_TO_MEMBER_GROUP,
    FROM_GROUP_TO_ALL_MEMBERS,
    FROM_USERVS,
    FROM_USERVS_TO_USERVS,

    VICKET,
    VICKET_USER_INFO,
    VICKET_CANCEL,
    VICKET_DELETE,
    VICKET_REQUEST,
    VICKET_SEND,
    VICKET_GROUP_NEW,
    VICKET_GROUP_EDIT,
    VICKET_GROUP_CANCEL,
    VICKET_GROUP_SUBSCRIBE,
    VICKET_GROUP_UPDATE_SUBSCRIPTION,
    VICKET_GROUP_USER_ACTIVATE,
    VICKET_GROUP_USER_DEACTIVATE,
    VICKET_INIT_PERIOD,
    BANKVS,
    BANKVS_NEW,

    WEB_SOCKET_INIT,
    WEB_SOCKET_MESSAGE,
    WEB_SOCKET_BAN_SESSION,

    WALLET_OPEN;
}
