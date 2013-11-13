package org.votingsystem.model;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum TypeVS {
    
    REQUEST_WITH_ERRORS(""),
    REQUEST_WITHOUT_FILE(""),
    EVENT_WITH_ERRORS(""),
    OK(""),
    ERROR(""),
	
    VOTING_EVENT(""),
    VOTING_EVENT_ERROR(""),
    VOTO(""),
    VOTE_ERROR(""),


    CONTROL_CENTER_ASSOCIATION(""),
    SIGN_EVENT(""),
    SIGN_EVENT_VALIDADO(""),
    SIGN_EVENT_ERROR(""),

    EVENT_SIGN_WITH_ERRORS(""),
    CLAIM_EVENT_SIGN(""),
    
    CLAIM_EVENT(""), 
    CLAIM_EVENT_ERROR(""),
     
    VOTE_RECEIPT_ERROR(""),
    VOTE_RECEIPT(""),
    VOTACION("");
       
    private String mensaje;
    
    TypeVS(String mensaje) {
        this.mensaje = mensaje;
    }
    public String getMensaje() {
        return this.mensaje;
    }
    
}
