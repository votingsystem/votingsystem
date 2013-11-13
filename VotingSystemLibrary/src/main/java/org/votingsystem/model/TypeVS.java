package org.votingsystem.model;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum TypeVS {
    
	CONTROL_CENTER_VALIDATED_VOTE(""),
	   
    REQUEST_WITH_ERRORS(""),
    REQUEST_WITHOUT_FILE(""),
    EVENT_WITH_ERRORS(""),
    OK(""),
    ERROR(""),
    USER_ERROR(""),
    RECEIPT(""),
    VOTING_EVENT(""),
    VOTING_EVENT_ERROR(""),
    VOTO(""),
    VOTE_ERROR(""),
    CANCEL_VOTE(""),
    CANCEL_VOTE_ERROR(""),
    CONTROL_CENTER_ASSOCIATION(""),
    CONTROL_CENTER_ASSOCIATION_ERROR(""),
    BACKUP(""),
    SIGN_EVENT(""),
    EVENT_SIGN_WITH_ERRORS(""),
    CLAIM_EVENT_SIGN(""),
    CLAIM_EVENT(""), 
    CLAIM_EVENT_ERROR(""),
    CLAIM_EVENT_SIGNATURE_ERROR(""),
    VOTE_RECEIPT_ERROR(""),
    VOTE_RECEIPT(""),
    ACCESS_REQUEST_ERROR(""),
    SMIME_CLAIM_SIGNATURE(""),
    SEND_SMIME_VOTE(""),
	REPRESENTATIVE_SELECTION(""),
    ACCESS_REQUEST(""), 
    CONTROL_CENTER_STATE_CHANGE_SMIME(""), 
    BACKUP_REQUEST(""), 
    MANIFEST_PUBLISHING(""), 
    MANIFEST_SIGN(""), 
    CLAIM_PUBLISHING(""),
    VOTING_PUBLISHING(""), 
    APPLET_MESSAGE(""), 
    VOTE_CANCELLATION(""),
    ACCESS_REQUEST_CANCELLATION(""),
    EVENT_CANCELLATION(""),
    SAVE_VOTE_RECEIPT(""),
    APPLET_PAUSED_MESSAGE(""),
    NEW_REPRESENTATIVE(""),
    REPRESENTATIVE_VOTING_HISTORY_REQUEST(""),
    REPRESENTATIVE_ACCREDITATIONS_REQUEST(""),
    MENSAJE_HERRAMIENTA_VALIDACION(""),
    REPRESENTATIVE_REVOKE("");
       
    private String message;
    
    TypeVS(String message) {
        this.message = message;
    }
    public String getMessage() {
        return this.message;
    }
    
}
