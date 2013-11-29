package org.votingsystem.model;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum TypeVS {

	CONTROL_CENTER_VALIDATED_VOTEVS,
    ACCESS_CONTROL_VALIDATED_VOTEVS,
	   
    REQUEST_WITH_ERRORS,
    REQUEST_WITHOUT_FILE,
    EVENT_WITH_ERRORS,
    OK,
    TEST,
    ERROR,
    USER_ERROR,
    RECEIPT,
    VOTING_EVENT,
    VOTING_EVENT_ERROR,
    VOTEVS,
    VOTE_ERROR,
    CANCEL_VOTE,
    CANCEL_VOTE_ERROR,
    CONTROL_CENTER_ASSOCIATION,
    CONTROL_CENTER_ASSOCIATION_ERROR,
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
    INDEX_REQUEST_ERROR,
    ACCESS_REQUEST_ERROR,
    SMIME_CLAIM_SIGNATURE,
    SEND_SMIME_VOTE,
	REPRESENTATIVE_SELECTION,
    ACCESS_REQUEST, 
    CONTROL_CENTER_STATE_CHANGE_SMIME, 
    BACKUP_REQUEST, 
    MANIFEST_PUBLISHING, 
    MANIFEST_SIGN, 
    CLAIM_PUBLISHING,
    VOTING_PUBLISHING,
    VOTE_CANCELLATION,
    ACCESS_REQUEST_CANCELLATION,
    EVENT_CANCELLATION,
    SAVE_VOTE_RECEIPT,
    NEW_REPRESENTATIVE,
    REPRESENTATIVE_VOTING_HISTORY_REQUEST,
    REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR,
    REPRESENTATIVE_REVOKE_ERROR,
    REPRESENTATIVE_ACCREDITATIONS_REQUEST,
    REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR,
    REPRESENTATIVE_SELECTION_ERROR,
    REPRESENTATIVE_DATA_ERROR,
    REPRESENTATIVE_DATA_OLD,
    REPRESENTATIVE_DATA,
    REPRESENTATIVE_REVOKE,
    TERMINATED;

    
}