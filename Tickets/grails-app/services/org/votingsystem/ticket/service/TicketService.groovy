package org.votingsystem.ticket.service

import org.apache.xerces.parsers.DOMParser
import org.apache.xerces.xni.parser.XMLDocumentFilter
import org.cyberneko.html.HTMLConfiguration
import org.cyberneko.html.filters.ElementRemover
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class TicketService {


	public ResponseVS processRequest(MessageSMIME messageSMIMEReq, Locale locale) {
        //validate amount in request

    }

    public ResponseVS cancel(MessageSMIME messageSMIMEReq, Locale locale) {

    }

}

