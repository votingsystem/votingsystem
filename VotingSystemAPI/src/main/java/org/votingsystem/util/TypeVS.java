package org.votingsystem.util;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public enum TypeVS {

    ALERT,
	CONTROL_CENTER_VALIDATED_VOTE,
    ACCESS_CONTROL_VALIDATED_VOTE,

    OK,
    EXCEPTION,
    TEST,
    ERROR,
    CANCELED,
    BATCH_RECEIPT,
    VOTING_EVENT,
    CANCEL_VOTE,
    CONNECT,
    DISCONNECT,
    BACKUP,
    SELECT_IMAGE,
    SEND_VOTE,
	REPRESENTATIVE_SELECTION,

    ANONYMOUS_SELECTION_CERT_REQUEST,
    ANONYMOUS_REPRESENTATIVE_SELECTION,
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION,

    BACKUP_REQUEST,
    VOTING_PUBLISHING,
    ACCESS_REQUEST,
    ACCESS_REQUEST_CANCELLATION,
    EVENT_CANCELLATION,
    KEYSTORE_SELECT,
    SAVE_SMIME,
    SEND_ANONYMOUS_DELEGATION,
    OPEN_SMIME,
    OPEN_SMIME_FROM_URL,
    CURRENCY_OPEN,
    OPERATION_CANCELED,
    OPERATION_FINISHED,
    NEW_REPRESENTATIVE,
    EDIT_REPRESENTATIVE,
    QR_MESSAGE_INFO,
    REPRESENTATIVE_VOTING_HISTORY_REQUEST,
    REPRESENTATIVE_ACCREDITATIONS_REQUEST,
    REPRESENTATIVE_DATA,
    REPRESENTATIVE_REVOKE,
    REPRESENTATIVE_STATE,
    TERMINATED,

    MESSAGEVS,
    MESSAGEVS_SIGN,
    MESSAGEVS_SIGN_RESPONSE,
    MESSAGEVS_TO_DEVICE,
    MESSAGEVS_FROM_DEVICE,
    MESSAGEVS_FROM_VS,

    LISTEN_TRANSACTIONS,
    INIT_SIGNED_SESSION,

    CERT_USER_NEW,
    CERT_CA_NEW,
    CERT_EDIT,

    FILE_FROM_URL,

    FROM_BANKVS,
    FROM_GROUP_TO_MEMBER_GROUP,
    FROM_GROUP_TO_ALL_MEMBERS,
    FROM_USERVS,

    CURRENCY,
    CURRENCY_USER_INFO,
    CURRENCY_DELETE,
    CURRENCY_REQUEST,
    CURRENCY_SEND,
    CURRENCY_CHANGE,
    CURRENCY_WALLET_CHANGE,
    CURRENCY_GROUP_NEW,
    CURRENCY_GROUP_EDIT,
    CURRENCY_GROUP_CANCEL,
    CURRENCY_GROUP_SUBSCRIBE,
    CURRENCY_GROUP_UPDATE_SUBSCRIPTION,
    CURRENCY_GROUP_USER_ACTIVATE,
    CURRENCY_GROUP_USER_DEACTIVATE,
    CURRENCY_PERIOD_INIT,
    BANKVS,
    BANKVS_NEW,
    TRANSACTIONVS_INFO,
    TRANSACTIONVS_RESPONSE,

    DELIVERY_WITHOUT_PAYMENT,
    DELIVERY_WITH_PAYMENT,
    REQUEST_FORM,

    WEB_SOCKET_INIT,
    WEB_SOCKET_BAN_SESSION,

    WALLET_OPEN,
    WALLET_SAVE,
    CURRENCY_IMPORT;

}
