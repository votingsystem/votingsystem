package org.votingsystem.accesscontrol.service

import grails.transaction.Transactional
import org.votingsystem.model.*
import org.votingsystem.util.DateUtils
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

class EventVSManifestSignatureCollectorService {
	
	def grailsApplication
	def messageSource
	def signatureVSService
	def subscriptionVSService
	
	@Transactional
	public ResponseVS saveManifestSignature(PDFDocumentVS pdfDocument, EventVS eventVS) {
		log.debug "saveManifestSignature - pdfDocument.id: ${pdfDocument.id} - eventVS: ${eventVS.id}";
        UserVS userVS = pdfDocument.userVS
        PDFDocumentVS pdfDocumentVS = PDFDocumentVS.findWhere(eventVS:eventVS, userVS:userVS,
                state:PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
        String msg
        if(pdfDocumentVS) {
            msg = messageSource.getMessage('pdfSignatureManifestRepeated',	[userVS.nif, eventVS.subject, DateUtils.
                    getDateStr(pdfDocumentVS.dateCreated)].toArray(), locale)
            log.debug ("saveManifestSignature - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
        } else {
            Date signatureTime = userVS.getTimeStampToken()?.getTimeStampInfo()?.getGenTime()
            if(!eventVS.isActive(signatureTime)) {
                msg = messageSource.getMessage("checkedDateRangeErrorMsg", [signatureTime,
                        eventVS.getDateBegin(), eventVS.getDateFinish()].toArray(), locale)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.MANIFEST_SIGN,
                        message:msg, eventVS:eventVS)
            }
            pdfDocumentVS.eventVS = eventVS;
            pdfDocumentVS.state = PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED
            pdfDocumentVS.save()
            msg = messageSource.getMessage('pdfSignatureManifestOK',
                    [eventVS.subject, pdfDocument.userVS.nif].toArray(), locale)
            log.debug ("saveManifestSignature - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg)
        }
	}

	
}