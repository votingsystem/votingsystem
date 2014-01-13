package org.votingsystem.accesscontrol.service

import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.DateUtils

class EventVSManifestSignatureCollectorService {
	
	def grailsApplication
	def messageSource
	def signatureVSService
	def subscriptionVSService
	
	
	public ResponseVS saveManifestSignature(PDFDocumentVS pdfDocument, EventVS eventVS, Locale locale) {
		log.debug "saveManifestSignature - pdfDocument.id: ${pdfDocument.id} - eventVS: ${eventVS.id}";
		try {
			UserVS userVS = pdfDocument.userVS
			PDFDocumentVS documento = PDFDocumentVS.findWhere(eventVS:eventVS, userVS:userVS,
				state:PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
			String messageValidacionDocumento
			if(documento) {
				messageValidacionDocumento = messageSource.getMessage(
						'pdfSignatureManifestRepeated',	[userVS.nif, eventVS.subject, DateUtils.
						getStringFromDate(documento.dateCreated)].toArray(), locale)
				log.debug ("saveManifestSignature - ${messageValidacionDocumento}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageValidacionDocumento)
			} else {
                Date signatureTime = userVS.getTimeStampToken()?.getTimeStampInfo()?.getGenTime()
                if(!eventVS.isActive(signatureTime)) {
                    String msg = messageSource.getMessage("checkedDateRangeErrorMsg", [signatureTime,
                            eventVS.getDateBegin(), eventVS.getDateFinish()].toArray(), locale)
                    log.error(msg)
                    return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.MANIFEST_SIGN,
                            message:msg, eventVS:eventVS)
                }
			
				PDFDocumentVS.withTransaction {
					pdfDocument.eventVS = eventVS;
					pdfDocument.state = PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED
					pdfDocument.save(flush:true)
				}
				messageValidacionDocumento = messageSource.getMessage(
					'pdfSignatureManifestOK',[eventVS.subject, pdfDocument.userVS.nif].toArray(), locale)
				log.debug ("saveManifestSignature - ${messageValidacionDocumento}")
				return new ResponseVS(statusCode:ResponseVS.SC_OK, message:messageValidacionDocumento)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:ex.getMessage())
		}

	}

	
}